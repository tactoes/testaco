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
