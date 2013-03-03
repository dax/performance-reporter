package models

import scalaz._
import Scalaz._
import java.util.Date
import java.sql.Timestamp

import play.api.db._
import play.api.Play.current

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

case class MetricValue(id: Option[Long] = None, datetime: Timestamp, value: Long, metricId: Long, runId: Long)

object MetricValues extends Table[MetricValue]("metric_value") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def datetime = column[Timestamp]("datetime")
  def value = column[Long]("value")
  def metricId = column[Long]("metric_id")
  def metric = foreignKey("metric_id", metricId, Metrics)(_.id)
  def runId = column[Long]("run_id")
  def run = foreignKey("run_id", runId, Runs)(_.id)
  def * = id.? ~ datetime ~ value ~ metricId ~ runId <>(
    { t => MetricValue(t._1, t._2, t._3, t._4, t._5) },
    { metricValue: MetricValue => Some((None, metricValue.datetime, metricValue.value,
          metricValue.metricId, metricValue.runId)) })

  def insert(datetime: Timestamp, value: Long, metricId: Long, runId: Long): Option[MetricValue] = DB.withSession { implicit session =>
    val metricValue = MetricValue(None, datetime, value, metricId, runId)
    val metricValueId = MetricValues.* returning MetricValues.id insert(metricValue)
    Some(metricValue.copy(id=Some(metricValueId)))
  }

  def deleteByRun(metricId: Long, runId: Long) = DB.withSession { implicit session =>
    (
      for { metricValue <- MetricValues if (metricValue.metricId === metricId) }
      yield (metricValue)
    ).delete
  }

  def create(runId: Long, metricId: Long, timestamp: Date, value: Long): ValidationNEL[String, MetricValue] = {
    insert(new Timestamp(timestamp.getTime), value, metricId, runId)
      .toSuccess(NonEmptyList("An error occured while creating a MetricValue"))
  }
}
