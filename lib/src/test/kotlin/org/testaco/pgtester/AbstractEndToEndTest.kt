package org.testaco.pgtester

import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testaco.TestacoExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

abstract class AbstractEndToEndTest {

    companion object {
        val POSTGRES_TEST_IMAGE = DockerImageName.parse("postgres:15.3")

        @JvmField
        val postgres = PostgreSQLContainer(POSTGRES_TEST_IMAGE).also { postgres -> postgres.start() }

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(applicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    "spring.datasource.jdbc-url=" + AbstractEndToEndTest.postgres.getJdbcUrl(),
                    "spring.datasource.username=" + AbstractEndToEndTest.postgres.getUsername(),
                    "spring.datasource.password=" + AbstractEndToEndTest.postgres.getPassword(),
                )
                    .applyTo(applicationContext.environment)
            }
        }

        @RegisterExtension
        @JvmStatic
        final var testaco = TestacoExtension()
    }
}
