package org.testaco.datatypes

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed class DateTimeHandling<JT>(override val name: String, override val sqlType: Int,
                                    override val sqlTypeName: String, open val allowedTimeDiffInSeconds: Int = 10):
  TestacoType<JT>(name, sqlType, sqlTypeName) {

  override fun compare(reference: String, database: String): Boolean {
    return ChronoUnit.SECONDS.between(
      if (reference.replace("\"","").equals("now")) {
        LocalDateTime.now()
      } else {
        println("reference "+reference)
        LocalDateTime.parse(reference.replace("\"",""), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },
      LocalDateTime.parse(database.replace("\"",""), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    ) <= allowedTimeDiffInSeconds
  }

  override fun localConfiguration(): Map<String, String> {
    return mapOf("allowedTimeDiffInSeconds" to allowedTimeDiffInSeconds.toString())
  }
}
