/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.testapps.toolbox.read.dataentries.utils

import android.content.Context
import com.android.healthconnect.testapps.toolbox.R
import java.time.Duration
import java.util.StringJoiner

class DurationFormatter {

    companion object {

        /**
         * Takes in a type Duration and formats into a string
         *
         * The smallest precision that this class can supports is seconds, any smaller like
         * nanoseconds are truncated. Largest unit the class supports are days.
         *
         * @param duration The duration to be formatted.
         * @param context The context that needs to be passed to use string res.
         * @return A String representation of the given duration of format "D days H hrs M mins S
         *   seconds"
         */
        fun format(duration: Duration, context: Context): String {
            val durationString = StringJoiner(" ")
            durationString.setEmptyValue(context.getString(R.string.zero_seconds))

            val days = duration.toDays()
            val hours = duration.toHoursPart()
            val minutes = duration.toMinutesPart()
            val seconds = duration.toSecondsPart()

            if (days.toInt() != 0) {
                durationString.add("$days ${context.getString(R.string.days)}")
            }
            if (hours != 0) {
                durationString.add("$hours ${context.getString(R.string.hour_label)}")
            }
            if (minutes != 0) {
                durationString.add("$minutes ${context.getString(R.string.minute_label)}")
            }
            if (seconds != 0) {
                durationString.add("$seconds ${context.getString(R.string.seconds)}")
            }
            return durationString.toString()
        }
    }
}
