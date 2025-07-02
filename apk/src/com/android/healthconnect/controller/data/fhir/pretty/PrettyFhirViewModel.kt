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
package com.android.healthconnect.controller.data.fhir.pretty

import android.health.connect.datatypes.FhirResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedMedicalDataEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhir
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhirDetailsHeader
import com.android.healthconnect.controller.data.fhir.api.FhirUseCase
import com.android.healthconnect.controller.data.formatters.medical.PrettyFhirFormatter
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonExtractor
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonGroup
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for the [PrettyFhirFragment] . */
@HiltViewModel
class PrettyFhirViewModel
@Inject
constructor(
    private val fhirUseCase: FhirUseCase,
    private val prettyFhirFormatter: PrettyFhirFormatter,
) : ViewModel() {

    companion object {
        private const val TAG = "FhirViewModel"
    }

    private val _prettyFhir = MutableLiveData<PrettyFhirState>()

    /** Provides a [FormattedPrettyFhir]s to be displayed in [PrettyFhirFragment]. */
    val prettyFhir: LiveData<PrettyFhirState>
        get() = _prettyFhir

    fun loadPrettyFhirResource(medicalDataEntry: FormattedMedicalDataEntry) {
        _prettyFhir.postValue(PrettyFhirState.Loading)

        viewModelScope.launch {
            when (val result = fhirUseCase.loadFhirResource(medicalDataEntry.medicalResourceId)) {
                is UseCaseResults.Success -> {
                    val formattedEntries = mutableListOf<FormattedEntry>()

                    val medicalDataEntryHeader = addMedicalDataEntryHeader(medicalDataEntry)
                    val formattedPrettyFhirEntries = addFormattedPrettyFhirEntries(result.data)

                    formattedEntries.add(medicalDataEntryHeader)
                    formattedEntries.add(FormattedEntry.ItemDataEntrySeparator())
                    formattedEntries.addAll(formattedPrettyFhirEntries)

                    _prettyFhir.postValue(PrettyFhirState.WithData(formattedEntries))
                }

                is UseCaseResults.Failed -> {
                    _prettyFhir.postValue(PrettyFhirState.Error)
                }
            }
        }
    }

    private fun addMedicalDataEntryHeader(
        medicalDataEntry: FormattedMedicalDataEntry
    ): FormattedPrettyFhirDetailsHeader {
        return FormattedPrettyFhirDetailsHeader(
            header = medicalDataEntry.header,
            title = medicalDataEntry.title,
        )
    }

    private fun addFormattedPrettyFhirEntries(
        rawFhirResource: FhirResource
    ): List<FormattedPrettyFhir> {
        val formattedEntries: MutableList<FormattedPrettyFhir> = mutableListOf()

        val prettyFhirJson = prettifyRawFhirResource(rawFhirResource)

        val formattedPrettyFhirList: List<FormattedPrettyFhir> =
            prettyFhirJson.map { prettyJsonGroup -> prettyFhirFormatter.format(prettyJsonGroup) }

        formattedEntries.addAll(formattedPrettyFhirList)
        return formattedEntries
    }

    private fun prettifyRawFhirResource(rawFhirResources: FhirResource): List<PrettyJsonGroup> {
        return PrettyJsonExtractor().extract(rawFhirResources.data)
    }

    sealed class RawFhirState {
        data object Loading : RawFhirState()

        data object Error : RawFhirState()

        data class WithData(val fhirResource: List<FormattedEntry>) : RawFhirState()
    }

    sealed class PrettyFhirState {
        data object Loading : PrettyFhirState()

        data object Error : PrettyFhirState()

        data class WithData(val prettyFhirResources: List<FormattedEntry>) : PrettyFhirState()
    }
}
