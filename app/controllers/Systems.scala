package controllers

import anorm._

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.Json._

import models.System

object Systems extends Controller {
  val systemForm = Form(
    "label" -> nonEmptyText
  )

  def index = Action {
    Redirect(routes.Systems.list)
  }

  def list = Action {
    Ok(views.html.systems.list(System.all()))
  }

  def show(id: Long) = Action {
    System.findById(id).map { system =>
      Ok(views.html.systems.show(system))
    }.getOrElse(NotFound)
  }

  def create = Action {
    Ok(views.html.systems.create(systemForm))
  }

  def newSystem = Action { implicit request =>
    println(systemForm)
    systemForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.systems.create(formWithErrors)),
      label => {
        System.create(System(NotAssigned, label, List())).map { system =>
          Redirect(routes.Systems.show(system.id.get))
        }.getOrElse {
          BadRequest(views.html.systems.list(System.all())) // TODO : print error
        }
      }
    )
  }

  def delete(id: Long) = Action {
    System.delete(id)
    Redirect(routes.Systems.list)
  }
}
