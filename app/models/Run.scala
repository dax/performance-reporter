package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Run(id: Pk[Long], label: String, systemId: Long, metrics: List[Metric] = List())

object Run {
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

  val withMetrics = (Run.run ~ Metric.nullableMetric ~ MetricValue.nullableMetricValue *).map {_.map{
      case anorm.~(anorm.~(run, metricOpt), metricValueOpt) => (run, metricOpt, metricValueOpt)
    }
    .groupBy(_._1).toSeq.headOption.map {
      case (run, metric_metricValue) => run.copy(metrics = metric_metricValue.groupBy(_._2).map {
          case (Some(metric), values) => metric.copy(values = values.flatMap(_._3))
      }.toList)
    }}

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

  def create(run: Run): Option[Run] = {
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
}
