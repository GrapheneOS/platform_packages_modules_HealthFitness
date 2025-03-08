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
package com.android.healthconnect.controller.permissions.app

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.healthconnect.controller.permissions.additionalaccess.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState.ALWAYS_ALLOW
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAppData
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** View model for {@link FitnessAppFragment} and {SettingsManageAppPermissionsFragment} . */
@HiltViewModel
class AppPermissionViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appInfoReader: AppInfoReader,
    private val loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase,
    private val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase,
    private val revokePermissionsStatusUseCase: RevokeHealthPermissionUseCase,
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase,
    private val deleteAppDataUseCase: DeleteAppDataUseCase,
    private val loadAccessDateUseCase: LoadAccessDateUseCase,
    private val loadGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    private val loadExerciseRoutePermissionUseCase: ILoadExerciseRoutePermissionUseCase,
    private val healthPermissionReader: HealthPermissionReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        private const val TAG = "AppPermissionViewModel"
    }

    private val _fitnessPermissions = MutableLiveData<List<FitnessPermission>>(emptyList())
    val fitnessPermissions: LiveData<List<FitnessPermission>>
        get() = _fitnessPermissions

    private val _grantedFitnessPermissions = MutableLiveData<Set<FitnessPermission>>(emptySet())
    val grantedFitnessPermissions: LiveData<Set<FitnessPermission>>
        get() = _grantedFitnessPermissions

    val allFitnessPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_fitnessPermissions) {
                postValue(
                    isAllFitnessPermissionsGranted(fitnessPermissions, grantedFitnessPermissions)
                )
            }
            addSource(_grantedFitnessPermissions) {
                postValue(
                    isAllFitnessPermissionsGranted(fitnessPermissions, grantedFitnessPermissions)
                )
            }
        }

    val atLeastOneFitnessPermissionGranted =
        MediatorLiveData(false).apply {
            addSource(_grantedFitnessPermissions) { grantedPermissions ->
                postValue(grantedPermissions.isNotEmpty())
            }
        }

    private val _medicalPermissions = MutableLiveData<List<MedicalPermission>>(emptyList())
    val medicalPermissions: LiveData<List<MedicalPermission>>
        get() = _medicalPermissions

    private val _grantedMedicalPermissions = MutableLiveData<Set<MedicalPermission>>(emptySet())
    val grantedMedicalPermissions: LiveData<Set<MedicalPermission>>
        get() = _grantedMedicalPermissions

    private var _additionalPermissions = MutableLiveData<List<AdditionalPermission>>(emptyList())
    val additionalPermissions: LiveData<List<AdditionalPermission>>
        get() = _additionalPermissions
    private var _grantedAdditionalPermissions =
        MutableLiveData<Set<AdditionalPermission>>(emptySet())
    @VisibleForTesting
    val grantedAdditionalPermissions: LiveData<Set<AdditionalPermission>>
        get() = _grantedAdditionalPermissions

    val allMedicalPermissionsGranted =
        MediatorLiveData(false).apply {
            addSource(_medicalPermissions) {
                postValue(
                    isAllMedicalPermissionsGranted(medicalPermissions, grantedMedicalPermissions)
                )
            }
            addSource(_grantedMedicalPermissions) {
                postValue(
                    isAllMedicalPermissionsGranted(medicalPermissions, grantedMedicalPermissions)
                )
            }
        }

    val atLeastOneMedicalPermissionGranted =
        MediatorLiveData(false).apply {
            addSource(_grantedMedicalPermissions) { grantedPermissions ->
                postValue(grantedPermissions.isNotEmpty())
            }
        }

    val atLeastOneHealthPermissionGranted =
        MediatorLiveData(false).apply {
            addSource(atLeastOneFitnessPermissionGranted) { value ->
                this.value = value || atLeastOneMedicalPermissionGranted.value ?: false
            }
            addSource(atLeastOneMedicalPermissionGranted) { value ->
                this.value = value || atLeastOneFitnessPermissionGranted.value ?: false
            }
        }

    private fun atLeastOneMedicalReadPermissionGranted(): Boolean =
        _grantedMedicalPermissions.value
            .orEmpty()
            .filterNot { perm ->
                perm.medicalPermissionType == MedicalPermissionType.ALL_MEDICAL_DATA
            }
            .isNotEmpty()

    private fun atLeastOneFitnessReadPermissionGranted(): Boolean =
        _grantedFitnessPermissions.value.orEmpty().any { perm ->
            perm.permissionsAccessType == PermissionsAccessType.READ
        }

    fun revokeFitnessShouldIncludeBackground(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND) &&
            atLeastOneFitnessReadPermissionGranted() &&
            !atLeastOneMedicalReadPermissionGranted()

    fun revokeFitnessShouldIncludePastData(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_HISTORY) &&
            atLeastOneFitnessReadPermissionGranted() &&
            !atLeastOneMedicalReadPermissionGranted()

    fun revokeMedicalShouldIncludeBackground(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND) &&
            atLeastOneMedicalReadPermissionGranted() &&
            !atLeastOneFitnessReadPermissionGranted()

    fun revokeMedicalShouldIncludePastData(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_HISTORY) &&
            atLeastOneMedicalReadPermissionGranted() &&
            !atLeastOneFitnessReadPermissionGranted()

    fun revokeAllShouldIncludeBackground(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND)

    fun revokeAllShouldIncludePastData(): Boolean =
        _additionalPermissions.value
            .orEmpty()
            .contains(AdditionalPermission.READ_HEALTH_DATA_HISTORY)

    private val _appInfo = MutableLiveData<AppMetadata>()
    val appInfo: LiveData<AppMetadata>
        get() = _appInfo

    private val _revokeAllHealthPermissionsState =
        MutableLiveData<RevokeAllState>(RevokeAllState.NotStarted)
    val revokeAllHealthPermissionsState: LiveData<RevokeAllState>
        get() = _revokeAllHealthPermissionsState

    private var healthPermissionsList: List<HealthPermissionStatus> = listOf()

    /**
     * Flag to prevent {@link SettingManageAppPermissionsFragment} from reloading the granted
     * permissions on orientation change
     */
    private var shouldLoadGrantedPermissions = true

    private val _showDisableExerciseRouteEvent = MutableLiveData(false)
    val showDisableExerciseRouteEvent =
        MediatorLiveData(DisableExerciseRouteDialogEvent()).apply {
            addSource(_showDisableExerciseRouteEvent) {
                postValue(
                    DisableExerciseRouteDialogEvent(
                        shouldShowDialog = _showDisableExerciseRouteEvent.value ?: false,
                        appName = _appInfo.value?.appName ?: "",
                    )
                )
            }
            addSource(_appInfo) {
                postValue(
                    DisableExerciseRouteDialogEvent(
                        shouldShowDialog = _showDisableExerciseRouteEvent.value ?: false,
                        appName = _appInfo.value?.appName ?: "",
                    )
                )
            }
        }

    private val _lastReadPermissionDisconnected = MutableLiveData(false)
    val lastReadPermissionDisconnected: LiveData<Boolean>
        get() = _lastReadPermissionDisconnected

    fun loadPermissionsForPackage(packageName: String) {
        // clear app permissions
        _fitnessPermissions.postValue(emptyList())
        _grantedFitnessPermissions.postValue(emptySet())
        _medicalPermissions.postValue(emptyList())
        _grantedMedicalPermissions.postValue(emptySet())
        _additionalPermissions.postValue(emptyList())
        _grantedAdditionalPermissions.postValue(emptySet())

        viewModelScope.launch { _appInfo.postValue(appInfoReader.getAppMetadata(packageName)) }
        if (isPackageSupported(packageName)) {
            loadAllPermissions(packageName)
        } else {
            // we only load granted permissions for not supported apps to allow users to revoke
            // these permissions.
            loadGrantedPermissionsForPackage(packageName)
        }
    }

    private fun loadAllPermissions(packageName: String) {
        viewModelScope.launch {
            healthPermissionsList = loadAppPermissionsStatusUseCase.invoke(packageName)
            _fitnessPermissions.postValue(
                healthPermissionsList
                    .map { it.healthPermission }
                    .filterIsInstance<FitnessPermission>()
            )
            _grantedFitnessPermissions.postValue(
                healthPermissionsList
                    .filter { it.isGranted }
                    .map { it.healthPermission }
                    .filterIsInstance<FitnessPermission>()
                    .toSet()
            )
            _medicalPermissions.postValue(
                healthPermissionsList
                    .map { it.healthPermission }
                    .filterIsInstance<MedicalPermission>()
            )
            _grantedMedicalPermissions.postValue(
                healthPermissionsList
                    .filter { it.isGranted }
                    .map { it.healthPermission }
                    .filterIsInstance<MedicalPermission>()
                    .toSet()
            )
            // invalid additional permissions filtered in the useCase
            _additionalPermissions.postValue(
                healthPermissionsList
                    .map { it.healthPermission }
                    .filterIsInstance<AdditionalPermission>()
            )
            _grantedAdditionalPermissions.postValue(
                healthPermissionsList
                    .filter { it.isGranted }
                    .map { it.healthPermission }
                    .filterIsInstance<AdditionalPermission>()
                    .toSet()
            )
        }
    }

    private fun loadGrantedPermissionsForPackage(packageName: String) {
        // Only reload the status the first time this method is called
        if (shouldLoadGrantedPermissions) {
            viewModelScope.launch {
                val grantedPermissions =
                    loadAppPermissionsStatusUseCase.invoke(packageName).filter { it.isGranted }
                healthPermissionsList = grantedPermissions

                // Only show app permissions that are granted
                _fitnessPermissions.postValue(
                    grantedPermissions
                        .map { it.healthPermission }
                        .filterIsInstance<FitnessPermission>()
                )
                _grantedFitnessPermissions.postValue(
                    grantedPermissions
                        .map { it.healthPermission }
                        .filterIsInstance<FitnessPermission>()
                        .toSet()
                )
                _medicalPermissions.postValue(
                    grantedPermissions
                        .map { it.healthPermission }
                        .filterIsInstance<MedicalPermission>()
                )
                _grantedMedicalPermissions.postValue(
                    grantedPermissions
                        .map { it.healthPermission }
                        .filterIsInstance<MedicalPermission>()
                        .toSet()
                )

                _grantedAdditionalPermissions.postValue(
                    grantedPermissions
                        .map { it.healthPermission }
                        .filterIsInstance<AdditionalPermission>()
                        .toSet()
                )
            }
            shouldLoadGrantedPermissions = false
        }
    }

    fun loadAccessDate(packageName: String): Instant? {
        return loadAccessDateUseCase.invoke(packageName)
    }

    fun updatePermission(
        packageName: String,
        fitnessPermission: FitnessPermission,
        grant: Boolean,
    ): Boolean {
        try {
            if (grant) {
                grantPermission(packageName, fitnessPermission)
            } else {
                if (shouldDisplayExerciseRouteDialog(packageName, fitnessPermission)) {
                    _showDisableExerciseRouteEvent.postValue(true)
                } else {
                    revokeFitnessPermission(fitnessPermission, packageName)
                }
            }

            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update fitness permission!", ex)
        }
        return false
    }

    fun updateAdditionalPermission(
        packageName: String,
        additionalPermission: AdditionalPermission,
        grant: Boolean
    ) : Boolean {
        try {
            val grantedPermissions = _grantedAdditionalPermissions.value.orEmpty().toMutableSet()
            if (grant) {
                grantPermissionsStatusUseCase.invoke(packageName,additionalPermission.toString())
                grantedPermissions.add(additionalPermission)
            } else {
                revokePermissionsStatusUseCase.invoke(packageName, additionalPermission.toString())
                grantedPermissions.remove(additionalPermission)
            }
            _grantedAdditionalPermissions.postValue(grantedPermissions)
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update additional permission!", ex)
        }
        return false
    }

    fun updatePermission(
        packageName: String,
        medicalPermission: MedicalPermission,
        grant: Boolean,
    ): Boolean {
        try {
            if (grant) {
                grantPermission(packageName, medicalPermission)
            } else {
                revokeMedicalPermission(medicalPermission, packageName)
            }

            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update medical permission!", ex)
        }
        return false
    }

    private fun grantPermission(packageName: String, fitnessPermission: FitnessPermission) {
        val grantedPermissions = _grantedFitnessPermissions.value.orEmpty().toMutableSet()
        grantPermissionsStatusUseCase.invoke(packageName, fitnessPermission.toString())
        grantedPermissions.add(fitnessPermission)
        _grantedFitnessPermissions.postValue(grantedPermissions)
    }

    private fun grantPermission(packageName: String, medicalPermission: MedicalPermission) {
        val grantedPermissions = _grantedMedicalPermissions.value.orEmpty().toMutableSet()
        grantPermissionsStatusUseCase.invoke(packageName, medicalPermission.toString())
        grantedPermissions.add(medicalPermission)
        _grantedMedicalPermissions.postValue(grantedPermissions)
    }

    private fun revokeFitnessPermission(fitnessPermission: FitnessPermission, packageName: String) {
        val grantedFitnessPermissions = _grantedFitnessPermissions.value.orEmpty().toMutableSet()
        val grantedMedicalPermissions = _grantedMedicalPermissions.value.orEmpty()

        val readPermissionsBeforeDisconnect =
            grantedFitnessPermissions.count { permission ->
                permission.permissionsAccessType == PermissionsAccessType.READ
            } +
                grantedMedicalPermissions.count { medicalPermission ->
                    medicalPermission.medicalPermissionType !=
                        MedicalPermissionType.ALL_MEDICAL_DATA
                }
        grantedFitnessPermissions.remove(fitnessPermission)
        val readPermissionsAfterDisconnect =
            grantedFitnessPermissions.count { permission ->
                permission.permissionsAccessType == PermissionsAccessType.READ
            } +
                grantedMedicalPermissions.count { medicalPermission ->
                    medicalPermission.medicalPermissionType !=
                        MedicalPermissionType.ALL_MEDICAL_DATA
                }
        _grantedFitnessPermissions.postValue(grantedFitnessPermissions)

        val lastReadPermissionRevoked =
            _grantedAdditionalPermissions.value.orEmpty().isNotEmpty() &&
                (readPermissionsBeforeDisconnect > readPermissionsAfterDisconnect) &&
                readPermissionsAfterDisconnect == 0

        if (lastReadPermissionRevoked) {
            _grantedAdditionalPermissions.value.orEmpty().forEach { permission ->
                revokePermissionsStatusUseCase.invoke(packageName, permission.additionalPermission)
            }
        }

        _lastReadPermissionDisconnected.postValue(lastReadPermissionRevoked)
        revokePermissionsStatusUseCase.invoke(packageName, fitnessPermission.toString())
    }

    private fun revokeMedicalPermission(medicalPermission: MedicalPermission, packageName: String) {
        val grantedMedicalPermissions = _grantedMedicalPermissions.value.orEmpty().toMutableSet()
        val grantedFitnessPermissions = _grantedFitnessPermissions.value.orEmpty()

        val readPermissionsBeforeDisconnect =
            grantedFitnessPermissions.count { permission ->
                permission.permissionsAccessType == PermissionsAccessType.READ
            } +
                grantedMedicalPermissions.count { permission ->
                    permission.medicalPermissionType != MedicalPermissionType.ALL_MEDICAL_DATA
                }
        grantedMedicalPermissions.remove(medicalPermission)
        val readPermissionsAfterDisconnect =
            grantedFitnessPermissions.count { permission ->
                permission.permissionsAccessType == PermissionsAccessType.READ
            } +
                grantedMedicalPermissions.count { permission ->
                    permission.medicalPermissionType != MedicalPermissionType.ALL_MEDICAL_DATA
                }
        _grantedMedicalPermissions.postValue(grantedMedicalPermissions)

        val lastReadPermissionRevoked =
            _grantedAdditionalPermissions.value.orEmpty().isNotEmpty() &&
                (readPermissionsBeforeDisconnect > readPermissionsAfterDisconnect) &&
                readPermissionsAfterDisconnect == 0

        if (lastReadPermissionRevoked) {
            _grantedAdditionalPermissions.value.orEmpty().forEach { permission ->
                revokePermissionsStatusUseCase.invoke(packageName, permission.additionalPermission)
            }
        }

        _lastReadPermissionDisconnected.postValue(lastReadPermissionRevoked)
        revokePermissionsStatusUseCase.invoke(packageName, medicalPermission.toString())
    }

    fun markLastReadShown() {
        _lastReadPermissionDisconnected.postValue(false)
    }

    private fun shouldDisplayExerciseRouteDialog(
        packageName: String,
        fitnessPermission: FitnessPermission,
    ): Boolean {
        if (fitnessPermission.toString() != READ_EXERCISE) {
            return false
        }

        return isExerciseRoutePermissionAlwaysAllow(packageName)
    }

    fun grantAllFitnessPermissions(packageName: String): Boolean {
        try {
            _fitnessPermissions.value?.forEach {
                grantPermissionsStatusUseCase.invoke(packageName, it.toString())
            }
            val grantedFitnessPermissions =
                _grantedFitnessPermissions.value.orEmpty().toMutableSet()
            grantedFitnessPermissions.addAll(_fitnessPermissions.value.orEmpty())
            _grantedFitnessPermissions.postValue(grantedFitnessPermissions)
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update fitness permissions!", ex)
        }
        return false
    }

    fun grantAllMedicalPermissions(packageName: String): Boolean {
        try {
            _medicalPermissions.value?.forEach {
                grantPermissionsStatusUseCase.invoke(packageName, it.toString())
            }
            val grantedMedicalPermissions =
                _grantedMedicalPermissions.value.orEmpty().toMutableSet()
            grantedMedicalPermissions.addAll(_medicalPermissions.value.orEmpty())
            _grantedMedicalPermissions.postValue(grantedMedicalPermissions)
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update medical permissions!", ex)
        }
        return false
    }

    fun disableExerciseRoutePermission(packageName: String) {
        revokeFitnessPermission(fromPermissionString(READ_EXERCISE), packageName)
        // the revokePermission call will automatically revoke all additional permissions
        // including Exercise Routes if the READ_EXERCISE permission is the last READ permission
        if (isExerciseRoutePermissionAlwaysAllow(packageName)) {
            revokePermissionsStatusUseCase(packageName, READ_EXERCISE_ROUTES)
        }
    }

    private fun isExerciseRoutePermissionAlwaysAllow(packageName: String): Boolean = runBlocking {
        when (val exerciseRouteState = loadExerciseRoutePermissionUseCase(packageName)) {
            is UseCaseResults.Success -> {
                exerciseRouteState.data.exerciseRoutePermissionState == ALWAYS_ALLOW
            }
            else -> false
        }
    }

    fun revokeAllHealthPermissions(packageName: String): Boolean {
        // TODO (b/325729045) if there is an error within the coroutine scope
        // it will not be caught by this statement in tests. Consider using LiveData instead
        try {
            viewModelScope.launch(ioDispatcher) {
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Loading)
                revokeAllHealthPermissionsUseCase.invoke(packageName)
                if (isPackageSupported(packageName)) {
                    loadPermissionsForPackage(packageName)
                }
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Updated)
                _grantedFitnessPermissions.postValue(emptySet())
                _grantedMedicalPermissions.postValue(emptySet())
                _grantedAdditionalPermissions.postValue(emptySet())
            }
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to update permissions!", ex)
        }
        return false
    }

    fun revokeAllFitnessAndMaybeAdditionalPermissions(packageName: String): Boolean {
        try {
            viewModelScope.launch(ioDispatcher) {
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Loading)
                _fitnessPermissions.value?.forEach {
                    revokePermissionsStatusUseCase.invoke(packageName, it.toString())
                }
                if (!atLeastOneMedicalReadPermissionGranted()) {
                    _grantedAdditionalPermissions.value?.forEach {
                        revokePermissionsStatusUseCase.invoke(packageName, it.additionalPermission)
                    }
                    _grantedAdditionalPermissions.postValue(emptySet())
                }
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Updated)
                _grantedFitnessPermissions.postValue(emptySet())
            }
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to revoke fitness permissions!", ex)
        }
        return false
    }

    fun revokeAllMedicalAndMaybeAdditionalPermissions(packageName: String): Boolean {
        try {
            viewModelScope.launch(ioDispatcher) {
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Loading)
                _medicalPermissions.value?.forEach {
                    revokePermissionsStatusUseCase.invoke(packageName, it.toString())
                }
                if (!atLeastOneFitnessReadPermissionGranted()) {
                    _grantedAdditionalPermissions.value?.forEach {
                        revokePermissionsStatusUseCase.invoke(packageName, it.additionalPermission)
                    }
                    _grantedAdditionalPermissions.postValue(emptySet())
                }
                _revokeAllHealthPermissionsState.postValue(RevokeAllState.Updated)
                _grantedMedicalPermissions.postValue(emptySet())
            }
            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to revoke medical permissions!", ex)
        }
        return false
    }

    fun deleteAppData(packageName: String, appName: String) {
        newDeleteAppData(packageName, appName)
    }

    private fun newDeleteAppData(packageName: String, appName: String) {
        viewModelScope.launch { deleteAppDataUseCase.invoke(DeleteAppData(packageName, appName)) }
    }

    fun shouldNavigateToAppPermissionsFragment(packageName: String): Boolean {
        return isPackageSupported(packageName) || hasGrantedPermissions(packageName)
    }

    private fun hasGrantedPermissions(packageName: String): Boolean {
        return loadGrantedHealthPermissionsUseCase(packageName)
            .map { permission -> fromPermissionString(permission) }
            .isNotEmpty()
    }

    private fun isAllFitnessPermissionsGranted(
        permissionsListLiveData: LiveData<List<FitnessPermission>>,
        grantedPermissionsLiveData: LiveData<Set<FitnessPermission>>,
    ): Boolean {
        val permissionsList = permissionsListLiveData.value.orEmpty()
        val grantedPermissions = grantedPermissionsLiveData.value.orEmpty()
        return if (permissionsList.isEmpty() || grantedPermissions.isEmpty()) {
            false
        } else {
            permissionsList.size == grantedPermissions.size
        }
    }

    private fun isAllMedicalPermissionsGranted(
        permissionsListLiveData: LiveData<List<MedicalPermission>>,
        grantedPermissionsLiveData: LiveData<Set<MedicalPermission>>,
    ): Boolean {
        val permissionsList = permissionsListLiveData.value.orEmpty()
        val grantedPermissions = grantedPermissionsLiveData.value.orEmpty()
        return if (permissionsList.isEmpty() || grantedPermissions.isEmpty()) {
            false
        } else {
            permissionsList.size == grantedPermissions.size
        }
    }

    /**
     * Returns True if the packageName meets the required conditions to use
     * health permissions.
     */
    fun isPackageSupported(packageName: String): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
                Flags.replaceBodySensorPermissionEnabled()) {
            return true
        }

        if (healthPermissionReader.isBodySensorSplitPermissionApp(packageName)) {
            return true
        }

        return healthPermissionReader.isRationaleIntentDeclared(packageName)
    }

    fun hideExerciseRoutePermissionDialog() {
        _showDisableExerciseRouteEvent.postValue(false)
    }

    sealed class RevokeAllState {
        object NotStarted : RevokeAllState()

        object Loading : RevokeAllState()

        object Updated : RevokeAllState()
    }

    data class DisableExerciseRouteDialogEvent(
        val shouldShowDialog: Boolean = false,
        val appName: String = "",
    )
}
