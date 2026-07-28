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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.testaco.config.LoadStrategy
import org.testaco.config.TestacoConfig
import org.testaco.dataset.DataSet
import org.testaco.schema.ColumnDef
import org.testaco.schema.Schema
import org.testaco.schema.TableDef
import org.testaco.util.ForeignKeyResolver
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement

class LoadOperationTypedBindingTest : FunSpec({

    test("execute uses typed jdbc setters for basic scalar values") {
        val calls = mutableListOf<String>()
        val preparedStatement = preparedStatementProxy(calls)
        val connection = connectionProxy(preparedStatement)

        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users" to TableDef(
                    columns = mapOf(
                        "id" to ColumnDef("bigint", false),
                        "name" to ColumnDef("text", false)
                    ),
                    primaryKey = listOf("id")
                )
            )
        )
        val dataSet = DataSet(
            mapOf(
                "public.users" to listOf(
                    mapOf("id" to 1L, "name" to "Alice")
                )
            )
        )

        LoadOperation.execute(connection, dataSet, schema, ForeignKeyResolver(schema), TestacoConfig(), LoadStrategy.INSERT)

        calls.shouldContain("setLong")
        calls.shouldContain("setString")
        calls.shouldNotContain("setObject")
    }
})

private fun preparedStatementProxy(calls: MutableList<String>): PreparedStatement {
    val handler = InvocationHandler { _, method, _ ->
        when (method.name) {
            "setLong", "setString", "setObject", "setNull", "executeUpdate", "close" -> {
                calls += method.name
                if (method.name == "executeUpdate") 1 else null
            }
            else -> defaultValue(method.returnType)
        }
    }
    return Proxy.newProxyInstance(
        PreparedStatement::class.java.classLoader,
        arrayOf(PreparedStatement::class.java),
        handler
    ) as PreparedStatement
}

private fun connectionProxy(preparedStatement: PreparedStatement): Connection {
    var autoCommit = true
    val statement = Proxy.newProxyInstance(
        Statement::class.java.classLoader,
        arrayOf(Statement::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "execute", "close" -> if (method.name == "execute") true else null
                else -> defaultValue(method.returnType)
            }
        }
    ) as Statement

    val handler = InvocationHandler { _, method, args ->
        when (method.name) {
            "setAutoCommit" -> {
                autoCommit = args?.get(0) as Boolean
                null
            }
            "getAutoCommit" -> autoCommit
            "prepareStatement" -> preparedStatement
            "createStatement" -> statement
            "commit", "rollback", "close" -> null
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
