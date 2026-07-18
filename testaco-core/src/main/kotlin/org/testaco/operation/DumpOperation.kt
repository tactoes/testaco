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

import org.testaco.config.TestacoConfig
import org.testaco.dataset.DataSet
import org.testaco.dataset.DataSetWriter
import org.testaco.schema.Schema
import java.nio.file.Path
import java.sql.Connection

object DumpOperation {

    fun toDataSet(connection: Connection, schema: Schema, config: TestacoConfig): DataSet {
        val tables = mutableMapOf<String, List<Map<String, Any?>>>()

        for ((tableName, tableDef) in schema.tables) {
            val (s, t) = tableName.split(".", limit = 2)
            val pk = tableDef.primaryKey
            val orderBy = if (pk.isNotEmpty()) pk.joinToString(", ") { "\"$it\"" } else "1"

            val rows = mutableListOf<Map<String, Any?>>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM \"$s\".\"$t\" ORDER BY $orderBy").use { rs ->
                    val meta = rs.metaData
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 1..meta.columnCount) {
                            val col = meta.getColumnName(i)
                            if (!config.isColumnIgnored(tableName, col)) row[col] = rs.getObject(i)
                        }
                        rows.add(row)
                    }
                }
            }
            tables[tableName] = rows
        }
        return DataSet(tables)
    }

    fun toFile(connection: Connection, schema: Schema, config: TestacoConfig, path: Path) {
        DataSetWriter.toFile(toDataSet(connection, schema, config), path)
    }
}
