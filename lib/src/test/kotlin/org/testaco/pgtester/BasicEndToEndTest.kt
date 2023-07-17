package org.testaco.pgtester

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testaco.pgtester.AbstractEndToEndTest.Companion.postgres
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [AbstractEndToEndTest.Companion.Initializer::class])
class BasicEndToEndTest @Autowired constructor(val context: ApplicationContext) {

    @Test
    fun `flyway applies schema updates`() {
        val statement = postgres
            .createConnection("")
            .createStatement()

        statement.execute("select count(*) from public.flyway_schema_history")
        assert(statement.resultSet.next(), { "Should be able to read the flyway migration history" })
        assert(statement.resultSet.getInt(1) == 2, { "Should have applied two schema changes" })
    }
}
