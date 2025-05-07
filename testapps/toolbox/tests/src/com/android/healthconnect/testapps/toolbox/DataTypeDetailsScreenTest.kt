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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.components.DataTypeDetailsScreen
import com.android.healthconnect.testapps.toolbox.read.navigation.Screen
import com.android.healthconnect.testapps.toolbox.viewmodels.DataState
import com.android.healthconnect.testapps.toolbox.viewmodels.LoadEntriesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class DataTypeDetailsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun checkTitle_isDisplayed() {
        val dataType = Constants.HealthPermissionType.STEPS
        val dataTypeDetails = Screen.DataTypeDetails(dataType = dataType)

        composeTestRule.setContent { DataTypeDetailsScreen(dataTypeDetails = dataTypeDetails) }

        composeTestRule.onNodeWithText("Steps").assertIsDisplayed()
    }

    @Test
    fun onLoadingState_loadingBarIsDisplayed() {

        val mockViewModel = mock<LoadEntriesViewModel>()
        val dataType = Constants.HealthPermissionType.STEPS
        val screenState = MutableStateFlow<DataState>(DataState.Loading)
        mockViewModel.stub { on { entriesState } doReturn screenState }

        composeTestRule.setContent {
            DataTypeDetailsScreen(
                dataTypeDetails = Screen.DataTypeDetails(dataType),
                viewModel = mockViewModel,
            )
        }

        composeTestRule.onNodeWithTag("loadingBar").assertExists()
        composeTestRule.onNodeWithTag("errorMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("entriesList").assertDoesNotExist()
    }

    @Test
    fun onErrorState_errorMessageIsDisplayed() {

        val mockViewModel = mock<LoadEntriesViewModel>()
        val dataType = Constants.HealthPermissionType.STEPS
        val errorMessage = "Test exception"
        val screenState = MutableStateFlow<DataState>(DataState.Error(Exception(errorMessage)))
        mockViewModel.stub { on { entriesState } doReturn screenState }

        composeTestRule.setContent {
            DataTypeDetailsScreen(
                dataTypeDetails = Screen.DataTypeDetails(dataType),
                viewModel = mockViewModel,
            )
        }

        composeTestRule.onNodeWithTag("loadingBar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("errorMessage").assertExists()
        composeTestRule.onNodeWithTag("entriesList").assertDoesNotExist()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun onSuccessState_recordListIsDisplayed() {

        val mockViewModel = mock<LoadEntriesViewModel>()
        val dataType = Constants.HealthPermissionType.STEPS
        val screenState = MutableStateFlow<DataState>(DataState.Success(emptyList()))
        mockViewModel.stub { on { entriesState } doReturn screenState }

        composeTestRule.setContent {
            DataTypeDetailsScreen(
                dataTypeDetails = Screen.DataTypeDetails(dataType),
                viewModel = mockViewModel,
            )
        }

        composeTestRule.onNodeWithTag("loadingBar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("errorMessage").assertDoesNotExist()
        composeTestRule.onNodeWithTag("entriesList").assertExists()
    }
}
