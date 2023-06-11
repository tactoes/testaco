package org.testaco.database.statement

import org.testaco.dataset.datatype.DataType
import java.sql.SQLException

/**
 * @author Manuel Laflamme
 * @version $Revision$
 * @since Mar 15, 2002
 */
interface IPreparedBatchStatement {
    @Throws(TypeCastException::class, SQLException::class)
    fun addValue(value: Any?, dataType: DataType<*>)

    @Throws(SQLException::class)
    fun addBatch()

    @Throws(SQLException::class)
    fun executeBatch(): Int

    @Throws(SQLException::class)
    fun clearBatch()

    @Throws(SQLException::class)
    fun close()
}
