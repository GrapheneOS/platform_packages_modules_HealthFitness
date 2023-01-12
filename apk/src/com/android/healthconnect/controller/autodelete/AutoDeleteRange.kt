/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.autodelete

import com.android.healthconnect.controller.utils.TimeSource
import java.time.Instant
import java.time.LocalDateTime

/**
 * Specifies the range of auto-deletion, where data wil be automatically deleted from (no data gets
 * deleted in case of [AutoDeleteRange.AUTO_DELETE_RANGE_NEVER]).
 */
enum class AutoDeleteRange {
    AUTO_DELETE_RANGE_NEVER,
    AUTO_DELETE_RANGE_THREE_MONTHS,
    AUTO_DELETE_RANGE_EIGHTEEN_MONTHS
}

private const val QUANTITY_0_MONTHS = 0
private const val QUANTITY_3_MONTHS = 3
private const val QUANTITY_18_MONTHS = 18

/** Returns the number of months corresponding to the given [AutoDeleteRange]. */
fun numberOfMonths(range: AutoDeleteRange): Int {
    return when (range) {
        AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS -> QUANTITY_3_MONTHS
        AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS -> QUANTITY_18_MONTHS
        AutoDeleteRange.AUTO_DELETE_RANGE_NEVER -> QUANTITY_0_MONTHS
    }
}

/**
 * Returns [AutoDeleteRange] corresponding to the given number of months or throws in case of
 * unsupported number of months.
 */
fun fromNumberOfMonths(numberOfMonths: Int): AutoDeleteRange {
    if (numberOfMonths == QUANTITY_0_MONTHS) return AutoDeleteRange.AUTO_DELETE_RANGE_NEVER
    if (numberOfMonths == QUANTITY_3_MONTHS) return AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS
    if (numberOfMonths == QUANTITY_18_MONTHS)
        return AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS
    throw UnsupportedOperationException("Number of months is not supported: $numberOfMonths")
}

/** Returns the [Instant] that is the start of the [AutoDeleteRange]. */
fun autoDeleteRangeStart(): Instant {
    return Instant.EPOCH
}

/**
 * Returns the [Instant] that is the end of the [AutoDeleteRange] or throws in case of
 * [AutoDeleteRange.AUTO_DELETE_RANGE_NEVER].
 */
fun autoDeleteRangeEnd(timeSource: TimeSource, range: AutoDeleteRange): Instant {
    val localDateTimeNow: LocalDateTime = timeSource.currentLocalDateTime()
    when (range) {
        AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS,
        AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS -> {
            val exactlyXMonthsAgo: LocalDateTime =
                localDateTimeNow.minusMonths(numberOfMonths(range).toLong())
            return exactlyXMonthsAgo
                .toLocalDate()
                .atStartOfDay(timeSource.deviceZoneOffset())
                .toInstant()
        }
        AutoDeleteRange.AUTO_DELETE_RANGE_NEVER -> {
            throw UnsupportedOperationException("Invalid auto-delete range to delete data.")
        }
    }
}
