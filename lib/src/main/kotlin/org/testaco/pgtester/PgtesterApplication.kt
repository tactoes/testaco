package org.testaco.pgtester

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PgtesterApplication

fun main(args: Array<String>) {
    runApplication<PgtesterApplication>(*args)
}
