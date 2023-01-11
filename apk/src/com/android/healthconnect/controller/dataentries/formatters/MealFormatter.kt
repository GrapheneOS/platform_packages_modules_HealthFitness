package com.android.healthconnect.controller.dataentries.formatters

import android.content.Context
import android.healthconnect.datatypes.MealType
import android.healthconnect.datatypes.MealType.MealTypes
import com.android.healthconnect.controller.R

object MealFormatter {
    fun formatMealType(context: Context, @MealTypes mealType: Int): String {
        return when (mealType) {
            MealType.MEAL_TYPE_UNKNOWN -> return context.getString(R.string.mealtype_unknown)
            MealType.MEAL_TYPE_BREAKFAST -> return context.getString(R.string.mealtype_breakfast)
            MealType.MEAL_TYPE_LUNCH -> return context.getString(R.string.mealtype_lunch)
            MealType.MEAL_TYPE_DINNER -> return context.getString(R.string.mealtype_dinner)
            MealType.MEAL_TYPE_SNACK -> return context.getString(R.string.mealtype_snack)
            else -> {
                throw IllegalArgumentException("Unrecognised meal type $mealType")
            }
        }
    }
}
