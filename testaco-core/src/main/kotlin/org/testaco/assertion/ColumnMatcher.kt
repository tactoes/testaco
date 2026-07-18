/*
 * Copyright 2026 Geir Hedemark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testaco.assertion

import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Base class for flexible column value matching in expected datasets.
 *
 * Supports:
 * - `~ignore` — Skip this column in comparison
 * - `~now` — Match current timestamp (±default tolerance)
 * - `~now±PT10S` — Match current timestamp ±10 seconds
 * - `~regex:.*pattern.*` — Match string against regex
 * - Any other value — Exact match (numeric comparison handles precision)
 *
 * Use in expected JSON datasets:
 * ```json
 * {
 *   "orders": [
 *     {
 *       "id": 1,
 *       "created_at": "~now",
 *       "updated_by": "~ignore"
 *     }
 *   ]
 * }
 * ```
 */
sealed class ColumnMatcher {

    /**
     * Tests if an actual database value matches this matcher's expectations.
     *
     * @param actual The value from the database
     * @return true if the actual value matches
     */
    abstract fun matches(actual: Any?): Boolean

    /**
     * Describes the mismatch between expected and actual values.
     *
     * @param actual The value that didn't match
     * @return A human-readable description of the mismatch
     */
    abstract fun describe(actual: Any?): String

    companion object {
        /**
         * Parses the expected JSON value into the appropriate matcher.
         *
         * @param expected The expected value from the JSON dataset
         * @param defaultTolerance Default time tolerance for ~now matching
         * @return A ColumnMatcher for this value
         */
        fun forExpected(expected: Any?, defaultTolerance: Duration): ColumnMatcher {
            if (expected is String) {
                if (expected == "~ignore") return IgnoreMatcher
                if (expected == "~now") return NowMatcher(defaultTolerance)
                if (expected.startsWith("~now±")) {
                    val tolerance = Duration.parse(expected.removePrefix("~now±"))
                    return NowMatcher(tolerance)
                }
                if (expected.startsWith("~regex:")) {
                    val pattern = expected.removePrefix("~regex:")
                    return RegexMatcher(Regex(pattern))
                }
            }
            return ExactMatcher(expected)
        }
    }
}

/** Matcher that always succeeds and skips the column. */
object IgnoreMatcher : ColumnMatcher() {
    override fun matches(actual: Any?) = true
    override fun describe(actual: Any?) = "ignored"
}

/**
 * Matches timestamps within a tolerance.
 *
 * @param tolerance How far the timestamp can deviate from now
 */
class NowMatcher(private val tolerance: Duration) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        val timestamp = when (actual) {
            is OffsetDateTime -> actual
            is java.sql.Timestamp -> actual.toInstant().atOffset(ZoneOffset.UTC)
            is String -> OffsetDateTime.parse(actual)
            else -> return false
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val diff = Duration.between(timestamp, now).abs()
        return diff <= tolerance
    }

    override fun describe(actual: Any?) = "expected ~now (±${tolerance}), got $actual"
}

class RegexMatcher(private val pattern: Regex) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        return actual?.toString()?.let { pattern.matches(it) } ?: false
    }

    override fun describe(actual: Any?) = "expected ~regex:${pattern.pattern}, got $actual"
}

class ExactMatcher(private val expected: Any?) : ColumnMatcher() {
    override fun matches(actual: Any?): Boolean {
        if (expected == null && actual == null) return true
        if (expected == null || actual == null) return false
        if (expected is Number && actual is Number) {
            return BigDecimal(expected.toString()).compareTo(BigDecimal(actual.toString())) == 0
        }
        return expected == actual
    }

    override fun describe(actual: Any?) = "expected $expected, got $actual"
}
