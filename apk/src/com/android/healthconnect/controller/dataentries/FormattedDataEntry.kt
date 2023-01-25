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
package com.android.healthconnect.controller.dataentries

import com.android.healthconnect.controller.shared.DataType
import java.time.Instant

sealed class FormattedEntry(open val uuid: String, open val startTime: Instant) {
    data class FormattedDataEntry(
        override val uuid: String,
        override val startTime: Instant,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        val dataType: DataType
    ) : FormattedEntry(uuid, startTime)

    data class FormattedSessionEntry(
        override val uuid: String,
        override val startTime: Instant,
        val header: String,
        val headerA11y: String,
        val title: String,
        val titleA11y: String,
        val dataType: DataType,
        val notes: String?
    ) : FormattedEntry(uuid, startTime)
}
