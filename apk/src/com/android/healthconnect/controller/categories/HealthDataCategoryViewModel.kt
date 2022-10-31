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
package com.android.healthconnect.controller.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HealthDataCategoryViewModel
@Inject
constructor(
    private val loadCategoriesUseCase: LoadCategoriesUseCase,
    private val loadAllCategoriesUseCase: LoadAllCategoriesUseCase,
) : ViewModel() {

    private val _categoriesData = MutableLiveData<List<HealthDataCategory>>()

    private val _allCategoriesData = MutableLiveData<List<AllCategoriesScreenHealthDataCategory>>()
    /**
     * Provides a list of HealthDataCategories displayed in {@link HealthDataCategoriesFragment}.
     */
    val categoriesData: LiveData<List<HealthDataCategory>>
        get() = _categoriesData

    /**
     * Provides a list of all HealthDataCategories displayed in {@link
     * HealthDataAllCategoriesFragment}.
     */
    val allCategoriesData: LiveData<List<AllCategoriesScreenHealthDataCategory>>
        get() = _allCategoriesData

    init {
        loadDataCategories()
        loadAllDataCategories()
    }

    private fun loadDataCategories() {
        viewModelScope.launch { _categoriesData.postValue(loadCategoriesUseCase.invoke()) }
    }

    private fun loadAllDataCategories() {
        viewModelScope.launch { _allCategoriesData.postValue(loadAllCategoriesUseCase.invoke()) }
    }
}
