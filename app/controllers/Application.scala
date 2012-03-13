package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import models.Run

object Application extends Controller {
	val runForm = Form(
		"label" -> nonEmptyText
	)

  def index = Action {
		Redirect(routes.Application.runs)
  }

	def runs = Action {
		Ok(views.html.index(Run.all(), runForm))
	}

	def newRun = Action { implicit request =>
		runForm.bindFromRequest.fold(
			errors => BadRequest(views.html.index(Run.all(), errors)),
			label => {
				Run.create(label)
				Redirect(routes.Application.runs)
			}
		)
	}

	def deleteRun(id: Long) = Action {
		Run.delete(id)
		Redirect(routes.Application.runs)
	}
}
