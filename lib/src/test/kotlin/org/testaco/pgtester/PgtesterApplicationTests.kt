package org.testaco.pgtester

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.ClassUtils
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [AbstractEndToEndTest.Initializer::class])
class PgtesterApplicationTests @Autowired constructor(val context: ApplicationContext) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `all service methods are annotated for authorization`() {
        // verifies that spring-declared services have authorization in order to avoid OWASP A01:2021 and/or CWE-862
        val services = context.getBeansWithAnnotation(Service::class.java)
        services.forEach { entry: Map.Entry<String, Any> ->
            val service = Class.forName(
                ClassUtils.getUserClass(entry.value).name,
            ).kotlin // Get rid of any proxy

            service.declaredFunctions.forEach { function ->
                if (function.visibility == KVisibility.PUBLIC) {
                    val annotations = function.annotations
                    assert(
                        annotations.any { annotation ->
                            annotation.toString().contains("NoAuthorization") ||
                                annotation.toString().contains("PreAuthorize")
                        },
                    ) { """Function ${function.name} in service $service does not have an NoAuthorization or PreAuthorize annotation. Please decide what to do about authorization.""" }
                }
            }
        }
    }
}
