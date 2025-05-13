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
 */
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters.shared

import android.content.Context
import android.health.connect.datatypes.MealType.MEAL_TYPE_BREAKFAST
import android.health.connect.datatypes.MealType.MEAL_TYPE_DINNER
import android.health.connect.datatypes.MealType.MEAL_TYPE_LUNCH
import android.health.connect.datatypes.MealType.MEAL_TYPE_SNACK
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealTypeFormatterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val formatter = MealTypeFormatter()

    @Test
    fun formatMealType_returnsCorrectString_breakfast() {
        val mealType = MEAL_TYPE_BREAKFAST
        val expectedMealType = "Breakfast"

        val actualMealType = formatter.format(mealType, context)

        assertThat(actualMealType).isEqualTo(expectedMealType)
    }

    @Test
    fun formatMealType_returnsCorrectString_lunch() {
        val mealType = MEAL_TYPE_LUNCH
        val expectedMealType = "Lunch"

        val actualMealType = formatter.format(mealType, context)

        assertThat(actualMealType).isEqualTo(expectedMealType)
    }

    @Test
    fun formatMealType_returnsCorrectString_dinner() {
        val mealType = MEAL_TYPE_DINNER
        val expectedMealType = "Dinner"

        val actualMealType = formatter.format(mealType, context)

        assertThat(actualMealType).isEqualTo(expectedMealType)
    }

    @Test
    fun formatMealType_returnsCorrectString_snack() {
        val mealType = MEAL_TYPE_SNACK
        val expectedMealType = "Snack"

        val actualMealType = formatter.format(mealType, context)

        assertThat(actualMealType).isEqualTo(expectedMealType)
    }

    @Test
    fun formatMealType_returnsNull_unknownType() {
        val mealType = 999

        val actualMealType = formatter.format(mealType, context)

        assertThat(actualMealType).isNull()
    }
}
