/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package com.android.healthconnect.controller.tests.permissions.connectedapps.wear

import android.health.connect.Constants
import android.health.connect.HealthPermissions
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.RecordTypeIdentifier
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.AppAccess
import com.android.healthconnect.controller.permissions.connectedapps.wear.PermissionAccess
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.recentaccess.LoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_3
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WearConnectedAppsViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val loadHealthPermissionApps: ILoadHealthPermissionApps = mock()
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase = mock()
    private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase = mock()
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase = mock()
    private val healthPermissionReader: HealthPermissionReader = mock()
    private val loadRecentAccessUseCase: LoadRecentAccessUseCase = mock()
    private val nonSystemApp1Metadata =
        AppMetadata(
            packageName = TEST_APP_PACKAGE_NAME,
            appName = TEST_APP_NAME,
            icon = null,
            isSystem = false,
        )
    private val nonSystemApp2Metadata =
        AppMetadata(
            packageName = TEST_APP_PACKAGE_NAME_2,
            appName = TEST_APP_NAME_2,
            icon = null,
            isSystem = false,
        )
    private val systemAppMetadata =
        AppMetadata(
            packageName = TEST_APP_PACKAGE_NAME_3,
            appName = TEST_APP_NAME_3,
            icon = null,
            isSystem = true,
        )
    private lateinit var viewModel: WearConnectedAppsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        whenever(healthPermissionReader.getSystemHealthPermissions())
            .thenReturn(listOf(READ_HEART_RATE, READ_SKIN_TEMPERATURE, READ_OXYGEN_SATURATION))
        runBlocking {
            whenever(loadHealthPermissionApps.invoke())
                .thenReturn(
                    listOf(
                        ConnectedAppMetadata(nonSystemApp1Metadata, ConnectedAppStatus.ALLOWED),
                        ConnectedAppMetadata(nonSystemApp2Metadata, ConnectedAppStatus.DENIED),
                        ConnectedAppMetadata(systemAppMetadata, ConnectedAppStatus.ALLOWED),
                    )
                )
            whenever(loadAppPermissionsStatusUseCase.invoke(TEST_APP_PACKAGE_NAME))
                .thenReturn(
                    listOf(
                        HealthPermissionStatus(
                            fromPermissionString(HealthPermissions.READ_HEART_RATE),
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            fromPermissionString(HealthPermissions.READ_SKIN_TEMPERATURE),
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            fromPermissionString(HealthPermissions.READ_OXYGEN_SATURATION),
                            isGranted = true,
                        ),
                    )
                )
            whenever(loadAppPermissionsStatusUseCase.invoke(TEST_APP_PACKAGE_NAME_2))
                .thenReturn(
                    listOf(
                        HealthPermissionStatus(
                            fromPermissionString(HealthPermissions.READ_HEART_RATE),
                            isGranted = false,
                        )
                    )
                )
            whenever(loadAppPermissionsStatusUseCase.invoke(TEST_APP_PACKAGE_NAME_3))
                .thenReturn(
                    listOf(
                        HealthPermissionStatus(
                            fromPermissionString(HealthPermissions.READ_HEART_RATE),
                            isGranted = true,
                        )
                    )
                )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRecentAccessMapping_multipleForOneDataType_showLatestAccessTime() = runTest {
        val newTime = NOW.minusSeconds(1)
        val oldTime = newTime.minusSeconds(60)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                        newTime.toEpochMilli(),
                        Constants.READ,
                    ),
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                        oldTime.toEpochMilli(),
                        Constants.READ,
                    ),
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0])
            .containsExactly(
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_HEART_RATE),
                    listOf(AppAccess(nonSystemApp1Metadata, lastAccessTime = newTime)),
                )
            )
    }

    @Test
    fun testRecentAccessMapping_readAndInsertAccessLogs_onlyShowReadLog() = runTest {
        val writeNewTime = NOW.minusSeconds(1)
        val readOldTime = writeNewTime.minusSeconds(60)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                        readOldTime.toEpochMilli(),
                        Constants.READ,
                    ),
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                        writeNewTime.toEpochMilli(),
                        Constants.UPSERT,
                    ),
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0])
            .containsExactly(
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_HEART_RATE),
                    listOf(AppAccess(nonSystemApp1Metadata, lastAccessTime = readOldTime)),
                )
            )
    }

    @Test
    fun testRecentAccessMapping_multipleDataTypes_showLatestOneForEach() = runTest {
        val heartRateTime = NOW.minusSeconds(1)
        val skinTempTime = NOW.minusSeconds(2)
        val spO2Time = NOW.minusSeconds(3)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                        heartRateTime.toEpochMilli(),
                        Constants.READ,
                    ),
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                        skinTempTime.toEpochMilli(),
                        Constants.READ,
                    ),
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION),
                        spO2Time.toEpochMilli(),
                        Constants.READ,
                    ),
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0])
            .containsExactly(
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_HEART_RATE),
                    listOf(AppAccess(nonSystemApp1Metadata, lastAccessTime = heartRateTime)),
                ),
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_SKIN_TEMPERATURE),
                    listOf(AppAccess(nonSystemApp1Metadata, lastAccessTime = skinTempTime)),
                ),
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_OXYGEN_SATURATION),
                    listOf(AppAccess(nonSystemApp1Metadata, lastAccessTime = spO2Time)),
                ),
            )
    }

    @Test
    fun testRecentAccessMapping_useCaseResultsFailed_skipProcessing() = runTest {
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Failed(Exception()))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0]).isEmpty()
    }

    @Test
    fun testRecentAccessMapping_nonSystemHealthPermission_skipProcessing() = runTest {
        val writeNewTime = NOW.minusSeconds(1)
        val readOldTime = writeNewTime.minusSeconds(60)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(RecordTypeIdentifier.RECORD_TYPE_STEPS),
                        readOldTime.toEpochMilli(),
                        Constants.READ,
                    )
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0]).isEmpty()
    }

    @Test
    fun testRecentAccessMapping_recordTypesEmpty_skipProcessing() = runTest {
        val writeNewTime = NOW.minusSeconds(1)
        val readOldTime = writeNewTime.minusSeconds(60)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(),
                        readOldTime.toEpochMilli(),
                        Constants.READ,
                    )
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0]).isEmpty()
    }

    @Test
    fun testRecentAccessMapping_multipleRecordTypesInOneAccessLog_skipProcessing() = runTest {
        val writeNewTime = NOW.minusSeconds(1)
        val readOldTime = writeNewTime.minusSeconds(60)
        val accessLogs =
            listOf(
                    AccessLog(
                        TEST_APP_PACKAGE_NAME,
                        listOf(
                            RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                            RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE,
                        ),
                        readOldTime.toEpochMilli(),
                        Constants.READ,
                    )
                )
                .sortedByDescending { it.accessTime }
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0]).isEmpty()
    }

    @Test
    fun testRecentAccessMapping_accessedAppPermissionRevoked_stillDisplayAccessLog() = runTest {
        val accessTime = NOW.minusSeconds(60)
        val accessLogs =
            listOf(
                AccessLog(
                    TEST_APP_PACKAGE_NAME_2, // This package has been denied health permission.
                    listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                    accessTime.toEpochMilli(),
                    Constants.READ,
                )
            )
        whenever(loadRecentAccessUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(accessLogs))
        viewModel = createViewModel()

        val actualPermissionAccessRecords = mutableListOf<List<PermissionAccess>>()
        val collectJob = launch {
            viewModel.dataTypeToAppToLastAccessTime.collect { value ->
                actualPermissionAccessRecords.add(value)
            }
        }
        advanceUntilIdle()
        collectJob.cancel()

        assertThat(actualPermissionAccessRecords[0])
            .containsExactly(
                PermissionAccess(
                    fromPermissionString(HealthPermissions.READ_HEART_RATE),
                    listOf(AppAccess(nonSystemApp2Metadata, accessTime)),
                )
            )
    }

    @Test
    fun testRemoveFitnessPermissionForAllApps_removesNonSystemAppsOnly() = runTest {
        val heartRatePermission = fromPermissionString(HealthPermissions.READ_HEART_RATE)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeFitnessPermissionForAllApps(heartRatePermission)
        advanceUntilIdle()

        verify(revokeHealthPermissionUseCase, times(1))
            .invoke(eq(nonSystemApp1Metadata.packageName), eq(HealthPermissions.READ_HEART_RATE))
        verify(revokeHealthPermissionUseCase, times(0))
            .invoke(
                eq(systemAppMetadata.packageName),
                eq(HealthPermissions.READ_HEART_RATE),
            ) // System app permission should not be revoked
        verify(revokeHealthPermissionUseCase, times(0))
            .invoke(
                eq(nonSystemApp2Metadata.packageName),
                eq(HealthPermissions.READ_HEART_RATE),
            ) // Non-system app 2 is originally denied, so it should not be revoked.
        with(viewModel) {
            assertThat(dataTypeToAllowedApps.value[heartRatePermission])
                .containsExactly(systemAppMetadata)
            assertThat(dataTypeToDeniedApps.value[heartRatePermission])
                .containsExactlyElementsIn(listOf(nonSystemApp2Metadata, nonSystemApp1Metadata))

            val appPermissions = appToAllowedDataTypes.value
            assertThat(appPermissions[systemAppMetadata]).containsExactly(heartRatePermission)
            assertThat(appPermissions[nonSystemApp1Metadata])
                .containsExactlyElementsIn(
                    listOf(
                        fromPermissionString(HealthPermissions.READ_SKIN_TEMPERATURE),
                        fromPermissionString(HealthPermissions.READ_OXYGEN_SATURATION),
                    )
                )
            assertThat(appPermissions).doesNotContainKey(nonSystemApp2Metadata)
        }
    }

    private fun createViewModel(): WearConnectedAppsViewModel {
        return WearConnectedAppsViewModel(
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            grantPermissionsStatusUseCase,
            revokeHealthPermissionUseCase,
            loadRecentAccessUseCase,
            healthPermissionReader,
        )
    }
}
