package org.testaco.dataset

import org.slf4j.LoggerFactory
import org.testaco.dataset.datatype.DataType
import java.sql.DatabaseMetaData

class Column @JvmOverloads constructor(val columnName: String, val dataType: DataType<*>,
                                       val sqlTypeName: String, val nullable: Nullable,
                                       val defaultValue: String? = null, val remarks: String? = null,
                                       val autoIncrement: AutoIncrement? = null
) {
    @JvmOverloads
    constructor(columnName: String, dataType: DataType<*>, nullable: Nullable = NULLABLE_UNKNOWN) : this(
        columnName,
        dataType,
        dataType.toString(),
        nullable,
        null
    )

    fun hasDefaultValue(): Boolean {
        return defaultValue != null
    }

    val isNotNullable: Boolean
        get() = nullable === NO_NULLS

    override fun toString(): String {
        return "(" + columnName + ", " + dataType + ", " + nullable + ")"
    }

    override fun equals(o: Any?): Boolean {
        logger.debug("equals(o={}) - start", o)
        if (this === o) return true
        if (o !is Column) return false
        val column = o
        if (columnName != column.columnName) return false
        if (dataType != column.dataType) return false
        if (nullable != column.nullable) return false
        if (sqlTypeName != column.sqlTypeName) return false

        // Default value is nullable
        if (defaultValue == null) {
            if (column.defaultValue != null) return false
        } else {
            if (defaultValue != column.defaultValue) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result: Int
        result = columnName.hashCode()
        result = 29 * result + dataType.hashCode()
        result = 29 * result + sqlTypeName.hashCode()
        result = 29 * result + nullable.hashCode()
        result = 29 * result + if (defaultValue == null) 0 else defaultValue.hashCode()
        return result
    }

    class Nullable(private val _name: String) {
        override fun toString(): String {
            return _name
        }
    }

    class AutoIncrement private constructor(val key: String) {

        override fun toString(): String {
            return "autoIncrement=$key"
        }

        companion object {
            val YES = AutoIncrement("YES")
            val NO = AutoIncrement("NO")
            val UNKNOWN = AutoIncrement("UNKNOWN")

            private val LOGGER = LoggerFactory.getLogger(AutoIncrement::class.java)

            /**
             * Searches the enumeration type for the given String provided by the JDBC driver.
             */
            fun autoIncrementValue(isAutoIncrement: String?): AutoIncrement {
                if (LOGGER.isDebugEnabled) logger.debug(
                    "autoIncrementValue(isAutoIncrement={}) - start",
                    isAutoIncrement
                )
                var result = UNKNOWN
                if (isAutoIncrement != null) {
                    if (isAutoIncrement.equals("YES", ignoreCase = true) || isAutoIncrement == "1") {
                        result = YES
                    } else if (isAutoIncrement.equals("NO", ignoreCase = true) || isAutoIncrement == "0") {
                        result = NO
                    }
                }
                return result
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Column::class.java)

        val NO_NULLS = Nullable("noNulls")
        val NULLABLE = Nullable("nullable")
        val NULLABLE_UNKNOWN = Nullable("nullableUnknown")

        /**
         * Returns the appropriate Nullable constant according specified JDBC
         * DatabaseMetaData constant.
         */
        fun nullableValue(nullable: Int): Nullable {
            if (logger.isDebugEnabled) logger.debug("nullableValue(nullable={}) - start", nullable.toString())
            return when (nullable) {
                DatabaseMetaData.columnNoNulls -> NO_NULLS
                DatabaseMetaData.columnNullable -> NULLABLE
                DatabaseMetaData.columnNullableUnknown -> NULLABLE_UNKNOWN
                else -> throw IllegalArgumentException(
                    "Unknown constant value "
                            + nullable
                )
            }
        }

        fun nullableValue(nullable: Boolean): Nullable {
            if (logger.isDebugEnabled) logger.debug("nullableValue(nullable={}) - start", nullable.toString())
            return if (nullable) NULLABLE else NO_NULLS
        }
    }
}
