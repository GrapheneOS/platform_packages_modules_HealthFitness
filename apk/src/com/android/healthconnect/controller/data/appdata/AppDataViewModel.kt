/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */
package com.android.healthconnect.controller.data.appdata

import android.health.connect.HealthDataCategory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.data.appdata.api.IGetAppFitnessPermissionTypesUseCase
import com.android.healthconnect.controller.data.appdata.api.IGetAppMedicalPermissionTypesUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.getAllSymptomPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** View model for the [AppDataFragment] . */
@HiltViewModel
class AppDataViewModel
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val getAppFitnessPermissionTypesUseCase: IGetAppFitnessPermissionTypesUseCase,
    private val getAppMedicalPermissionTypesUseCase: IGetAppMedicalPermissionTypesUseCase,
) : DeletionDataViewModel() {

    companion object {
        private const val TAG = "AppDataViewModel"
    }

    private val _appFitnessData = MutableLiveData<AppDataState>()
    private val _appMedicalData = MutableLiveData<AppDataState>()

    /** Provides a list of [PermissionTypesPerCategory]s of [FitnessPermissionType]s. */
    val appFitnessData: LiveData<AppDataState>
        get() = _appFitnessData

    /** Provides a list of [PermissionTypesPerCategory]s of [MedicalPermissionType]s. */
    val appMedicalData: LiveData<AppDataState>
        get() = _appMedicalData

    /**
     * Provides a list of all [PermissionTypesPerCategory]s to be displayed in [AppDataFragment].
     */
    val fitnessAndMedicalData: MediatorLiveData<AppDataState> =
        MediatorLiveData<AppDataState>().apply {
            value = AppDataState.Loading
            addSource(_appFitnessData) {
                postValue(getCombinedAppData(appFitnessData, appMedicalData))
            }
            addSource(_appMedicalData) {
                postValue(getCombinedAppData(appFitnessData, appMedicalData))
            }
        }

    private val _appInfo = MutableLiveData<AppMetadata>()

    val appInfo: LiveData<AppMetadata>
        get() = _appInfo

    fun prepareDeletionType(): DeletionType.DeleteHealthPermissionTypesFromApp? {
        val typesToDelete = setOfPermissionTypesToBeDeleted.value.orEmpty().toMutableSet()
        if (typesToDelete.contains(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)) {
            typesToDelete.remove(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
            typesToDelete.addAll(getAllSymptomPermissionTypes())
        }

        return _appInfo.value?.let { currentAppInfo ->
            DeletionType.DeleteHealthPermissionTypesFromApp(
                healthPermissionTypes = typesToDelete,
                totalPermissionTypes = numOfPermissionTypes,
                packageName = currentAppInfo.packageName,
                appName = currentAppInfo.appName,
            )
        }
    }

    fun loadAppData(packageName: String) {
        _appFitnessData.postValue(AppDataState.Loading)
        _appMedicalData.postValue(AppDataState.Loading)
        numOfPermissionTypes = 0
        viewModelScope.launch {
            val fitnessData = async { getAppFitnessPermissionTypesUseCase.invoke(packageName) }
            val medicalData = async { getAppMedicalPermissionTypesUseCase.invoke(packageName) }

            handleResult(fitnessData.await(), _appFitnessData)
            handleResult(medicalData.await(), _appMedicalData)
        }
    }

    private fun handleResult(
        result: UseCaseResults<List<PermissionTypesPerCategory>>,
        liveData: MutableLiveData<AppDataState>,
    ) {
        when (result) {
            is UseCaseResults.Success -> {
                liveData.postValue(AppDataState.WithData(result.data))
                numOfPermissionTypes += result.data.sumOf { it.data.size }
                // TODO (b/376085888) Extract to separate useCase
                // If Symptoms is part of the categories loaded, we must manually add
                // the number of permission types
                if (
                    result.data.any { permTypesPerCat ->
                        permTypesPerCat.category == HealthDataCategory.SYMPTOMS &&
                            permTypesPerCat.data.isNotEmpty()
                    }
                ) {
                    numOfPermissionTypes += getAllSymptomPermissionTypes().size - 1
                }
            }
            is UseCaseResults.Failed -> liveData.postValue(AppDataState.Error)
        }
    }

    private fun getCombinedAppData(
        appFitnessData: LiveData<AppDataState>,
        appMedicalData: LiveData<AppDataState>,
    ): AppDataState {
        val fitnessData = appFitnessData.value ?: AppDataState.Loading
        val medicalData = appMedicalData.value ?: AppDataState.Loading

        return when {
            fitnessData is AppDataState.WithData && medicalData is AppDataState.WithData ->
                AppDataState.WithData(fitnessData.dataMap + medicalData.dataMap)
            fitnessData is AppDataState.WithData -> fitnessData
            medicalData is AppDataState.WithData -> medicalData
            fitnessData is AppDataState.Error && medicalData is AppDataState.Error ->
                AppDataState.Error
            else -> AppDataState.Loading
        }
    }

    fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appInfo.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    sealed class AppDataState {
        data object Loading : AppDataState()

        data object Error : AppDataState()

        data class WithData(val dataMap: List<PermissionTypesPerCategory>) : AppDataState()
    }
}
