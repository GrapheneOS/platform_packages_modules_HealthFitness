package com.android.healthconnect.controller.permissions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint
/** Fragment for displaying permission switches. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class PermissionsFragment : Hilt_PermissionsFragment() {
    companion object {
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        @JvmStatic fun newInstance() = PermissionsFragment()
    }
    private val viewModel: PermissionsViewModel by viewModels()
    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }
    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.permissions_screen, rootKey)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.permissions.observe(viewLifecycleOwner) { permissions ->
            updateDataList(permissions)
        }
    }
    private fun updateDataList(permissions: PermissionsState) {
        mReadPermissionCategory?.removeAll()
        mWritePermissionCategory?.removeAll()
        permissions.readPermissions.forEach { permission ->
            mReadPermissionCategory?.addPreference(
                    SwitchPreference(requireContext()).also { it.title = permission })
        }
        permissions.writePermissions.forEach { permission ->
            mWritePermissionCategory?.addPreference(
                    SwitchPreference(requireContext()).also { it.title = permission })
        }
    }
}
