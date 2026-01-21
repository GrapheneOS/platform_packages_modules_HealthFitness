/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.utils

import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord

/**
 * Returns a string representation of the [Record]. Workaround for the missing toString
 * implementations in records.
 */
internal fun Record.asString(): String = buildString {
    append(this@asString.javaClass.simpleName)
    append('(')
    append(metadata.asString())
    append(", ")
    append(timesAsString())
    append(", ")
    append(dataAsString())
    append(')')
}

private fun Metadata.asString(): String =
    "packageName=${dataOrigin.packageName}, deviceUdi=${device.udi}}"

private fun Record.timesAsString(): String =
    when (this) {
        is InstantRecord -> "time=$time"
        is IntervalRecord -> "startTime=$startTime, endTime=$endTime"
        else -> ""
    }

private fun Record.dataAsString(): String =
    when (this) {
        is StepsRecord -> "count=$count"
        else -> ""
    }
