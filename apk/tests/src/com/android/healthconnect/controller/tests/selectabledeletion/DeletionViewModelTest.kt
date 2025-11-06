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
package com.android.healthconnect.controller.tests.selectabledeletion

import android.health.connect.datatypes.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllSymptomsDataFromInactiveAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeleteEntriesUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesFromAppUseCase
import com.android.healthconnect.controller.selectabledeletion.api.DeletePermissionTypesUseCase
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
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
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeletionViewModelTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val deletePermissionTypesUseCase: DeletePermissionTypesUseCase =
        mock(DeletePermissionTypesUseCase::class.java)
    private val deleteEntriesUseCase: DeleteEntriesUseCase = mock(DeleteEntriesUseCase::class.java)
    private val deleteAppDataUseCase: DeleteAppDataUseCase = mock(DeleteAppDataUseCase::class.java)
    private val deletePermissionTypesFromAppUseCase: DeletePermissionTypesFromAppUseCase =
        mock(DeletePermissionTypesFromAppUseCase::class.java)
    private val deleteAllSymptomsDataFromInactiveAppUseCase:
        DeleteAllSymptomsDataFromInactiveAppUseCase =
        mock(DeleteAllSymptomsDataFromInactiveAppUseCase::class.java)

    private lateinit var viewModel: DeletionViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel =
            DeletionViewModel(
                deleteAppDataUseCase,
                deletePermissionTypesUseCase,
                deleteEntriesUseCase,
                deletePermissionTypesFromAppUseCase,
                deleteAllSymptomsDataFromInactiveAppUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun permissionTypes_resetPermissionTypesReloadNeeded_valueSetCorrectly() = runTest {
        val testObserver = TestObserver<Boolean>()
        viewModel.permissionTypesReloadNeeded.observeForever(testObserver)
        viewModel.resetPermissionTypesReloadNeeded()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(false)
    }

    @Test
    fun permissionTypes_deleteSet_setCorrectly() {
        val deleteSet =
            setOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.HEART_RATE,
                FitnessPermissionType.STEPS,
            )
        val numPermissionTypes = 4
        viewModel.setDeletionType(
            DeletionType.DeleteHealthPermissionTypes(deleteSet, numPermissionTypes)
        )

        assertThat(viewModel.getDeletionType() is DeletionType.DeleteHealthPermissionTypes).isTrue()
        assertThat(
                (viewModel.getDeletionType() as DeletionType.DeleteHealthPermissionTypes)
                    .healthPermissionTypes
            )
            .isEqualTo(
                setOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.HEART_RATE,
                    FitnessPermissionType.STEPS,
                )
            )
        assertThat(
                (viewModel.getDeletionType() as DeletionType.DeleteHealthPermissionTypes)
                    .totalPermissionTypes
            )
            .isEqualTo(numPermissionTypes)
    }

    @Test
    fun permissionTypes_delete_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.DISTANCE),
                totalPermissionTypes = 4,
            )
        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()

        verify(deletePermissionTypesUseCase).invoke(deletionType)
    }

    @Test
    fun permissionTypes_deleteWithRemovingPermissions_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.DISTANCE),
                totalPermissionTypes = 1,
            )
        viewModel.removePermissions = true
        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()

        verify(deletePermissionTypesUseCase).invoke(deletionType)
    }

    @Test
    fun permissionTypes_deleteFitnessAndMedical_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.DISTANCE, MedicalPermissionType.VACCINES),
                4,
            )
        viewModel.setDeletionType(deletionType = deletionType)
        viewModel.delete()
        advanceUntilIdle()

        verify(deletePermissionTypesUseCase).invoke(deletionType)
    }

    @Test
    fun permissionTypes_deleteMedical_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteHealthPermissionTypes(setOf(MedicalPermissionType.VACCINES), 2)
        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()

        verify(deletePermissionTypesUseCase).invoke(deletionType)
    }

    // TODO
    @Test
    fun permissionTypesFromApp_resetAppPermissionTypesReloadNeeded_valueSetCorrectly() = runTest {
        val testObserver = TestObserver<Boolean>()
        viewModel.appPermissionTypesReloadNeeded.observeForever(testObserver)
        viewModel.resetAppPermissionTypesReloadNeeded()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(false)
    }

    @Test
    fun permissionTypesFromApp_deleteSet_setCorrectly() = runTest {
        val deleteSet =
            setOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.HEART_RATE,
                FitnessPermissionType.STEPS,
            )
        val numPermissionTypes = 4
        viewModel.setDeletionType(
            DeletionType.DeleteHealthPermissionTypesFromApp(
                deleteSet,
                numPermissionTypes,
                "some.package",
                "appName",
            )
        )

        assertThat(viewModel.getDeletionType() is DeletionType.DeleteHealthPermissionTypesFromApp)
            .isTrue()
        val actualDeletionType =
            viewModel.getDeletionType() as DeletionType.DeleteHealthPermissionTypesFromApp
        assertThat(actualDeletionType.healthPermissionTypes)
            .isEqualTo(
                setOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.HEART_RATE,
                    FitnessPermissionType.STEPS,
                )
            )
        assertThat(actualDeletionType.totalPermissionTypes).isEqualTo(numPermissionTypes)
        assertThat(actualDeletionType.packageName).isEqualTo("some.package")
        assertThat(actualDeletionType.appName).isEqualTo("appName")
    }

    @Test
    fun permissionTypesFromApp_deleteFitnessWithoutRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(FitnessPermissionType.DISTANCE)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    healthPermissionTypes = permissions,
                    totalPermissionTypes = 4,
                    packageName = "some.package",
                    appName = "appName",
                )
            viewModel.setDeletionType(deletionType)
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, false)
        }

    @Test
    fun permissionTypesFromApp_deleteFitnessWithRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(FitnessPermissionType.STEPS)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    permissions,
                    totalPermissionTypes = 4,
                    "some.package",
                    "appName",
                )
            viewModel.setDeletionType(deletionType)
            viewModel.removePermissions = true
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, true)
        }

    @Test
    fun permissionTypesFromApp_deleteMedicalWithoutRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(MedicalPermissionType.VACCINES)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    healthPermissionTypes = permissions,
                    totalPermissionTypes = 2,
                    packageName = "some.package",
                    appName = "appName",
                )

            viewModel.setDeletionType(deletionType)
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, false)
        }

    @Test
    fun permissionTypesFromApp_deleteMedicalWithRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(MedicalPermissionType.MEDICATIONS)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    healthPermissionTypes = permissions,
                    totalPermissionTypes = 1,
                    packageName = "some.package",
                    appName = "appName",
                )
            viewModel.setDeletionType(deletionType)
            viewModel.removePermissions = true
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, true)
        }

    @Test
    fun permissionTypesFromApp_deleteFitnessAndMedicalWithoutRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(MedicalPermissionType.VACCINES, FitnessPermissionType.STEPS)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    healthPermissionTypes = permissions,
                    totalPermissionTypes = 3,
                    packageName = "some.package",
                    appName = "appName",
                )
            viewModel.setDeletionType(deletionType)
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, false)
        }

    @Test
    fun permissionTypesFromApp_deleteFitnessAndMedicalWithRemovingPermissions_deletionInvokedCorrectly() =
        runTest {
            val permissions = setOf(MedicalPermissionType.VACCINES, FitnessPermissionType.STEPS)
            val deletionType =
                DeletionType.DeleteHealthPermissionTypesFromApp(
                    healthPermissionTypes = permissions,
                    totalPermissionTypes = 2,
                    packageName = "some.package",
                    appName = "appName",
                )
            viewModel.setDeletionType(deletionType)
            viewModel.removePermissions = true
            viewModel.delete()
            advanceUntilIdle()

            verify(deletePermissionTypesFromAppUseCase).invoke("some.package", permissions, true)
        }

    @Test
    fun entries_resetEntriesReloadNeeded_valueSetCorrectly() = runTest {
        val testObserver = TestObserver<Boolean>()
        viewModel.entriesReloadNeeded.observeForever(testObserver)
        viewModel.resetEntriesReloadNeeded()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(false)
    }

    @Test
    fun entries_deleteSet_setCorrectly() {
        val deletionType =
            DeletionType.DeleteEntries(
                mapOf(
                    FORMATTED_STEPS.uuid to FORMATTED_STEPS.dataType,
                    FORMATTED_STEPS_2.uuid to FORMATTED_STEPS_2.dataType,
                ),
                4,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = Instant.now(),
            )
        viewModel.setDeletionType(deletionType)

        assertThat(viewModel.getDeletionType() is DeletionType.DeleteEntries).isTrue()
        assertThat((viewModel.getDeletionType() as DeletionType.DeleteEntries).idsToDataTypes)
            .containsExactlyEntriesIn(
                mapOf(
                    FORMATTED_STEPS.uuid to FORMATTED_STEPS.dataType,
                    FORMATTED_STEPS_2.uuid to FORMATTED_STEPS_2.dataType,
                )
            )
    }

    @Test
    fun entries_delete_deletionInvokesCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteEntries(
                mapOf(
                    FORMATTED_STEPS.uuid to FORMATTED_STEPS.dataType,
                    FORMATTED_STEPS_2.uuid to FORMATTED_STEPS_2.dataType,
                ),
                4,
                period = DateNavigationPeriod.PERIOD_DAY,
                Instant.now(),
            )
        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()

        verify(deleteEntriesUseCase).invoke(deletionType)
    }

    @Test
    fun appEntries_delete_deletionInvokesCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteEntriesFromApp(
                mapOf(
                    FORMATTED_STEPS.uuid to FORMATTED_STEPS.dataType,
                    FORMATTED_STEPS_2.uuid to FORMATTED_STEPS_2.dataType,
                ),
                "package.name",
                "appName",
                4,
                DateNavigationPeriod.PERIOD_DAY,
                Instant.now(),
            )

        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()
        verify(deleteEntriesUseCase).invoke(deletionType.toDeleteEntries())
    }

    @Test
    fun appData_delete_deletionInvokedCorrectly() = runTest {
        val deletionType = DeletionType.DeleteAppData("package.name", "appName")

        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()
        verify(deleteAppDataUseCase).invoke(deletionType)
    }

    @Test
    fun inactiveAppData_delete_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteInactiveAppData(
                packageName = "package.name",
                appName = "app.name",
                healthPermissionType = FitnessPermissionType.STEPS,
            )

        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()
        verify(deletePermissionTypesFromAppUseCase)
            .invoke("package.name", setOf(FitnessPermissionType.STEPS))
    }

    @Test
    fun inactiveSymptomData_delete_deletionInvokedCorrectly() = runTest {
        val deletionType =
            DeletionType.DeleteAllSymptomsDataFromInactiveApp(
                packageName = "package.name",
                appName = "app.name",
            )

        viewModel.setDeletionType(deletionType)
        viewModel.delete()
        advanceUntilIdle()
        verify(deleteAllSymptomsDataFromInactiveAppUseCase).invoke(deletionType)
    }
}

private val FORMATTED_STEPS =
    FormattedEntry.FormattedDataEntry(
        uuid = "test_id",
        header = "7:06 - 7:06",
        headerA11y = "from 7:06 to 7:06",
        title = "12 steps",
        titleA11y = "12 steps",
        dataType = StepsRecord::class,
    )
private val FORMATTED_STEPS_2 =
    FormattedEntry.FormattedDataEntry(
        uuid = "test_id_2",
        header = "8:06 - 8:06",
        headerA11y = "from 8:06 to 8:06",
        title = "15 steps",
        titleA11y = "15 steps",
        dataType = StepsRecord::class,
    )
