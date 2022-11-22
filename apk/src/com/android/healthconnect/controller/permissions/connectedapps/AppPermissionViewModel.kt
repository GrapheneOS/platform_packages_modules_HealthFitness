package com.android.healthconnect.controller.permissions.connectedapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.connectedApp.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedApp.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link ConnectedAppFragment} . */
@HiltViewModel
class AppPermissionViewModel
@Inject
constructor(
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
    private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase,
    private val revokePermissionsStatusUseCase: RevokeHealthPermissionUseCase
) : ViewModel() {

    private val _grantedPermissions = MutableLiveData<List<HealthPermissionStatus>>()
    val grantedPermissions: LiveData<List<HealthPermissionStatus>>
        get() = _grantedPermissions

    fun loadForPackage(packageName: String) {
        viewModelScope.launch {
            _grantedPermissions.postValue(loadAppPermissionsStatusUseCase.invoke(packageName))
        }
    }

    fun updatePermissions(packageName: String, healthPermission: HealthPermission, grant: Boolean) {
        if (grant) {
            grantPermissionsStatusUseCase.invoke(packageName, healthPermission.toString())
        } else {
            // TODO(magdi) find revoke permission reasons
            revokePermissionsStatusUseCase.invoke(packageName, healthPermission.toString(), "user")
        }
    }
}
