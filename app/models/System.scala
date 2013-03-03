package models

import play.api.db._
import play.api.Play.current

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

case class System(id: Option[Long], label: String) {
  def runs: Seq[Run] = DB.withSession { implicit session =>
    (
      for { run <- Runs if (run.systemId === id) }
      yield (run)
    ).list
  }

  def addRun(run: Run): System = {
    this.runs :+ run
    this
  }
}

object Systems extends Table[System]("system") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def label = column[String]("label")
  def * = id.? ~ label <> ({ t => System(t._1, t._2) },
    { sys: System => Some((None, sys.label)) })

  def findByIdOrInsert(id: Long): Option[System] = DB.withSession { implicit session =>
    findById(id).orElse {
      create("No Label")
    }
  }

  def findById(id: Long): Option[System] = DB.withSession { implicit session =>
    (
      for { system <- Systems if (system.id === id) }
      yield (system)
    ).firstOption
  }

  def all(): List[System] = DB.withSession { implicit session =>
    (
      for { system <- Systems }
      yield (system)
    ).list
  }

  def deleteById(systemId: Long) = DB.withSession { implicit session =>
    (
      for { system <- Systems if (system.id === systemId) }
      yield (system)
    ).delete
  }

  def insert(label: String) = DB.withSession { implicit session =>
    val system = System(None, label)
    val systemId = Systems.* returning Systems.id insert(system)
    Some(system.copy(id=Some(systemId)))
  }

  def create = insert(_)
}
