package controllers

import java.util.Date

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.json.Json._
import play.api.libs.functional.syntax._

import models._
import models.Runs._

object Runs extends Controller {
  val JSON_ERROR = Map("status" -> toJson("KO"))
  val JSON_SUCESS = Map("status" -> toJson("OK"))

  private def jsonError(message: String) = toJson(JSON_ERROR.updated("message", toJson(message)))
  private def jsonError(messages: List[String]) = toJson(JSON_ERROR.updated("messages", toJson(messages)))

  def list(systemId: Long) = Action {
    Ok(views.html.runs.list(models.Runs.findBySystem(systemId)))
  }

  def show(id: Long) = Action {
    models.Runs.findById(id).map { run =>
      Ok(views.html.runs.show(run))
    }.getOrElse(NotFound)
  }

  def newRun(systemId: Long) = Action(parse.json) { request =>
    request.body.validate[JsonRun].map { jsonRun =>
      models.Runs.fromJson(jsonRun, systemId).fold(
        errors => InternalServerError(jsonError(errors.list)),
        runId => Ok(toJson(Map("id" -> runId)))
      )
    }.recoverTotal {
      error => BadRequest(JsError.toFlatJson(error))
    }
  }

  def delete(id: Long) = Action {
    models.Runs.deleteById(id)
    Redirect(routes.Systems.list) // TODO
  }
}
