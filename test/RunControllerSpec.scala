import org.specs2.mutable._

import anorm._
import java.util.Date

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models._

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
            Json.parse("""{
              "label": "test1",
              "metrics": {
                "nginx": [[1335164577000, 1],
                          [1335164578000, 2],
                          [1335164579000, 3]],
                "app": [[1335164580000, 4],
                        [1335164581000, 5],
                        [1335164582000, 6]]
              }
              }""")
          ))

        status(result) must equalTo(OK)
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
        contentAsString(result) must /("id" -> 1)

        Run.findById(1).map { run =>
          run.systemId must equalTo(1)
          run.label must equalTo("test1")
          run.metrics must have size(2)
          run.metrics must contain(Metric(Id(1), "nginx", run.systemId,
              List(MetricValue(Id(1), new Date(1335164577000L), 1, 1, run.id.get),
                MetricValue(Id(2), new Date(1335164578000L), 2, 1, run.id.get),
                MetricValue(Id(3), new Date(1335164579000L), 3, 1, run.id.get))))
          run.metrics must contain(Metric(Id(1), "nginx", run.systemId,
              List(MetricValue(Id(1), new Date(1335164577000L), 1, 1, run.id.get),
                MetricValue(Id(2), new Date(1335164578000L), 2, 1, run.id.get),
                MetricValue(Id(3), new Date(1335164579000L), 3, 1, run.id.get))))
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
