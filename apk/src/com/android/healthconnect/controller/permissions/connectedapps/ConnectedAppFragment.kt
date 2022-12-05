package com.android.healthconnect.controller.permissions.connectedapps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedApp.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for connected app screen. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class ConnectedAppFragment : Hilt_ConnectedAppFragment() {

    companion object {
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val DELETE_APP_DATA_PREFERENCE = "delete_app_data"
        @JvmStatic
        fun newInstance(packageName: String) =
            ConnectedAppFragment().apply { mPackageName = packageName }
    }

    private var mPackageName: String = ""
    private val viewModel: AppPermissionViewModel by viewModels()

    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val mDeleteAllDataPreference: Preference? by lazy {
        preferenceScreen.findPreference(DELETE_APP_DATA_PREFERENCE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connected_app_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mPackageName.isEmpty()) {
            mPackageName = arguments?.get("packageName") as String
        }
        viewModel.loadForPackage(mPackageName)
        viewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        mDeleteAllDataPreference?.setOnPreferenceClickListener {
            //TODO(b/246776055) Implement deletion flow for app
            true
        }
    }

    private fun updatePermissions(permissions: List<HealthPermissionStatus>) {
        mReadPermissionCategory?.removeAll()
        mWritePermissionCategory?.removeAll()

        permissions.forEach { permissionStatus ->
            val permission = permissionStatus.healthPermission
            val category =
                if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                    mReadPermissionCategory
                } else {
                    mWritePermissionCategory
                }

            category?.addPreference(
                SwitchPreference(requireContext()).also {
                    it.setTitle(fromPermissionType(permission.healthPermissionType).uppercaseLabel)
                    it.isChecked = permissionStatus.isGranted
                    it.setOnPreferenceChangeListener { preference, newValue ->
                        val checked = (preference as SwitchPreference).isChecked
                        viewModel.updatePermission(
                            mPackageName, permissionStatus.healthPermission, !checked)
                        true
                    }
                })
        }
    }
}
