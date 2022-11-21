package com.android.healthconnect.controller.permissions.connectedapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link ConnectedAppFragment} . */
@HiltViewModel
class AppPermissionViewModel
@Inject
constructor(private val getGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase) :
    ViewModel() {

    private val _grantedPermissions = MutableLiveData<List<HealthPermission>>()

    val grantedPermissions: LiveData<List<HealthPermission>>
        get() = _grantedPermissions

    fun loadForPackage(packageName: String) {
        viewModelScope.launch {
            _grantedPermissions.postValue(
                getGrantedHealthPermissionsUseCase.invoke(packageName).mapNotNull { permission ->
                    HealthPermission.fromPermissionString(permission)
                })
        }
    }
}
