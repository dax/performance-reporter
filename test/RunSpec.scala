import org.specs2.mutable._

import anorm._
import java.util.Date

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models._
import models.Run._

class RunSpec extends Specification {
  "A List of Tuple" should {
    "be clusterable" in {
      List(2 -> 4, 1 -> 2, 1 -> 3)
      .clusterBy(e => e._1 -> e._2) must
      havePairs(1 -> List(2, 3), 2 -> List(4))
    }
  }

  "A Run" should {
    "be created" in {
      val runMetadata = List(MetaDataItem("run.id", false, classOf[Long].getName),
        MetaDataItem("run.label", false, classOf[String].getName),
        MetaDataItem("run.system_id", false, classOf[Long].getName))
      "from a row" in {
        val parsedRow = Run.run(MockRow(List(1, "run", 1), MetaData(runMetadata)))

        parsedRow must beEqualTo(Success(Run(Id(1), "run", 1, List())))
      }

      "with runs from a row" in {
        val metricValueMetadata = List(MetaDataItem("metric_value.id", true, classOf[Long].getName),
          MetaDataItem("metric_value.datetime", true, classOf[Date].getName),
          MetaDataItem("metric_value.value", true, classOf[Long].getName),
          MetaDataItem("metric_value.metric_id", true, classOf[Long].getName),
          MetaDataItem("metric_value.run_id", true, classOf[Long].getName))
        val metricMetadata = List(MetaDataItem("metric.id", true, classOf[Long].getName),
          MetaDataItem("metric.label", true, classOf[String].getName),
          MetaDataItem("metric.system_id", true, classOf[Long].getName))

        val metadata = MetaData(runMetadata ++ metricValueMetadata ++ metricMetadata)

        "and metrics and metric values" in {
        val parsedRow = Run.withMetrics(Stream(
            SqlRow(metadata, List(1, "run1", 42,
                111, new Date(1335164579), 10, 11, 1,
                11, "metric11", 42)),
            SqlRow(metadata, List(1, "run1", 42,
                112, new Date(1335164578), 20, 11, 1,
                11, "metric11", 42)),
            SqlRow(metadata, List(1, "run1", 42,
                121, new Date(1335164577), 30, 12, 1,
                12, "metric12", 42)),
            SqlRow(metadata, List(1, "run1", 42,
                122, new Date(1335164576), 40, 12, 1,
                12, "metric12", 42))
          ))

        parsedRow must beEqualTo(Success(Some(
              Run(Id(1), "run1", 42, List(
                  Metric(Id(11), "metric11", 42, List(
                      MetricValue(Id(111), new Date(1335164579), 10, 11, 1),
                      MetricValue(Id(112), new Date(1335164578), 20, 11, 1)
                    )),
                  Metric(Id(12), "metric12", 42, List(
                      MetricValue(Id(121), new Date(1335164577), 30, 12, 1),
                      MetricValue(Id(122), new Date(1335164576), 40, 12, 1)
                    ))
                ))
            )))
        }

        "and metrics but no metric values" in {
          val parsedRow = Run.withMetrics(Stream(
              SqlRow(metadata, List(1, "run1", 42,
                  null, null, null, null, null,
                  11, "metric11", 42)),
              SqlRow(metadata, List(1, "run1", 42,
                  null, null, null, null, null,
                  12, "metric12", 42))
            ))

          parsedRow must beEqualTo(Success(Some(
                Run(Id(1), "run1", 42, List(
                    Metric(Id(11), "metric11", 42, List()),
                    Metric(Id(12), "metric12", 42, List())
                  ))
              )))
        }

        "but no metrics" in {
          val parsedRow = Run.withMetrics(Stream(
              SqlRow(metadata, List(1, "run1", 42,
                  null, null, null, null, null,
                  null, null, null)),
              SqlRow(metadata, List(1, "run1", 42,
                  null, null, null, null, null,
                  null, null, null))
            ))

          parsedRow must beEqualTo(Success(Some(
                Run(Id(1), "run1", 42, List())
              )))
        }

      }
    }
  }
}
