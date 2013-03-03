package models

import scalaz._
import Scalaz._

import collection._
import generic.CanBuildFrom

import play.api.db._
import play.api.Play.current

import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Json._
import play.api.libs.functional.syntax._

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

case class Run(id: Option[Long] = None, label: String, systemId: Long) {
  def metrics: Seq[Metric] = DB.withSession { implicit session =>
    (
      for { metric <- Metrics if (metric.systemId === systemId) }
      yield (metric)
    ).list
  }
}

case class JsonRun(label: String, metrics: Map[String, List[List[Long]]])

object Runs extends Table[Run]("run") {
  implicit val runReads: Reads[JsonRun] = (
    (__ \ "label").read[String] and
    (__ \ "metrics").read(map(
                            Reads.list[List[Long]](
                              Reads.list[Long]
                            )
                          ))
  )(JsonRun)

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def label = column[String]("label")
  def systemId = column[Long]("system_id")
  def system = foreignKey("system_id", systemId, Systems)(_.id)
  def * = id.? ~ label ~ systemId <> ({ t => Run(t._1, t._2, t._3) },
    { run: Run => Some((None, run.label, run.systemId)) })

  private def listToValidation[A, B](listOfValidations: Seq[ValidationNEL[A, B]]): ValidationNEL[A, Seq[B]] =
    listOfValidations.sequence[PartialApply1Of2[ValidationNEL, A]#Apply, B]

  def findById(id: Long): Option[Run] = DB.withSession { implicit session =>
    (
      for { run <- Runs if (run.id === id) }
      yield (run)
    ).firstOption
  }

  def findBySystem(systemId: Long): List[Run] = DB.withSession { implicit session =>
    (
      for { run <- Runs if (run.systemId === systemId) }
      yield (run)
    ).list
  }

  def deleteById(id: Long) = DB.withSession { implicit session =>
    (
      for { run <- Runs if (run.id === id) }
      yield (run)
    ).delete
  }

  def insert(label: String, systemId: Long): Option[Run] = DB.withSession { implicit session =>
    val run = Run(None, label, systemId)
    val runId = Runs.* returning Runs.id insert(run)
    Some(run.copy(id=Some(runId)))
  }

  def fromJson(jsonRun: JsonRun, systemId: Long) = create(jsonRun.label, systemId, jsonRun.metrics)

  def create(label: String, systemId: Long, metrics: Map[String, List[List[Long]]]) = DB.withSession { implicit session =>
    def createMetrics(runId: Long): ValidationNEL[String, Seq[Metric]] = listToValidation(
      (
        for ((metricLabel, values) <- metrics)
        yield Metrics.create(runId, systemId, metricLabel, values)
      ).toSeq
    )

    for (system  <- Systems.findByIdOrInsert(systemId)
         .toSuccess(NonEmptyList("An error occurred while creating a System"));
         run     <- insert(label, systemId)
         .toSuccess(NonEmptyList("An error occurred while creating a Run"));
         runId   <- run.id.toSuccess(NonEmptyList(""));
         metrics <- createMetrics(runId))
    yield runId
  }
}
