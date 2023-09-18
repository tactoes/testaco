package org.testaco.pgtester

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [AbstractEndToEndTest.Companion.Initializer::class])
class BasicEndToEndTest @Autowired constructor(val context: ApplicationContext) : AbstractEndToEndTest() {

    @Test
    fun `flyway applies schema updates`() {
        val statement = postgres
            .createConnection("")
            .createStatement()

        statement.execute("select count(*) from test.flyway_schema_history")
        assert(statement.resultSet.next(), { "Should be able to read the flyway migration history" })
        println("resultset size "+statement.resultSet.getInt(1))
        assert(statement.resultSet.getInt(1) == 3, { "Should have applied two schema changes, plus schema creation" })
    }

    @Test
    fun `can load a data set`() {
        testaco.loadDataSet("datasource", "start.json")
    }
}
