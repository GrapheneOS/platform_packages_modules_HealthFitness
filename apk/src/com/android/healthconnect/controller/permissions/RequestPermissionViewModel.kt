package com.android.healthconnect.controller.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link PermissionsFragment} . */
@HiltViewModel
class RequestPermissionViewModel
@Inject
constructor(private val grantHealthPermissionUseCase: GrantHealthPermissionUseCase) : ViewModel() {

    private val permissionSelection: MutableMap<HealthPermission, Boolean> = mutableMapOf()
    private val grants: MutableMap<HealthPermission, PermissionState> = mutableMapOf()
    private val _grants = MutableLiveData<Map<HealthPermission, PermissionState>>()

    val permissionResults: LiveData<Map<HealthPermission, PermissionState>>
        get() = _grants

    fun getPermissionSelection(): Map<HealthPermission, Boolean> {
        return permissionSelection.toMap()
    }

    fun savePermissionSelection(selections: Map<HealthPermission, Boolean>) {
        permissionSelection.putAll(selections)
    }

    fun request(packageName: String, permissions: Collection<HealthPermission>) {
        viewModelScope.launch {
            permissions.forEach { permission ->
                try {
                    grantHealthPermissionUseCase.invoke(packageName, permission.toString())
                    grants[permission] = PermissionState.GRANTED
                } catch (e: SecurityException) {
                    grants[permission] = PermissionState.NOT_GRANTED
                } catch (e: Exception) {
                    grants[permission] = PermissionState.ERROR
                }
            }
            _grants.postValue(grants)
        }
    }
}
