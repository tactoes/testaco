package org.testaco.pgtester

import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testaco.TestacoExtension

abstract class AbstractEndToEndTest {

    companion object {
        @RegisterExtension
        @JvmStatic
        var testaco = TestacoExtension()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.jdbc-url=" + TestacoExtension.postgres.jdbcUrl,
                "spring.datasource.username=" + TestacoExtension.postgres.username,
                "spring.datasource.password=" + TestacoExtension.postgres.password,
            )
                .applyTo(applicationContext.environment)
        }
    }

}
