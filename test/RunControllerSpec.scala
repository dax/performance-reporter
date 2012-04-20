import org.specs2.mutable._

import anorm.NotAssigned

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models.Run
import models.System

class RunControllerSpec extends Specification {

  "The Run controller" should {
		"list Runs of a System" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				System.create(System(NotAssigned, "first system)", List()))
				val Some(result) = routeAndCall(FakeRequest(GET, "/systems/1/runs"))

				status(result) must equalTo(OK)
				contentType(result) must beSome("text/html")
				charset(result) must beSome("utf-8")
				contentAsString(result) must contain("0 run(s)")
			}
		}

		"create a new Run from Json" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				System.create(System(NotAssigned, "first system", List()))
				val Some(result) = routeAndCall(
					FakeRequest(POST, "/systems/1/runs", FakeHeaders(Map("Content-type" -> Seq("application/json"))),
						Json.parse("""{"label": "test1", "values": [1, 2, 3]}""")
					))

				status(result) must equalTo(OK)
				contentType(result) must beSome("application/json")
				charset(result) must beSome("utf-8")
				contentAsString(result) must /("id" -> 1)

				Run.findById(1).map { run =>
					run.system must equalTo(1)
					run.values must contain(1, 2, 3).only.inOrder
					run.label must equalTo("test1")
				}.getOrElse(failure("Expected Run with id 1 not found"))

				val Some(listResult) = routeAndCall(FakeRequest(GET, "/systems/1/runs"))

				status(listResult) must equalTo(OK)
				contentType(listResult) must beSome("text/html")
				charset(listResult) must beSome("utf-8")
				contentAsString(listResult) must contain("1 run(s)")
			}
		}
	}
}
