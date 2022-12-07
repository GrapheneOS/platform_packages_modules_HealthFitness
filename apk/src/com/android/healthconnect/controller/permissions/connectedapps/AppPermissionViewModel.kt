package com.android.healthconnect.controller.permissions.connectedapps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.connectedApp.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedApp.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.AppInfoReader
import com.android.healthconnect.controller.shared.AppMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AppPermissionViewModel"
    }

    private val _appPermissions = MutableLiveData<List<HealthPermissionStatus>>(emptyList())
    val appPermissions: LiveData<List<HealthPermissionStatus>>
        get() = _appPermissions

    private val _allAppPermissionsGranted = MutableLiveData<Boolean>(false)
    val allAppPermissionsGranted: LiveData<Boolean>
        get() = _allAppPermissionsGranted

    private val _appInfo = MutableLiveData<AppMetadata>()
    val appInfo: LiveData<AppMetadata>
        get() = _appInfo

    fun loadForPackage(packageName: String) {
        viewModelScope.launch {
            val permissions = loadAppPermissionsStatusUseCase.invoke(packageName)
            _appPermissions.postValue(permissions)
            permissions.forEach {
                Log.i(TAG, "loadForPackage: ${it.healthPermission} ${it.isGranted}")
            }
            _allAppPermissionsGranted.postValue(permissions.all { it.isGranted })
        }
    }

    fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appInfo.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    fun updatePermission(packageName: String, healthPermission: HealthPermission, grant: Boolean) {
        if (grant) {
            grantPermissionsStatusUseCase.invoke(packageName, healthPermission.toString())
        } else {
            // TODO(magdi) find revoke permission reasons
            revokePermissionsStatusUseCase.invoke(packageName, healthPermission.toString(), "user")
        }
        _allAppPermissionsGranted.postValue(_appPermissions.value?.all { it.isGranted })
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
}
