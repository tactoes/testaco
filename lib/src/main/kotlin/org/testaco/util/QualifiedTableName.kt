package org.testaco.util

class QualifiedTableName @JvmOverloads constructor(
    tableName: String,
    defaultSchema: String?,
) {
    val schema: String?

    val table: String

    init {
        val parts: List<String> = tableName.split('.', limit = 2)
        table = if (parts.size < 2) {
            tableName
        } else {
            parts[1]
        }
        schema = if (parts.size < 2) {
            defaultSchema
        } else {
            parts[0]
        }
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append(javaClass.name).append("[")
        sb.append("schema=").append(schema)
        sb.append(", table=").append(table)
        sb.append("]")
        return sb.toString()
    }

    // TODO: Quote special characters according to database type, e.g. postgres "-"
    fun qualifiedTableName(): String = "$schema.$table"
}
