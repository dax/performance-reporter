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
  val runForm = Form(
    "label" -> nonEmptyText
  )

  def index = Action {
    Redirect(routes.Runs.runs)
  }

  def runs = Action {
    Ok(views.html.runs.index(Run.all(), runForm))
  }

  def newRun = Action(parse.json) { request =>
    (request.body \ "label").asOpt[String].map { label =>
      Run.create(Run(NotAssigned, label)).map { run =>
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
          Map("status" -> "KO", "message" -> "Missing parameter [label]")
        ))
    }
  }

  def deleteRun(id: Long) = Action {
    Run.delete(id)
    Redirect(routes.Runs.runs)
  }
}
