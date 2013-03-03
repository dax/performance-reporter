import org.specs2.mutable._

import java.util.Date
import java.sql.Timestamp

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.{Play, Application}

import models._

class RunControllerSpec extends Specification {

  "The Run controller" should {
    "list Runs of a System" in new WithDbData {
      Systems.create("first system")
      val Some(result) = route(FakeRequest(GET, "/systems/1/runs"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("0 run(s)")
    }

    "create a new Run from Json" in new WithDbData {
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
      val Some(result) = route(request)

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
      charset(result) must beSome("utf-8")
      contentAsString(result) must /("id" -> 1)

      val runOpt = Runs.findById(1)
      runOpt must beSome
      val run = runOpt.get
      run.systemId must equalTo(1)
      run.label must equalTo("test1")
      run.metrics must have size(2)
      run.metrics must contain(Metric(Some(1), "nginx", run.systemId))
      run.metrics.filter(_.label == "nginx").head.values must equalTo(
        List(MetricValue(Some(1), new Timestamp(1335164577000L), 1, 1, run.id.get),
          MetricValue(Some(2), new Timestamp(1335164578000L), 2, 1, run.id.get),
          MetricValue(Some(3), new Timestamp(1335164579000L), 3, 1, run.id.get))
      )
      run.metrics must contain(Metric(Some(2), "app", run.systemId))
      run.metrics.filter(_.label == "app").head.values must equalTo(
        List(MetricValue(Some(4), new Timestamp(1335164580000L), 4, 2, run.id.get),
          MetricValue(Some(5), new Timestamp(1335164581000L), 5, 2, run.id.get),
          MetricValue(Some(6), new Timestamp(1335164582000L), 6, 2, run.id.get))
      )

      val Some(listResult) = route(FakeRequest(GET, "/systems/1/runs"))

      status(listResult) must equalTo(OK)
      contentType(listResult) must beSome("text/html")
      charset(listResult) must beSome("utf-8")
      contentAsString(listResult) must contain("1 run(s)")
    }
  }
}
