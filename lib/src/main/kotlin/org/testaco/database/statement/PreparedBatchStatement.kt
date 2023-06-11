package org.testaco.database.statement

import org.testaco.dataset.datatype.DataType
import org.testaco.dataset.datatype.TypeCastException
import java.sql.Connection
import java.sql.SQLException

class PreparedBatchStatement internal constructor(sql: String?, connection: Connection?) :
    AbstractPreparedBatchStatement(
        sql!!, connection!!
    ) {
    private var _index = 0
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
        _statement.addBatch()
        _index = 0
    }

    @Throws(SQLException::class)
    override fun executeBatch(): Int {
        val results = _statement.executeBatch()
        var result = 0
        for (i in results.indices) {
            result += results[i]
        }
        return result
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
        _statement.clearBatch()
    }
}
