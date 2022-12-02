package com.android.healthconnect.controller.permissions

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for displaying permission switches. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class PermissionsFragment : Hilt_PermissionsFragment() {

    companion object {
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        @JvmStatic
        fun newInstance(permissions: Map<HealthPermission, Boolean>) =
            PermissionsFragment().apply { permissionMap = permissions.toMutableMap() }
    }

    private var permissionMap: MutableMap<HealthPermission, Boolean> = HashMap()

    private val allowAllPreference: SwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.permissions_screen, rootKey)
            updateDataList()
            setupAllowAll()
        }
    }

    fun getPermissionAssignments(): Map<HealthPermission, Boolean> {
        return permissionMap.toMap()
    }

    private fun setupAllowAll() {
        allowAllPreference?.setOnPreferenceChangeListener { _, newValue ->
            val grant = newValue as Boolean
            (0..(mReadPermissionCategory?.preferenceCount?.minus(1) ?: -1)).forEach { i ->
                (mReadPermissionCategory?.getPreference(i) as SwitchPreference).isChecked = grant
            }
            (0..(mWritePermissionCategory?.preferenceCount?.minus(1) ?: -1)).forEach { i ->
                (mWritePermissionCategory?.getPreference(i) as SwitchPreference).isChecked = grant
            }
            permissionMap.keys.forEach { permission -> permissionMap[permission] = grant }
            true
        }
    }

    private fun updateDataList() {
        mReadPermissionCategory?.removeAll()
        mWritePermissionCategory?.removeAll()

        permissionMap.forEach { entry ->
            val permission = entry.key
            val value = entry.value
            if (permission.permissionsAccessType.equals(PermissionsAccessType.READ)) {
                mReadPermissionCategory?.addPreference(
                    SwitchPreference(requireContext()).also {
                        it.setDefaultValue(value)
                        it.setTitle(
                            HealthPermissionStrings.fromPermissionType(
                                    permission.healthPermissionType)
                                .label)
                        it.setOnPreferenceChangeListener { _, newValue ->
                            permissionMap[permission] = newValue as Boolean
                            true
                        }
                    })
            }
        }

        permissionMap.forEach { entry ->
            val permission = entry.key
            val value = entry.value
            if (permission.permissionsAccessType.equals(PermissionsAccessType.WRITE)) {
                mWritePermissionCategory?.addPreference(
                    SwitchPreference(requireContext()).also {
                        it.setDefaultValue(value)
                        it.setTitle(
                            HealthPermissionStrings.fromPermissionType(
                                    permission.healthPermissionType)
                                .label)
                        it.setOnPreferenceChangeListener { _, newValue ->
                            permissionMap[permission] = newValue as Boolean
                            true
                        }
                    })
            }
        }
        if (mReadPermissionCategory?.preferenceCount == 0) {
            mReadPermissionCategory?.isVisible = false
        }
        if (mWritePermissionCategory?.preferenceCount == 0) {
            mWritePermissionCategory?.isVisible = false
        }
    }
}
