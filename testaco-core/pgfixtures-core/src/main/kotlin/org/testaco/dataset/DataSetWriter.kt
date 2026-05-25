package org.testaco.dataset

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Path

object DataSetWriter {

    private val mapper = ObjectMapper().registerKotlinModule().enable(SerializationFeature.INDENT_OUTPUT)

    fun toJson(dataSet: DataSet): String = mapper.writeValueAsString(dataSet.tables)

    fun toFile(dataSet: DataSet, path: Path) {
        Files.createDirectories(path.parent)
        Files.writeString(path, toJson(dataSet))
    }
}
