package models

import java.util.{Date}

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class MetricValue(id: Pk[Long], datetime: Date, value: Long, metricId: Long, runId: Long)

object MetricValue {
  val metricValue = {
    get[Pk[Long]]("metric_value.id") ~
    get[Date]("metric_value.datetime") ~
    get[Long]("metric_value.value") ~
    get[Long]("metric_value.metric_id") ~
    get[Long]("metric_value.run_id") map {
      case id~datetime~value~metricId~runId =>
        MetricValue(id, new Date(datetime.getTime), value, metricId, runId)
    }
  }

  val nullableMetricValue = {
    get[Option[Pk[Long]]]("metric_value.id") ~
    get[Option[Date]]("metric_value.datetime") ~
    get[Option[Long]]("metric_value.value") ~
    get[Option[Long]]("metric_value.metric_id") ~
    get[Option[Long]]("metric_value.run_id") map {
      case None~None~None~None~None => None
      case Some(id)~Some(datetime)~Some(value)~Some(metricId)~Some(runId) =>
        Some(MetricValue(id, new Date(datetime.getTime), value, metricId, runId))
    }
  }

  def create(metricValue: MetricValue): Option[MetricValue] = {
    DB.withConnection { implicit c =>
      SQL("""
        INSERT INTO metric_value (datetime, value, metric_id, run_id)
        VALUES ({datetime}, {value}, {metric}, {run})
        """).on(
        'datetime -> metricValue.datetime,
        'value -> metricValue.value,
        'metric -> metricValue.metricId,
        'run -> metricValue.runId
      ).executeInsert().map { id =>
        metricValue.copy(id = Id(id))
     }
    }
  }

  def deleteByRun(metricId: Long, runId: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM metric_value WHERE metric_id = {metric} AND run = {run}").on(
        'metric -> metricId,
        'run -> runId
      ).executeUpdate()
    }
  }
}
