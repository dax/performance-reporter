import org.specs2.mutable._

import anorm._
import java.util.Date

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.{Play, Application}

import models._

class RunControllerSpec extends Specification {

  "The Run controller" should {
    "list Runs of a System" in {
      running(FakeApplication()) {
        System.create("first system")
        val Some(result) = route(FakeRequest(GET, "/systems/1/runs"))

        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html")
        charset(result) must beSome("utf-8")
        contentAsString(result) must contain("0 run(s)")
      }
    }

    "create a new Run from Json" in {
      running(FakeApplication()) {
        val request = FakeRequest(POST, "/systems/1/runs")
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.obj(
              "label" -> "test1",
              "metrics" -> Json.obj(
                "nginx" -> Json.arr(
                  Json.arr(1335164577000L, 1),
                  Json.arr(1335164578000L, 2),
                  Json.arr(1335164579000L, 3)
                ),
                "app" -> Json.arr(
                  Json.arr(1335164580000L, 4),
                  Json.arr(1335164581000L, 5),
                  Json.arr(1335164582000L, 6)
                )
              )
            ))
        val Some(result) = route(request, request.body.asJson.get)

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

        val Some(listResult) = route(FakeRequest(GET, "/systems/1/runs"))

        status(listResult) must equalTo(OK)
        contentType(listResult) must beSome("text/html")
        charset(listResult) must beSome("utf-8")
        contentAsString(listResult) must contain("1 run(s)")
      }
    }
  }
}
