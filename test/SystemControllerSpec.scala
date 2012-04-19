import org.specs2.mutable._

import anorm.NotAssigned

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models.{System, Run}

class SystemControllerSpec extends Specification {

  "The System controller" should {
    "redirect index request to Systems list" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/"))

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/systems")
    }

    "list Systems" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				System.create(System(NotAssigned, "first system", List()))
        val Some(result) = routeAndCall(FakeRequest(GET, "/systems"))

        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html")
        charset(result) must beSome("utf-8")
				contentAsString(result) must contain("1 system(s)")
      }
    }

    "create a new System" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val Some(result) = routeAndCall(FakeRequest(POST, "/systems").withFormUrlEncodedBody("label" -> "first system"))

        status(result) must equalTo(SEE_OTHER)

				System.findById(1).map { system =>
					system.label must equalTo("first system")
				}.getOrElse(failure("Expected System with id 1 not found"))
      }
    }

		"show a System with runs" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				val system = System.create(System(NotAssigned, "first system", List()))
				Run.create(Run(NotAssigned, "run1", system.get.id.get))
				Run.create(Run(NotAssigned, "run2", system.get.id.get))

				val retrievedSystem = System.findById(system.get.id.get).get
				retrievedSystem.label must equalTo("first system")
				retrievedSystem.runs.length must equalTo(2)
				retrievedSystem.runs(0).label must equalTo("run1")
				retrievedSystem.runs(1).label must equalTo("run2")
			}
		}
  }
}
