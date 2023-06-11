package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import org.testaco.dataset.datatype.ToleratedDeltaMap.Precision
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Extended version of the [NumberDataType]. Extends the
 * [.compare] method in order to respect precision tolerance.
 * This is comparable to the JUnit method
 * `assert(double val1, double val2, double toleratedDelta)`.
 */
class NumberTolerantDataType internal constructor(name: String?, sqlType: Int, val toleratedDelta: Precision)
    : NumberDataType(name!!, sqlType) {

    /**
     * The only method overwritten from the base implementation to compare numbers allowing a tolerance
     * @see org.testaco.dataset.datatype.AbstractDataType.compareNonNulls
     */
    protected fun compareNonNulls(value1cast: Any, value2cast: Any?): Int {
        logger.debug("compareNonNulls(value1={}, value2={}) - start", value1cast, value2cast)
        return if (value1cast is BigDecimal && value2cast is BigDecimal) {
            val bdValue1 = value1cast
            val bdValue2 = value2cast
            val diff = bdValue1.subtract(bdValue2)
            // Exact match
            if (isZero(diff)) {
                return 0
            }
            val toleratedDeltaValue = toleratedDelta.delta
            if (!toleratedDelta.isPercentage) {
                if (diff.abs().compareTo(toleratedDeltaValue) <= 0) {
                    // within tolerance delta, so accept
                    if (logger.isDebugEnabled) logger.debug(
                        "Values val1={}, val2={} differ but are within tolerated delta {}",
                        *arrayOf<Any>(bdValue1, bdValue2, toleratedDeltaValue)
                    )
                    0
                } else {
                    // TODO it would be beautiful to report a precise description about difference and tolerated delta values in the assertion
                    // Therefore think about introducing a method "DataType.getCompareInfo()"
                    diff.signum()
                }
            } else {
                // percentage comparison
                val scale = toleratedDeltaValue.scale() + 2
                val toleratedValue =
                    bdValue1.multiply(toleratedDeltaValue.divide(C_100, scale, RoundingMode.HALF_UP))
                if (diff.abs().compareTo(toleratedValue) <= 0) {
                    // within tolerance delta, so accept
                    if (logger.isDebugEnabled) logger.debug(
                        "Values val1={}, val2={} differ but are within tolerated delta {}",
                        *arrayOf<Any>(bdValue1, bdValue2, toleratedValue)
                    )
                    0
                } else {
                    // TODO it would be beautiful to report a precise description about difference and tolerated delta values in the assertion
                    // Therefore think about introducing a method "DataType.getCompareInfo()"
                    diff.signum()
                }
            }
        } else {
            val value1 = value1cast as BigDecimal
            val value2 = value2cast as BigDecimal?
            value1.compareTo(value2)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NumberTolerantDataType::class.java)
        private val C_100 = BigDecimal("100")

        fun isZero(value: BigDecimal): Boolean {
            return value.signum() == 0
        }
    }
}
