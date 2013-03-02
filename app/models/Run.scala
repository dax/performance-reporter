package models

import scalaz._
import Scalaz._

import collection._
import generic.CanBuildFrom

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Json._
import play.api.libs.functional.syntax._

case class Run(id: Pk[Long], label: String, systemId: Long, metrics: List[Metric] = List())

case class JsonRun(label: String, metrics: Map[String, List[List[Long]]])

class Clusterable[C[A] <: TraversableOnce[A], A](coll: C[A]) {
  def clusterBy[K, V, That](f: A => (K, V))(implicit cbf: CanBuildFrom[C[A], V, That]): immutable.Map[K, That] = {
    coll.foldLeft(mutable.Map.empty[K, mutable.Builder[V, That]]) {
      (acc, e) => {
        val (key, value) = f(e)
        acc += (key -> (acc.getOrElseUpdate(key, cbf(coll)) += value))
      }
    }.foldLeft(immutable.Map.newBuilder[K, That]) {
      case (acc, (key, value)) => {
        acc += ((key, value.result))
      }
    }.result
  }
}


object Run {
  implicit val runReads: Reads[JsonRun] = (
    (__ \ "label").read[String] and
    (__ \ "metrics").read(map(
        Reads.list[List[Long]](
          Reads.list[Long]
        )
      ))
  )(JsonRun)

  private def listToValidation[A, B](listOfValidations: Seq[ValidationNEL[A, B]]): ValidationNEL[A, Seq[B]] =
  listOfValidations.sequence[PartialApply1Of2[ValidationNEL, A]#Apply, B]

  implicit def clusterable[A, C[A] <: TraversableOnce[A]](coll: C[A]): Clusterable[C, A] = new Clusterable[C, A](coll)

  val run = {
    get[Pk[Long]]("run.id") ~
    get[String]("run.label") ~
    get[Long]("run.system_id") map {
      case id~label~systemId => Run(id, label, systemId)
    }
  }

  def nullableRun = {
    get[Option[Pk[Long]]]("run.id") ~
    get[Option[String]]("run.label") ~
    get[Option[Long]]("run.system_id") map {
      case None~None~None => None
      case Some(id)~Some(label)~Some(systemId) => Some(Run(id, label, systemId))
    }
  }

  def flattenSet(resultSet: anorm.~[anorm.~[Run, Option[Metric]], Option[MetricValue]]): (Run, Option[Metric], Option[MetricValue]) = resultSet match {
    case anorm.~(anorm.~(run, metricOpt), metricValueOpt) => (run, metricOpt, metricValueOpt)
  }

  def createMetrics(metricsAndMetricValues: List[(Option[Metric], Option[MetricValue])]): List[Metric] = {
    (for ((Some(metric), valueOpts) <- metricsAndMetricValues.clusterBy(e => e._1 -> e._2))
      yield metric.copy(values = valueOpts.flatMap(e => e))).toList
  }

  def createRun(runsMetricsAndMetricValues: List[(Run, Option[Metric], Option[MetricValue])]): Option[Run] = {
    for ((run, metricsAndMetricValues) <- runsMetricsAndMetricValues.clusterBy(e => e._1 -> (e._2, e._3)).toSeq.headOption)
      yield run.copy(metrics = createMetrics(metricsAndMetricValues))
  }

  val withMetrics = (Run.run ~ Metric.nullableMetric ~ MetricValue.nullableMetricValue *).map { e =>
    createRun(e.map(flattenSet))
  }

  def findById(id: Long): Option[Run] = DB.withConnection { implicit c =>
    SQL("""
      SELECT run.id, run.label, run.system_id, metric_value.id, metric_value.datetime, metric_value.value, metric_value.metric_id, metric_value.run_id, metric.id, metric.label, metric.system_id
      FROM run
      LEFT OUTER JOIN metric_value ON run.id = metric_value.run_id
      LEFT OUTER JOIN metric ON metric.id = metric_value.metric_id
      WHERE run.id = {id}
      """).on(
      'id -> id
    ).as(withMetrics)
  }

  def findBySystem(systemId: Long): List[Run] = DB.withConnection { implicit c =>
    SQL("""
      SELECT run.id, run.label, run.system_id
      FROM run
      WHERE run.system_id = {system}
      """).on(
      'system -> systemId
    ).as(run *)
  }

  def insert(run: Run): Option[Run] = {
    DB.withConnection { implicit c =>
      SQL("""
        INSERT INTO run (label, system_id)
        VALUES ({label}, {system})
        """).on(
        'label -> run.label,
        'system -> run.systemId
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

  def fromJson(jsonRun: JsonRun, systemId: Long) = create(jsonRun.label, jsonRun.metrics, systemId)

  def create(label: String, metrics: Map[String, List[List[Long]]], systemId: Long) = {
    def createMetrics(runId: Long): ValidationNEL[String, Seq[Metric]] = listToValidation(
      (for ((metricLabel, values) <- metrics)
        yield Metric.create(runId, systemId, metricLabel, values)).toSeq
    )

    for (system  <- System.findByIdOrInsert(systemId)
      .toSuccess(NonEmptyList("An error occurred while creating a System"));
      run     <- Run.insert(Run(NotAssigned, label, systemId))
      .toSuccess(NonEmptyList("An error occurred while creating a Run"));
      runId   <- run.id.toOption.toSuccess(NonEmptyList(""));
      metrics <- createMetrics(runId))
    yield run
  }
}
