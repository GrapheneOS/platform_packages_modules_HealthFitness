/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import android.healthconnect.TimeRangeFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.connectedApps.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedApps.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.settings.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.shared.AppInfoReader
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link ConnectedAppFragment} . */
@HiltViewModel
class AppPermissionViewModel
@Inject
constructor(
        private val appInfoReader: AppInfoReader,
        private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
        private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase,
        private val revokePermissionsStatusUseCase: RevokeHealthPermissionUseCase,
        private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase,
        private val deleteAppDataUseCase: DeleteAppDataUseCase,
        private val loadAccessDateUseCase: LoadAccessDateUseCase
) : ViewModel() {

    private val _appPermissions = MutableLiveData<List<HealthPermissionStatus>>(emptyList())
    val appPermissions: LiveData<List<HealthPermissionStatus>>
        get() = _appPermissions

    private val _allAppPermissionsGranted = MutableLiveData<Boolean>(false)
    val allAppPermissionsGranted: LiveData<Boolean>
        get() = _allAppPermissionsGranted

    private val _atLeastOnePermissionGranted = MutableLiveData(false)
    val atLeastOnePermissionGranted: LiveData<Boolean>
        get() = _atLeastOnePermissionGranted

    private val _appInfo = MutableLiveData<AppMetadata>()
    val appInfo: LiveData<AppMetadata>
        get() = _appInfo

    fun loadForPackage(packageName: String) {
        viewModelScope.launch {
            val permissions = loadAppPermissionsStatusUseCase.invoke(packageName)
            _appPermissions.postValue(permissions)
            _allAppPermissionsGranted.postValue(permissions.all { it.isGranted })
            _atLeastOnePermissionGranted.postValue(permissions.any { it.isGranted })
        }
    }

    fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appInfo.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    fun loadAccessDate(packageName: String): Instant? {
        return loadAccessDateUseCase.invoke(packageName)
    }

    fun updatePermission(packageName: String, healthPermission: HealthPermission, grant: Boolean) {
        if (grant) {
            grantPermissionsStatusUseCase.invoke(packageName, healthPermission.toString())
        } else {
            // TODO(magdi) find revoke permission reasons
            revokePermissionsStatusUseCase.invoke(packageName, healthPermission.toString(), "user")
        }
        loadForPackage(packageName)
    }

    fun grantAllPermissions(packageName: String) {
        appPermissions.value?.forEach {
            grantPermissionsStatusUseCase.invoke(packageName, it.healthPermission.toString())
        }
        loadForPackage(packageName)
    }

    fun revokeAllPermissions(packageName: String) {
        viewModelScope.launch {
            // TODO(b/245514289) move this to a background thread.
            revokeAllHealthPermissionsUseCase.invoke(packageName)
            loadForPackage(packageName)
        }
    }

    fun deleteAppData(packageName: String, appName: String) {
        viewModelScope.launch {
            val appData = DeletionType.DeletionTypeAppData(packageName, appName)
            val timeRangeFilter =
                TimeRangeFilter.Builder(Instant.EPOCH, Instant.ofEpochMilli(Long.MAX_VALUE)).build()
            deleteAppDataUseCase.invoke(appData, timeRangeFilter)
        }
    }
}
