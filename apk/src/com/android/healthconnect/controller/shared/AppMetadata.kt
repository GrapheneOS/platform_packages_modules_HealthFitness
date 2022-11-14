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
package com.android.healthconnect.controller.shared

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R

/** Represents an app being displayed in Health Connect. */
data class AppMetadata(@StringRes val appName: Int, @DrawableRes val icon: Int)

// Placeholder constants
val APP_1 = AppMetadata(R.string.app_1, R.drawable.ic_sleep)
val APP_2 = AppMetadata(R.string.app_2, R.drawable.ic_health_data)
// Placeholder constants
val APP_3 = AppMetadata(R.string.app_3, R.drawable.ic_cycle_tracking)
val APP_4 = AppMetadata(R.string.app_4, R.drawable.ic_vitals)
