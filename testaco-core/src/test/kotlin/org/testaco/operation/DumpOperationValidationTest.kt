/*
 * Copyright 2026 Geir Hedemark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testaco.operation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import org.testaco.config.TestacoConfig
import org.testaco.schema.ColumnDef
import org.testaco.schema.Schema
import org.testaco.schema.TableDef
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.Connection

class DumpOperationValidationTest : FunSpec({

    test("toDataSet rejects unsafe table identifiers before query execution") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users\"; DROP TABLE users; --" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id")
                )
            )
        )

        shouldThrow<IllegalArgumentException> {
            DumpOperation.toDataSet(failOnStatementConnection(), schema, TestacoConfig())
        }
    }
})

private fun failOnStatementConnection(): Connection {
    val handler = InvocationHandler { _, method, _ ->
        when (method.name) {
            "createStatement" -> error("createStatement should not be called for invalid identifiers")
            "close" -> null
            else -> defaultValue(method.returnType)
        }
    }
    return Proxy.newProxyInstance(
        Connection::class.java.classLoader,
        arrayOf(Connection::class.java),
        handler
    ) as Connection
}

private fun defaultValue(returnType: Class<*>): Any? = when (returnType) {
    java.lang.Boolean.TYPE -> false
    java.lang.Byte.TYPE -> 0.toByte()
    java.lang.Short.TYPE -> 0.toShort()
    java.lang.Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Float.TYPE -> 0f
    java.lang.Double.TYPE -> 0.0
    java.lang.Character.TYPE -> '\u0000'
    else -> null
}
