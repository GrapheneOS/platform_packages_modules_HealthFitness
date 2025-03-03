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

import android.health.connect.datatypes.Record
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.healthconnect.testapps.toolbox.read.navigation.Screen
import com.android.healthconnect.testapps.toolbox.viewmodels.DataState
import com.android.healthconnect.testapps.toolbox.viewmodels.LoadEntriesViewModel
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.controller.LoadEntriesInput
import java.time.Instant
import java.time.temporal.ChronoUnit


/**
 * Screen that displays details of selected data type
 */
@Composable
fun DataTypeDetailsScreen(modifier: Modifier = Modifier,
                          dataTypeDetails: Screen.DataTypeDetails,
                          viewModel: LoadEntriesViewModel = viewModel(factory = LoadEntriesViewModel.Factory)
){
    LaunchedEffect(Unit) {
        val startTime = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val endTime = Instant.now()
        viewModel.loadEntries(
            LoadEntriesInput(
                dataType = dataTypeDetails.dataType,
                startTime = startTime,
                endTime = endTime
            )
        )
    }

    Column {
        // Header
        Text(
            text = stringResource(id = dataTypeDetails.dataType.title),
            style = MaterialTheme.typography.titleLarge,
        )

        // Body
        val entriesState = viewModel.entriesState.collectAsState().value
        return when(entriesState){
            is DataState.Loading -> LoadingComponent()
            is DataState.Success -> RecordList(entriesState.records)
            is DataState.Error -> ErrorComponent(entriesState.exception)
        }
    }
}

@Composable
fun RecordList(
    records: List<Record>
){


}

@Composable
fun LoadingComponent(){
    Text(
        text = stringResource(id = R.string.loading_data)
    )
}

@Composable
fun ErrorComponent(e: Exception){
    Column {
        Text(
            text = stringResource(id = R.string.error),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = e.localizedMessage!!,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}