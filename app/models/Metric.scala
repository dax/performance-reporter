package models

import scalaz._
import Scalaz._
import java.util.Date

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Metric(id: Pk[Long], label: String, systemId: Long, values: List[MetricValue] = List())

object Metric {
  private def listToValidation[A, B](listOfValidations: Seq[ValidationNEL[A, B]]): ValidationNEL[A, Seq[B]] =
  listOfValidations.sequence[PartialApply1Of2[ValidationNEL, A]#Apply, B]

  val metric = {
    get[Pk[Long]]("metric.id") ~
    get[String]("metric.label") ~
    get[Long]("metric.system_id") map {
      case id~label~systemId => Metric(id, label, systemId)
    }
  }

  val nullableMetric = {
    get[Option[Pk[Long]]]("metric.id") ~
    get[Option[String]]("metric.label") ~
    get[Option[Long]]("metric.system_id") map {
      case None~None~None => None
      case Some(id)~Some(label)~Some(systemId) => Some(Metric(id, label, systemId))
    }
  }

  val withMetricValues = (Metric.nullableMetric ~ MetricValue.nullableMetricValue *).map(_.groupBy(_._1).toSeq.headOption.map {
      case (None, _) => None
      case (Some(metric), metricValues) => metric.copy(values = metricValues.map(_._2).flatten)
    })

  def findById(id: Long): Option[Metric] = DB.withConnection { implicit c =>
    SQL("""
      SELECT metric.id, metric.label, metric.system_id
      FROM metric
      WHERE metric.id = {id}
      """).on(
      'id -> id
    ).as(metric.singleOpt)
  }

  def findByLabelAndSystemOrInsert(label: String, systemId: Long): Option[Metric] =
    findByLabelAndSystem(label, systemId).orElse {
      Metric.insert(Metric(NotAssigned, label, systemId))
    }

  def findByLabelAndSystem(label: String, systemId: Long): Option[Metric] = {
    DB.withConnection { implicit c =>
      SQL("""
        SELECT metric.id, metric.label, metric.system_id
        FROM metric
        WHERE metric.system_id = {system}
        AND metric.label = {label}
        """).on(
        'system -> systemId,
        'label -> label
      ).as(metric.singleOpt)
    }
  }

  def insert(metric: Metric): Option[Metric] = {
    DB.withConnection { implicit c =>
      SQL("""
        INSERT INTO metric (label, system_id)
        VALUES ({label}, {system})
        """).on(
        'label -> metric.label,
        'system -> metric.systemId
      ).executeInsert().map { id =>
        metric.copy(id = Id(id))
     }
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM metric WHERE id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }

  def create(runId: Long, systemId: Long, metricLabel: String, values: List[List[Long]]): ValidationNEL[String, Metric] = {
    def createMetricValues(metricValues: List[List[Long]], metricId: Long) = listToValidation(
      for (List(timestamp, value) <- metricValues)
        yield MetricValue.create(runId, metricId, new Date(timestamp), value)
      )

    for (metric    <- Metric.findByLabelAndSystemOrInsert(metricLabel, systemId)
      .toSuccess(NonEmptyList("An error occurred while creating a Metric"));
      metricId     <- metric.id.toOption.toSuccess(NonEmptyList(""));
      metricValues <- createMetricValues(values, metricId))
    yield metric
  }
}
