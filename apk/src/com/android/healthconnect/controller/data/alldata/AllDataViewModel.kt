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
package com.android.healthconnect.controller.data.alldata

import android.health.connect.HealthDataCategory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.alldata.api.IGetFitnessPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.data.alldata.api.IGetMedicalPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.getAllSymptomPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for the [AllDataFragment] . */
@HiltViewModel
class AllDataViewModel
@Inject
constructor(
    private val getFitnessPermissionTypesWithDataUseCase: IGetFitnessPermissionTypesWithDataUseCase,
    private val getMedicalPermissionTypesWithDataUseCase: IGetMedicalPermissionTypesWithDataUseCase,
) : DeletionDataViewModel() {

    private val _allData = MutableLiveData<AllDataState>()

    /** Provides a list of [PermissionTypesPerCategory]s to be displayed in [AllDataFragment]. */
    val allData: LiveData<AllDataState>
        get() = _allData

    fun prepareDeletionType(): DeletionType.DeleteHealthPermissionTypes {
        val typesToDelete = setOfPermissionTypesToBeDeleted.value.orEmpty().toMutableSet()
        // We use SYMPTOM_ABDOMINAL_PAIN as the "representative" symptom type for all symptoms,
        // which unlike other permission types are grouped together in the UI. That means that if we
        // see this in the list we should delete all symptom records regardless of type.
        if (typesToDelete.contains(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)) {
            typesToDelete.remove(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
            typesToDelete.addAll(getAllSymptomPermissionTypes())
        }
        return DeletionType.DeleteHealthPermissionTypes(typesToDelete, numOfPermissionTypes)
    }

    fun loadAllFitnessData() {
        _allData.postValue(AllDataState.Loading)
        viewModelScope.launch {
            when (val result = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _allData.postValue(AllDataState.WithData(result.data))
                    numOfPermissionTypes = result.data.sumOf { it.data.size }
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
                is UseCaseResults.Failed -> {
                    _allData.postValue(AllDataState.Error)
                }
            }
        }
    }

    fun loadAllMedicalData() {
        _allData.postValue(AllDataState.Loading)
        viewModelScope.launch {
            when (val result = getMedicalPermissionTypesWithDataUseCase.invoke(Unit)) {
                is UseCaseResults.Success -> {
                    _allData.postValue(AllDataState.WithData(result.data))
                    numOfPermissionTypes = result.data.sumOf { it.data.size }
                }
                is UseCaseResults.Failed -> {
                    _allData.postValue(AllDataState.Error)
                }
            }
        }
    }

    fun loadAllFitnessAndMedicalData() {
        _allData.postValue(AllDataState.Loading)
        viewModelScope.launch {
            val fitnessResult = getFitnessPermissionTypesWithDataUseCase.invoke(Unit)
            val medicalResult = getMedicalPermissionTypesWithDataUseCase.invoke(Unit)

            if (
                fitnessResult is UseCaseResults.Success && medicalResult is UseCaseResults.Success
            ) {
                val combinedData = fitnessResult.data + medicalResult.data
                _allData.postValue(AllDataState.WithData(combinedData))
                numOfPermissionTypes = combinedData.sumOf { it.data.size }
                // If Symptoms is part of the categories loaded, we must manually add
                // the number of permission types
                if (
                    fitnessResult.data.any { permTypesPerCat ->
                        permTypesPerCat.category == HealthDataCategory.SYMPTOMS &&
                            permTypesPerCat.data.isNotEmpty()
                    }
                ) {
                    numOfPermissionTypes += getAllSymptomPermissionTypes().size - 1
                }
            } else {
                _allData.postValue(AllDataState.Error)
            }
        }
    }

    sealed class AllDataState {
        data object Loading : AllDataState()

        data object Error : AllDataState()

        data class WithData(val dataMap: List<PermissionTypesPerCategory>) : AllDataState()
    }
}
