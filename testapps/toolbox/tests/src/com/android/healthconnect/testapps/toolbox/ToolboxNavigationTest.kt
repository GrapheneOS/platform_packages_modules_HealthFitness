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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.newUi.navigation.ToolboxNavigation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolboxNavigationTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun onDataTypeListClick_navigatesTo_ReadDataScreen() {

        composeTestRule.setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                ToolboxNavigation(
                    navController = rememberNavController(),
                    scaffoldPadding = innerPadding,
                )
            }
        }

        // Navigates to new home page.
        composeTestRule.onNodeWithText("Read Data").performClick()
        composeTestRule.waitForIdle()
        // Navigates to steps details page.
        composeTestRule.onNodeWithText("Steps").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Steps").assertIsDisplayed()
    }
}
