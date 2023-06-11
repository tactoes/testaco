package org.testaco.database.statement

import org.slf4j.LoggerFactory
import org.testaco.dataset.datatype.DataType
import org.testaco.dataset.datatype.TypeCastException
import java.sql.SQLException

class AutomaticPreparedBatchStatement(private val _statement: IPreparedBatchStatement, private val _threshold: Int) :
    IPreparedBatchStatement {
    private var _batchCount = 0
    private var _result = 0

    @Throws(TypeCastException::class, SQLException::class)
    override fun addValue(value: Any?, dataType: DataType<*>) {
        _statement.addValue(value, dataType)
    }

    @Throws(SQLException::class)
    override fun addBatch() {
        _statement.addBatch()
        _batchCount++
        if (_batchCount % _threshold == 0) {
            _result += _statement.executeBatch()
        }
    }

    @Throws(SQLException::class)
    override fun executeBatch(): Int {
        _result += _statement.executeBatch()
        return _result
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
        _statement.clearBatch()
        _batchCount = 0
    }

    @Throws(SQLException::class)
    override fun close() {
        _statement.close()
    }
}
