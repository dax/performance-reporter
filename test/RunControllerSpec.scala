import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

class RunControllerSpec extends Specification {

  "The Run controller" should {
    "redirect index request to Runs list" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/"))

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/runs")
    }

    "respond to the index Action" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val Some(result) = routeAndCall(FakeRequest(GET, "/runs"))

        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html")
        charset(result) must beSome("utf-8")
      }
    }

    "create a new Run from Json" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val Some(result) = routeAndCall(
          FakeRequest(POST, "/runs", FakeHeaders(Map("Content-type" -> Seq("application/json"))),
            Json.parse("""{"label": "test1"}""")
          ))

        status(result) must equalTo(OK)
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
        contentAsString(result) must /("id" -> 1)
      }
    }

  }
}
