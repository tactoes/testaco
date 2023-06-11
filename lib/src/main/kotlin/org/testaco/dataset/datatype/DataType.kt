
package org.testaco.dataset.datatype

import org.testaco.util.RelativeDateTimeParser
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import kotlin.reflect.KClass

/**
 * Data type that maps [java.sql.Types] objects to their
 * java counterparts. It also provides immutable constants for the most common data types.
 *
 * @see [sun JDBC object mapping](http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html.table1)
 */
interface DataType<T : Any> {
    /**
     * Returns the specified value typecasted to this `DataType`
     */
    abstract fun typeCast(value: Any): Any?

    /**
     * Returns the corresponding [java.sql.Types].
     */
    val sqlType: Int
    val typeClass: KClass<T>
    val isNumber: Boolean
    val isDateTime: Boolean

    @Throws(SQLException::class)
    fun getSqlValue(column: Int, resultSet: ResultSet): T?

    @Throws(SQLException::class)
    fun setSqlValue(value: Any, column: Int, statement: PreparedStatement)

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(DataType::class.java)
        val UNKNOWN = UnknownDataType()
        val CHAR = StringDataType(
            "CHAR", Types.CHAR
        )
        val VARCHAR = StringDataType(
            "VARCHAR", Types.VARCHAR
        )
        val LONGVARCHAR = StringDataType(
            "LONGVARCHAR", Types.LONGVARCHAR
        )
        val CLOB = ClobDataType()
        val NUMERIC = NumberDataType(
            "NUMERIC", Types.NUMERIC
        )
        val DECIMAL = NumberDataType(
            "DECIMAL", Types.DECIMAL
        )
        val BOOLEAN = BooleanDataType()
        val BIT = BitDataType()
        val TINYINT = IntegerDataType(
            "TINYINT", Types.TINYINT
        )
        val SMALLINT = IntegerDataType(
            "SMALLINT", Types.SMALLINT
        )
        val INTEGER = IntegerDataType(
            "INTEGER", Types.INTEGER
        )

        //    public static final DataType BIGINT = new LongDataType();
        val BIGINT = BigIntegerDataType()

        /**
         * Auxiliary for the BIGINT type using a long. Is currently only
         * needed for method [DataType.forObject].
         */
        val BIGINT_AUX_LONG = LongDataType()
        val REAL = FloatDataType()
        val FLOAT = DoubleDataType(
            "FLOAT", Types.FLOAT
        )
        val DOUBLE = DoubleDataType(
            "DOUBLE", Types.DOUBLE
        )

        // To calculate consistent relative date and time.
        val RELATIVE_DATE_TIME_PARSER: RelativeDateTimeParser = RelativeDateTimeParser()
        val DATE = DateDataType()
        val TIME = TimeDataType()
        val TIMESTAMP = TimestampDataType()
        val BINARY = UuidAwareBytesDataType(
            "BINARY", Types.BINARY
        )
        val VARBINARY = UuidAwareBytesDataType(
            "VARBINARY", Types.VARBINARY
        )
        val LONGVARBINARY = UuidAwareBytesDataType(
            "LONGVARBINARY", Types.LONGVARBINARY
        )
        val BLOB = BlobDataType()

        //New JDBC 4.0 types:
        //todo: ROWID = -8, NCLOB = 2011, SQLXML = 2009.
        val NCHAR = StringDataType(
            "NCHAR", -15
        )
        val NVARCHAR = StringDataType(
            "NVARCHAR", -9
        )
        val LONGNVARCHAR = StringDataType(
            "LONGNVARCHAR", -16
        )
        private val TYPES = arrayOf(
            VARCHAR, CHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR, CLOB, NUMERIC, DECIMAL, BOOLEAN, BIT, INTEGER,
            TINYINT, SMALLINT, BIGINT, REAL, DOUBLE, FLOAT, DATE, TIME, TIMESTAMP,
            VARBINARY, BINARY, LONGVARBINARY, BLOB,  //auxiliary types at the very end
            BIGINT_AUX_LONG
        )

        /**
         * Typecast the specified value to string.
         */
        @Throws(TypeCastException::class)
        fun asString(value: Any?): String? {
            logger.debug("asString(value={}) - start", value)
            return VARCHAR.typeCast(value) as String?
        }

        /**
         * Returns the `DataType` corresponding to the specified Sql
         * type. See [java.sql.Types].
         *
         */
        @Throws(DataTypeException::class)
        fun forSqlType(sqlType: Int): DataType<*> {
            if (logger.isDebugEnabled) logger.debug("forSqlType(sqlType={}) - start", sqlType)
            for (i in TYPES.indices) {
                if (sqlType == TYPES[i].sqlType) {
                    return TYPES[i]
                }
            }
            return UNKNOWN
        }

        /**
         * Returns the `DataType` corresponding to the specified value
         * runtime class. This method returns `DataType.UNKNOWN`
         * if the value is `null` or runtime class not recognized.
         */
        fun forObject(value: Any?): DataType<*> {
            logger.debug("forObject(value={}) - start", value)
            if (value == null) {
                return UNKNOWN
            }
            for (i in TYPES.indices) {
                val typeClass = TYPES[i].typeClass
                if (typeClass.isInstance(value)) {
                    return TYPES[i]
                }
            }
            return UNKNOWN
        }

        /**
         * Performs a quick check to test if the specified string uses extended
         * syntax.
         *
         * @param input
         * a string to check.
         * @return `true` if the input uses extended syntax; `false`
         * otherwise.
         */
        @JvmStatic fun isExtendedSyntax(input: String): Boolean {
            return !input.isEmpty() && input[0] == '['
        }
    }
}
