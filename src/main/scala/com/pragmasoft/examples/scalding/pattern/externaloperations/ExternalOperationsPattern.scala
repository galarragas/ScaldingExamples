package com.pragmasoft.examples.scalding.pattern.externaloperations

import cascading.pipe.Pipe
import com.twitter.scalding._
import org.joda.time.format.DateTimeFormat
import SampleJobPipeTransformations.SampleJobPipeTransformationsWrapper
import com.twitter.scalding.Osv

package object schemas {
  val INPUT_SCHEMA = List('date, 'userid, 'url)
  val WITH_DAY_SCHEMA = List('date, 'userid, 'url, 'day)
  val EVENT_COUNT_SCHEMA = List('day, 'userid, 'event_count)
  val OUTPUT_SCHEMA = List('day, 'userid, 'email, 'address, 'event_count)

  val USER_DATA_SCHEMA = List('userid, 'email, 'address)
}

object BasicConversions {
  implicit def pipeToRichPipe(pipe: Pipe) : RichPipe = new RichPipe(pipe)
  implicit def richPipeToPipe(rp: RichPipe) : Pipe = rp.pipe
}

object SampleJobPipeTransformations extends FieldConversions with TupleConversions  {
  import schemas._
  import BasicConversions._

  implicit def wrapPipe(self: Pipe): SampleJobPipeTransformationsWrapper =
    new SampleJobPipeTransformationsWrapper(new RichPipe(self))
  implicit class SampleJobPipeTransformationsWrapper(val self: RichPipe) {

    val INPUT_DATE_PATTERN: String = "dd/MM/yyyy HH:mm:ss"

    /* Input schema: INPUT_SCHEMA
     * Output schema: WITH_DAY_SCHEMA */
    def addDayColumn : Pipe = self.map('date -> 'day) {
      date: String => DateTimeFormat.forPattern(INPUT_DATE_PATTERN)
        .parseDateTime(date).toString("yyyy/MM/dd");
    }

    /** Input schema: WITH_DAY_SCHEMA
      * Output schema: EVENT_COUNT_SCHEMA */
    def countUserEventsPerDay : Pipe =
      self.groupBy(('day, 'userid)) { _.size('event_count) }

    /** Joins with userData to add email and address
      *
      * Input schema: WITH_DAY_SCHEMA
      * User data schema: USER_DATA_SCHEMA
      * Output schema: OUTPUT_SCHEMA */
    def addUserInfo(userData: Pipe) : Pipe =
      self.joinWithLarger('userid -> 'userid, userData).project(OUTPUT_SCHEMA)
  }
}

import SampleJobPipeTransformations._
import schemas._

class SampleJob(args: Args) extends Job(args) {

  Osv(args("eventsPath"), INPUT_SCHEMA).read
    .addDayColumn
    .countUserEventsPerDay
    .addUserInfo(Osv(args("userInfoPath"), USER_DATA_SCHEMA).read)
    .write(Tsv(args("outputPath"), OUTPUT_SCHEMA))
}