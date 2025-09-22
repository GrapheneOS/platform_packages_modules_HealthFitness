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

package com.android.healthconnect.controller.newHome

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_FITNESS
import com.android.healthconnect.controller.shared.Constants.LOCK_SCREEN_BANNER_SEEN_MEDICAL
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.KeyguardManagerUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val loadHealthPermissionApps: ILoadHealthPermissionApps,
    private val loadAllDataUseCase: AllDataUseCase,
    private val keyguardManagerUtil: KeyguardManagerUtil,
) : ViewModel() {
    companion object {
        private const val TAG = "NewHomeViewModel"
    }

    private val sharedPreferences =
        context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
    private var _isDeviceSecure = true
    private var _isBannerSeenWithFitnessData = true
    private var _isBannerSeenWithMedicalData = true

    // TODO or mediatorLiveData?
    private val _homeFragmentState = MutableLiveData<HomeFragmentState>()
    val homeFragmentState: LiveData<HomeFragmentState>
        get() = _homeFragmentState

    init {
        loadData()
    }

    fun loadData() {
        _homeFragmentState.postValue(HomeFragmentState.Loading)
        viewModelScope.launch {
            val appsResult = loadHealthPermissionApps.invoke(Unit)
            val dataResult = loadAllDataUseCase.loadHasAnyMedicalData()

            _isDeviceSecure = keyguardManagerUtil.isDeviceSecure(context) != false
            _isBannerSeenWithFitnessData =
                sharedPreferences.getBoolean(LOCK_SCREEN_BANNER_SEEN_FITNESS, false)
            _isBannerSeenWithMedicalData =
                sharedPreferences.getBoolean(LOCK_SCREEN_BANNER_SEEN_MEDICAL, false)

            if (appsResult is UseCaseResults.Failed || dataResult is UseCaseResults.Failed) {
                _homeFragmentState.postValue(HomeFragmentState.Error)
            } else {
                val newData =
                    HomeFragmentState.WithData(
                        connectedApps =
                            (appsResult as UseCaseResults.Success)
                                .data
                                .filter {
                                    it.status != ConnectedAppStatus.INACTIVE &&
                                        it.status != ConnectedAppStatus.NEEDS_UPDATE
                                }
                                .sortedWith(
                                    compareBy<ConnectedAppMetadata> { getSortOrder(it.status) }
                                        .thenBy { it.appMetadata.appName }
                                )
                    )
                _homeFragmentState.postValue(newData)
            }
        }
    }

    private fun getSortOrder(status: ConnectedAppStatus): Int {
        return when (status) {
            ConnectedAppStatus.ALLOWED -> 1
            ConnectedAppStatus.DENIED -> 2
            else -> 3
        }
    }

    sealed class HomeFragmentState {
        object Loading : HomeFragmentState()

        object Error : HomeFragmentState()

        data class WithData(
            val connectedApps: List<ConnectedAppMetadata>,
            val bannerState: HomeBannerState = HomeBannerState.NoBanner,
        ) : HomeFragmentState()
    }

    sealed class HomeBannerState {
        object NoBanner : HomeBannerState()

        data class ShowBanners(val bannerList: List<BannerData>) : HomeBannerState()
    }

    // TODO BannerFactory
    data class BannerData(val title: String, val summary: String, val buttonText: String)
}
