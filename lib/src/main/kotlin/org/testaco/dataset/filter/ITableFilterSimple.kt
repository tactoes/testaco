package org.testaco.dataset.filter

interface ITableFilterSimple {
    fun accept(tableName: String): Boolean
}
