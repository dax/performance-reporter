import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._
import org.specs2.execute._
import java.sql.SQLException

import scala.slick.driver.H2Driver.simple._
import play.api.db.slick.DB
import scala.slick.lifted.DDL

import models._

abstract class WithDbData extends WithApplication {
  def create(ddl: DDL) = {
    DB.withSession { implicit session =>
      try { ddl.create } catch { case _: SQLException => }
      }
  }

  def drop(ddl: DDL) = {
    DB.withSession { implicit session =>
      try { ddl.drop } catch { case _: SQLException => }
      }
    }

  override def around[T](t: => T)(implicit evidence: AsResult[T]): Result = super.around {
    DB.withSession { implicit session =>
      create(Systems.ddl)
      create(Runs.ddl)
      create(Metrics.ddl)
      create(MetricValues.ddl)
      val result = AsResult(t)
      drop(MetricValues.ddl)
      drop(Metrics.ddl)
      drop(Runs.ddl)
      drop(Systems.ddl)
      result
    }
  }
}
