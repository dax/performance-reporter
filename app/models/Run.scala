package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Run(id: Pk[Long], label: String, system: Long, values: Seq[Int])

object Run {
  val run = {
    get[Pk[Long]]("run.id") ~
    get[String]("run.label") ~
		get[Long]("run.system_id") ~
		get[String]("run.values") map {
      case id~label~system~values =>
			  Run(id, label, system, values.split(',').map(_.toInt).toSeq)
    }
  }

  val nullableRun = {
    get[Option[Pk[Long]]]("run.id") ~
    get[Option[String]]("run.label") ~
		get[Option[Long]]("run.system_id") ~
		get[Option[String]]("run.values") map {
			case None~None~None~None => None
      case Some(id)~Some(label)~Some(system)~Some(values) =>
			  Some(Run(id, label, system, values.split(',').map(_.toInt).toSeq))
    }
  }

  def findById(id: Long): Option[Run] = DB.withConnection { implicit c =>
    SQL("""
			SELECT run.id, run.label, run.system_id, run.values
			FROM run
			WHERE run.id = {id}
			""").on(
			'id -> id
		).as(run.singleOpt)
  }

  def findBySystem(systemId: Long): List[Run] = DB.withConnection { implicit c =>
    SQL("""
			SELECT run.id, run.label, run.system_id, run.values
			FROM run
			WHERE run.system_id = {system}
			""").on(
			'system -> systemId
		).as(run *)
  }

  def create(run: Run): Option[Run] = {
    DB.withConnection { implicit c =>
      SQL("""
				INSERT INTO run (label, system_id, values)
				VALUES ({label}, {system}, {values})
				""").on(
        'label -> run.label,
				'system -> run.system,
				'values -> run.values.mkString(",")
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
