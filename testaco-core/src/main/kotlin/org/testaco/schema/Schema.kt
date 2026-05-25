/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.schema

data class Schema(
    val schemas: List<String>,
    val tables: Map<String, TableDef>
)

data class TableDef(
    val columns: Map<String, ColumnDef>,
    val primaryKey: List<String> = emptyList(),
    val foreignKeys: List<ForeignKeyDef> = emptyList()
)

data class ColumnDef(
    val type: String,
    val nullable: Boolean
)

data class ForeignKeyDef(
    val columns: List<String>,
    val references: String,
    val referencedColumns: List<String>
)
