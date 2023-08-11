package org.testaco.pgtester

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testaco.IgnoredTable
import org.testaco.TestacoConfiguration
import org.testaco.TestacoDatabase

@Configuration
class TestConfiguration {
    @Bean
    fun testacoConfiguration(): TestacoConfiguration =
        TestacoConfiguration(
            listOf(
                TestacoDatabase(
                    dataSource = "datasource",
                    schema = "test"),
            ),
        )
}
