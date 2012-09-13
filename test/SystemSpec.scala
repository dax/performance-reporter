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
              MetaDataItem("system.id", false, classOf[Long].getName),
              MetaDataItem("system.label", false, classOf[String].getName)
          ))))

      parsedRow must beEqualTo(Success(System(Id(1), "system", List())))
    }

    "be created with runs from a row" in {
      val parsedRow = System.withRuns(Stream(
          SqlRow(
            MetaData(List(
                MetaDataItem("system.id", false, classOf[Long].getName),
                MetaDataItem("system.label", false, classOf[String].getName),
                MetaDataItem("run.id", true, classOf[Long].getName),
                MetaDataItem("run.label", true, classOf[String].getName),
                MetaDataItem("run.system_id", true, classOf[Long].getName)
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
