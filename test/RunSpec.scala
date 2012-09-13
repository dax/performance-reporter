import org.specs2.mutable._

import anorm._
import java.util.Date

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models._

class RunSpec extends Specification {

  "A Run" should {
    "be created from a row" in {
      val parsedRow = Run.run(MockRow(List(1, "run", 1),
          MetaData(List(
              MetaDataItem("run.id", false, classOf[Long].getName),
              MetaDataItem("run.label", false, classOf[String].getName),
              MetaDataItem("run.system_id", false, classOf[Long].getName)
          ))))

      parsedRow must beEqualTo(Success(Run(Id(1), "run", 1, List())))
    }

    "be created with runs from a row" in {
      val metadata = MetaData(List(
          MetaDataItem("run.id", false, classOf[Long].getName),
          MetaDataItem("run.label", false, classOf[String].getName),
          MetaDataItem("run.system_id", false, classOf[Long].getName),
          MetaDataItem("metric_value.id", true, classOf[Long].getName),
          MetaDataItem("metric_value.datetime", true, classOf[Date].getName),
          MetaDataItem("metric_value.value", true, classOf[Long].getName),
          MetaDataItem("metric_value.metric_id", true, classOf[Long].getName),
          MetaDataItem("metric_value.run_id", true, classOf[Long].getName),
          MetaDataItem("metric.id", true, classOf[Long].getName),
          MetaDataItem("metric.label", true, classOf[String].getName),
          MetaDataItem("metric.system_id", true, classOf[Long].getName)
        ))
      val parsedRow = Run.withMetrics(Stream(
          SqlRow(metadata, List(1, "run1", 42, 111, new Date(1335164579), 10, 11, 1, 11, "metric11", 42)),
          SqlRow(metadata, List(1, "run1", 42, 112, new Date(1335164578), 20, 11, 1, 11, "metric11", 42)),
          SqlRow(metadata, List(1, "run1", 42, 121, new Date(1335164577), 30, 12, 1, 12, "metric12", 42)),
          SqlRow(metadata, List(1, "run1", 42, 122, new Date(1335164576), 40, 12, 1, 12, "metric12", 42)
          )))

      parsedRow must beEqualTo(Success(Some(
            Run(
              Id(1),
              "run1",
              42,
              List(
                Metric(Id(11), "metric11", 42, List(
                    MetricValue(Id(111), new Date(1335164579), 10, 11, 1),
                    MetricValue(Id(112), new Date(1335164578), 20, 11, 1)
                  )),
                Metric(Id(12), "metric12", 42, List(
                    MetricValue(Id(121), new Date(1335164577), 30, 12, 1),
                    MetricValue(Id(122), new Date(1335164576), 40, 12, 1)
                  ))
              )
            )
          )))
    }
  }
}
