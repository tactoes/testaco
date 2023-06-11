package org.testaco.util

import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 *
 * A parser for relative date time string.<br></br>
 * The basic format is `[now{diff...}{time}]`.<br></br>
 * 'diff' consists of two parts 1) a number with a leading plus or minus sign
 * and 2) a character represents temporal unit. See the table below for the
 * supported units. There can be multiple 'diff's and they can be specified in
 * any order.<br></br>
 * 'time' is a string that can be parsed by
 * `LocalTime#parse(). If specified, it is used instead of the current time.<br></br>
 * Both 'diff' and 'time' are optional.<br></br>
 * Whitespaces are allowed before and after each 'diff'.
` *
 * <h3>Unit</h3>
 *
 *  * y : years
 *  * M : months
 *  * d : days
 *  * h : hours
 *  * m : minutes
 *  * s : seconds
 *
 *
 *
 * Here are some examples.
 *
 *
 *  * `[now]` : current date time.
 *  * `[now-1d]` : the same time yesterday.
 *  * `[now+1y+1M-2h]` : a year and a month from today, two hours
 * earlier.
 *  * `[now+1d 10:00]` : 10 o'clock tomorrow.
 *
 */
class RelativeDateTimeParser @JvmOverloads constructor(
    private var clock: Clock = Clock.fixed(
        Instant.now(),
        ZoneId.systemDefault()
    )
) {
    private var now: LocalDateTime? = null

    init {
        cacheLocalDateTime(clock)
    }

    fun parse(input: String?): LocalDateTime? {
        if (input == null || input.isEmpty()) {
            throw IllegalArgumentException(
                "Relative datetime input must not be null or empty."
            )
        }
        val matcher = inputPattern.matcher(input)
        if (!matcher.matches()) {
            throw IllegalArgumentException(
                "'" + input
                        + "' does not match the expected pattern [now{diff}{time}]. "
                        + "Please see the data types documentation for the details. "
                        + "http://testaco.sourceforge.net/datatypes.html#relativedatetime"
            )
        }
        var datetime = initLocalDateTime(matcher)
        val diffStr = matcher.group(GROUP_DIFFS)
        if (diffStr.isEmpty()) {
            return datetime
        }
        val diffMatcher = diffPattern.matcher(diffStr)
        while (diffMatcher.find()) {
            val diff = diffMatcher.group()
            val amountLength = diff.length - 1
            val unit = resolveUnit(diff[amountLength])
            val amount = diff.substring(0, amountLength).toLong()
            datetime = datetime!!.plus(amount, unit)
        }
        return datetime
    }

    fun getClock(): Clock {
        return clock
    }

    fun setClock(clock: Clock) {
        this.clock = clock
        cacheLocalDateTime(clock)
    }

    private fun initLocalDateTime(matcher: Matcher): LocalDateTime? {
        val timeStr = matcher.group(GROUP_TIME)
        return if (timeStr.isEmpty()) {
            now
        } else {
            val time = LocalTime.parse(timeStr)
            LocalDateTime.of(now!!.toLocalDate(), time)
        }
    }

    private fun cacheLocalDateTime(clock: Clock) {
        now = LocalDateTime.now(clock)
    }

    companion object {
        private val inputPattern = Pattern.compile(
            "^\\[[nN][oO][wW]\\s*(([-+][0-9]+[yMdhms]\\s*)*)([0-9:]*)?\\]$"
        )
        private val GROUP_DIFFS = 1
        private val GROUP_TIME = 3
        private val diffPattern = Pattern.compile("([+-][0-9]+[yMdhms])")
        private fun resolveUnit(c: Char): TemporalUnit {
            return when (c) {
                'y' -> ChronoUnit.YEARS
                'M' -> ChronoUnit.MONTHS
                'd' -> ChronoUnit.DAYS
                'h' -> ChronoUnit.HOURS
                'm' -> ChronoUnit.MINUTES
                's' -> ChronoUnit.SECONDS
                else -> throw IllegalArgumentException(
                    "'" + c
                            + "' is not a valid unit. It has to be one of 'yMdhms'."
                )
            }
        }
    }
}
