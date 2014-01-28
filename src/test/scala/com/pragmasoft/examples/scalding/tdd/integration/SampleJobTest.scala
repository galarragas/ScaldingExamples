package com.pragmasoft.examples.scalding.tdd.integration

import _root_.com.pragmasoft.examples.scalding.pattern.externaloperations.SampleJob
import _root_.com.pragmasoft.examples.scalding.pattern.externaloperations.sampleJobSchema._
import _root_.com.twitter.scalding._
import _root_.com.twitter.scalding.Osv
import org.scalatest.{Matchers, FlatSpec}
import scala.collection.mutable

class SampleJobTest extends FlatSpec with Matchers with FieldConversions with TupleConversions {

  "A sample job" should "do the full transformation" in {

    val events = List(
      ("11/02/2013 10:22:11", 1000002l, "http://www.youtube.com"),
      ("11/02/2013 10:22:11", 1000003l, "http://www.youtube.com"),
      ("11/02/2013 10:22:11", 1000002l, "http://www.google.com"),
      ("11/02/2013 10:22:11", 1000002l, "http://www.facebook.com"),
      ("11/02/2013 10:22:11", 1000002l, "http://mail.google.com")
    )
    val userInfo = List(
      (1000002l, "stefano@email.com", "10 Downing St. London"),
      (1000003l, "antonios@email.com", "1 Kingdom St. London")
    )

    val expectedOutput = List(
      ("2013/02/11", 1000002l, "stefano@email.com", "10 Downing St. London", 4l),
      ("2013/02/11", 1000002l, "antonios@email.com", "1 Kingdom St. London", 1l)
    )

    JobTest(classOf[SampleJob].getName)
      .arg("eventsPath", "eventsPathFile")
      .arg("userInfoPath", "userInfoPathFile")
      .arg("outputPath", "outputPathFile")
      .source(Tsv("eventsPathFile", INPUT_SCHEMA), events)
      .source(Tsv("userInfoPathFile", USER_DATA_SCHEMA), userInfo)
      .sink(Tsv("outputPathFile", OUTPUT_SCHEMA)) {
        buffer: mutable.Buffer[(String, Long, String, String, Long)] =>
            buffer.toList should equal(expectedOutput)
      }
      .run
  }
}