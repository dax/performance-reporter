package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Run(id: Pk[Long], label: String)

object Run {
  val run = {
    get[Pk[Long]]("id") ~
    get[String]("label") map {
      case id~label => Run(id, label)
    }
  }

  def all(): List[Run] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM run").as(run *)
  }

  def create(run: Run): Option[Run] = {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO run (label) VALUES ({label})").on(
        'label -> run.label
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
