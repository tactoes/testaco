package org.testaco.ext.postgresql

import org.testaco.dataset.datatype.AbstractDataType
import org.testaco.dataset.datatype.TypeCastException
import java.lang.reflect.InvocationTargetException
import java.sql.*

class UuidType : AbstractDataType<String>("uuid", Types.OTHER, String::class, false, false) {
    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): String? {
        val s = resultSet.getString(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            s
        }
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun setSqlValue(
        value: Any, column: Int,
        statement: PreparedStatement
    ) {
        statement.setObject(column, getUUID(value, statement.connection))
    }

    @Throws(TypeCastException::class)
    override fun typeCast(value: Any): Any {
        return value.toString()
    }

    @Throws(TypeCastException::class)
    private fun getUUID(value: Any, connection: Connection): Any? {
        val tempUUID: Any?
        try {
            val aPGObjectClass = super.loadClass("org.postgresql.util.PGobject", connection)
            val ct = aPGObjectClass.getConstructor(null)
            tempUUID = ct.newInstance(null)
            val setTypeMethod = aPGObjectClass.getMethod(
                "setType", *arrayOf<Class<*>>(
                    String::class.java
                )
            )
            setTypeMethod.invoke(tempUUID, *arrayOf<Any>("uuid"))
            val setValueMethod = aPGObjectClass.getMethod(
                "setValue", *arrayOf<Class<*>>(
                    String::class.java
                )
            )
            setValueMethod.invoke(tempUUID, *arrayOf<Any>(value.toString()))
        } catch (e: ClassNotFoundException) {
            throw TypeCastException(value, this, e)
        } catch (e: InvocationTargetException) {
            throw TypeCastException(value, this, e)
        } catch (e: NoSuchMethodException) {
            throw TypeCastException(value, this, e)
        } catch (e: IllegalAccessException) {
            throw TypeCastException(value, this, e)
        } catch (e: InstantiationException) {
            throw TypeCastException(value, this, e)
        }
        return tempUUID
    }
}
