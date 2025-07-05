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
package com.android.healthconnect.controller.data.fhir.raw

import android.health.connect.MedicalResourceId
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.fhir.api.FhirUseCase
import com.android.healthconnect.controller.data.formatters.medical.RawFhirFormatter
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for the [RawFhirFragment] . */
@HiltViewModel
class RawFhirViewModel
@Inject
constructor(private val fhirUseCase: FhirUseCase, private val formatter: RawFhirFormatter) :
    ViewModel() {

    companion object {
        private const val TAG = "RawFhirViewModel"
    }

    private val _rawFhir = MutableLiveData<RawFhirState>()

    /**
     * Provides a [android.health.connect.datatypes.FhirResource]s to be displayed in
     * [RawFhirFragment].
     */
    val rawFhir: LiveData<RawFhirState>
        get() = _rawFhir

    fun loadRawFhirResource(medicalResourceId: MedicalResourceId) {
        _rawFhir.postValue(RawFhirState.Loading)
        viewModelScope.launch {
            when (val result = fhirUseCase.loadFhirResource(medicalResourceId)) {
                is UseCaseResults.Success -> {
                    val formattedRawFhir =
                        FormattedEntry.FormattedRawFhir(
                            formatter.format(result.data),
                            formatter.fhirContentDescription(result.data),
                        )
                    _rawFhir.postValue(RawFhirState.WithData(listOf(formattedRawFhir)))
                }
                is UseCaseResults.Failed -> {
                    _rawFhir.postValue(RawFhirState.Error)
                }
            }
        }
    }

    sealed class RawFhirState {
        data object Loading : RawFhirState()

        data object Error : RawFhirState()

        data class WithData(val fhirResource: List<FormattedEntry>) : RawFhirState()
    }
}
