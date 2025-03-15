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


import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.Constants.HealthDataCategory
import com.android.healthconnect.testapps.toolbox.read.components.DataTypeListScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DataTypeListTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp(){
        composeTestRule.setContent {
            DataTypeListScreen(
                onNavigateToDataTypeDetailsScreen = {}
            )
        }
    }

    @Test
    fun allCategoriesDisplayed(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val categoryNodes = composeTestRule.onAllNodesWithTag("category")


        categoryNodes
            .filterToOne(hasText(context.getString(R.string.activity_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.body_measurements_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.sleep_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.vitals_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.cycle_tracking_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.nutrition_category)))
            .performScrollTo()
            .assertIsDisplayed()
        categoryNodes
            .filterToOne(hasText(context.getString(R.string.wellness_category)))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun allDataTypesDisplayed_activity(){

        assertDataTypesDisplayed(HealthDataCategory.ACTIVITY)
    }

    @Test
    fun allDataTypesDisplayed_bodyMeasurements(){

        assertDataTypesDisplayed(HealthDataCategory.BODY_MEASUREMENTS)
    }

    @Test
    fun allDataTypesDisplayed_sleep(){

        assertDataTypesDisplayed(HealthDataCategory.SLEEP)
    }

    @Test
    fun allDataTypesDisplayed_vitals(){

        assertDataTypesDisplayed(HealthDataCategory.VITALS)
    }

    @Test
    fun allDataTypesDisplayed_cycleTracking(){

        assertDataTypesDisplayed(HealthDataCategory.CYCLE_TRACKING)
    }

    @Test
    fun allDataTypesDisplayed_nutrition(){

        assertDataTypesDisplayed(HealthDataCategory.NUTRITION)
    }

    @Test
    fun allDataTypesDisplayed_wellness(){

        assertDataTypesDisplayed(HealthDataCategory.WELLNESS)
    }

    private fun assertDataTypesDisplayed(category: HealthDataCategory){

        val context: Context = ApplicationProvider.getApplicationContext()
        val dataTypes = category.healthPermissionTypes

        for(dataType in dataTypes){
            val dataTypeLabel = dataType.title

            composeTestRule
                .onNode(hasText(context.getString(dataTypeLabel)) and hasTestTag("dataType"))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }
}
