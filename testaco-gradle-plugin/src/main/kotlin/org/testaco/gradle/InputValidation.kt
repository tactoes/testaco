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

package org.testaco.gradle

/**
 * Validates input for Gradle tasks.
 */
object InputValidation {
    private val VALID_IDENTIFIER = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    private val VALID_SCHEMA_TABLE = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?$")

    /**
     * Validates that a table name is safe (alphanumeric, underscores, optional schema prefix).
     *
     * @param table The table name to validate
     * @throws IllegalArgumentException if the table name is invalid
     */
    fun validateTableName(table: String) {
        require(table.isNotBlank()) { "Table name cannot be blank" }
        require(VALID_SCHEMA_TABLE.matches(table)) {
            "Invalid table name '$table'. Must be alphanumeric and start with letter or underscore. Format: [schema.]table"
        }
    }

    /**
     * Validates that a column name is safe (alphanumeric, underscores).
     *
     * @param column The column name to validate
     * @throws IllegalArgumentException if the column name is invalid
     */
    fun validateColumnName(column: String) {
        require(column.isNotBlank()) { "Column name cannot be blank" }
        require(VALID_IDENTIFIER.matches(column)) {
            "Invalid column name '$column'. Must be alphanumeric and start with letter or underscore"
        }
    }

    /**
     * Validates that a column type is not empty and contains no dangerous characters.
     *
     * @param type The SQL type to validate
     * @throws IllegalArgumentException if the type is invalid
     */
    fun validateColumnType(type: String) {
        require(type.isNotBlank()) { "Column type cannot be blank" }
        require(!type.contains(";") && !type.contains("--") && !type.contains("/*")) {
            "Invalid column type '$type'. Type cannot contain SQL comment or statement delimiters"
        }
    }

    /**
     * Validates that a nullable value is either "true" or "false".
     *
     * @param nullable The nullable value to validate
     * @throws IllegalArgumentException if the value is not "true" or "false"
     */
    fun validateNullable(nullable: String) {
        require(nullable in setOf("true", "false")) {
            "Invalid nullable value '$nullable'. Must be 'true' or 'false'"
        }
    }
}
