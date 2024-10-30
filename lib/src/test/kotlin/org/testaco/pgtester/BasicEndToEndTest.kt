package org.testaco.pgtester

import java.io.File
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.io.FileMatchers.anExistingFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testaco.TestacoExtension.Companion.referenceSchemas
import org.testaco.datatypes.TestacoType
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = [AbstractEndToEndTest.Initializer::class])
class BasicEndToEndTest @Autowired constructor(val context: ApplicationContext) :
  AbstractEndToEndTest() {

  @Test
  fun `flyway applies schema updates`() {
    val c = postgres.createConnection("")
    c.autoCommit = false
    val statement = c.createStatement()

    statement.execute("select count(*) from test.flyway_schema_history")
    assert(statement.resultSet.next(), { "Should be able to read the flyway migration history" })
    assert(
      statement.resultSet.getInt(1) == 3,
      { "Should have applied two schema changes, plus schema creation" },
    )
    c.commit()
    c.close()
  }

  @Test
  fun `timestamp allowed time diff is actually read from schema file`() {
    with(testaco) {
      val referenceSchema =
        referenceSchemas["datasource"] ?: error("Could not find reference schema for 'datasource'")
      val columnDefinition: TestacoType<*> = referenceSchema.findColumn("names", "created")
      assert(
        columnDefinition.localConfiguration()["allowedTimeDiffInSeconds"]!! == "15",
        {
          "Time diff was columnDefinition.localConfiguration().get(\"allowedTimeDiffInSeconds\"), should be 15"
        },
      )
    }
  }

  @Test
  fun `can load a data set and then dump the database`() {
    with(testaco) {
      loadDataSet("datasource", "start.json")
      dumpDataSet("datasource", "dumpstart.json")
      compareDataSet("datasource", "start.json")
    }
  }

  @Test
  fun `fails on reference data set with extra column`() {
    with(testaco) {
      loadDataSet("datasource", "start.json")
      MatcherAssert.assertThat(
        assertThrows<IllegalStateException> { compareDataSet("datasource", "extracolumn.json") }
          .message,
        CoreMatchers.startsWith("Could not find column with name boguscolumn in db"),
      )
    }
  }

  @Test
  fun `fails on reference data set with missing column and writes file to file system`() {
    with(testaco) {
      loadDataSet("datasource", "start.json")
      MatcherAssert.assertThat(
        assertThrows<IllegalStateException> { compareDataSet("datasource", "missingcolumn.json") }
          .message,
        CoreMatchers.startsWith("Could not find column with name alias in ref"),
      )
      val datasetDumped = File("target/testaco/datasource/missingcolumn_result.json")
      MatcherAssert.assertThat(datasetDumped, anExistingFile())

      val dumpedFileContent = getFileContent("target/testaco/datasource/missingcolumn_result.json")
      val referenceFileContent = getFileContent("src/test/resources/db/testaco/datasource/start.json")
      JSONAssert.assertEquals(referenceFileContent, dumpedFileContent, true)
    }
  }

  private fun getFileContent(filename: String): String =
    File(filename).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
