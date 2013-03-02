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
      running(FakeApplication()) {
        val Some(result) = route(FakeRequest(GET, "/"))

        status(result) must equalTo(SEE_OTHER)
        redirectLocation(result) must beSome.which(_ == "/systems")
      }
    }

    "list Systems" in {
      running(FakeApplication()) {
        System.create("first system")
        val Some(result) = route(FakeRequest(GET, "/systems"))

        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html")
        charset(result) must beSome("utf-8")
        contentAsString(result) must contain("1 system(s)")
      }
    }

    // TODO : doesn't work anymore with Play-2.1-SNAPSHOT
    // "create a new System" in {
    //   running(FakeApplication()) {
    //     val request = FakeRequest(POST, "/systems").withFormUrlEncodedBody("label" -> "first system")
    //     println(request.body.asFormUrlEncoded.get)
    //     val Some(result) = route(request, request.body.asFormUrlEncoded.get)

    //     println(contentAsString(result))
    //     status(result) must equalTo(SEE_OTHER)

    //     System.findById(1).map { system =>
    //       system.label must equalTo("first system")
    //     }.getOrElse(failure("Expected System with id 1 not found"))
    //   }
    // }

    "show a System with runs" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val systemId = System.create("first system").get.id.get
        Run.create("run1", Map.empty[String, List[List[Long]]], systemId)
        Run.create("run2", Map.empty[String, List[List[Long]]], systemId)

        val retrievedSystem = System.findById(systemId).get
        retrievedSystem.label must equalTo("first system")
        retrievedSystem.runs.length must equalTo(2)
        retrievedSystem.runs(0).label must equalTo("run1")
        retrievedSystem.runs(1).label must equalTo("run2")
      }
    }
  }
}
