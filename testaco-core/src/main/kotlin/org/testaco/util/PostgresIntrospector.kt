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

package org.testaco.util

import org.testaco.schema.*
import java.sql.Connection

class PostgresIntrospector(private val connection: Connection) {

    fun introspect(schemas: List<String>): Schema {
        val tables = mutableMapOf<String, TableDef>()

        for (schemaName in schemas) {
            val tableNames = getTableNames(schemaName)
            for (tableName in tableNames) {
                val qualifiedName = "$schemaName.$tableName"
                val columns = getColumns(schemaName, tableName)
                val primaryKey = getPrimaryKey(schemaName, tableName)
                val foreignKeys = getForeignKeys(schemaName, tableName)
                tables[qualifiedName] = TableDef(columns = columns, primaryKey = primaryKey, foreignKeys = foreignKeys)
            }
        }

        return Schema(schemas = schemas, tables = tables)
    }

    private fun getTableNames(schema: String): List<String> {
        val sql = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            ORDER BY table_name
        """
        val result = mutableListOf<String>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.executeQuery().use { rs ->
                while (rs.next()) result.add(rs.getString("table_name"))
            }
        }
        return result
    }

    private fun getColumns(schema: String, table: String): Map<String, ColumnDef> {
        val sql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
        """
        val columns = mutableMapOf<String, ColumnDef>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    columns[rs.getString("column_name")] = ColumnDef(
                        type = normalizeType(rs.getString("data_type")),
                        nullable = rs.getString("is_nullable") == "YES"
                    )
                }
            }
        }
        return columns
    }

    private fun getPrimaryKey(schema: String, table: String): List<String> {
        val sql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
        """
        val result = mutableListOf<String>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) result.add(rs.getString("column_name"))
            }
        }
        return result
    }

    private fun getForeignKeys(schema: String, table: String): List<ForeignKeyDef> {
        val sql = """
            SELECT kcu.column_name, ccu.table_schema AS ref_schema, ccu.table_name AS ref_table,
                   ccu.column_name AS ref_column, tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage ccu
              ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'FOREIGN KEY'
            ORDER BY tc.constraint_name, kcu.ordinal_position
        """
        val fkMap = mutableMapOf<String, Triple<MutableList<String>, String, MutableList<String>>>()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val cn = rs.getString("constraint_name")
                    val entry = fkMap.getOrPut(cn) {
                        Triple(mutableListOf(), "${rs.getString("ref_schema")}.${rs.getString("ref_table")}", mutableListOf())
                    }
                    entry.first.add(rs.getString("column_name"))
                    entry.third.add(rs.getString("ref_column"))
                }
            }
        }
        return fkMap.values.map { (cols, ref, refCols) ->
            ForeignKeyDef(columns = cols, references = ref, referencedColumns = refCols)
        }
    }

    private fun normalizeType(pgType: String): String = when (pgType) {
        "character varying" -> "varchar"
        "timestamp without time zone" -> "timestamp"
        else -> pgType
    }
}
