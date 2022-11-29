/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.exportdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.categories.LoadCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ExportDataViewModel
@Inject
constructor(
    private val loadCategoriesUseCase: LoadCategoriesUseCase,
    private val exportDataUseCase: ExportDataUseCase
) : ViewModel() {

    private val _allCategoryStates = MutableLiveData<ArrayList<ExportDataSelectionItem>>()

    val allCategoryStates: LiveData<ArrayList<ExportDataSelectionItem>>
        get() = _allCategoryStates

    fun setCategoryStates(categoryStates: ArrayList<ExportDataSelectionItem>) {
        _allCategoryStates.postValue(categoryStates)
    }

    private val allCategoryStateMap =
        mapOf(
            HealthDataCategory.ACTIVITY to
                ExportDataSelectionItem(HealthDataCategory.ACTIVITY, true, R.id.activity_check_box),
            HealthDataCategory.BODY_MEASUREMENTS to
                ExportDataSelectionItem(
                    HealthDataCategory.BODY_MEASUREMENTS, true, R.id.body_measurements_check_box),
            HealthDataCategory.CYCLE_TRACKING to
                ExportDataSelectionItem(
                    HealthDataCategory.CYCLE_TRACKING, true, R.id.cycle_tracking_check_box),
            HealthDataCategory.NUTRITION to
                ExportDataSelectionItem(
                    HealthDataCategory.NUTRITION, true, R.id.nutrition_check_box),
            HealthDataCategory.SLEEP to
                ExportDataSelectionItem(HealthDataCategory.SLEEP, true, R.id.sleep_check_box),
            HealthDataCategory.VITALS to
                ExportDataSelectionItem(HealthDataCategory.VITALS, true, R.id.vitals_check_box))

    fun setAllCategoriesChecked(isChecked: Boolean) {
        for (index in 0 until _allCategoryStates.value!!.size) {
            val item = _allCategoryStates.value!![index]
            item.selected = isChecked
            _allCategoryStates.value!![index] = item
        }
        _allCategoryStates.value = _allCategoryStates.value
    }

    init {
        loadCategoryStates()
    }

    private fun loadCategoryStates() {
        viewModelScope.launch {
            val availableCategories = loadCategoriesUseCase.invoke()

            val allCategoryStates = arrayListOf<ExportDataSelectionItem>()
            for (category in availableCategories) {
                allCategoryStates.add(allCategoryStateMap[category]!!)
            }
            _allCategoryStates.postValue(allCategoryStates)
        }
    }

    fun startExport() {
        val exportParameters =
            _allCategoryStates.value!!
                .filter { currentState -> currentState.selected }
                .map { cs -> cs.category }
        viewModelScope.launch { exportDataUseCase.invoke(exportParameters) }
    }
}
