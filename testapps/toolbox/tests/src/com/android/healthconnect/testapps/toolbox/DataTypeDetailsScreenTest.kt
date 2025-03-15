/**
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.testapps.toolbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.components.DataTypeDetailsScreen
import com.android.healthconnect.testapps.toolbox.read.navigation.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DataTypeDetailsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun checkTitle_isDisplayed(){
        val dataType = Constants.HealthPermissionType.STEPS
        val dataTypeDetails = Screen.DataTypeDetails(dataType = dataType)

        composeTestRule.setContent {
            DataTypeDetailsScreen(dataTypeDetails = dataTypeDetails)
        }

        composeTestRule
            .onNodeWithText("Steps")
            .assertIsDisplayed()
    }
}