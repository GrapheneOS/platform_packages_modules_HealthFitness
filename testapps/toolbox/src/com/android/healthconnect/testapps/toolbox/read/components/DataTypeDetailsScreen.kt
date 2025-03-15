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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.healthconnect.testapps.toolbox.read.navigation.Screen


/**
 * Screen that displays details of selected data type
 */
@Composable
fun DataTypeDetailsScreen(modifier: Modifier = Modifier, dataTypeDetails: Screen.DataTypeDetails){

    Text(
        modifier = Modifier.testTag("dataTypeTitle"),
        text = stringResource(id = dataTypeDetails.dataType.title),
        style = MaterialTheme.typography.titleLarge
    )
}