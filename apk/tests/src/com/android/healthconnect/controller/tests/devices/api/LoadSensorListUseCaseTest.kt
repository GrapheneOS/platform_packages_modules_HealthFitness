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

package com.android.healthconnect.controller.tests.devices.api

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.platform.test.annotations.EnableFlags
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.devices.api.LoadSensorListUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
class LoadSensorListUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    private var sensorManager: SensorManager = mock()
    private val mockContext: Context =
        org.mockito.kotlin.mock<Context>() {
            on { getSystemService(Context.SENSOR_SERVICE) } doReturn sensorManager
        }

    private lateinit var loadSensorList: LoadSensorListUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        loadSensorList = LoadSensorListUseCase(mockContext, Dispatchers.Main)
    }

    @Test
    fun invoke_whenSensorManagerReturnsSensors_returnsSensors() = runTest {
        val sensorList = listOf(mock<Sensor> { on { type } doReturn Sensor.TYPE_STEP_COUNTER })
        whenever(sensorManager.getSensorList(Sensor.TYPE_ALL)).then { sensorList }

        val result = loadSensorList.invoke(Unit)

        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(sensorList)
    }

    @Test
    fun invoke_whenSensorManagerReturnsEmptyList_returnsEmptyList() = runTest {
        whenever(sensorManager.getSensorList(Sensor.TYPE_ALL)).then { emptyList<Sensor>() }

        val result = loadSensorList.invoke(Unit)

        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun invoke_onException_returnsFailed() = runTest {
        whenever(sensorManager.getSensorList(Sensor.TYPE_ALL)).thenThrow(RuntimeException("Error"))

        val result = loadSensorList.invoke(Unit)

        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(RuntimeException::class.java)
    }
}
