import org.specs2.mutable._

import anorm._
import java.util.Date

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json._
import play.api.libs.json.Json._

import models._

class SystemSpec extends Specification {

  "A System" should {
    "be created from a row" in {
      val parsedRow = System.system(MockRow(List(1, "system"),
          MetaData(List(
              MetaDataItem(ColumnName("system.id", None), false, classOf[Long].getName),
              MetaDataItem(ColumnName("system.label", None), false, classOf[String].getName)
          ))))

      parsedRow must beEqualTo(Success(System(Id(1), "system", List())))
    }

    "be created with runs from a row" in {
      val parsedRow = System.withRuns(Stream(
          SqlRow(
            MetaData(List(
                MetaDataItem(ColumnName("system.id", None), false, classOf[Long].getName),
                MetaDataItem(ColumnName("system.label", None), false, classOf[String].getName),
                MetaDataItem(ColumnName("run.id", None), true, classOf[Long].getName),
                MetaDataItem(ColumnName("run.label", None), true, classOf[String].getName),
                MetaDataItem(ColumnName("run.system_id", None), true, classOf[Long].getName)
              )),
            List(1, "system", 11, "run11", 1)
          )))

      parsedRow must beEqualTo(Success(Some(
            System(
              Id(1),
              "system",
              List(
                Run(Id(11), "run11", 1, List())
              )
            )
          )))
    }
  }
}
