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
package com.android.healthconnect.controller.tests.newhome

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.os.OutcomeReceiver
import androidx.test.core.app.ApplicationProvider
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.newHome.HomeViewModel
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_4
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.utils.KeyguardManagerUtil
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class HomeViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var viewModel: HomeViewModel
    private lateinit var loadAllDataUseCase: AllDataUseCase

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loadHealthPermissionApps: ILoadHealthPermissionApps =
        FakeHealthPermissionAppsUseCase()
    private val keyguardManagerUtil: KeyguardManagerUtil = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val manager: HealthConnectManager = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        loadAllDataUseCase = AllDataUseCase(manager, Dispatchers.Main)

        viewModel =
            HomeViewModel(
                context,
                loadHealthPermissionApps,
                loadAllDataUseCase,
                keyguardManagerUtil,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadData_whenAppLoadFails_setsErrorState() = runTest {
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).setForceFail(true)
        doAnswer(prepareAnswer(listOf()))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val testObserver = TestObserver<HomeViewModel.HomeFragmentState>()
        viewModel.homeFragmentState.observeForever(testObserver)
        viewModel.loadData()
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result).isInstanceOf(HomeViewModel.HomeFragmentState.Error::class.java)
    }

    @Test
    fun loadData_whenMedicalDataLoadFails_setsErrorState() = runTest {
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).addToList(
            ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.DENIED)
        )
        doAnswer(prepareFailureAnswer())
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val testObserver = TestObserver<HomeViewModel.HomeFragmentState>()
        viewModel.homeFragmentState.observeForever(testObserver)
        viewModel.loadData()
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result).isInstanceOf(HomeViewModel.HomeFragmentState.Error::class.java)
    }

    @Test
    fun loadData_success_filtersInactiveAndNeedsUpdateApps() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.INACTIVE)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.NEEDS_UPDATE)
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).updateList(
            listOf(app1, app2, app3, app4)
        )
        doAnswer(prepareAnswer(listOf()))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val testObserver = TestObserver<HomeViewModel.HomeFragmentState>()
        viewModel.homeFragmentState.observeForever(testObserver)
        viewModel.loadData()
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        val state = result as HomeViewModel.HomeFragmentState.WithData
        assertThat(state.connectedApps).containsExactly(app1, app2)
    }

    @Test
    fun loadData_success_sortsAppsByStatusThenByName() = runTest {
        val app1 = ConnectedAppMetadata(TEST_APP, ConnectedAppStatus.ALLOWED)
        val app2 = ConnectedAppMetadata(TEST_APP_2, ConnectedAppStatus.DENIED)
        val app3 = ConnectedAppMetadata(TEST_APP_3, ConnectedAppStatus.ALLOWED)
        val app4 = ConnectedAppMetadata(TEST_APP_4, ConnectedAppStatus.DENIED)
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).updateList(
            listOf(app1, app2, app3, app4)
        )
        doAnswer(prepareAnswer(listOf()))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val testObserver = TestObserver<HomeViewModel.HomeFragmentState>()
        viewModel.homeFragmentState.observeForever(testObserver)
        viewModel.loadData()
        advanceUntilIdle()

        val result = testObserver.getLastValue()
        assertThat(result).isInstanceOf(HomeViewModel.HomeFragmentState.WithData::class.java)
        val state = result as HomeViewModel.HomeFragmentState.WithData
        assertThat(state.connectedApps).containsExactly(app1, app3, app2, app4).inOrder()
    }

    private fun prepareAnswer(
        medicalResourceTypeInfo: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfo)
            medicalResourceTypeInfo
        }
        return answer
    }

    private fun prepareFailureAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, HealthConnectException>
            receiver.onError(HealthConnectException(HealthConnectException.ERROR_UNKNOWN))
            null
        }
        return answer
    }
}
