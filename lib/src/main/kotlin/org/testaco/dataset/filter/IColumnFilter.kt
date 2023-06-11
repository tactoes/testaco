package org.testaco.dataset.filter

import org.testaco.dataset.Column

interface IColumnFilter {
    fun accept(tableName: String, column: Column): Boolean
}
