package models

import scalaz._
import Scalaz._
import java.util.Date

import play.api.db._
import play.api.Play.current

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

case class Metric(id: Option[Long], label: String, systemId: Long) {
  def values: Seq[MetricValue] = DB.withSession { implicit session =>
    (
      for { value <- MetricValues if (value.metricId === id) }
      yield (value)
    ).list
  }
}

object Metrics extends Table[Metric]("metric") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def label = column[String]("label")
  def systemId = column[Long]("system_id")
  def system = foreignKey("system_id", systemId, Systems)(_.id)
  def * = id.? ~ label ~ systemId <> ({ t => Metric(t._1, t._2, t._3) },
    { metric: Metric => Some((None, metric.label, metric.systemId)) })

  private def listToValidation[A, B](listOfValidations: Seq[ValidationNEL[A, B]]): ValidationNEL[A, Seq[B]] =
    listOfValidations.sequence[PartialApply1Of2[ValidationNEL, A]#Apply, B]

  def findById(id: Long): Option[Metric] = DB.withSession { implicit session =>
    (
      for { metric <- Metrics if (metric.id === id) }
      yield (metric)
    ).firstOption
  }

  def findByLabelAndSystemOrInsert(label: String, systemId: Long): Option[Metric] = DB.withSession { implicit session =>
    findByLabelAndSystem(label, systemId).orElse {
      insert(label, systemId)
    }
  }

  def findByLabelAndSystem(label: String, systemId: Long): Option[Metric] = DB.withSession { implicit session =>
    (
      for { metric <- Metrics if (metric.label === label) && (metric.systemId === systemId) }
      yield (metric)
    ).firstOption
  }

  def delete(id: Long) = DB.withSession { implicit session =>
    (
      for { metric <- Metrics if (metric.id === id) }
      yield (metric)
    ).delete
  }

  def insert(label: String, systemId: Long): Option[Metric] = DB.withSession { implicit session =>
    val metric = Metric(None, label, systemId)
    val metricId = Metrics.* returning Metrics.id insert(metric)
    Some(metric.copy(id=Some(metricId)))
  }

  def create(runId: Long, systemId: Long, metricLabel: String, values: List[List[Long]]): ValidationNEL[String, Metric] = {
    def createMetricValues(metricValues: List[List[Long]], metricId: Long) = listToValidation(
      for (List(timestamp, value) <- metricValues)
      yield MetricValues.create(runId, metricId, new Date(timestamp), value)
    )

    for (metric       <- findByLabelAndSystemOrInsert(metricLabel, systemId)
         .toSuccess(NonEmptyList("An error occurred while creating a Metric"));
         metricId     <- metric.id.toSuccess(NonEmptyList(""));
         metricValues <- createMetricValues(values, metricId))
    yield metric
  }
}
