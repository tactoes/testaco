package org.testaco.pgtester

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.format.DateTimeParseException

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = [AbstractEndToEndTest.Initializer::class])
class TimestampTest @Autowired constructor(val context: ApplicationContext) : AbstractEndToEndTest() {
  val localPath = "datasource/start.json"

  @Test
  fun `default reference data file should equal itself`() {
    with (testaco) {
      loadDataSet("datasource", "start.json")
      compareDataSet("datasource", "start.json")
    }
  }

  @Test
  fun `default reference data file with wrong timestamp format should fail`() {
    with (testaco) {
      loadDataSet("datasource", "start.json")
      MatcherAssert.assertThat(
        assertThrows<DateTimeParseException> {
          compareDataSet("datasource", "brokentimestamp.json")
        }.message,
        CoreMatchers.containsString("Text '20-24-10-21T00:00:00' could not be parsed")
      )
    }
  }

  @Test
  fun `it is possible to handle timestamps using the now keyword`() {
    with (testaco) {
      loadDataSet("datasource", "start.json")

      val c = postgres.createConnection("")
      c.autoCommit = false
      val statement = c.createStatement()

      statement.execute("update names set created=now() where id=1")
      c.commit()
      c.close()

      compareDataSet("datasource", "timestampwithnow.json")
    }
  }
}
