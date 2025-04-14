/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.MealType
import com.android.healthconnect.controller.R

object MealFormatter {
    fun formatMealType(context: Context, mealType: Int): String {
        return when (mealType) {
            MealType.MEAL_TYPE_UNKNOWN -> context.getString(R.string.mealtype_unknown)
            MealType.MEAL_TYPE_BREAKFAST -> context.getString(R.string.mealtype_breakfast)
            MealType.MEAL_TYPE_LUNCH -> context.getString(R.string.mealtype_lunch)
            MealType.MEAL_TYPE_DINNER -> context.getString(R.string.mealtype_dinner)
            MealType.MEAL_TYPE_SNACK -> context.getString(R.string.mealtype_snack)
            else -> {
                throw IllegalArgumentException("Unrecognised meal type $mealType")
            }
        }
    }
}
