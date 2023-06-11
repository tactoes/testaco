package org.testaco.database.statement

import org.testaco.dataset.datatype.DataType
import org.testaco.dataset.datatype.TypeCastException
import java.sql.Connection
import java.sql.SQLException

class SimplePreparedStatement(sql: String, connection: Connection) : AbstractPreparedBatchStatement(
    sql, connection
) {
    private var _index = 0
    private var _result = 0

    @Throws(TypeCastException::class, SQLException::class)
    override fun addValue(value: Any?, dataType: DataType<*>) {
        if (value == null) {
            _statement.setNull(++_index, dataType.sqlType)
        } else {
            dataType.setSqlValue(value, ++_index, _statement)
        }
    }

    @Throws(SQLException::class)
    override fun addBatch() {
        val result = _statement.execute()
        if (!result) {
            _result += _statement.updateCount
        }
        _index = 0
    }

    @Throws(SQLException::class)
    override fun executeBatch(): Int {
        val result = _result
        clearBatch()
        return result
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
        _index = 0
        _result = 0
    }
}
