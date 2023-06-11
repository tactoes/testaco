package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import org.testaco.dataset.datatype.ToleratedDeltaMap.ToleratedDelta
import java.sql.Types
import java.util.*

open class DefaultDataTypeFactory : IDataTypeFactory {
    private val _toleratedDeltaMap: ToleratedDeltaMap = ToleratedDeltaMap()

    /**
     * @see org.testaco.dataset.datatype.IDataTypeFactory.createDataType
     */
    @Throws(DataTypeException::class)
    override fun createDataType(sqlType: Int, sqlTypeName: String): DataType<*> {
        if (logger.isDebugEnabled) logger.debug(
            "createDataType(sqlType={}, sqlTypeName={}) - start",
            sqlType,
            sqlTypeName
        )
        var dataType: DataType<*> = DataType.UNKNOWN
        if (sqlType != Types.OTHER) {
            dataType = DataType.forSqlType(sqlType)
        } else {
            // Necessary for compatibility with testaco 1.5 and older
            // BLOB
            if ("BLOB" == sqlTypeName) {
                return DataType.BLOB
            }

            // CLOB
            if ("CLOB" == sqlTypeName) {
                return DataType.CLOB
            }
        }
        return dataType
    }

    /**
     * @see org.testaco.dataset.datatype.IDataTypeFactory.createDataType
     */
    @Throws(DataTypeException::class)
    override fun createDataType(sqlType: Int, sqlTypeName: String, tableName: String, columnName: String): DataType<*> {
        if (logger.isDebugEnabled) logger.debug(
            "createDataType(sqlType={} , sqlTypeName={}, tableName={}, columnName={}) - start",
            sqlType, sqlTypeName, tableName, columnName)
        if (sqlType == Types.NUMERIC || sqlType == Types.DECIMAL) {
            // Check if the user has set a tolerance delta for this floating point field
            val delta: ToleratedDelta? = _toleratedDeltaMap.findToleratedDelta(tableName, columnName)
            // Found a toleratedDelta object
            if (delta != null) {
                if (logger.isDebugEnabled) logger.debug(
                    "Creating NumberTolerantDataType for table={}, column={}, toleratedDelta={}",
                    *arrayOf<Any>(tableName, columnName, delta.toleratedDelta)
                )

                // Use a special data type to implement the tolerance for numbers (floating point things)
                return NumberTolerantDataType(
                    "NUMERIC_WITH_TOLERATED_DELTA",
                    sqlType, delta.toleratedDelta
                )
            }
        }

        // In all other cases (default) use the default data type creation
        return this.createDataType(sqlType, sqlTypeName)
    }

    val toleratedDeltaMap: ToleratedDeltaMap
        /**
         * @return The whole map of tolerated delta objects that have been set until now
         * @since 2.3.0
         */
        get() = _toleratedDeltaMap

    /**
     * Adds a tolerated delta to this data type factory to be used for numeric comparisons
     * @param delta The new tolerated delta object
     * @since 2.3.0
     */
    fun addToleratedDelta(delta: ToleratedDelta?) {
        _toleratedDeltaMap.addToleratedDelta(delta)
    }

    /**
     * Returns a string representation of this [DefaultDataTypeFactory] instance
     * @since 2.4.6
     */
    override fun toString(): String {
        val sb = StringBuffer()
        sb.append(javaClass.name).append("[")
        sb.append("_toleratedDeltaMap=").append(_toleratedDeltaMap)
        sb.append("]")
        return sb.toString()
    }

    override val validDbProducts = listOf("derby")

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(DefaultDataTypeFactory::class.java)
    }
}
