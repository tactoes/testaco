package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import org.testaco.dataset.datatype.ToleratedDeltaMap.ToleratedDelta
import java.math.BigDecimal
import kotlin.collections.HashMap

/**
 * Container that manages a map of [ToleratedDelta] objects to be used
 * for numeric comparisons with an allowed deviation of two values
 */
class ToleratedDeltaMap {
    private val _toleratedDeltas: MutableMap<String, ToleratedDelta> = HashMap()
    private val logger = LoggerFactory.getLogger(ToleratedDeltaMap::class.java)
    fun findToleratedDelta(tableName: String, columnName: String): ToleratedDelta? {
        val mapKey = buildMapKey(tableName, columnName)
        return _toleratedDeltas[mapKey]
    }

    fun addToleratedDelta(delta: ToleratedDelta?) {
        if (delta == null) {
            throw NullPointerException("The parameter 'delta' must not be null")
        }
        val key = buildMapKey(delta)
        // Put the new object into the map
        val removed = _toleratedDeltas.put(key, delta)
        //Give a hint to the user when an already existing object has been overwritten/replaced
        if (removed != null) {
            logger.debug(
                "Replaced old tolerated delta object from map with key {}. Old replaced object={}",
                key,
                removed
            )
        }
    }

    /**
     * Simple bean that holds the tolerance for floating point comparisons for a specific
     * database column.
     */
    class ToleratedDelta(val tableName: String, val columnName: String, val toleratedDelta: Precision) {

        constructor(tableName: String, columnName: String, toleratedDelta: Double) : this(
            tableName,
            columnName,
            Precision(BigDecimal(toleratedDelta.toString()))
        )

        constructor(tableName: String, columnName: String, toleratedDelta: BigDecimal) : this(
            tableName,
            columnName,
            Precision(toleratedDelta)
        )

        constructor(tableName: String, columnName: String, toleratedDelta: BigDecimal, isPercentage: Boolean) : this(
            tableName,
            columnName,
            Precision(toleratedDelta, isPercentage)
        )

        fun matches(tableName: String, columnName: String): Boolean {
            return this.tableName == tableName && this.columnName == columnName
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("tableName=").append(tableName)
            sb.append(", columnName=").append(columnName)
            sb.append(", toleratedDelta=").append(toleratedDelta)
            return sb.toString()
        }
    }

    /**
     * Container for the tolerated delta of two values that are compared to each other.
     */
    class Precision @JvmOverloads constructor(delta: BigDecimal, percentage: Boolean = false) {
        val isPercentage: Boolean
        val delta: BigDecimal

        init {
            require(delta.compareTo(ZERO) >= 0) { "The given delta '$delta' must be >= 0" }
            this.delta = delta
            isPercentage = percentage
        }

        companion object {
            private val ZERO = BigDecimal("0.0")
        }
    }

    companion object {
        fun buildMapKey(tableName: String, columnName: String): String {
            return "$tableName.$columnName"
        }

        fun buildMapKey(delta: ToleratedDelta): String {
            return buildMapKey(delta.tableName, delta.columnName)
        }
    }
}