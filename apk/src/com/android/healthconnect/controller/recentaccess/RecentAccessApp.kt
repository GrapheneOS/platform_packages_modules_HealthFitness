/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.recentaccess

import com.android.healthconnect.controller.shared.AppMetadata
import com.google.common.collect.ImmutableSet
import java.time.Instant

/** Represents one app that recently accessed health data. */
data class RecentAccessApp(
    val metadata: AppMetadata,
    val instantTime: Instant,
    val dataTypesWritten: ImmutableSet<String>
)

// Placeholder constants
val APP_1 = AppMetadata("package.name1", "app name A", null)

val APP_2 = AppMetadata("package.name2", "app name B", null)
val RECENT_APP_1 =
    RecentAccessApp(APP_1, Instant.parse("2022-10-20T18:40:13.00Z"), ImmutableSet.of("Read"))

val RECENT_APP_2 =
    RecentAccessApp(
        APP_2, Instant.parse("2022-10-20T19:53:14.00Z"), ImmutableSet.of("Read", "Write"))

val RECENT_ACCESS_APPS = listOf(RECENT_APP_1, RECENT_APP_2)
