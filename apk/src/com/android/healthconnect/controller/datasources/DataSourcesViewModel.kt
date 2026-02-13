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

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthDataCategory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListInput
import com.android.healthconnect.controller.matchmaking.api.GetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.LoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.shared.usecase.LoadPriorityListUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.collections.filterNot
import kotlin.collections.find
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class DataSourcesViewModel
@Inject
constructor(
    private val loadDatesWithDataUseCase: BaseUseCase<Int, List<AggregationCardInfo>>,
    @LoadPotentialPriorityListUseCase
    private val loadPotentialAppSourcesUseCase:
        BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>,
    @LoadPriorityListUseCase
    private val loadPriorityListUseCase: BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>,
    private val updatePriorityListUseCase: BaseUseCase<UpdatePriorityListInput, Unit>,
    private val getDeviceDataSourcesInfoUseCase: GetDeviceDataSourcesInfoUseCase,
    private val appInfoReader: AppInfoReader,
) : ViewModel() {

    companion object {
        private const val TAG = "DataSourcesViewModel"
    }

    private val _aggregationCardsData = MutableLiveData<AggregationCardsState>()

    private val _updatedAggregationCardsData = MutableLiveData<AggregationCardsState>()

    // Used to control the reloading of the aggregation cards after reordering the priority list
    // To avoid reloading the whole screen when only the cards need updating
    // TODO (b/305907256) improve flow by observing the aggregationCardsData directly
    val updatedAggregationCardsData: LiveData<AggregationCardsState>
        get() = _updatedAggregationCardsData

    private val _potentialAppSources = MutableLiveData<PotentialAppSourcesState>()

    private val _shouldShowAddAnAppButton: MutableLiveData<Boolean> = MutableLiveData(false)

    // Used to make sure Add an app button appears when removing an item from the priority list
    val shouldShowAddAnAppButton: LiveData<Boolean>
        get() = _shouldShowAddAnAppButton

    private val _priorityListState = MutableLiveData<PriorityListState>()

    private val _deviceDataSourcesInfo = MutableLiveData<DeviceDataSourcesState>()

    private val _dataSourcesAndAggregationsInfo = MediatorLiveData<DataSourcesAndAggregationsInfo>()
    val dataSourcesAndAggregationsInfo: LiveData<DataSourcesAndAggregationsInfo>
        get() = _dataSourcesAndAggregationsInfo

    private val _dataSourcesInfo = MediatorLiveData<DataSourcesInfo>()
    val dataSourcesInfo: LiveData<DataSourcesInfo>
        get() = _dataSourcesInfo

    init {
        _dataSourcesAndAggregationsInfo.addSource(_priorityListState) { priorityListState ->
            if (!priorityListState.shouldObserve) {
                return@addSource
            }
            _dataSourcesAndAggregationsInfo.value =
                DataSourcesAndAggregationsInfo(
                    priorityListState = priorityListState,
                    potentialAppSourcesState = _potentialAppSources.value,
                    aggregationCardsState = _aggregationCardsData.value,
                )
        }
        _dataSourcesAndAggregationsInfo.addSource(_potentialAppSources) { potentialAppSourcesState
            ->
            if (!potentialAppSourcesState.shouldObserve) {
                return@addSource
            }
            _dataSourcesAndAggregationsInfo.value =
                DataSourcesAndAggregationsInfo(
                    priorityListState = _priorityListState.value,
                    potentialAppSourcesState = potentialAppSourcesState,
                    aggregationCardsState = _aggregationCardsData.value,
                )
        }
        _dataSourcesAndAggregationsInfo.addSource(_aggregationCardsData) { aggregationCardsState ->
            if (!aggregationCardsState.shouldObserve) {
                return@addSource
            }
            _dataSourcesAndAggregationsInfo.value =
                DataSourcesAndAggregationsInfo(
                    priorityListState = _priorityListState.value,
                    potentialAppSourcesState = _potentialAppSources.value,
                    aggregationCardsState = aggregationCardsState,
                )
        }

        _dataSourcesInfo.addSource(_priorityListState) { priorityListState ->
            _dataSourcesInfo.value =
                DataSourcesInfo(
                    priorityListState = priorityListState,
                    potentialAppSourcesState = _potentialAppSources.value,
                    deviceDataSourcesState = _deviceDataSourcesInfo.value,
                )
        }

        _dataSourcesInfo.addSource(_potentialAppSources) { potentialAppSourcesState ->
            _dataSourcesInfo.value =
                DataSourcesInfo(
                    priorityListState = _priorityListState.value,
                    potentialAppSourcesState = potentialAppSourcesState,
                    deviceDataSourcesState = _deviceDataSourcesInfo.value,
                )
        }

        if (deviceDataProvidersApi()) {
            _dataSourcesInfo.addSource(_deviceDataSourcesInfo) { deviceDataSourcesState ->
                _dataSourcesInfo.value =
                    DataSourcesInfo(
                        priorityListState = _priorityListState.value,
                        potentialAppSourcesState = _potentialAppSources.value,
                        deviceDataSourcesState = deviceDataSourcesState,
                    )
            }
        }
    }

    private var currentSelection = HealthDataCategory.ACTIVITY

    fun getCurrentSelection(): Int = currentSelection

    fun setCurrentSelection(category: @HealthDataCategoryInt Int) {
        currentSelection = category
    }

    fun loadData(category: @HealthDataCategoryInt Int) {
        loadMostRecentAggregations(category)
        loadCurrentPriorityList(category)
        loadPotentialAppSources(category)
        if (deviceDataProvidersApi()) {
            loadDeviceDataSourcesInfo()
        }
    }

    private fun loadMostRecentAggregations(category: @HealthDataCategoryInt Int) {
        _aggregationCardsData.postValue(AggregationCardsState.Loading(true))
        viewModelScope.launch {
            when (val aggregationInfoResult = loadDatesWithDataUseCase.invoke(category)) {
                is UseCaseResults.Success -> {
                    _aggregationCardsData.postValue(
                        AggregationCardsState.WithData(true, aggregationInfoResult.data)
                    )
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Failed loading dates with data ", aggregationInfoResult.exception)
                    _aggregationCardsData.postValue(AggregationCardsState.LoadingFailed(true))
                }
            }
        }
    }

    fun showAddAnAppButton() {
        _shouldShowAddAnAppButton.postValue(true)
    }

    fun loadPotentialAppSources(
        category: @HealthDataCategoryInt Int,
        shouldObserve: Boolean = true,
    ) {
        _shouldShowAddAnAppButton.postValue(false)
        _potentialAppSources.postValue(PotentialAppSourcesState.Loading(shouldObserve))
        viewModelScope.launch {
            when (val appSourcesResult = loadPotentialAppSourcesUseCase.invoke(category)) {
                is UseCaseResults.Success -> {
                    _potentialAppSources.postValue(
                        PotentialAppSourcesState.WithData(
                            shouldObserve,
                            appSourcesResult.data.filter {
                                !deviceDataProvidersApi() ||
                                    it.packageName != DEVICE_DATA_PROVIDER_PACKAGE
                            },
                        )
                    )
                }
                is UseCaseResults.Failed -> {
                    Log.e(
                        TAG,
                        "Failed to load possible priority list candidates",
                        appSourcesResult.exception,
                    )
                    _potentialAppSources.postValue(
                        PotentialAppSourcesState.LoadingFailed(shouldObserve)
                    )
                }
            }
        }
    }

    private fun loadCurrentPriorityList(
        category: @HealthDataCategoryInt Int,
        shouldObserve: Boolean = true,
    ) {
        _priorityListState.postValue(PriorityListState.Loading(shouldObserve))
        viewModelScope.launch {
            when (val result = loadPriorityListUseCase.invoke(category)) {
                is UseCaseResults.Success ->
                    _priorityListState.postValue(
                        if (result.data.isEmpty()) {
                            PriorityListState.WithData(shouldObserve, listOf())
                        } else {
                            PriorityListState.WithData(
                                shouldObserve,
                                result.data.filter {
                                    !deviceDataProvidersApi() ||
                                        it.packageName != DEVICE_DATA_PROVIDER_PACKAGE
                                },
                            )
                        }
                    )
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Load error ", result.exception)
                    _priorityListState.postValue(PriorityListState.LoadingFailed(shouldObserve))
                }
            }
        }
    }

    fun updatePriorityList(newPriorityList: List<String>, category: @HealthDataCategoryInt Int) {
        _priorityListState.postValue(PriorityListState.Loading(false))
        viewModelScope.launch {
            val mergedPriorityList = mergedCurrentDevicePriorityList(newPriorityList)
            updatePriorityListUseCase.invoke(UpdatePriorityListInput(mergedPriorityList, category))
            updateMostRecentAggregations(category)
            val appMetadataList: List<AppMetadata> =
                mergedPriorityList.map { appInfoReader.getAppMetadata(it) }
            _priorityListState.postValue(PriorityListState.WithData(false, appMetadataList))
        }
    }

    /**
     * Handles the transition from the legacy "android" package name to the current device's unique
     * identifier.
     *
     * Originally, all local records were labeled under the "android" package. With the move to
     * multi-device support, the local device now uses a unique synthetic package name. This method
     * ensures that records from both the old ("android") and new (synthetic package) packages are
     * grouped together during aggregation to maintain data consistency for long-time users.
     *
     * The anchor is the package with the new synthetic package name, as the "android" device will
     * be hidden from the UI.
     */
    private fun mergedCurrentDevicePriorityList(newPriorityList: List<String>): List<String> {
        // TODO(b/435165781): Remove method when "android" is migrated
        if (!deviceDataProvidersApi()) {
            return newPriorityList
        }

        val currentDeviceInfo = getCurrentDeviceInfo()
        if (currentDeviceInfo == null) {
            return newPriorityList
        }

        val currentDeviceSpn = currentDeviceInfo.deviceDataOrigin.packageName
        return if (!newPriorityList.contains(currentDeviceSpn)) {
            newPriorityList.filterNot { it == DEVICE_DATA_PROVIDER_PACKAGE }
        } else {
            newPriorityList.toMutableList().apply {
                add(indexOf(currentDeviceSpn), DEVICE_DATA_PROVIDER_PACKAGE)
            }
        }
    }

    private fun loadDeviceDataSourcesInfo() {
        _deviceDataSourcesInfo.postValue(DeviceDataSourcesState.Loading())
        viewModelScope.launch {
            when (val result = getDeviceDataSourcesInfoUseCase.invoke(Unit)) {
                is UseCaseResults.Success ->
                    _deviceDataSourcesInfo.postValue(DeviceDataSourcesState.WithData(result.data))
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Load error ", result.exception)
                    _deviceDataSourcesInfo.postValue(DeviceDataSourcesState.LoadingFailed())
                }
            }
        }
    }

    private fun updateMostRecentAggregations(category: @HealthDataCategoryInt Int) {
        _aggregationCardsData.postValue(AggregationCardsState.Loading(false))
        _updatedAggregationCardsData.postValue(AggregationCardsState.Loading(true))
        viewModelScope.launch {
            val job = async { loadDatesWithDataUseCase.invoke(category) }
            delay(1000)

            when (val aggregationInfoResult = job.await()) {
                is UseCaseResults.Success -> {
                    _aggregationCardsData.postValue(
                        AggregationCardsState.WithData(false, aggregationInfoResult.data)
                    )
                    _updatedAggregationCardsData.postValue(
                        AggregationCardsState.WithData(true, aggregationInfoResult.data)
                    )
                }
                is UseCaseResults.Failed -> {
                    Log.e(TAG, "Failed loading dates with data ", aggregationInfoResult.exception)
                    _aggregationCardsData.postValue(AggregationCardsState.LoadingFailed(false))
                    _updatedAggregationCardsData.postValue(
                        AggregationCardsState.LoadingFailed(true)
                    )
                }
            }
        }
    }

    fun getPriorityList(): List<AppMetadata> =
        when (val list = _priorityListState.value) {
            is PriorityListState.WithData -> list.priorityList
            else -> emptyList()
        }

    fun getCurrentDeviceInfo(): DeviceDataSourceInfo? =
        when (val sourceInfos = _deviceDataSourcesInfo.value) {
            is DeviceDataSourcesState.WithData ->
                sourceInfos.deviceDataSourcesInfo.find { it.isCurrentDevice }
            else -> null
        }

    sealed class AggregationCardsState(open val shouldObserve: Boolean) {
        data class Loading(override val shouldObserve: Boolean) :
            AggregationCardsState(shouldObserve)

        data class LoadingFailed(override val shouldObserve: Boolean) :
            AggregationCardsState(shouldObserve)

        data class WithData(
            override val shouldObserve: Boolean,
            val dataTotals: List<AggregationCardInfo>,
        ) : AggregationCardsState(shouldObserve)
    }

    sealed class PotentialAppSourcesState(open val shouldObserve: Boolean) {
        data class Loading(override val shouldObserve: Boolean) :
            PotentialAppSourcesState(shouldObserve)

        data class LoadingFailed(override val shouldObserve: Boolean) :
            PotentialAppSourcesState(shouldObserve)

        data class WithData(
            override val shouldObserve: Boolean,
            val appSources: List<AppMetadata>,
        ) : PotentialAppSourcesState(shouldObserve)
    }

    sealed class PriorityListState(open val shouldObserve: Boolean) {
        data class Loading(override val shouldObserve: Boolean) : PriorityListState(shouldObserve)

        data class LoadingFailed(override val shouldObserve: Boolean) :
            PriorityListState(shouldObserve)

        data class WithData(
            override val shouldObserve: Boolean,
            val priorityList: List<AppMetadata>,
        ) : PriorityListState(shouldObserve)
    }

    sealed class DeviceDataSourcesState {
        class Loading : DeviceDataSourcesState()

        class LoadingFailed : DeviceDataSourcesState()

        data class WithData(val deviceDataSourcesInfo: Set<DeviceDataSourceInfo>) :
            DeviceDataSourcesState()
    }

    class DataSourcesInfo(
        val priorityListState: PriorityListState?,
        val potentialAppSourcesState: PotentialAppSourcesState?,
        val deviceDataSourcesState: DeviceDataSourcesState?,
    ) {
        fun isLoading(): Boolean {
            return priorityListState is PriorityListState.Loading ||
                potentialAppSourcesState is PotentialAppSourcesState.Loading ||
                (deviceDataProvidersApi() &&
                    deviceDataSourcesState is DeviceDataSourcesState.Loading)
        }

        fun isLoadingFailed(): Boolean {
            return priorityListState is PriorityListState.LoadingFailed ||
                potentialAppSourcesState is PotentialAppSourcesState.LoadingFailed ||
                (deviceDataProvidersApi() &&
                    deviceDataSourcesState is DeviceDataSourcesState.LoadingFailed)
        }

        fun isWithData(): Boolean {
            return priorityListState is PriorityListState.WithData &&
                potentialAppSourcesState is PotentialAppSourcesState.WithData &&
                (!deviceDataProvidersApi() ||
                    deviceDataSourcesState is DeviceDataSourcesState.WithData)
        }
    }

    data class DataSourcesAndAggregationsInfo(
        val priorityListState: PriorityListState?,
        val potentialAppSourcesState: PotentialAppSourcesState?,
        val aggregationCardsState: AggregationCardsState?,
    ) {
        fun isLoading(): Boolean {
            return priorityListState is PriorityListState.Loading ||
                potentialAppSourcesState is PotentialAppSourcesState.Loading ||
                aggregationCardsState is AggregationCardsState.Loading
        }

        fun isLoadingFailed(): Boolean {
            return priorityListState is PriorityListState.LoadingFailed ||
                potentialAppSourcesState is PotentialAppSourcesState.LoadingFailed ||
                aggregationCardsState is AggregationCardsState.LoadingFailed
        }

        fun isWithData(): Boolean {
            return priorityListState is PriorityListState.WithData &&
                potentialAppSourcesState is PotentialAppSourcesState.WithData &&
                aggregationCardsState is AggregationCardsState.WithData
        }
    }
}
