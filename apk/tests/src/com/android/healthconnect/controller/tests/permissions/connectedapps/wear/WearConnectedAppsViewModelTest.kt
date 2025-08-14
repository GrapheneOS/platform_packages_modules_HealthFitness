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

import android.content.Context
import android.health.connect.Constants
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.RecordTypeIdentifier
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.PermissionsLastAccess
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearHealthAppData
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_HISTORY
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeRecentAccessUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    private val loadHealthPermissionApps: ILoadHealthPermissionApps =
        FakeHealthPermissionAppsUseCase()
    private val loadAppPermissionsStatusUseCase: ILoadAppPermissionsStatusUseCase =
        FakeLoadAppPermissionsStatusUseCase()
    private val loadRecentAccessUseCase: ILoadRecentAccessUseCase = FakeRecentAccessUseCase()

    @BindValue val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase = mock()
    @BindValue val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    private lateinit var wearConnectedAppsViewModel: WearConnectedAppsViewModel
    lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        Dispatchers.setMain(testDispatcher)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        whenever(healthPermissionReader.getSystemHealthPermissions()).then {
            listOf(READ_HEART_RATE, READ_SKIN_TEMPERATURE, READ_OXYGEN_SATURATION)
        }

        wearConnectedAppsViewModel =
            WearConnectedAppsViewModel(
                context,
                loadHealthPermissionApps,
                loadAppPermissionsStatusUseCase,
                grantPermissionsStatusUseCase,
                revokeHealthPermissionUseCase,
                loadRecentAccessUseCase,
                healthPermissionReader,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).reset()
        (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase).reset()
        (loadRecentAccessUseCase as FakeRecentAccessUseCase).reset()
        wearConnectedAppsViewModel.updateShowSystem(false)
    }

    @Test
    fun loadConnectedApps_whenNoAppsAvailable_returnsEmptyList() = runTest {
        setupConnectedApps(
            listOf(),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )
        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()

        assertThat(actualWearApps.last()).isEmpty()
    }

    @Test
    fun loadConnectedApps_includesSystemApps() = runTest {
        setupSystemAndNonSystemApps()
        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()

        assertThat(actualWearApps.last())
            .containsExactly(
                WearHealthAppData(
                    packageName = appMetadataOne.packageName,
                    appMetadata = appMetadataOne,
                    isSystem = false,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_HEART_RATE_PERMISSION,
                            DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                            DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.HEART_RATE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
                WearHealthAppData(
                    packageName = appMetadataTwo.packageName,
                    appMetadata = appMetadataTwo,
                    isSystem = false,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_HEART_RATE_PERMISSION,
                            GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.HEART_RATE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
                WearHealthAppData(
                    packageName = appMetadataThree.packageName,
                    appMetadata = appMetadataThree,
                    isSystem = false,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                            DENIED_READ_HEART_RATE_PERMISSION,
                        ),
                ),
                WearHealthAppData(
                    packageName = systemAppMetadataOne.packageName,
                    appMetadata = systemAppMetadataOne,
                    isSystem = true,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_HEART_RATE_PERMISSION,
                            GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                            GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.HEART_RATE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
                WearHealthAppData(
                    packageName = systemAppMetadataTwo.packageName,
                    appMetadata = systemAppMetadataTwo,
                    isSystem = true,
                    healthPermissionStatus =
                        listOf(
                            DENIED_READ_HEART_RATE_PERMISSION,
                            DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                            DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.SKIN_TEMPERATURE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
            )
    }

    @Test
    fun loadConnectedApps_filtersOutInvalidPermissions() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        HealthPermissionStatus(
                            healthPermission =
                                HealthPermission.FitnessPermission(
                                    FitnessPermissionType.HEART_RATE,
                                    PermissionsAccessType.WRITE,
                                ),
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission =
                                HealthPermission.FitnessPermission(
                                    FitnessPermissionType.STEPS,
                                    PermissionsAccessType.READ,
                                ),
                            isGranted = false,
                        ),
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        HealthPermissionStatus(
                            healthPermission =
                                HealthPermission.FitnessPermission(
                                    FitnessPermissionType.BODY_FAT,
                                    PermissionsAccessType.READ,
                                ),
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_HEALTH_DATA_HISTORY,
                            isGranted = false,
                        ),
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        assertThat(actualWearApps.last())
            .containsExactly(
                WearHealthAppData(
                    packageName = appMetadataOne.packageName,
                    appMetadata = appMetadataOne,
                    isSystem = false,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_HEART_RATE_PERMISSION,
                            DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                            DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.HEART_RATE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
                WearHealthAppData(
                    packageName = systemAppMetadataOne.packageName,
                    appMetadata = systemAppMetadataOne,
                    isSystem = true,
                    healthPermissionStatus =
                        listOf(
                            GRANTED_READ_HEART_RATE_PERMISSION,
                            GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                        ),
                    lastAccessTime = NOW,
                    accessLogs =
                        listOf(
                            PermissionsLastAccess(
                                healthPermissionType = FitnessPermissionType.HEART_RATE,
                                lastAccessTime = NOW,
                            )
                        ),
                ),
            )
    }

    @Test
    fun loadConnectedApps_updatesSystemHealthPermissions() = runTest {
        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualSystemPermissions = mutableListOf<List<HealthPermission>>()
        val systemPermissionsCollectJob = launch {
            wearConnectedAppsViewModel.systemHealthPermissions.collect { value ->
                actualSystemPermissions.add(value)
            }
        }

        advanceUntilIdle()
        systemPermissionsCollectJob.cancel()

        assertThat(actualSystemPermissions.last())
            .containsExactlyElementsIn(
                listOf(
                    READ_HEART_RATE_PERMISSION,
                    READ_OXYGEN_SATURATION_PERMISSION,
                    READ_SKIN_TEMPERATURE_PERMISSION,
                )
            )
    }

    @Test
    fun loadConnectedApps_sortsSystemHealthPermissions() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_SKIN_TEMPERATURE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualSystemPermissions = mutableListOf<List<HealthPermission>>()
        val systemPermissionsCollectJob = launch {
            wearConnectedAppsViewModel.systemHealthPermissions.collect { value ->
                actualSystemPermissions.add(value)
            }
        }

        advanceUntilIdle()
        systemPermissionsCollectJob.cancel()

        assertThat(actualSystemPermissions.last())
            .containsExactlyElementsIn(
                listOf(
                    READ_HEART_RATE_PERMISSION,
                    READ_SKIN_TEMPERATURE_PERMISSION,
                    READ_OXYGEN_SATURATION_PERMISSION,
                )
            )
    }

    @Test
    fun recentAccess_whenMultipleAccessLogsForDataType_showsLatestAccessTime() = runTest {
        val time1 = NOW.minusSeconds(300)
        val time2 = NOW.minusSeconds(1)
        val time3 = NOW
        val time4 = NOW.plusSeconds(1)
        val time5 = NOW.plusSeconds(40)

        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            time3.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            time1.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            time5.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_SKIN_TEMPERATURE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            time2.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            time3.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            time4.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(2)
        assertThat(result[0].lastAccessTime).isEqualTo(time5)
        assertThat(result[1].lastAccessTime).isEqualTo(time4)
    }

    @Test
    fun recentAccess_readAndInsertAccessLogs_onlyShowReadLog() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.UPSERT,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.plusSeconds(40).toEpochMilli(),
                            Constants.UPSERT,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION),
                            NOW.plusSeconds(40).toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        setupConnectedApps(
            listOf(app1),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(1)
        assertThat(result[0].lastAccessTime).isEqualTo(NOW.plusSeconds(40))
        assertThat(result[0].accessLogs)
            .containsExactly(
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.HEART_RATE,
                    lastAccessTime = NOW,
                ),
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.OXYGEN_SATURATION,
                    lastAccessTime = NOW.plusSeconds(40),
                ),
            )
    }

    @Test
    fun recentAccess_multipleDataTypes_showLatestOneForEach() = runTest {
        val time1 = NOW.minusSeconds(300)
        val time2 = NOW.minusSeconds(1)
        val time3 = NOW
        val time4 = NOW.plusSeconds(1)
        val time5 = NOW.plusSeconds(40)
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            time1.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            time3.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            time5.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION),
                            time4.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION),
                            time2.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        setupConnectedApps(
            listOf(app1),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(1)
        assertThat(result[0].lastAccessTime).isEqualTo(time5)
        assertThat(result[0].accessLogs)
            .containsExactly(
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.HEART_RATE,
                    lastAccessTime = time3,
                ),
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.SKIN_TEMPERATURE,
                    lastAccessTime = time5,
                ),
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.OXYGEN_SATURATION,
                    lastAccessTime = time4,
                ),
            )
    }

    @Test
    fun recentAccess_useCaseResultsFailed_returnsEmptyAccessList() = runTest {
        setupSystemAndNonSystemApps()
        (loadRecentAccessUseCase as FakeRecentAccessUseCase).setForceFail(true)
        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        result.forEach { app ->
            assertThat(app.lastAccessTime).isNull()
            assertThat(app.accessLogs).isEmpty()
        }
    }

    @Test
    fun recentAccess_filtersOutNonSystemHealthPermissionLogs() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_STEPS),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_BODY_FAT),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(2)
        assertThat(result[0].lastAccessTime).isEqualTo(NOW)
        assertThat(result[0].accessLogs)
            .containsExactly(
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.HEART_RATE,
                    lastAccessTime = NOW,
                )
            )
        assertThat(result[1].lastAccessTime).isEqualTo(NOW)
        assertThat(result[1].accessLogs)
            .containsExactly(
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.HEART_RATE,
                    lastAccessTime = NOW,
                )
            )
    }

    @Test
    fun recentAccess_whenNoAccessLogs_returnsEmptyList() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        result.forEach { assertThat(it.accessLogs).isEmpty() }
    }

    @Test
    fun recentAccess_multipleRecordTypesInOneAccessLog_skipProcessing() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE,
                            ),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                            ),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        setupConnectedApps(
            listOf(app1),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(1)
        assertThat(result[0].accessLogs).isEmpty()
    }

    @Test
    fun recentAccess_accessedAppPermissionRevoked_stillDisplayAccessLog() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus = listOf(DENIED_READ_HEART_RATE_PERMISSION),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(
            listOf(app1),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        val actualWearApps = mutableListOf<List<WearHealthAppData>>()
        val wearConnectedAppsCollectJob = launch {
            wearConnectedAppsViewModel.wearHealthApps.collect { value -> actualWearApps.add(value) }
        }

        advanceUntilIdle()
        wearConnectedAppsCollectJob.cancel()
        val result = actualWearApps.last()
        assertThat(result).hasSize(1)
        assertThat(result[0].accessLogs)
            .containsExactly(
                PermissionsLastAccess(
                    healthPermissionType = FitnessPermissionType.HEART_RATE,
                    lastAccessTime = NOW,
                )
            )
    }

    @Test
    fun updateShowSystem_updatesCorrectly() = runTest {
        wearConnectedAppsViewModel.updateShowSystem(true)
        advanceUntilIdle()
        assertThat(wearConnectedAppsViewModel.showSystemFlow.value).isTrue()

        wearConnectedAppsViewModel.updateShowSystem(false)
        advanceUntilIdle()
        assertThat(wearConnectedAppsViewModel.showSystemFlow.value).isFalse()
    }

    @Test
    fun updatePermissionToGrant_grantsPermission_reloadsApps() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
                recentAccess = listOf(),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat(
                (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).numberOfInvocations
            )
            .isEqualTo(2)
        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat((loadRecentAccessUseCase as FakeRecentAccessUseCase).numberOfInvocations)
            .isEqualTo(2)
        // invoked once for each connected app. not invoked in setup since no apps are mocked at
        // that point
        assertThat(
                (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase)
                    .numberOfInvocations
            )
            .isEqualTo(2)

        wearConnectedAppsViewModel.updatePermission(
            READ_SKIN_TEMPERATURE_PERMISSION,
            appMetadataOne,
            grant = true,
        )
        advanceUntilIdle()

        // Since we are mocking the UseCases, we cannot expect the permissions to change here, but
        // we can
        // verify that we call the useCases again for reloading
        verify(grantPermissionsStatusUseCase)
            .invoke(appMetadataOne.packageName, READ_SKIN_TEMPERATURE_PERMISSION.toString())
        assertThat(loadHealthPermissionApps.numberOfInvocations).isEqualTo(3)
        assertThat(loadRecentAccessUseCase.numberOfInvocations).isEqualTo(3)
        assertThat(loadAppPermissionsStatusUseCase.numberOfInvocations).isEqualTo(4)
    }

    @Test
    fun updatePermissionToRevoke_revokesPermission_reloadsApps() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
                recentAccess = listOf(),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat(
                (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).numberOfInvocations
            )
            .isEqualTo(2)
        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat((loadRecentAccessUseCase as FakeRecentAccessUseCase).numberOfInvocations)
            .isEqualTo(2)
        // invoked once for each connected app. not invoked in setup since no apps are mocked at
        // that point
        assertThat(
                (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase)
                    .numberOfInvocations
            )
            .isEqualTo(2)

        wearConnectedAppsViewModel.updatePermission(
            READ_HEART_RATE_PERMISSION,
            appMetadataOne,
            grant = false,
        )
        advanceUntilIdle()
        verify(revokeHealthPermissionUseCase)
            .invoke(appMetadataOne.packageName, READ_HEART_RATE_PERMISSION.toString())

        // Since we are mocking the UseCases, we cannot expect the permissions to change here, but
        // we can
        // verify that we call the useCases again for reloading
        assertThat(loadHealthPermissionApps.numberOfInvocations).isEqualTo(3)
        assertThat(loadRecentAccessUseCase.numberOfInvocations).isEqualTo(3)
        assertThat(loadAppPermissionsStatusUseCase.numberOfInvocations).isEqualTo(4)
    }

    @Test
    fun updateLastPermissionToRevoke_revokesPermissionAndBackground_reloadsApps() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                recentAccess = listOf(),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat(
                (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).numberOfInvocations
            )
            .isEqualTo(2)
        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat((loadRecentAccessUseCase as FakeRecentAccessUseCase).numberOfInvocations)
            .isEqualTo(2)
        // invoked once for each connected app. not invoked in setup since no apps are mocked at
        // that point
        assertThat(
                (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase)
                    .numberOfInvocations
            )
            .isEqualTo(2)

        wearConnectedAppsViewModel.updatePermission(
            READ_HEART_RATE_PERMISSION,
            appMetadataOne,
            grant = false,
        )
        advanceUntilIdle()
        verify(revokeHealthPermissionUseCase)
            .invoke(appMetadataOne.packageName, READ_HEART_RATE_PERMISSION.toString())
        verify(revokeHealthPermissionUseCase)
            .invoke(appMetadataOne.packageName, READ_HEALTH_DATA_IN_BACKGROUND.additionalPermission)

        // Since we are mocking the UseCases, we cannot expect the permissions to change here, but
        // we can
        // verify that we call the useCases again for reloading
        assertThat(loadHealthPermissionApps.numberOfInvocations).isEqualTo(3)
        assertThat(loadRecentAccessUseCase.numberOfInvocations).isEqualTo(3)
        assertThat(loadAppPermissionsStatusUseCase.numberOfInvocations).isEqualTo(4)
    }

    @Test
    fun removePermissionForAllApps_removesNonSystemAppsOnly() = runTest {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                recentAccess = listOf(),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus = listOf(GRANTED_READ_HEART_RATE_PERMISSION),
                recentAccess = listOf(),
            )
        setupConnectedApps(
            listOf(app1, app2),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        advanceUntilIdle()

        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat(
                (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).numberOfInvocations
            )
            .isEqualTo(2)
        // invoked once in the setup method, and once when calling loadConnectedApps()
        assertThat((loadRecentAccessUseCase as FakeRecentAccessUseCase).numberOfInvocations)
            .isEqualTo(2)
        // invoked once for each connected app. not invoked in setup since no apps are mocked at
        // that point
        assertThat(
                (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase)
                    .numberOfInvocations
            )
            .isEqualTo(2)

        wearConnectedAppsViewModel.removeFitnessPermissionForAllApps(READ_HEART_RATE_PERMISSION)
        advanceUntilIdle()

        verify(revokeHealthPermissionUseCase)
            .invoke(appMetadataOne.packageName, READ_HEART_RATE_PERMISSION.toString())
        verify(revokeHealthPermissionUseCase)
            .invoke(appMetadataOne.packageName, READ_HEALTH_DATA_IN_BACKGROUND.additionalPermission)
        verify(revokeHealthPermissionUseCase, never())
            .invoke(eq(systemAppMetadataOne.packageName), any())

        // Since we are mocking the UseCases, we cannot expect the permissions to change here, but
        // we can
        // verify that we call the useCases again for reloading
        assertThat(loadHealthPermissionApps.numberOfInvocations).isEqualTo(3)
        assertThat(loadRecentAccessUseCase.numberOfInvocations).isEqualTo(3)
        assertThat(loadAppPermissionsStatusUseCase.numberOfInvocations).isEqualTo(4)
    }

    private fun setupSystemAndNonSystemApps() {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEART_RATE_PERMISSION,
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app5 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataTwo,
                permissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(
            listOf(app1, app2, app3, app4, app5),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )
    }
}
