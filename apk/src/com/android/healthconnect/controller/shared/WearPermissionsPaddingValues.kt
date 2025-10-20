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
 *
 *
 */
package com.android.healthconnect.controller.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/** An object to centralize hardcoded padding values for Wear Permissions Compose UI. */
object WearPermissionsPaddingValues {
    // Padding values
    private val PADDING_XXSMALL = 2.dp
    private val PADDING_XSMALL = 6.dp
    private val PADDING_SMALL = 8.dp
    private val PADDING_NORMAL = 12.dp
    private val PADDING_LARGE = 16.dp

    // Headers
    val dataTypeCategoryHeaderPaddingValues =
        PaddingValues(start = PADDING_NORMAL, bottom = PADDING_SMALL)

    val allowedAppsHeaderPaddingValues =
        PaddingValues(start = PADDING_NORMAL, bottom = PADDING_XSMALL)

    val allowedToReadHeaderPaddingValues =
        PaddingValues(start = PADDING_NORMAL, top = PADDING_SMALL, bottom = PADDING_SMALL)

    val allowedToAccessHeaderPaddingValues = PaddingValues(start = PADDING_NORMAL)

    val singleAppDataTypeHeaderPaddingValues = PaddingValues(all = PADDING_SMALL)

    // Descriptions and texts
    val backgroundModeDescriptionPaddingValues =
        PaddingValues(start = PADDING_NORMAL, top = PADDING_SMALL)

    val permissionSensorAccessNotePaddingValues =
        PaddingValues(start = PADDING_NORMAL, top = PADDING_SMALL, bottom = PADDING_SMALL)

    val givePermissionPromptPaddingValues =
        PaddingValues(start = PADDING_NORMAL, top = PADDING_SMALL, bottom = PADDING_LARGE)

    // Buttons
    val defaultButtonPaddingValues = PaddingValues(top = PADDING_SMALL)

    val additionalAccessButtonPaddingValues = PaddingValues(top = PADDING_SMALL)

    val removeAllAppsButtonPaddingValues =
        PaddingValues(
            start = PADDING_XXSMALL,
            top = PADDING_XXSMALL,
            end = PADDING_XXSMALL,
            bottom = PADDING_SMALL,
        )

    val showSystemAppsButtonPaddingValues =
        PaddingValues(start = PADDING_XXSMALL, end = PADDING_XXSMALL)
}
