/*
 * Copyright (C) 2026 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.tests.permissions.request.wear

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.MedicalScreenState
import com.android.healthconnect.controller.permissions.request.PermissionsActivityState
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.permissions.request.wear.WearGrantPermissionsActivity
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WearGrantPermissionsActivityTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)

    private val context: Context = ApplicationProvider.getApplicationContext()

    // Initialize these BEFORE the hiltRule.inject() to ensure they are never null
    private val healthPermissionsLiveData = MutableLiveData<List<HealthPermission>>()
    private val appMetadataLiveData = MutableLiveData<AppMetadata>()
    private val fitnessScreenState = MutableLiveData<FitnessScreenState>()
    private val medicalScreenState = MutableLiveData<MedicalScreenState>()
    private val additionalScreenState = MutableLiveData<AdditionalScreenState>()
    private val permissionsActivityState = MutableLiveData<PermissionsActivityState>()
    private val fitnessPermissionsList = MutableLiveData<List<HealthPermission.FitnessPermission>>()
    private val additionalPermissionsList =
        MutableLiveData<List<HealthPermission.AdditionalPermission>>()
    private val grantedMedicalPermissions =
        MutableLiveData<Set<HealthPermission.MedicalPermission>>()
    private val grantedFitnessPermissions =
        MutableLiveData<Set<HealthPermission.FitnessPermission>>()
    private val grantedAdditionalPermissions =
        MutableLiveData<Set<HealthPermission.AdditionalPermission>>()

    @BindValue @JvmField val mockViewModel: RequestPermissionViewModel = mock()

    @Before
    fun setup() {
        // Explicitly stub every possible LiveData getter in the ViewModel
        whenever(mockViewModel.grantableHealthPermissionsList).thenReturn(healthPermissionsLiveData)
        whenever(mockViewModel.appMetadata).thenReturn(appMetadataLiveData)
        whenever(mockViewModel.fitnessScreenState).thenReturn(fitnessScreenState)
        whenever(mockViewModel.medicalScreenState).thenReturn(medicalScreenState)
        whenever(mockViewModel.additionalScreenState).thenReturn(additionalScreenState)
        whenever(mockViewModel.permissionsActivityState).thenReturn(permissionsActivityState)
        whenever(mockViewModel.fitnessPermissionsList).thenReturn(fitnessPermissionsList)
        whenever(mockViewModel.additionalPermissionsList).thenReturn(additionalPermissionsList)
        whenever(mockViewModel.grantedMedicalPermissions).thenReturn(grantedMedicalPermissions)
        whenever(mockViewModel.grantedFitnessPermissions).thenReturn(grantedFitnessPermissions)
        whenever(mockViewModel.grantedAdditionalPermissions)
            .thenReturn(grantedAdditionalPermissions)

        // Provide non-null defaults to prevent Compose observeAsState NPE
        fitnessScreenState.postValue(FitnessScreenState.NoFitnessData)
        medicalScreenState.postValue(MedicalScreenState.NoMedicalData)
        additionalScreenState.postValue(AdditionalScreenState.NoAdditionalData)
        permissionsActivityState.postValue(PermissionsActivityState.NoPermissions)
        grantedMedicalPermissions.postValue(emptySet())
        grantedFitnessPermissions.postValue(emptySet())
        grantedAdditionalPermissions.postValue(emptySet())

        runBlocking {
            // Using mockito-kotlin's 'any()' which safely infers the non-null String type
            whenever(mockViewModel.isAnyPermissionUserFixed(any(), any())).thenReturn(false)
        }

        // Idiomatic mockito-kotlin stubbing block
        val mockMetadata =
            mock<AppMetadata> {
                on { packageName } doReturn "android.permissionui.cts.usepermission"
                on { appName } doReturn "CTS Test App"
            }
        appMetadataLiveData.postValue(mockMetadata)

        hiltRule.inject()
    }

    private fun createStartIntent(): Intent {
        return Intent(context, WearGrantPermissionsActivity::class.java).apply {
            putExtra(
                EXTRA_REQUEST_PERMISSIONS_NAMES,
                arrayOf("android.permission.health.READ_HEART_RATE"),
            )
            putExtra(Intent.EXTRA_PACKAGE_NAME, "android.permissionui.cts.usepermission")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun onCreate_whenPermissionsLoading_doesNotFinish() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            healthPermissionsLiveData.value = null
        }

        val scenario = ActivityScenario.launch<WearGrantPermissionsActivity>(createStartIntent())

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        scenario.close()
    }

    @Test
    fun onCreate_whenPermissionsEmptyAfterLoad_finishes() {
        val scenario = ActivityScenario.launch<WearGrantPermissionsActivity>(createStartIntent())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            healthPermissionsLiveData.value = emptyList()
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
        scenario.close()
    }

    @Test
    fun onCreate_whenPermissionsLoaded_staysAlive() {
        val scenario = ActivityScenario.launch<WearGrantPermissionsActivity>(createStartIntent())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Use a REAL permission object instead of a mock so Compose has actual data to draw
            val realPermission =
                HealthPermission.fromPermissionString("android.permission.health.READ_HEART_RATE")
                    as HealthPermission.FitnessPermission

            // Mirror the expected state transition
            healthPermissionsLiveData.value = listOf(realPermission)
            fitnessPermissionsList.value = listOf(realPermission)
            permissionsActivityState.value = PermissionsActivityState.ShowFitness
            fitnessScreenState.value =
                FitnessScreenState.ShowFitnessRead(
                    hasMedical = false,
                    appMetadata = appMetadataLiveData.value!!,
                    fitnessPermissions = listOf(realPermission),
                    historyGranted = false,
                )
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        scenario.close()
    }
}
