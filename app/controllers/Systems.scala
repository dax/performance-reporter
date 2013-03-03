package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.Json._

import models.Systems._

object Systems extends Controller {
  val systemForm = Form(
    "label" -> nonEmptyText
  )

  def index = Action {
    Redirect(routes.Systems.list)
  }

  def list = Action {
    Ok(views.html.systems.list(models.Systems.all()))
  }

  def show(id: Long) = Action {
    models.Systems.findById(id).map { system =>
      Ok(views.html.systems.show(system))
    }.getOrElse(NotFound)
  }

  def create = Action {
    Ok(views.html.systems.create(systemForm))
  }

  def newSystem = Action { implicit request =>
    systemForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.systems.create(formWithErrors)),
      label =>
        models.Systems.create(label).map {
          system => Redirect(routes.Systems.show(system.id.get))
        }.getOrElse {
          BadRequest(views.html.systems.list(models.Systems.all())) // TODO : print error
        }
      )
  }

  def delete(id: Long) = Action {
    models.Systems.deleteById(id)
    Redirect(routes.Systems.list)
  }
}
