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

package com.android.healthconnect.testapps.toolbox.read.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.UIConstants.PADDING_MEDIUM
import com.android.healthconnect.testapps.toolbox.UIConstants.PADDING_SMALL
import com.android.healthconnect.testapps.toolbox.read.components.states.ErrorMessage
import com.android.healthconnect.testapps.toolbox.read.components.states.LoadingBar
import com.android.healthconnect.testapps.toolbox.read.controller.LoadEntriesInput
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.navigation.Screen
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.mapEntryToComposable
import com.android.healthconnect.testapps.toolbox.viewmodels.DataState
import com.android.healthconnect.testapps.toolbox.viewmodels.LoadEntriesViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

/** Screen that displays details of selected data type */
typealias ComposableView = @Composable () -> Unit

@Composable
fun DataTypeDetailsScreen(
    modifier: Modifier = Modifier,
    dataTypeDetails: Screen.DataTypeDetails,
    viewModel: LoadEntriesViewModel = viewModel(factory = LoadEntriesViewModel.Factory),
) {
    LaunchedEffect(Unit) {
        val startTime = Instant.now().truncatedTo(DAYS)
        val endTime = Instant.now()
        viewModel.loadEntries(
            LoadEntriesInput(
                dataType = dataTypeDetails.dataType,
                startTime = startTime,
                endTime = endTime,
            )
        )
    }

    Column {
        // Header
        Text(
            modifier = Modifier.padding(start = PADDING_SMALL, bottom = PADDING_SMALL),
            text = stringResource(id = dataTypeDetails.dataType.title),
            style = MaterialTheme.typography.displayMedium,
        )

        // Body
        val entriesState = viewModel.entriesState.collectAsState().value
        return when (entriesState) {
            is DataState.Loading -> LoadingBar()
            is DataState.Success -> DataEntriesList(entriesState.entries)
            is DataState.Error -> ErrorMessage(entriesState.exception)
        }
    }
}

@Composable
fun DataEntriesList(entries: List<FormattedEntry>) {

    if (entries.isEmpty()) {
        return Text(
            text = stringResource(id = R.string.no_data),
            modifier = Modifier.padding(PADDING_MEDIUM).testTag("entriesList"),
        )
    }
    val formattedEntries: List<ComposableView> =
        entries.map { entry -> mapEntryToComposable(entry) }

    // List of entries
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(PADDING_MEDIUM)) {
        items(formattedEntries) { entry -> entry.invoke() }
    }
}
