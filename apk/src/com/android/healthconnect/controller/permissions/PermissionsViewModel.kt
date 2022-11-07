package com.android.healthconnect.controller.permissions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PermissionsViewModel
@Inject
constructor(private val loadPermissionsUseCase: LoadPermissionsUseCase) : ViewModel() {
    private val _permissions = MutableLiveData<PermissionsState>()
    val permissions: LiveData<PermissionsState>
        get() = _permissions
    init {
        loadPermissions()
    }
    private fun loadPermissions() {
        viewModelScope.launch { _permissions.postValue(loadPermissionsUseCase.invoke()) }
    }
}