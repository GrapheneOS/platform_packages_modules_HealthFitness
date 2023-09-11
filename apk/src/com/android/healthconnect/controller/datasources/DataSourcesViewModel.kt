/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.datasources

import android.health.connect.HealthDataCategory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.datasources.api.LoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.LoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataSourcesViewModel
@Inject
constructor(
    private val loadDatesWithDataUseCase: LoadMostRecentAggregationsUseCase,
    private val loadAppSourcesUseCase: LoadPotentialPriorityListUseCase
): ViewModel() {

    companion object {
        private const val TAG = "DataSourcesViewModel"
    }
    private val _aggregationCardsData = MutableLiveData<AggregationCardsState>()

    val aggregationCardsData: LiveData<AggregationCardsState>
        get() = _aggregationCardsData

    private val _potentialAppSources = MutableLiveData<PotentialAppSourcesState>()

    val potentialAppSources: LiveData<PotentialAppSourcesState>
        get() = _potentialAppSources

    private var currentSelection = HealthDataCategory.ACTIVITY

    fun getCurrentSelection(): Int = currentSelection

    fun setCurrentSelection(category: @HealthDataCategoryInt Int) {
        currentSelection = category
    }

    fun loadMostRecentAggregations() {
        _aggregationCardsData.postValue(AggregationCardsState.Loading)
        viewModelScope.launch {
            when (val aggregationInfoResult = loadDatesWithDataUseCase.invoke()) {
                is UseCaseResults.Success -> {
                    _aggregationCardsData.postValue(AggregationCardsState.WithData(aggregationInfoResult.data))
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Failed loading dates with data ", aggregationInfoResult.exception)
                    _aggregationCardsData.postValue(AggregationCardsState.LoadingFailed)
                }
            }
        }
    }

    fun loadPotentialAppSources(category: Int) {
        _potentialAppSources.postValue(PotentialAppSourcesState.Loading)
        viewModelScope.launch {
            when (val appSourcesResult = loadAppSourcesUseCase.invoke(category)) {
                is UseCaseResults.Success ->
                    _potentialAppSources.postValue(PotentialAppSourcesState.WithData(appSourcesResult.data))
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Failed to load possible priority list candidates", appSourcesResult.exception)
                    _potentialAppSources.postValue(PotentialAppSourcesState.LoadingFailed)
                }

            }
        }
    }

    sealed class AggregationCardsState {
        object Loading: AggregationCardsState()
        object LoadingFailed: AggregationCardsState()
        data class WithData(val dataTotals: List<AggregationCardInfo>) : AggregationCardsState()
    }

    sealed class PotentialAppSourcesState {
        object Loading: PotentialAppSourcesState()
        object LoadingFailed: PotentialAppSourcesState()
        data class WithData(val appSources: List<AppMetadata>): PotentialAppSourcesState()
    }

}