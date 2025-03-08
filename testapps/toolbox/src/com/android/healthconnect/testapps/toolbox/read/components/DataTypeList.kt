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

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.healthconnect.testapps.toolbox.Constants.HealthDataCategory
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType


@Composable
fun DataTypeList(modifier: Modifier = Modifier){

    val categories = HealthDataCategory.entries
    val categoriesAndDataTypes = mutableListOf<Any>()
    for (category in categories) {
        categoriesAndDataTypes.add(category)
        categoriesAndDataTypes.addAll(category.healthPermissionTypes)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(all = 8.dp)
        .verticalScroll(rememberScrollState())){

        for (categoryAndDataType in categoriesAndDataTypes){

            when(categoryAndDataType){
                is HealthPermissionType -> {
                    Text(
                        text = stringResource(id = categoryAndDataType.title),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            .testTag("dataType")
                    )
                }
                is HealthDataCategory -> {
                    Text(
                        text = stringResource(id = categoryAndDataType.title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 8.dp)
                            .testTag("category")
                    )
                }
            }
        }
    }
}