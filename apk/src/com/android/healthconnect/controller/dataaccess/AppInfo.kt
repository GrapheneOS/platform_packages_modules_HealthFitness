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

package com.android.healthconnect.controller.dataaccess

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R

/** Represents an app with the necessary information to display across the Health Connect UI. */
// TODO(b/245513815): Add inactive status.
data class AppInfo(@StringRes val appName: Int, @DrawableRes val icon: Int)

// TODO(b/245513815): Remove these placeholder constants after API integration.
val APP_1 = AppInfo(R.string.app_1, R.drawable.ic_sleep)
val APP_2 = AppInfo(R.string.app_2, R.drawable.ic_health_data)
val APP_3 = AppInfo(R.string.app_3, R.drawable.ic_sleep)
val APP_4 = AppInfo(R.string.app_4, R.drawable.ic_health_data)
val EXAMPLE_APPS = listOf(APP_1, APP_2)
val MORE_EXAMPLE_APPS = listOf(APP_3, APP_4)
