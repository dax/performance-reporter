package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Run(id: Pk[Long], label: String, system: Long)

object Run {
  val run = {
    get[Pk[Long]]("run.id") ~
    get[String]("run.label") ~
		get[Long]("run.system_id") map {
      case id~label~system => Run(id, label, system)
    }
  }

  val nullableRun = {
    get[Option[Pk[Long]]]("run.id") ~
    get[Option[String]]("run.label") ~
		get[Option[Long]]("run.system_id") map {
			case None~None~None => None
      case Some(id)~Some(label)~Some(system) => Some(Run(id, label, system))
    }
  }

  def findById(id: Long): Option[Run] = DB.withConnection { implicit c =>
    SQL("SELECT run.id, run.label, run.system_id FROM run WHERE run.id = {id}").on(
			'id -> id
		).as(run.singleOpt)
  }

  def findBySystem(systemId: Long): List[Run] = DB.withConnection { implicit c =>
    SQL("SELECT run.id, run.label, run.system_id FROM run WHERE run.system_id = {system}").on(
			'system -> systemId
		).as(run *)
  }

  def create(run: Run): Option[Run] = {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO run (label, system_id) VALUES ({label}, {system})").on(
        'label -> run.label,
				'system -> run.system
      ).executeInsert().map { id =>
        run.copy(id = Id(id))
     }
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM run WHERE id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }
}
