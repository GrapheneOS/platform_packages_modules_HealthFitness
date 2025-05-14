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
import com.android.healthconnect.testapps.toolbox.R

class MealTypeFormatter {

    fun format(mealType: Int, context: Context): String? {
        return when (mealType) {
            MEAL_TYPE_BREAKFAST -> context.getString(R.string.meal_type_breakfast)
            MEAL_TYPE_DINNER -> context.getString(R.string.meal_type_dinner)
            MEAL_TYPE_LUNCH -> context.getString(R.string.meal_type_lunch)
            MEAL_TYPE_SNACK -> context.getString(R.string.meal_type_snack)
            else -> null
        }
    }
}
