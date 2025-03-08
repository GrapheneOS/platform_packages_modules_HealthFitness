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
import com.android.healthconnect.testapps.toolbox.read.components.DataTypeList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DataTypeListTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun allCategoriesDisplayed(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val categoryNodes = composeTestRule.onAllNodesWithTag("category")

        composeTestRule.setContent { DataTypeList() }

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
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.ACTIVITY)
    }

    @Test
    fun allDataTypesDisplayed_bodyMeasurements(){
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.BODY_MEASUREMENTS)
    }

    @Test
    fun allDataTypesDisplayed_sleep(){
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.SLEEP)
    }

    @Test
    fun allDataTypesDisplayed_vitals(){
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.VITALS)
    }

    @Test
    fun allDataTypesDisplayed_cycleTracking(){
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.CYCLE_TRACKING)
    }

    @Test
    fun allDataTypesDisplayed_nutrition(){
        composeTestRule.setContent { DataTypeList() }

        assertDataTypesDisplayed(HealthDataCategory.NUTRITION)
    }

    @Test
    fun allDataTypesDisplayed_wellness(){
        composeTestRule.setContent { DataTypeList() }

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
