import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import models.{Systems, Runs}

class SystemControllerSpec extends Specification {

  "The System controller" should {
    "redirect index request to Systems list" in new WithApplication {
      val Some(result) = route(FakeRequest(GET, "/"))

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/systems")
    }

    "list Systems" in new WithDbData {
      models.Systems.create("first system")
      val Some(result) = route(FakeRequest(GET, "/systems"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("1 system(s)")
    }

    // TODO : doesn't work anymore with Play-2.1-SNAPSHOT
    "create a new System" in new WithDbData {
      val request = FakeRequest(POST, "/systems").withFormUrlEncodedBody("label" -> "first system")
      val Some(result) = route(request, request.body.asFormUrlEncoded.get)

      status(result) must equalTo(SEE_OTHER)

      models.Systems.findById(1).map { system =>
        system.label must equalTo("first system")
      }.getOrElse(failure("Expected System with id 1 not found"))
    }

    "show a System with runs" in new WithDbData {
      val systemId = models.Systems.create("first system").get.id.get
      models.Runs.create("run1", systemId, Map.empty[String, List[List[Long]]])
      models.Runs.create("run2", systemId, Map.empty[String, List[List[Long]]])

      val retrievedSystem = models.Systems.findById(systemId).get
      retrievedSystem.label must equalTo("first system")
      retrievedSystem.runs.length must equalTo(2)
      retrievedSystem.runs(0).label must equalTo("run1")
      retrievedSystem.runs(1).label must equalTo("run2")
    }

  }
}
