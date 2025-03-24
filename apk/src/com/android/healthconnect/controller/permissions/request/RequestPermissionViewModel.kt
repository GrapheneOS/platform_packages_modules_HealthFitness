/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.permissions.request

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.additionalaccess.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isAdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessReadPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessWritePermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalReadPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalWritePermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

/** View model for {@link PermissionsFragment} . */
@HiltViewModel
class RequestPermissionViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appInfoReader: AppInfoReader,
    private val healthPermissionReader: HealthPermissionReader,
    private val grantHealthPermissionUseCase: GrantHealthPermissionUseCase,
    private val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase,
    private val getGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
    private val loadAccessDateUseCase: LoadAccessDateUseCase,
    private val loadDeclaredHealthPermissionUseCase: LoadDeclaredHealthPermissionUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "RequestPermissionViewMo"
    }

    private val _appMetaData = MutableLiveData<AppMetadata>()
    val appMetadata: LiveData<AppMetadata>
        get() = _appMetaData

    /** List of grantable [MedicalPermission]s */
    private val _medicalPermissionsList = MutableLiveData<List<MedicalPermission>>()

    /** List of grantable [FitnessPermission]s */
    private val _fitnessPermissionsList = MutableLiveData<List<FitnessPermission>>()
    // TODO: b/376526849 - Use FitnessScreenState and AdditionalScreenState in Wear UI
    val fitnessPermissionsList: LiveData<List<FitnessPermission>>
        get() = _fitnessPermissionsList

    /** List of grantable [AdditionalPermission]s */
    private val _additionalPermissionsList = MutableLiveData<List<AdditionalPermission>>()
    // TODO: b/376526849 - Use FitnessScreenState and AdditionalScreenState in Wear UI
    val additionalPermissionsList: LiveData<List<AdditionalPermission>>
        get() = _additionalPermissionsList

    /** List of grantable [HealthPermissions]s */
    private val _healthPermissionsList = MutableLiveData<List<HealthPermission>>()
    val grantableHealthPermissionsList: LiveData<List<HealthPermission>>
        get() = _healthPermissionsList

    /** Screen states */
    private val _medicalScreenState =
        MediatorLiveData<MedicalScreenState>().apply {
            addSource(_appMetaData) { appMetadata ->
                this.postValue(
                    getMedicalScreenState(appMetadata, _medicalPermissionsList.value.orEmpty())
                )
            }
            addSource(_medicalPermissionsList) { medicalPermissions ->
                this.postValue(getMedicalScreenState(appMetadata.value, medicalPermissions))
            }
        }
    val medicalScreenState: LiveData<MedicalScreenState>
        get() = _medicalScreenState

    private val _fitnessScreenState =
        MediatorLiveData<FitnessScreenState>().apply {
            addSource(_appMetaData) { appMetadata ->
                this.postValue(
                    getFitnessScreenState(appMetadata, _healthPermissionsList.value.orEmpty())
                )
            }
            addSource(_healthPermissionsList) { healthPermissions ->
                this.postValue(getFitnessScreenState(appMetadata.value, healthPermissions))
            }
        }
    val fitnessScreenState: LiveData<FitnessScreenState>
        get() = _fitnessScreenState

    private val _additionalScreenState =
        MediatorLiveData<AdditionalScreenState>().apply {
            addSource(_appMetaData) { appMetadata ->
                this.postValue(
                    getAdditionalScreenState(
                        appMetadata,
                        _additionalPermissionsList.value.orEmpty(),
                    )
                )
            }
            addSource(_additionalPermissionsList) { additionalPermissions ->
                this.postValue(getAdditionalScreenState(appMetadata.value, additionalPermissions))
            }
        }

    val additionalScreenState: LiveData<AdditionalScreenState>
        get() = _additionalScreenState

    private val _permissionsActivityState = MutableLiveData<PermissionsActivityState>()
    val permissionsActivityState: LiveData<PermissionsActivityState>
        get() = _permissionsActivityState

    /** Permission grants */
    /** [MedicalPermission]s that have been granted locally via a toggle, but not yet requested */
    private val _grantedMedicalPermissions = MutableLiveData<Set<MedicalPermission>>(emptySet())
    val grantedMedicalPermissions: LiveData<Set<MedicalPermission>>
        get() = _grantedMedicalPermissions

    /** [FitnessPermission]s that have been granted locally via a toggle, but not yet requested */
    private val _grantedFitnessPermissions = MutableLiveData<Set<FitnessPermission>>(emptySet())
    val grantedFitnessPermissions: LiveData<Set<FitnessPermission>>
        get() = _grantedFitnessPermissions

    /**
     * [AdditionalPermission]s that have been granted locally via a toggle, but not yet requested
     */
    private val _grantedAdditionalPermissions =
        MutableLiveData<Set<AdditionalPermission>>(emptySet())
    val grantedAdditionalPermissions: LiveData<Set<AdditionalPermission>>
        get() = _grantedAdditionalPermissions

    /** Used to control the enabled state of the Allow all switch */
    private val _allMedicalPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_medicalPermissionsList) {
                postValue(
                    areAllPermissionsGranted(_medicalPermissionsList, grantedMedicalPermissions)
                )
            }
            addSource(_grantedMedicalPermissions) {
                postValue(
                    areAllPermissionsGranted(_medicalPermissionsList, grantedMedicalPermissions)
                )
            }
        }
    val allMedicalPermissionsGranted: LiveData<Boolean>
        get() = _allMedicalPermissionsGranted

    /** Used to control the enabled state of the Allow all switch */
    private val _allFitnessPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_fitnessPermissionsList) {
                postValue(
                    areAllPermissionsGranted(_fitnessPermissionsList, grantedFitnessPermissions)
                )
            }
            addSource(_grantedFitnessPermissions) {
                postValue(
                    areAllPermissionsGranted(_fitnessPermissionsList, grantedFitnessPermissions)
                )
            }
        }
    val allFitnessPermissionsGranted: LiveData<Boolean>
        get() = _allFitnessPermissionsGranted

    /** Retains the originally requested permissions and their state. */
    private var requestedPermissions: MutableMap<HealthPermission, PermissionState> = mutableMapOf()

    /**
     * A map of permissions that have been requested and their state. The union of this and
     * [requestedPermissions] will be returned to the caller as an intent extra.
     */
    private var grants: MutableMap<HealthPermission, PermissionState> = mutableMapOf()

    /** Indicates whether the medical data type request has been concluded. */
    private var medicalPermissionsConcluded = false

    fun isMedicalPermissionRequestConcluded(): Boolean = medicalPermissionsConcluded

    fun setMedicalPermissionRequestConcluded(boolean: Boolean) {
        medicalPermissionsConcluded = boolean
    }

    /** Indicates whether the fitness data type request has been concluded. */
    private var fitnessPermissionsConcluded = false

    fun isFitnessPermissionRequestConcluded(): Boolean = fitnessPermissionsConcluded

    fun setFitnessPermissionRequestConcluded(boolean: Boolean) {
        fitnessPermissionsConcluded = boolean
    }

    /**
     * If no read permissions granted, the AdditionalPermissions request screen will not be shown
     */
    private var anyReadPermissionsGranted: Boolean = false

    fun isAnyReadPermissionGranted(): Boolean = anyReadPermissionsGranted

    private var anyFitnessReadPermissionsGranted: Boolean = false

    private var anyMedicalReadPermissionsGranted: Boolean = false

    /** Whether to modify the historic access text on the [FitnessPermissionsFragment] */
    private var historyAccessGranted: Boolean = false

    fun isHistoryAccessGranted(): Boolean = historyAccessGranted

    private fun loadAccessDate(packageName: String) = loadAccessDateUseCase.invoke(packageName)

    private var initialRequestedPermissions: Array<out String> = arrayOf()
    private lateinit var packageName: String
    private var anyMedicalPermissionsDeclared: Boolean = false

    fun init(packageName: String, permissions: Array<out String>) {
        initialRequestedPermissions = permissions
        this.packageName = packageName
        loadAppInfo(packageName)
        loadPermissions(packageName, permissions)
    }

    /** Whether the user has enabled this permission in the Permission Request screen. */
    fun isPermissionLocallyGranted(permission: HealthPermission): Boolean {
        return when (permission) {
            is FitnessPermission -> {
                _grantedFitnessPermissions.value.orEmpty().contains(permission)
            }
            is MedicalPermission -> {
                _grantedMedicalPermissions.value.orEmpty().contains(permission)
            }
            else -> {
                _grantedAdditionalPermissions.value.orEmpty().contains(permission)
            }
        }
    }

    /** Returns true if any of the requested permissions is USER_FIXED, false otherwise. */
    fun isAnyPermissionUserFixed(packageName: String, permissions: Array<out String>): Boolean {
        val declaredPermissions = loadDeclaredHealthPermissionUseCase.invoke(packageName)
        val validPermissions = permissions.filter { declaredPermissions.contains(it) }
        val permissionFlags =
            getHealthPermissionsFlagsUseCase.invoke(packageName, validPermissions.toList())
        val userFixedPermissions =
            permissionFlags
                .filter { (_, flags) -> flags.and(PackageManager.FLAG_PERMISSION_USER_FIXED) != 0 }
                .keys
                .toList()
        if (userFixedPermissions.isNotEmpty()) {
            Log.e(TAG, "Permissions are user-fixed: $userFixedPermissions")
            return true
        }
        return false
    }

    /** Mark a permission as locally granted */
    fun updateHealthPermission(permission: HealthPermission, grant: Boolean) {
        when (permission) {
            is FitnessPermission -> {
                updateFitnessPermission(permission, grant)
            }
            is MedicalPermission -> {
                updateMedicalPermission(permission, grant)
            }
            is AdditionalPermission -> {
                updateAdditionalPermission(permission, grant)
            }
        }
    }

    /** Mark all [MedicalPermission]s as locally granted */
    fun updateMedicalPermissions(grant: Boolean) {
        if (grant) {
            _grantedMedicalPermissions.setValue(_medicalPermissionsList.value.orEmpty().toSet())
        } else {
            _grantedMedicalPermissions.setValue(emptySet())
        }
    }

    /** Mark all [FitnessPermission]s as locally granted */
    fun updateFitnessPermissions(grant: Boolean) {
        if (grant) {
            _grantedFitnessPermissions.setValue(_fitnessPermissionsList.value.orEmpty().toSet())
        } else {
            _grantedFitnessPermissions.setValue(emptySet())
        }
    }

    /** Mark all [AdditionalPermission]s as locally granted */
    fun updateAdditionalPermissions(grant: Boolean) {
        if (grant) {
            _grantedAdditionalPermissions.value = _additionalPermissionsList.value.orEmpty().toSet()
        } else {
            _grantedAdditionalPermissions.value = emptySet()
        }
    }

    /** Grants/Revokes all the [MedicalPermission]s sent by the caller. */
    fun requestMedicalPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is MedicalPermission }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
        reloadPermissions()
    }

    /** Grants/Revokes all the [FitnessPermission]s sent by the caller. */
    fun requestFitnessPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is FitnessPermission }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
        reloadPermissions()
    }

    /** Grants/Revokes all the [AdditionalPermission]s sent by the caller. */
    fun requestAdditionalPermissions(packageName: String) {
        requestedPermissions
            .filterKeys { it is AdditionalPermission }
            .filterKeys { it != AdditionalPermission.READ_EXERCISE_ROUTES }
            .forEach { (permission, permissionState) ->
                internalGrantOrRevokePermission(packageName, permission, permissionState)
            }
    }

    /** Grants/Revokes all the [HealthPermission]s sent by the caller. */
    fun requestHealthPermissions(packageName: String) {
        requestedPermissions.forEach { (permission, permissionState) ->
            internalGrantOrRevokePermission(packageName, permission, permissionState)
        }
    }

    /**
     * Updates the internal grants map without granting or revoking permissions. This is used when
     * the request permissions screen is not shown because a permission is USER_FIXED or no valid
     * permissions are requested. In that case, we don't want to revoke other permissions, e.g.
     * Exercise Routes, because the user hasn't specifically fixed them.
     */
    fun updatePermissionGrants() {
        requestedPermissions.forEach { (permission, permissionState) ->
            updateGrants(permission, permissionState)
        }
    }

    /**
     * Returns a map of all [HealthPermission]s that have been requested by the caller and their
     * current grant state. A permission may be granted if it was already granted when the request
     * was made, or if it was granted during this permission request. Similarly for not granted
     * permissions.
     */
    fun getPermissionGrants(): MutableMap<HealthPermission, PermissionState> {
        val permissionGrants = requestedPermissions.toMutableMap()
        permissionGrants.putAll(grants)
        return permissionGrants
    }

    private fun <T> areAllPermissionsGranted(
        permissionsListLiveData: LiveData<List<T>>,
        grantedPermissionsLiveData: LiveData<Set<T>>,
    ): Boolean {
        val permissionsList = permissionsListLiveData.value.orEmpty()
        val grantedPermissions = grantedPermissionsLiveData.value.orEmpty()
        return if (permissionsList.isEmpty() || grantedPermissions.isEmpty()) {
            false
        } else {
            permissionsList.size == grantedPermissions.size
        }
    }

    private fun isHistoryReadPermission(permission: String): Boolean {
        return permission == HealthPermissions.READ_HEALTH_DATA_HISTORY
    }

    /** Reloads permissions after one type of permissions have been granted in a flow */
    private fun reloadPermissions() {
        loadPermissions(packageName, initialRequestedPermissions)
    }

    private fun loadPermissions(packageName: String, permissions: Array<out String>) {
        val grantedPermissions = getGrantedHealthPermissionsUseCase.invoke(packageName)

        anyFitnessReadPermissionsGranted = grantedPermissions.any { isFitnessReadPermission(it) }
        anyMedicalReadPermissionsGranted = grantedPermissions.any { isMedicalReadPermission(it) }

        anyReadPermissionsGranted =
            anyFitnessReadPermissionsGranted || anyMedicalReadPermissionsGranted

        historyAccessGranted =
            grantedPermissions.any { permission -> isHistoryReadPermission(permission) }
        var validPermissions = loadDeclaredHealthPermissionUseCase.invoke(packageName)
        // On Wear, only the system permissions are considered valid to be requested.
        // TODO: b/404305506 - Consider moving this filter upstream into HealthPermissionReader.
        if (
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
                Flags.replaceBodySensorPermissionEnabled()
        ) {
            var allowedPermissionsToRequest =
                healthPermissionReader.getSystemHealthPermissions().toMutableList().also {
                    it.add(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)
                }.toSet()

            validPermissions =
                validPermissions.filter { permission ->
                    allowedPermissionsToRequest.contains(permission)
                }
        }
        anyMedicalPermissionsDeclared = validPermissions.any { isMedicalPermission(it) }

        val filteredPermissions =
            permissions
                // Do not show undeclared or invalid permissions
                .filter { permission -> validPermissions.contains(permission) }
                .mapNotNull { permissionString ->
                    try {
                        HealthPermission.fromPermissionString(permissionString)
                    } catch (exception: IllegalArgumentException) {
                        Log.e(TAG, "Unrecognized health exception!", exception)
                        null
                    }
                }
                // Add the requested permissions and their states to requestedPermissions
                .onEach { permission -> addToRequestedPermissions(grantedPermissions, permission) }
                // Finally, filter out the granted permissions
                .filterNot { permission -> grantedPermissions.contains(permission.toString()) }
                .toMutableList()

        val fitnessNotGrantedPermissions =
            if (isFitnessPermissionRequestConcluded()) emptyList()
            else
                filteredPermissions
                    .filter { permission -> isFitnessPermission(permission.toString()) }
                    .map { permission -> permission as FitnessPermission }

        val medicalNotGrantedPermissions =
            if (isMedicalPermissionRequestConcluded()) emptyList()
            else
                filteredPermissions
                    .filter { permission -> isMedicalPermission(permission.toString()) }
                    .map { permission -> permission as MedicalPermission }

        val additionalNotGrantedPermissions =
            filteredPermissions
                .filter { permission -> isAdditionalPermission(permission.toString()) }
                .filterNot { permission ->
                    permission.toString() == HealthPermissions.READ_EXERCISE_ROUTES
                }
                .map { permission -> permission as AdditionalPermission }
                // Filter out additional permissions if the correct read permissions were not
                // granted
                .filterNot { permission ->
                    !anyFitnessReadPermissionsGranted &&
                        permission == AdditionalPermission.READ_HEALTH_DATA_HISTORY
                }
                .filterNot { permission ->
                    !anyReadPermissionsGranted &&
                        permission == AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND
                }

        _fitnessPermissionsList.value = fitnessNotGrantedPermissions
        _medicalPermissionsList.value = medicalNotGrantedPermissions
        _additionalPermissionsList.value = additionalNotGrantedPermissions
        _healthPermissionsList.value =
            fitnessNotGrantedPermissions +
                medicalNotGrantedPermissions +
                additionalNotGrantedPermissions

        val anyMedicalRequested = medicalNotGrantedPermissions.isNotEmpty()
        val anyFitnessRequested = fitnessNotGrantedPermissions.isNotEmpty()
        val anyAdditionalRequested = additionalNotGrantedPermissions.isNotEmpty()

        val permissionsActivityState =
            if (anyMedicalRequested) {
                val isMedicalOnlyWrite =
                    medicalNotGrantedPermissions.size == 1 &&
                        medicalNotGrantedPermissions.contains(
                            MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
                        )
                PermissionsActivityState.ShowMedical(isMedicalOnlyWrite)
            } else if (anyFitnessRequested) {
                PermissionsActivityState.ShowFitness
            } else if (anyAdditionalRequested) {
                PermissionsActivityState.ShowAdditional(additionalNotGrantedPermissions.size == 1)
            } else {
                PermissionsActivityState.NoPermissions
            }
        _permissionsActivityState.value = permissionsActivityState
    }

    private fun getMedicalScreenState(
        appMetadata: AppMetadata?,
        medicalPermissions: List<MedicalPermission>,
    ): MedicalScreenState {
        val containsReadMedical = medicalPermissions.any { isMedicalReadPermission(it) }
        val containsWriteMedical = medicalPermissions.any { isMedicalWritePermission(it) }
        val isMedicalOnlyWrite = medicalPermissions.size == 1 && containsWriteMedical
        if (appMetadata == null) {
            return MedicalScreenState.NoMedicalData
        }

        return if (isMedicalOnlyWrite) {
            MedicalScreenState.ShowMedicalWrite(appMetadata, medicalPermissions)
        } else if (containsReadMedical && containsWriteMedical) {
            MedicalScreenState.ShowMedicalReadWrite(appMetadata, medicalPermissions)
        } else if (containsReadMedical) {
            MedicalScreenState.ShowMedicalRead(appMetadata, medicalPermissions)
        } else {
            MedicalScreenState.NoMedicalData
        }
    }

    private fun getFitnessScreenState(
        appMetadata: AppMetadata?,
        healthPermissions: List<HealthPermission>,
    ): FitnessScreenState {
        if (appMetadata == null) {
            return FitnessScreenState.NoFitnessData
        }

        val containsFitnessRead = healthPermissions.any { isFitnessReadPermission(it) }
        val containsFitnessWrite = healthPermissions.any { isFitnessWritePermission(it) }
        val fitnessPermissions = healthPermissions.filterIsInstance<FitnessPermission>()
        return if (containsFitnessRead && containsFitnessWrite) {
            FitnessScreenState.ShowFitnessReadWrite(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                fitnessPermissions = fitnessPermissions,
                historyGranted = historyAccessGranted,
            )
        } else if (containsFitnessRead) {
            FitnessScreenState.ShowFitnessRead(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                fitnessPermissions = fitnessPermissions,
                historyGranted = historyAccessGranted,
            )
        } else if (containsFitnessWrite) {
            FitnessScreenState.ShowFitnessWrite(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                fitnessPermissions = fitnessPermissions,
            )
        } else {
            FitnessScreenState.NoFitnessData
        }
    }

    private fun getAdditionalScreenState(
        appMetadata: AppMetadata?,
        additionalPermissions: List<AdditionalPermission>,
    ): AdditionalScreenState {
        if (appMetadata == null) {
            return AdditionalScreenState.NoAdditionalData
        }

        val containsBackground = additionalPermissions.any { it.isBackgroundReadPermission() }
        val containsHistory = additionalPermissions.any { it.isHistoryReadPermission() }
        val dataAccessDate = loadAccessDate(packageName)

        return if (containsBackground && containsHistory) {
            AdditionalScreenState.ShowCombined(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                isMedicalReadGranted = anyMedicalReadPermissionsGranted,
                isFitnessReadGranted = anyFitnessReadPermissionsGranted,
                dataAccessDate = dataAccessDate,
            )
        } else if (containsBackground) {
            AdditionalScreenState.ShowBackground(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                isMedicalReadGranted = anyMedicalReadPermissionsGranted,
                isFitnessReadGranted = anyFitnessReadPermissionsGranted,
            )
        } else if (containsHistory) {
            AdditionalScreenState.ShowHistory(
                hasMedical = anyMedicalPermissionsDeclared,
                appMetadata = appMetadata,
                isMedicalReadGranted = anyMedicalReadPermissionsGranted,
                dataAccessDate = dataAccessDate,
            )
        } else {
            AdditionalScreenState.NoAdditionalData
        }
    }

    /** Adds a permission to the [requestedPermissions] map with its original granted state */
    private fun addToRequestedPermissions(
        grantedPermissions: List<String>,
        permission: HealthPermission,
    ) {
        val isPermissionGranted = grantedPermissions.contains(permission.toString())
        if (isPermissionGranted) {
            requestedPermissions[permission] = PermissionState.GRANTED
        } else {
            requestedPermissions[permission] = PermissionState.NOT_GRANTED
        }
    }

    private fun updateFitnessPermission(permission: FitnessPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedFitnessPermissions.value.orEmpty().toMutableSet()

        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedFitnessPermissions.postValue(updatedGrantedPermissions)
    }

    private fun updateMedicalPermission(permission: MedicalPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedMedicalPermissions.value.orEmpty().toMutableSet()

        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedMedicalPermissions.postValue(updatedGrantedPermissions)
    }

    private fun updateAdditionalPermission(permission: AdditionalPermission, grant: Boolean) {
        val updatedGrantedPermissions = _grantedAdditionalPermissions.value.orEmpty().toMutableSet()
        if (grant) {
            updatedGrantedPermissions.add(permission)
        } else {
            updatedGrantedPermissions.remove(permission)
        }
        _grantedAdditionalPermissions.postValue(updatedGrantedPermissions)
    }

    private fun loadAppInfo(packageName: String) {
        viewModelScope.launch { _appMetaData.postValue(appInfoReader.getAppMetadata(packageName)) }
    }

    /** Updates grants without granting or revoking permissions. */
    private fun updateGrants(permission: HealthPermission, permissionState: PermissionState) {
        val granted =
            isPermissionLocallyGranted(permission) || permissionState == PermissionState.GRANTED

        if (granted) {
            grants[permission] = PermissionState.GRANTED
        } else {
            grants[permission] = PermissionState.NOT_GRANTED
        }
    }

    /** Grants or revokes permissions according to the state in the internal [grants] variable. */
    private fun internalGrantOrRevokePermission(
        packageName: String,
        permission: HealthPermission,
        permissionState: PermissionState,
    ) {
        val granted =
            isPermissionLocallyGranted(permission) || permissionState == PermissionState.GRANTED

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
}

/** Represents a UI state for the [PermissionsActivity] */
sealed class PermissionsActivityState {
    data class ShowMedical(val isWriteOnly: Boolean) : PermissionsActivityState()

    data object ShowFitness : PermissionsActivityState()

    data class ShowAdditional(val singlePermission: Boolean) : PermissionsActivityState()

    data object NoPermissions : PermissionsActivityState()
}

/**
 * Represents a UI state for the [MedicalPermissionsFragment] and [MedicalWritePermissionFragment]
 */
sealed class MedicalScreenState : RequestPermissionsScreenState() {
    data object NoMedicalData : MedicalScreenState()

    data class ShowMedicalWrite(
        val appMetadata: AppMetadata,
        val medicalPermissions: List<MedicalPermission>,
    ) : MedicalScreenState()

    data class ShowMedicalRead(
        val appMetadata: AppMetadata,
        val medicalPermissions: List<MedicalPermission>,
    ) : MedicalScreenState()

    data class ShowMedicalReadWrite(
        val appMetadata: AppMetadata,
        val medicalPermissions: List<MedicalPermission>,
    ) : MedicalScreenState()
}

/** Represents a UI state for the [FitnessPermissionsFragment] */
sealed class FitnessScreenState(open val hasMedical: Boolean) : RequestPermissionsScreenState() {
    data object NoFitnessData : FitnessScreenState(hasMedical = false)

    data class ShowFitnessRead(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val fitnessPermissions: List<FitnessPermission>,
        val historyGranted: Boolean,
    ) : FitnessScreenState(hasMedical)

    data class ShowFitnessWrite(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val fitnessPermissions: List<FitnessPermission>,
    ) : FitnessScreenState(hasMedical)

    data class ShowFitnessReadWrite(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val fitnessPermissions: List<FitnessPermission>,
        val historyGranted: Boolean,
    ) : FitnessScreenState(hasMedical)
}

/**
 * Represents a UI state for the [SingleAdditionalPermissionFragment] and
 * [CombinedAdditionalPermissionsFragment]
 */
sealed class AdditionalScreenState(open val hasMedical: Boolean) : RequestPermissionsScreenState() {
    data object NoAdditionalData : AdditionalScreenState(hasMedical = false)

    data class ShowHistory(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val isMedicalReadGranted: Boolean,
        val dataAccessDate: Instant?,
    ) : AdditionalScreenState(hasMedical)

    data class ShowBackground(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val isMedicalReadGranted: Boolean,
        val isFitnessReadGranted: Boolean,
    ) : AdditionalScreenState(hasMedical)

    data class ShowCombined(
        override val hasMedical: Boolean,
        val appMetadata: AppMetadata,
        val isMedicalReadGranted: Boolean,
        val isFitnessReadGranted: Boolean,
        val dataAccessDate: Instant?,
    ) : AdditionalScreenState(hasMedical)
}

/** Parent class for permission-related screen states */
open class RequestPermissionsScreenState
