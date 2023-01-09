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
package com.android.healthconnect.controller.permissions.connectedapps

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.fromHealthPermissionType
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.connectedApps.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedapps.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.permissions.connectedapps.shared.DisconnectDialogFragment
import com.android.healthconnect.controller.permissions.connectedapps.shared.DisconnectDialogFragment.Companion.DISCONNECT_ALL_EVENT
import com.android.healthconnect.controller.permissions.connectedapps.shared.DisconnectDialogFragment.Companion.DISCONNECT_CANCELED_EVENT
import com.android.healthconnect.controller.permissions.connectedapps.shared.DisconnectDialogFragment.Companion.KEY_DELETE_DATA
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.settingslib.widget.AppHeaderPreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.MainSwitchPreference
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for connected app screen. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class ConnectedAppFragment : Hilt_ConnectedAppFragment() {

    companion object {
        private const val PERMISSION_HEADER = "manage_app_permission_header"
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val READ_CATEGORY = "read_permission_category"
        private const val WRITE_CATEGORY = "write_permission_category"
        private const val DELETE_APP_DATA_PREFERENCE = "delete_app_data"
        private const val FOOTER_KEY = "connected_app_footer"
        private const val PARAGRAPH_SEPARATOR = "\n\n"
    }

    private var packageName: String = ""
    private var appName: String = ""
    private val viewModel: AppPermissionViewModel by viewModels()

    private val header: AppHeaderPreference? by lazy {
        preferenceScreen.findPreference(PERMISSION_HEADER)
    }

    private val allowAllPreference: MainSwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val mDeleteAllDataPreference: Preference? by lazy {
        preferenceScreen.findPreference(DELETE_APP_DATA_PREFERENCE)
    }

    private val mConnectedAppFooter: FooterPreference? by lazy {
        preferenceScreen.findPreference(FOOTER_KEY)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.connected_app_screen, rootKey)

        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
            requireArguments().getString(EXTRA_PACKAGE_NAME) != null) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
        if (requireArguments().containsKey(EXTRA_APP_NAME) &&
            requireArguments().getString(EXTRA_APP_NAME) != null) {
            appName = requireArguments().getString(EXTRA_APP_NAME)!!
        }
        viewModel.loadAppInfo(packageName)
        viewModel.loadForPackage(packageName)

        viewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        setupAllowAllPreference()
        setupDeleteAllPreference()
        setupHeader()
        setupFooter()
    }

    private fun setupHeader() {
        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            header?.apply {
                setIcon(appMetadata.icon)
                setTitle(appMetadata.appName)
            }
        }
    }

    private fun setupDeleteAllPreference() {
        mDeleteAllDataPreference?.setOnPreferenceClickListener {
            val deletionType = DeletionType.DeletionTypeAppData(packageName, appName)
            childFragmentManager.setFragmentResult(
                START_DELETION_EVENT, bundleOf(DELETION_TYPE to deletionType))
            true
        }
    }

    private fun setupAllowAllPreference() {
        allowAllPreference?.addOnSwitchChangeListener { preference, grantAll ->
            if (preference.isPressed) {
                if (grantAll) {
                    viewModel.grantAllPermissions(packageName)
                } else {
                    showRevokeAllPermissions()
                }
            }
        }
        viewModel.allAppPermissionsGranted.observe(viewLifecycleOwner) { isAllGranted ->
            allowAllPreference?.isChecked = isAllGranted
        }
    }

    private fun showRevokeAllPermissions() {
        childFragmentManager.setFragmentResultListener(DISCONNECT_CANCELED_EVENT, this) { _, _ ->
            allowAllPreference?.isChecked = true
        }

        childFragmentManager.setFragmentResultListener(DISCONNECT_ALL_EVENT, this) { _, bundle ->
            viewModel.revokeAllPermissions(packageName)
            if (bundle.containsKey(KEY_DELETE_DATA) && bundle.getBoolean(KEY_DELETE_DATA)) {
                viewModel.deleteAppData(packageName, appName)
            }
        }

        DisconnectDialogFragment(appName).show(childFragmentManager, DisconnectDialogFragment.TAG)
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
                    val healthCategory = fromHealthPermissionType(permission.healthPermissionType)
                    it.setIcon(healthCategory.icon)
                    it.setTitle(fromPermissionType(permission.healthPermissionType).uppercaseLabel)
                    it.isChecked = permissionStatus.isGranted
                    it.setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        viewModel.updatePermission(
                            packageName, permissionStatus.healthPermission, checked)
                        true
                    }
                })
        }
    }

    private fun setupFooter() {
        viewModel.atLeastOnePermissionGranted.observe(viewLifecycleOwner) { isAtLeastOneGranted ->
            updateFooter(isAtLeastOneGranted)
        }
    }

    private fun updateFooter(isAtLeastOneGranted: Boolean) {
        val title =
            getString(R.string.other_android_permissions) +
                PARAGRAPH_SEPARATOR +
                getString(R.string.manage_permissions_rationale, appName)

        // TODO (b/261395536) update with the time the first permission was granted
        //        if (isAtLeastOneGranted) {
        //            val dataAccessDate = Instant.now().toLocalDate()
        //            title =
        //                getString(R.string.manage_permissions_time_frame, appName, dataAccessDate)
        // +
        //                    PARAGRAPH_SEPARATOR +
        //                    title
        //        }

        mConnectedAppFooter?.setTitle(title)
        mConnectedAppFooter?.setLearnMoreText(getString(R.string.manage_permissions_learn_more))
        // TODO (b/262060317) add link to app privacy policy
        mConnectedAppFooter?.setLearnMoreAction {}
    }
}
