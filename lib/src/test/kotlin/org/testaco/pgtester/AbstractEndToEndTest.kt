package org.testaco.pgtester

import com.zaxxer.hikari.HikariDataSource
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testaco.TestacoExtension
import org.testaco.TestacoExtension.Companion.springContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

abstract class AbstractEndToEndTest {

    abstract fun dataSourceName(): String

    @BeforeEach
    fun noInUseConnections() {
        val dataSource: HikariDataSource = (springContext!!.getBean(dataSourceName()) as DataSource?) as HikariDataSource
        assert (dataSource.hikariPoolMXBean.activeConnections == 0, {"Number of active connections was ${dataSource.hikariPoolMXBean.activeConnections} instead of 0"})
    }

    companion object {
        val POSTGRES_TEST_IMAGE = DockerImageName.parse("postgres:17.0-alpine")

        @JvmField
        val postgres = PostgreSQLContainer(POSTGRES_TEST_IMAGE).also { postgres -> postgres.start() }

        @RegisterExtension
        @JvmStatic
        var testaco = TestacoExtension(postgres)
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.jdbc-url=" + postgres.jdbcUrl,
                "spring.datasource.username=" + postgres.username,
                "spring.datasource.password=" + postgres.password,
            )
                .applyTo(applicationContext.environment)
        }
    }

}
