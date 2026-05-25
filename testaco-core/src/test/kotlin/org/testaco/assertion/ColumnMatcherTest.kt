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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ColumnMatcherTest : FunSpec({

    test("exact matcher matches equal values") {
        val matcher = ColumnMatcher.forExpected("hello", Duration.ofSeconds(5))
        matcher.matches("hello") shouldBe true
        matcher.matches("world") shouldBe false
    }

    test("exact matcher matches null") {
        val matcher = ColumnMatcher.forExpected(null, Duration.ofSeconds(5))
        matcher.matches(null) shouldBe true
        matcher.matches("nope") shouldBe false
    }

    test("~ignore always matches") {
        val matcher = ColumnMatcher.forExpected("~ignore", Duration.ofSeconds(5))
        matcher.matches("anything") shouldBe true
        matcher.matches(null) shouldBe true
    }

    test("~now matches timestamp near current time") {
        val matcher = ColumnMatcher.forExpected("~now", Duration.ofSeconds(5))
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        matcher.matches(now) shouldBe true
    }

    test("~now rejects timestamp far from current time") {
        val matcher = ColumnMatcher.forExpected("~now", Duration.ofSeconds(5))
        val old = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        matcher.matches(old) shouldBe false
    }

    test("~now with custom tolerance") {
        val matcher = ColumnMatcher.forExpected("~now±PT30S", Duration.ofSeconds(5))
        val recent = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(20)
        matcher.matches(recent) shouldBe true
    }

    test("~regex matches pattern") {
        val matcher = ColumnMatcher.forExpected("~regex:[A-Z]{3}-\\d+", Duration.ofSeconds(5))
        matcher.matches("ABC-123") shouldBe true
        matcher.matches("abc-123") shouldBe false
    }

    test("numeric comparison handles BigDecimal vs Int") {
        val matcher = ColumnMatcher.forExpected(99.99, Duration.ofSeconds(5))
        matcher.matches(java.math.BigDecimal("99.99")) shouldBe true
    }
})
