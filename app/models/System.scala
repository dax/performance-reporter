package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current


case class System(id: Pk[Long], label: String, runs: Seq[Run]) {
	def addRun(run: Run): System = {
		this.runs :+ run
		this
	}
}

object System {
  val system = {
    get[Pk[Long]]("system.id") ~
    get[String]("system.label") map {
      case id~label => System(id, label, List())
    }
  }

  val withRuns = (System.system ~ Run.nullableRun *).map(_.groupBy(_._1).toSeq.headOption.map {
			case (sys, r) => sys.copy(runs = r.map(_._2).flatten)
		})

  def findById(id: Long): Option[System] = DB.withConnection { implicit c =>
    SQL("""
			SELECT system.id, system.label, run.id, run.label, run.system_id, run.values
			FROM system
			LEFT OUTER JOIN run ON system.id = run.system_id
			WHERE system.id = {id}
			""")
		.on(
			'id -> id
		).as(withRuns)
  }

  def all(): List[System] = DB.withConnection { implicit c =>
    SQL("SELECT system.id, system.label FROM system").as(system *)
  }

  def create(system: System): Option[System] = {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO system (label) VALUES ({label})").on(
        'label -> system.label
      ).executeInsert().map { id =>
        system.copy(id = Id(id))
     }
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM system WHERE id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }
}
