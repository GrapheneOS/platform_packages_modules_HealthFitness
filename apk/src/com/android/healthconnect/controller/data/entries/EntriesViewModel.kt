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
package com.android.healthconnect.controller.data.entries

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.TOTAL_CALORIES_BURNED
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for [AppEntriesFragment] and [EntriesFragment]. */
@HiltViewModel
class EntriesViewModel
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val loadDataEntriesUseCase: LoadDataEntriesUseCase,
    private val loadMenstruationDataUseCase: LoadMenstruationDataUseCase,
    private val loadDataAggregationsUseCase: LoadDataAggregationsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "DataEntriesFragmentView"
        private val AGGREGATE_HEADER_DATA_TYPES = listOf(STEPS, DISTANCE, TOTAL_CALORIES_BURNED)
    }

    private val _appEntries = MutableLiveData<EntriesFragmentState>()
    val appEntries: LiveData<EntriesFragmentState>
        get() = _appEntries

    val currentSelectedDate = MutableLiveData<Instant>()
    val period = MutableLiveData<DateNavigationPeriod>()

    private val _appInfo = MutableLiveData<AppMetadata>()
    val appInfo: LiveData<AppMetadata>
        get() = _appInfo

    fun loadEntries(
        permissionType: HealthPermissionType,
        selectedDate: Instant,
        period: DateNavigationPeriod
    ) {
        loadData(permissionType, packageName = null, selectedDate, period, showDataOrigin = true)
    }

    fun loadEntries(
        permissionType: HealthPermissionType,
        packageName: String,
        selectedDate: Instant,
        period: DateNavigationPeriod
    ) {
        loadData(permissionType, packageName, selectedDate, period, showDataOrigin = false)
    }

    private fun loadData(
        permissionType: HealthPermissionType,
        packageName: String?,
        selectedDate: Instant,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean
    ) {
        _appEntries.postValue(EntriesFragmentState.Loading)
        currentSelectedDate.postValue(selectedDate)
        this.period.postValue(period)

        viewModelScope.launch {
            val list = ArrayList<FormattedEntry>()
            val entriesResults =
                when (permissionType) {
                    // Special-casing Menstruation as it spans multiple days
                    HealthPermissionType.MENSTRUATION -> {
                        loadMenstruation(packageName, selectedDate, period, showDataOrigin)
                    }
                    else -> {
                        loadAppEntries(
                            permissionType, packageName, selectedDate, period, showDataOrigin)
                    }
                }
            when (entriesResults) {
                is UseCaseResults.Success -> {
                    list.addAll(entriesResults.data)
                    if (list.isEmpty()) {
                        _appEntries.postValue(EntriesFragmentState.Empty)
                    } else {
                        addAggregation(
                            permissionType, packageName, selectedDate, period, list, showDataOrigin)
                        _appEntries.postValue(EntriesFragmentState.With(list))
                    }
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Loading error ", entriesResults.exception)
                    _appEntries.postValue(EntriesFragmentState.LoadingFailed)
                }
            }
        }
    }

    private suspend fun loadAppEntries(
        permissionType: HealthPermissionType,
        packageName: String?,
        selectedDate: Instant,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean
    ): UseCaseResults<List<FormattedEntry>> {
        val input =
            LoadDataEntriesInput(permissionType, packageName, selectedDate, period, showDataOrigin)
        return loadDataEntriesUseCase.invoke(input)
    }

    private suspend fun loadMenstruation(
        packageName: String?,
        selectedDate: Instant,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean
    ): UseCaseResults<List<FormattedEntry>> {
        val input = LoadMenstruationDataInput(packageName, selectedDate, period, showDataOrigin)
        return loadMenstruationDataUseCase.invoke(input)
    }

    private suspend fun loadAggregation(
        permissionType: HealthPermissionType,
        packageName: String?,
        selectedDate: Instant,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean
    ): UseCaseResults<FormattedEntry.FormattedAggregation> {
        val input =
            LoadAggregationInput(permissionType, packageName, selectedDate, period, showDataOrigin)
        return loadDataAggregationsUseCase.invoke(input)
    }

    fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appInfo.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    private suspend fun addAggregation(
        permissionType: HealthPermissionType,
        packageName: String?,
        selectedDate: Instant,
        period: DateNavigationPeriod,
        list: ArrayList<FormattedEntry>,
        showDataOrigin: Boolean
    ) {
        if (permissionType in AGGREGATE_HEADER_DATA_TYPES) {
            when (val aggregationResult =
                loadAggregation(
                    permissionType, packageName, selectedDate, period, showDataOrigin)) {
                is UseCaseResults.Success -> {
                    list.add(0, aggregationResult.data)
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Failed to load aggregation!", aggregationResult.exception)
                }
            }
        }
    }

    sealed class EntriesFragmentState {
        object Loading : EntriesFragmentState()

        object Empty : EntriesFragmentState()

        object LoadingFailed : EntriesFragmentState()

        data class With(val entries: List<FormattedEntry>) : EntriesFragmentState()
    }
}
