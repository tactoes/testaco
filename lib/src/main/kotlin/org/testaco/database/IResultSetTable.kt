package org.testaco.database

import org.testaco.dataset.ITable

interface IResultSetTable : ITable {
    fun close()
}
