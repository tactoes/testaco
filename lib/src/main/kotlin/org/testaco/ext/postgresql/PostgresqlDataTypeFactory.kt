package org.testaco.ext.postgresql

import org.testaco.dataset.datatype.DataType
import org.testaco.dataset.datatype.DefaultDataTypeFactory
import org.testaco.datatypes.postgres.*
import java.sql.Types

open class PostgresqlDataTypeFactory : DefaultDataTypeFactory() {

    override fun createDataType(sqlType: Int, sqlTypeName: String): DataType<*> {
        if (sqlType == Types.OTHER) {
            // Treat Postgresql UUID types as VARCHARS
            if ("uuid" == sqlTypeName) {
                return UuidType()
            }
        }
/*            } else if ("interval" == sqlTypeName) {
                return IntervalType()
            } else if ("inet" == sqlTypeName) {
                return InetType()
            } else if ("geometry" == sqlTypeName) {
                return GeometryType()
            } else if ("citext" == sqlTypeName) {
                return CitextType()
            } else {
                throw RuntimeException("Testaco does not support user data types out of the box. Please implement your own data type factory")
            }
        } else if (sqlType == Types.BIGINT && "oid" == sqlTypeName) {
            return PostgreSQLOidDataType()
        }
*/        return super.createDataType(sqlType, sqlTypeName)
    }

    override val validDbProducts = listOf("PostgreSQL")
}
