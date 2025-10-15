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
package com.android.healthconnect.controller.tests.utils.di

import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadSymptomEntriesUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class FakeLoadSymptomEntriesUseCase @Inject constructor() :
    LoadSymptomEntriesUseCase(Dispatchers.Main, FakeLoadEntriesHelper()) {
    private var FAKE_SYMPTOM_ENTRIES = emptyList<FormattedEntry>()
    var wasInvoked = false
        private set

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        wasInvoked = true
        return FAKE_SYMPTOM_ENTRIES
    }

    fun updateList(list: List<FormattedEntry>) {
        FAKE_SYMPTOM_ENTRIES = list
    }

    fun reset() {
        FAKE_SYMPTOM_ENTRIES = emptyList()
        wasInvoked = false
    }
}
