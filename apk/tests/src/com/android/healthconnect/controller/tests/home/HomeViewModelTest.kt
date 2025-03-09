/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.home

import android.content.Context
import android.content.SharedPreferences
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import androidx.test.core.app.ApplicationProvider
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.home.HomeViewModel
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.getDataOrigin
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class HomeViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val testDispatcher = UnconfinedTestDispatcher()

    private val keyguardManagerUtils: KeyguardManagerUtil =
        Mockito.mock(KeyguardManagerUtil::class.java)
    private val loadHealthPermissionApps: ILoadHealthPermissionApps =
        FakeHealthPermissionAppsUseCase()
    private val manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        sharedPreferences =
            context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)

        viewModel =
            HomeViewModel(
                loadHealthPermissionApps,
                AllDataUseCase(manager, Dispatchers.Main),
                keyguardManagerUtils,
            )
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun isAnyMedicalData_noMedicalData_returnsFalse() = runTest {
        doAnswer(prepareAnswer(emptyList()))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())

        val testObserver = TestObserver<Boolean>()
        viewModel.hasAnyMedicalData.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(false)
    }

    @Test
    fun isAnyMedicalData_hasMedicalData_returnsTrue() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())

        val testObserver = TestObserver<Boolean>()
        viewModel.hasAnyMedicalData.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(true)
    }

    // region lock screen banner

    @Test
    fun lockScreenBanner_deviceSecure_bannerNotSeen_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(true)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_bannerAlreadySeenWithFitness_hasFitnessData_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_bannerAlreadySeenWithFitness_hasMedicalData_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(true)
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_hasBothData_bannerSeenWithBoth_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(true)
        mockFitnessData()
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_hasBothData_bannerSeenWithFitnessData_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = true,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasBothData_bannerSeenWithMedicalData_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(true)
        mockFitnessData()
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = true,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasBothData_bannerNotSeen_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = true,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasFitnessData_bannerNotSeen_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = false,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasFitnessData_bannerSeenWithMedical_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(true)
        mockFitnessData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = true,
                    hasAnyMedicalData = false,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasFitnessData_bannerSeenWithFitness_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(false)
        mockFitnessData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_hasFitnessData_bannerSeenWithBoth_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(true)
        mockFitnessData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_hasMedicalData_bannerNotSeen_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(false)
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        val medicalDataObserver = TestObserver<Boolean>()
        viewModel.hasAnyMedicalData.observeForever(medicalDataObserver)

        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(medicalDataObserver.getLastValue()).isEqualTo(true)
        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = false,
                    hasAnyMedicalData = true,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasMedicalData_bannerSeenWithFitness_bannerShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(false)
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(
                HomeViewModel.LockScreenBannerState.ShowBanner(
                    hasAnyFitnessData = false,
                    hasAnyMedicalData = true,
                )
            )
    }

    @Test
    fun lockScreenBanner_hasMedicalData_bannerSeenWithMedical_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(false)
        setLockScreenBannerSeenMedical(true)
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    @Test
    fun lockScreenBanner_hasMedicalData_bannerSeenWithBoth_bannerNotShown() = runTest {
        whenever(keyguardManagerUtils.isDeviceSecure(any())).thenReturn(false)
        setLockScreenBannerSeenFitness(true)
        setLockScreenBannerSeenMedical(true)
        mockMedicalData()

        val testObserver = TestObserver<HomeViewModel.LockScreenBannerState>()
        viewModel.showLockScreenBanner.observeForever(testObserver)
        viewModel.loadHasAnyMedicalData()
        viewModel.loadShouldShowLockScreenBanner(sharedPreferences, context)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(HomeViewModel.LockScreenBannerState.NoBanner)
    }

    // endregion

    private fun mockFitnessData() {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                        ),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                    ),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME))),
                    ),
            )
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    private fun mockMedicalData() {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    private fun setLockScreenBannerSeenFitness(seen: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS, seen)
        editor.apply()
    }

    private fun setLockScreenBannerSeenMedical(seen: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL, seen)
        editor.apply()
    }

    private fun prepareAnswer(
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(recordTypeInfoMap)
            recordTypeInfoMap
        }
        return answer
    }

    private fun prepareAnswer(
        MedicalResourceTypeInfo: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(MedicalResourceTypeInfo)
            MedicalResourceTypeInfo
        }
        return answer
    }
}
