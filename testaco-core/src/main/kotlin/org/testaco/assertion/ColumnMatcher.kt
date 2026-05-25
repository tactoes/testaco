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
 */package org.testaco.assertion

import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

sealed class ColumnMatcher {

    abstract fun matches(actual: Any?): Boolean
    abstract fun describe(actual: Any?): String

    companion object {
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

object IgnoreMatcher : ColumnMatcher() {
    override fun matches(actual: Any?) = true
    override fun describe(actual: Any?) = "ignored"
}

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
