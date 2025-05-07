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
package com.android.healthconnect.testapps.toolbox.newUi.components.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.UIConstants.PADDING_LARGE
import com.android.healthconnect.testapps.toolbox.newUi.components.shared.Header

/** Entry point for screens that use Jetpack Compose. */
@Composable
fun HomeMenuScreen(modifier: Modifier = Modifier, onNavigateToDataTypeListScreen: () -> Unit) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier.fillMaxSize().padding(PADDING_LARGE).verticalScroll(rememberScrollState()),
    ) {
        Header(text = stringResource(id = R.string.health_connect_toolbox))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(PADDING_LARGE),
        ) {
            HomeMenuButton(
                onClick = { onNavigateToDataTypeListScreen() },
                text = stringResource(id = R.string.read_data),
            )
        }
    }
}

@Composable
fun HomeMenuButton(onClick: () -> Unit, text: String) {

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text)
    }
}
