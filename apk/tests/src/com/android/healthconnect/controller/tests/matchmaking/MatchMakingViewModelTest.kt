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

package com.android.healthconnect.controller.tests.matchmaking

import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.StepsRecord
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.LoadingFailed
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.WithData
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MatchMakingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @BindValue lateinit var appInfoReader: AppInfoReader

    private val getMatchingAppsUseCase: GetMatchingAppsUseCase = mock()

    private lateinit var viewModel: MatchmakingViewModel

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        appInfoReader = createFakeAppInfoReader()
        viewModel = MatchmakingViewModel(getMatchingAppsUseCase, appInfoReader, SavedStateHandle())
    }

    @Test
    fun loadMatchmakingApps_withSuccess_updatesStateToWithData() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val appMetadata = AppMetadata(TEST_APP_NAME_2, TEST_APP_PACKAGE_NAME_2, null)
        val expected =
            setOf(
                MatchmakingAppData(
                    appMetadata,
                    setOf(
                        HealthPermission.fromPermissionString(WRITE_STEPS)
                            as HealthPermission.FitnessPermission
                    ),
                )
            )
        val useCaseResult = UseCaseResults.Success(expected)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)

        viewModel.loadMatchmakingApps(packageName, recordTypes)

        val state = viewModel.matchmakingState.value
        assertThat(state).isInstanceOf(WithData::class.java)
        assertThat((state as WithData).apps).isEqualTo(expected)
    }

    @Test
    fun loadMatchmakingApps_withError_updatesStateToLoadingFailed() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val exception = IllegalStateException("Error")
        val useCaseResult = UseCaseResults.Failed(exception)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)

        viewModel.loadMatchmakingApps(packageName, recordTypes)

        assertThat(viewModel.matchmakingState.value).isInstanceOf(LoadingFailed::class.java)
    }
}
