package controllers

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
  def list(systemId: Long) = Action {
    Ok(views.html.runs.list(Run.findBySystem(systemId)))
  }

  def show(id: Long) = Action {
    Run.findById(id).map { run =>
      Ok(views.html.runs.show(run))
    }.getOrElse(NotFound)
  }

  def newRun(systemId: Long) = Action(parse.json) { request =>
    ((request.body \ "label").asOpt[String],
      (request.body \ "metrics").asOpt[Map[String, List[List[Long]]]]) match {
      case (Some(label), Some(inputMetrics)) => {
				System.findById(systemId).orElse {
					System.create(System(NotAssigned, "No Label", List()))
				}.map { system =>
          val systemId = system.id.get
				  Run.create(Run(NotAssigned, label, systemId)).map { run =>
            val values = inputMetrics.map { case (metricLabel, metricValues) =>
              Metric.findByLabelAndSystem(metricLabel, systemId).orElse {
                Metric.create(Metric(NotAssigned, metricLabel, systemId))
              }.map { metric =>
                val metricId = metric.id.get
                val runId = run.id.get
                metricValues.foreach {case List(timestamp, value) =>
                  MetricValue.create(MetricValue(NotAssigned, new Date(timestamp),
                      value, metricId, runId)).getOrElse {
					          BadRequest(toJson(
							          Map("status" -> "KO", "message" -> "Unable to create new MetricValue")
						          ))
                  }
                  case _ => // Do nothing
                }
              }.getOrElse {
					      BadRequest(toJson(
							      Map("status" -> "KO", "message" -> "Unable to create new Metric")
						      ))
              }
            }
					  Ok(toJson(
							  Map("id" -> run.id.get)
						  ))
          }.getOrElse {
					  BadRequest(toJson(
							  Map("status" -> "KO", "message" -> "Unable to create new Run")
						  ))
          }
				}.getOrElse {
					BadRequest(toJson(
							Map("status" -> "KO", "message" -> "Unable to create new System")
						))
				}
      }
      case _ => {
        BadRequest(toJson(
            Map("status" -> "KO", "message" -> "Message malformed")
          ))
      }
    }
  }

  def delete(id: Long) = Action {
    Run.delete(id)
    Redirect(routes.Systems.list) // TODO
  }
}
