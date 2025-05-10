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
package com.android.healthconnect.testapps.toolbox.read.components.baseentries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.UIConstants.PADDING_LARGE
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedNutritionEntry

@Composable
fun NutritionEntry(nutritionEntry: FormattedNutritionEntry) {

    var samplesDisplayed by remember { mutableStateOf(false) }
    val rotationAngle by
        animateFloatAsState(
            targetValue = if (samplesDisplayed) 180f else 0f,
            animationSpec = tween(durationMillis = 300),
        )

    Column {
        // Data entry / header
        Row(
            modifier = Modifier.fillMaxWidth().clickable { samplesDisplayed = !samplesDisplayed },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DataEntry(header = nutritionEntry.header, value = nutritionEntry.value)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
            )
        }

        // Samples list (drop down)
        AnimatedVisibility(visible = samplesDisplayed) {
            if (!nutritionEntry.mealType.isNullOrEmpty()) {
                DataEntry(header = stringResource(R.string.type), value = nutritionEntry.mealType)
            }
            Text(modifier = Modifier.padding(start = PADDING_LARGE), text = nutritionEntry.samples)
        }
    }
}
