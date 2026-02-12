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
package com.android.healthconnect.controller.tests.datasources.api

import android.content.Context
import android.health.connect.FetchDataOriginsPriorityOrderResponse
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.datasources.api.LoadPriorityListUseCase
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadPriorityListUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val coroutineTestRule = CoroutineTestRule()

    private val manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)
    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var usecase: LoadPriorityListUseCase
    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        usecase = LoadPriorityListUseCase(manager, appInfoReader, Dispatchers.Main)
    }

    @Test
    fun loadPriorityList_listOfAppsInPriorityListReturnedCorrectly() = runTest {
        val dataOriginsPriorityOrderResponse =
            FetchDataOriginsPriorityOrderResponse(
                mutableListOf(
                    getDataOrigin(TEST_APP_PACKAGE_NAME),
                    getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                )
            )

        Mockito.doAnswer(prepareAnswer(dataOriginsPriorityOrderResponse))
            .`when`(manager)
            .fetchDataOriginsPriorityOrder(eq(HealthDataCategory.ACTIVITY), any(), any())

        val loadedAppsPriorityList = usecase.invoke(HealthDataCategory.ACTIVITY)

        assertThat(loadedAppsPriorityList is UseCaseResults.Success).isTrue()
        assertThat((loadedAppsPriorityList as UseCaseResults.Success).data.size).isEqualTo(2)

        assertThat(loadedAppsPriorityList.data)
            .contains(appInfoReader.getAppMetadata(TEST_APP_PACKAGE_NAME))

        assertThat(loadedAppsPriorityList.data)
            .contains(appInfoReader.getAppMetadata(TEST_APP_PACKAGE_NAME_2))
    }

    private fun prepareAnswer(
        fetchDataOriginsPriorityOrderResponse: FetchDataOriginsPriorityOrderResponse
    ): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<FetchDataOriginsPriorityOrderResponse, *>
            receiver.onResult(fetchDataOriginsPriorityOrderResponse)
            null
        }
        return answer
    }
}
