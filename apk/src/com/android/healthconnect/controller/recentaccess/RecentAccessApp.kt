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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.google.common.collect.ImmutableSet
import java.time.Instant

/** Represents one app that recently accessed health data. */
data class RecentAccessApp(
    @StringRes val appName: Int,
    @DrawableRes val icon: Int,
    val instantTime: Instant,
    val dataTypesWritten: ImmutableSet<String>
)

// Placeholder constants
val RECENT_APP_1 =
    RecentAccessApp(
        R.string.recent_app_1,
        R.drawable.ic_sleep,
        Instant.parse("2022-10-20T18:40:13.00Z"),
        ImmutableSet.of("Read"))

val RECENT_APP_2 =
    RecentAccessApp(
        R.string.recent_app_2,
        R.drawable.ic_health_data,
        Instant.parse("2022-10-20T19:53:14.00Z"),
        ImmutableSet.of("Read", "Write"))

val RECENT_ACCESS_APPS = listOf(RECENT_APP_1, RECENT_APP_2)
