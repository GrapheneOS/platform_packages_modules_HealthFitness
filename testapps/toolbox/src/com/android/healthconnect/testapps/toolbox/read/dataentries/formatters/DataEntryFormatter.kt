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
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry

class DataEntryFormatter(private val stepsFormatter: StepsFormatter = StepsFormatter()) {

    fun format(record: Record, context: Context): FormattedEntry {
        return when (record) {
            is StepsRecord -> stepsFormatter.format(record, context)
            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }
}
