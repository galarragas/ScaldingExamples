package com.pragmasoft.examples.scalding.pattern

import com.twitter.scalding.{Tsv, Job, Args}
import org.joda.time.format.DateTimeFormat

class SampleJob(args: Args) extends Job(args) {
  val INPUT_DATE_PATTERN: String = "dd/MM/yyyy HH:mm:ss"

  val INPUT_SCHEMA = List('date, 'userid, 'url)
  val USER_DATA_SCHEMA = List('userid, 'email, 'address)
  val OUTPUT_SCHEMA = List('day, 'userid, 'email, 'address, 'event_count)

  Tsv(args("eventsPath"), INPUT_SCHEMA).read
    .map('date -> 'day) {
      date: String => DateTimeFormat.forPattern(INPUT_DATE_PATTERN)
        .parseDateTime(date).toString("yyyy/MM/dd");
    }
    .groupBy(('day, 'userid)) { _.size('event_count) }
    .joinWithLarger('userid -> 'userid, Tsv(args("userInfoPath"), USER_DATA_SCHEMA).read)
    .project(OUTPUT_SCHEMA)
    .write(Tsv(args("outputPath"), OUTPUT_SCHEMA))
}

