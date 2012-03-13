package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Run(id: Long, label: String)

object Run {
	val run = {
		get[Long]("id") ~
		get[String]("label") map {
			case id~label => Run(id, label)
		}
	}

	def all(): List[Run] = DB.withConnection { implicit c =>
		SQL("select * from run").as(run *)
	}

	def create(label: String) {
		DB.withConnection { implicit c =>
			SQL("insert into run (label) values ({label})").on(
				'label -> label
			).executeUpdate()
		}
	}

	def delete(id: Long) {
		DB.withConnection { implicit c =>
			SQL("delete from run where id = {id}").on(
				'id -> id
			).executeUpdate()
		}
	}
}
