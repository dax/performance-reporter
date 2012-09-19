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
import play.api.libs.json.Json._

import models._

object Runs extends Controller {
  val JSON_ERROR = Map("status" -> "OK")
  val JSON_SUCESS = Map("status" -> "KO")

  def list(systemId: Long) = Action {
    Ok(views.html.runs.list(Run.findBySystem(systemId)))
  }

  def show(id: Long) = Action {
    Run.findById(id).map { run =>
      Ok(views.html.runs.show(run))
    }.getOrElse(NotFound)
  }

  def jsonError(message: String) = toJson(JSON_ERROR ++ List("message" -> message))
  def listToEither[A, B](listOfEithers: Traversable[Either[A, B]]): Either[A, Traversable[B]] = {
    listOfEithers.partition(_.isLeft) match {
      case (Nil, listOfRights) => Right(for (Right(e) <- listOfRights) yield e)
      case (listOfLefts, _) => Left((for (Left(e) <- listOfLefts) yield e).head)
    }
  }

  def createMetricValue(timestamp: Long, value: Long, metricId: Long, runId: Long): Either[SimpleResult[JsValue], MetricValue] = {
    for (metricValue <- MetricValue.create(MetricValue(NotAssigned, new Date(timestamp),
          value, metricId, runId))
      .toRight(InternalServerError(jsonError("An error occured while creating a MetricValue"))).right)
      yield metricValue
  }

  def createMetric(metricLabel: String, metricValues: List[List[Long]], systemId: Long, runId: Long): Either[SimpleResult[JsValue], Metric] = {
    for (metric <- Metric.findByLabelAndSystem(metricLabel, systemId).orElse {
        Metric.create(Metric(NotAssigned, metricLabel, systemId))
      }.toRight(InternalServerError(jsonError("An error occurred while creating a Metric"))).right;
      metricId <- metric.id.toOption.toRight(InternalServerError(jsonError("Error XXX"))).right;
      metricValues <- listToEither(for (List(timestamp, value) <- metricValues)
        yield createMetricValue(timestamp, value, metricId, runId)
      ))
      yield metric
  }

  def newRun(systemId: Long) = Action(parse.json) { request =>
    ((request.body \ "label").asOpt[String],
      (request.body \ "metrics").asOpt[Map[String, List[List[Long]]]]) match {
      case (labelOpt, inputMetricsOpt) => {
        val result = for (label <- labelOpt.toRight(BadRequest(jsonError("Not acceptable message format"))).right;
                          inputMetrics <- inputMetricsOpt.toRight(BadRequest(jsonError("Not acceptable message format"))).right;
                          system <- System.findById(systemId).orElse {
                                      System.create(System(NotAssigned, "No Label", List()))
                                    }.toRight(InternalServerError(jsonError("An error occurred while creating a System"))).right;
                          systemId <- system.id.toOption.toRight(InternalServerError(jsonError("Error XXX"))).right;
                          run <- Run.create(Run(NotAssigned, label, systemId)).toRight(
                                   InternalServerError(jsonError("An error occurred while creating a Run"))).right;
                          runId <- run.id.toOption.toRight(InternalServerError(jsonError("Error XXX"))).right;
                          metrics <- listToEither(for ((metricLabel, metricValues) <- inputMetrics)
                            yield createMetric(metricLabel, metricValues, systemId, runId)))
                       yield runId

        result.fold(e => e, r => Ok(toJson(Map("id" -> r))))
      }
    }
  }

  def delete(id: Long) = Action {
    Run.delete(id)
    Redirect(routes.Systems.list) // TODO
  }
}
