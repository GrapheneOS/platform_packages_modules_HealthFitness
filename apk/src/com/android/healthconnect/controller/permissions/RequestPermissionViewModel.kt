package com.android.healthconnect.controller.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.shared.AppInfoReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link PermissionsFragment} . */
@HiltViewModel
class RequestPermissionViewModel
@Inject
constructor(
        private val appInfoReader: AppInfoReader,
        private val grantHealthPermissionUseCase: GrantHealthPermissionUseCase,
        private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
        private val getGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase
) : ViewModel() {

    private val _appMetaData = MutableLiveData<AppMetadata>()
    val appMetadata: LiveData<AppMetadata>
        get() = _appMetaData

    private val _permissionsList = MutableLiveData<List<HealthPermission>>()
    val permissionsList: LiveData<List<HealthPermission>>
        get() = _permissionsList

    private val _grantedPermissions = MutableLiveData<Set<HealthPermission>>(emptySet())
    val grantedPermissions: LiveData<Set<HealthPermission>>
        get() = _grantedPermissions

    private val _allPermissionsGranted =
        MediatorLiveData<Boolean>(false).apply {
            addSource(_permissionsList) {
                postValue(isAllPermissionsGranted(permissionsList, grantedPermissions))
            }
            addSource(_grantedPermissions) {
                postValue(isAllPermissionsGranted(permissionsList, grantedPermissions))
            }
        }
    val allPermissionsGranted: LiveData<Boolean>
        get() = _allPermissionsGranted

    private fun isAllPermissionsGranted(
        permissionsListLiveData: LiveData<List<HealthPermission>>,
        grantedPermissionsLiveData: LiveData<Set<HealthPermission>>
    ): Boolean {
        val permissionsList = permissionsListLiveData.value.orEmpty()
        val grantedPermissions = grantedPermissionsLiveData.value.orEmpty()
        return if (permissionsList.isEmpty() || grantedPermissions.isEmpty()) {
            false
        } else {
            permissionsList.size == grantedPermissions.size
        }
    }

    fun init(packageName: String, permissions: Array<out String>) {
        loadAppInfo(packageName)
        loadPermissions(packageName, permissions)
    }

    fun isPermissionGranted(permission: HealthPermission): Boolean {
        return _grantedPermissions.value.orEmpty().contains(permission)
    }

    private fun loadPermissions(packageName: String, permissions: Array<out String>) {
        val grantedPermissions = getGrantedHealthPermissionsUseCase.invoke(packageName)
        val filteredPermissions =
            permissions
                .filter { permissionString -> !grantedPermissions.contains(permissionString) }
                .mapNotNull { permissionString ->
                    HealthPermission.fromPermissionString(permissionString)
                }

        _permissionsList.postValue(filteredPermissions)
    }

    fun updatePermission(permission: HealthPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedPermissions.value.orEmpty().toMutableSet()
        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedPermissions.postValue(updatedGrantedPermissions)
    }

    fun updatePermissions(grant: Boolean) {
        if (grant) {
            _grantedPermissions.postValue(_permissionsList.value.orEmpty().toSet())
        } else {
            _grantedPermissions.postValue(emptySet())
        }
    }

    private fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appMetaData.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    fun request(packageName: String): MutableMap<HealthPermission, PermissionState> {
        val grants: MutableMap<HealthPermission, PermissionState> = mutableMapOf()
        _permissionsList.value.orEmpty().forEach { permission ->
            val granted = isPermissionGranted(permission)
            try {
                if (granted) {
                    grantHealthPermissionUseCase.invoke(packageName, permission.toString())
                    grants[permission] = PermissionState.GRANTED
                } else {
                    revokeHealthPermissionUseCase.invoke(packageName, permission.toString())
                    grants[permission] = PermissionState.NOT_GRANTED
                }
            } catch (e: SecurityException) {
                grants[permission] = PermissionState.NOT_GRANTED
            } catch (e: Exception) {
                grants[permission] = PermissionState.ERROR
            }
        }
        return grants
    }
}
