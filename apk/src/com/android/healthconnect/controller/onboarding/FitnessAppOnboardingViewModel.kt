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

package com.android.healthconnect.controller.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.request.FitnessPermissionsFragment
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class FitnessAppOnboardingViewModel
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
    private val grantHealthPermissionUseCase: GrantHealthPermissionUseCase,
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
) : ViewModel() {
    companion object {
        private const val TAG = "FitnessAppOnboardingViewModel"
    }

    private var invocations = 0

    private val _appMetaData = MutableLiveData<AppMetadata>()
    private val _localFitnessPermissions = MutableLiveData<Map<FitnessPermission, Boolean>>()

    /** Used to control the enabled state of the Allow all switch */
    private val _allFitnessPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_localFitnessPermissions) {
                postValue(_localFitnessPermissions.value.orEmpty().values.all { it })
            }
        }
    val allFitnessPermissionsGranted: LiveData<Boolean>
        get() = _allFitnessPermissionsGranted

    private lateinit var initialPermissions: Map<FitnessPermission, Boolean>

    private lateinit var packageName: String

    /** Whether to modify the historic access text on the [FitnessPermissionsFragment] */
    private var historyAccessGranted: Boolean = false

    private val _fitnessAppOnboardingFragmentState =
        MediatorLiveData<FitnessAppOnboardingFragmentState>().apply {
            addSource(_appMetaData) { appMetadata ->
                this.postValue(
                    getFitnessAppOnboardingFragmentState(
                        appMetadata,
                        _localFitnessPermissions.value.orEmpty(),
                    )
                )
            }
            addSource(_localFitnessPermissions) { permissionsMap ->
                this.postValue(
                    getFitnessAppOnboardingFragmentState(_appMetaData.value, permissionsMap)
                )
            }
        }
    val fitnessAppOnboardingFragmentState: LiveData<FitnessAppOnboardingFragmentState>
        get() = _fitnessAppOnboardingFragmentState

    private fun getFitnessAppOnboardingFragmentState(
        appMetadata: AppMetadata?,
        permissionsMap: Map<FitnessPermission, Boolean>,
    ): FitnessAppOnboardingFragmentState {
        if (appMetadata == null) {
            return FitnessAppOnboardingFragmentState.NoFitnessData
        }

        val containsReadPermissions =
            permissionsMap.keys.any { it.permissionsAccessType == PermissionsAccessType.READ }
        val containsWritePermissions =
            permissionsMap.keys.any { it.permissionsAccessType == PermissionsAccessType.WRITE }
        return if (containsReadPermissions && containsWritePermissions) {
            FitnessAppOnboardingFragmentState.ShowFitnessReadWrite(
                appMetadata,
                permissionsMap,
                historyAccessGranted,
            )
        } else if (containsReadPermissions) {
            FitnessAppOnboardingFragmentState.ShowFitnessRead(
                appMetadata,
                permissionsMap,
                historyAccessGranted,
            )
        } else if (containsWritePermissions) {
            FitnessAppOnboardingFragmentState.ShowFitnessWrite(appMetadata, permissionsMap)
        } else {
            FitnessAppOnboardingFragmentState.NoFitnessData
        }
    }

    fun init(packageName: String) {
        this.packageName = packageName
        loadAppInfo(packageName)
        loadPermissions(packageName)
    }

    private fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appMetaData.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    private fun loadPermissions(packageName: String) {
        viewModelScope.launch {
            val healthPermissions = loadAppPermissionsStatusUseCase.invoke(packageName)

            historyAccessGranted =
                healthPermissions
                    .filter { (it.healthPermission is HealthPermission.AdditionalPermission) }
                    .filter { it.isGranted }
                    .map { (it.healthPermission as HealthPermission.AdditionalPermission) }
                    .any { it.isHistoryReadPermission() }

            initialPermissions =
                healthPermissions
                    .filter { it.healthPermission is FitnessPermission }
                    .associate { (it.healthPermission as FitnessPermission) to it.isGranted }

            // Prevent this from resetting if we are just rotating the screen
            if (invocations == 0) {
                _localFitnessPermissions.postValue(initialPermissions)
            }
            invocations += 1
        }
    }

    fun updatePermission(permission: FitnessPermission, grant: Boolean) {
        val updatedLocalPermissions = _localFitnessPermissions.value.orEmpty().toMutableMap()
        updatedLocalPermissions[permission] = grant
        _localFitnessPermissions.value = updatedLocalPermissions
    }

    fun updateAllPermissions(grant: Boolean) {
        val updatedLocalPermissions = _localFitnessPermissions.value.orEmpty().toMutableMap()
        updatedLocalPermissions.replaceAll { k, v -> grant }
        _localFitnessPermissions.value = updatedLocalPermissions
    }

    fun done() {
        val updatedLocalPermissions = _localFitnessPermissions.value.orEmpty().toMutableMap()
        updatedLocalPermissions.forEach { permission, isGranted ->
            if (isGranted) {
                grantHealthPermissionUseCase.invoke(packageName, permission.toString())
            } else {
                // revoke permission so that the backend can see that this app was already
                // interacted with and not count it as a potential app in the future
                revokeHealthPermissionUseCase.invoke(packageName, permission.toString())
            }
        }
    }

    sealed class FitnessAppOnboardingFragmentState {
        object NoFitnessData : FitnessAppOnboardingFragmentState()

        data class ShowFitnessRead(
            val appMetadata: AppMetadata,
            val permissionsMap: Map<FitnessPermission, Boolean>,
            val historyGranted: Boolean,
        ) : FitnessAppOnboardingFragmentState()

        data class ShowFitnessWrite(
            val appMetadata: AppMetadata,
            val permissionsMap: Map<FitnessPermission, Boolean>,
        ) : FitnessAppOnboardingFragmentState()

        data class ShowFitnessReadWrite(
            val appMetadata: AppMetadata,
            val permissionsMap: Map<FitnessPermission, Boolean>,
            val historyGranted: Boolean,
        ) : FitnessAppOnboardingFragmentState()
    }
}
