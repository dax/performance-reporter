package controllers

import scalaz._
import Scalaz._

import anorm._
import java.util.Date

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Json._

import models._


object Runs extends Controller {
  case class JsonRun(label: String, metrics: Map[String, List[List[Long]]])
  implicit val runReads: Reads[JsonRun] = (
    (__ \ "label").read[String] and
    (__ \ "metrics").read(map(
        Reads.list[List[Long]](
          Reads.list[Long] provided (minLength[List[Long]](2) andThen maxLength[List[Long]](2))
        )
      ))
  )(JsonRun)

  val JSON_ERROR = Map("status" -> toJson("KO"))
  val JSON_SUCESS = Map("status" -> toJson("OK"))

  private def jsonError(message: String) = toJson(JSON_ERROR.updated("message", toJson(message)))
  private def jsonError(messages: List[String]) = toJson(JSON_ERROR.updated("messages", toJson(messages)))

  private def listToValidation[A, B](listOfValidations: Seq[ValidationNEL[A, B]]): ValidationNEL[A, Seq[B]] =
    listOfValidations.sequence[PartialApply1Of2[ValidationNEL, A]#Apply, B]

  def list(systemId: Long) = Action {
    Ok(views.html.runs.list(Run.findBySystem(systemId)))
  }

  def show(id: Long) = Action {
    Run.findById(id).map { run =>
      Ok(views.html.runs.show(run))
    }.getOrElse(NotFound)
  }


  def newRun(systemId: Long) = Action(parse.json) { request =>
    request.body.validate[JsonRun].fold(
      error => BadRequest(jsonError(error.toString)),
      jsonRun => {
        def createMetrics(systemId: Long, runId: Long): ValidationNEL[String, Seq[Metric]] = {
          def createMetricValues(metricValues: List[List[Long]], metricId: Long) = {
            def createMetricValue(timestamp: Date, value: Long) =
              for (metricValue <- MetricValue.create(
                                    MetricValue(NotAssigned, timestamp, value, metricId, runId)
                                  ).toSuccess(NonEmptyList("An error occured while creating a MetricValue")))
              yield metricValue

            listToValidation(for (List(timestamp, value) <- metricValues)
              yield createMetricValue(new Date(timestamp), value)
            )
          }

          def createMetric(metricLabel: String, values: List[List[Long]]) =
            for (metric       <- Metric.findByLabelAndSystemOrCreate(metricLabel, systemId)
                                 .toSuccess(NonEmptyList("An error occurred while creating a Metric"));
                 metricId     <- metric.id.toOption.toSuccess(NonEmptyList("Error XXX"));
                 metricValues <- createMetricValues(values, metricId))
            yield metric

          listToValidation(
            (for ((metricLabel, values) <- jsonRun.metrics)
              yield createMetric(metricLabel, values)).toSeq
          )
        }

        val result: ValidationNEL[String, Long] =
          for (system  <- System.findByIdOrCreate(systemId)
                          .toSuccess(NonEmptyList("An error occurred while creating a System"));
               run     <- Run.create(Run(NotAssigned, jsonRun.label, systemId))
                          .toSuccess(NonEmptyList("An error occurred while creating a Run"));
               runId   <- run.id.toOption.toSuccess(NonEmptyList("Error XXX"));
               metrics <- createMetrics(systemId, runId))
          yield runId

        result.fold(
          errors => InternalServerError(jsonError(errors.list)),
          runId => Ok(toJson(Map("id" -> runId)))
        )
      }
    )
  }

  def delete(id: Long) = Action {
    Run.delete(id)
    Redirect(routes.Systems.list) // TODO
  }
}
