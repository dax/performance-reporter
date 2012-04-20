package controllers

import anorm._

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.Json._

import models.Run

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
			(request.body \ "values").asOpt[Seq[Int]]) match {
			case (Some(label), Some(values)) => {
				Run.create(Run(NotAssigned, label, systemId, values)).map { run =>
					Ok(toJson(
							Map("id" -> run.id.get)
						))
				}.getOrElse {
					BadRequest(toJson(
							Map("status" -> "KO", "message" -> "Unable to create new Run")
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
