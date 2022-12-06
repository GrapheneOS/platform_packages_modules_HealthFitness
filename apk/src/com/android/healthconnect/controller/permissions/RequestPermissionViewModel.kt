package com.android.healthconnect.controller.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.AppInfoReader
import com.android.healthconnect.controller.shared.AppMetadata
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
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase
) : ViewModel() {

    private val permissionSelection: MutableMap<HealthPermission, Boolean> = mutableMapOf()

    private val _appMetaData = MutableLiveData<AppMetadata>()
    val appMetadata: LiveData<AppMetadata>
        get() = _appMetaData

    private val _permissionResults = MutableLiveData<Map<HealthPermission, PermissionState>>()
    val permissionResults: LiveData<Map<HealthPermission, PermissionState>>
        get() = _permissionResults

    fun getPermissionSelection(): Map<HealthPermission, Boolean> {
        return permissionSelection.toMap()
    }

    fun savePermissionSelection(selections: Map<HealthPermission, Boolean>) {
        permissionSelection.putAll(selections)
    }

    fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appMetaData.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    fun request(packageName: String, permissions: Map<HealthPermission, Boolean>) {
        viewModelScope.launch {
            val grants: MutableMap<HealthPermission, PermissionState> = mutableMapOf()
            permissions.forEach { entry ->
                val permission = entry.key
                try {
                    if (entry.value) {
                        grantHealthPermissionUseCase.invoke(packageName, permission.toString())
                        grants[entry.key] = PermissionState.GRANTED
                    } else {
                        revokeHealthPermissionUseCase.invoke(packageName, permission.toString())
                        grants[entry.key] = PermissionState.NOT_GRANTED
                    }
                } catch (e: SecurityException) {
                    grants[entry.key] = PermissionState.NOT_GRANTED
                } catch (e: Exception) {
                    grants[entry.key] = PermissionState.ERROR
                }
            }
            _permissionResults.postValue(grants)
        }
    }
}
