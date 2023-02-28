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

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.deletion.DeletionViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment.Companion.DISCONNECT_ALL_EVENT
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment.Companion.DISCONNECT_CANCELED_EVENT
import com.android.healthconnect.controller.permissions.shared.DisconnectDialogFragment.Companion.KEY_DELETE_DATA
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromHealthPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.AppAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.AppHeaderPreference
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for connected app screen. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
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
    init {
        this.setPageName(PageName.APP_ACCESS_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private var packageName: String = ""
    private var appName: String = ""
    private val appPermissionViewModel: AppPermissionViewModel by viewModels()
    private val deletionViewModel: DeletionViewModel by activityViewModels()
    private val permissionMap: MutableMap<HealthPermission, HealthSwitchPreference> = mutableMapOf()

    private val header: AppHeaderPreference? by lazy {
        preferenceScreen.findPreference(PERMISSION_HEADER)
    }

    private val allowAllPreference: HealthMainSwitchPreference? by lazy {
        preferenceScreen.findPreference(ALLOW_ALL_PREFERENCE)
    }

    private val mReadPermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(READ_CATEGORY)
    }

    private val mWritePermissionCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(WRITE_CATEGORY)
    }

    private val mDeleteAppDataPreference: HealthPreference? by lazy {
        preferenceScreen.findPreference(DELETE_APP_DATA_PREFERENCE)
    }

    private val mConnectedAppFooter: FooterPreference? by lazy {
        preferenceScreen.findPreference(FOOTER_KEY)
    }

    private val dateFormatter: LocalDateTimeFormatter by lazy {
        LocalDateTimeFormatter(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.connected_app_screen, rootKey)

        mDeleteAppDataPreference?.logName = AppAccessElement.DELETE_APP_DATA_BUTTON
        allowAllPreference?.logNameActive = AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_ACTIVE
        allowAllPreference?.logNameInactive = AppAccessElement.ALLOW_ALL_PERMISSIONS_SWITCH_INACTIVE

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

        appPermissionViewModel.loadAppInfo(packageName)
        appPermissionViewModel.loadForPackage(packageName)

        appPermissionViewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        appPermissionViewModel.grantedPermissions.observe(viewLifecycleOwner) { granted ->
            permissionMap.forEach { (healthPermission, switchPreference) ->
                if (healthPermission in granted && !switchPreference.isChecked) {
                    switchPreference.isChecked = true
                }
            }
        }

        deletionViewModel.appPermissionReloadNeeded.observe(viewLifecycleOwner) { isReloadNeeded ->
            if (isReloadNeeded) appPermissionViewModel.loadForPackage(packageName)
        }

        appPermissionViewModel.revokeAllPermissionsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AppPermissionViewModel.RevokeAllState.Loading -> {
                    showLoadingDialog()
                }
                else -> {
                    dismissLoadingDialog()
                }
            }
        }

        setupAllowAllPreference()
        setupDeleteAllPreference()
        setupHeader()
        setupFooter()
    }

    private fun setupHeader() {
        appPermissionViewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            header?.apply {
                icon = appMetadata.icon
                title = appMetadata.appName
            }
        }
    }

    private fun setupDeleteAllPreference() {
        mDeleteAppDataPreference?.setOnPreferenceClickListener {
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
                    appPermissionViewModel.grantAllPermissions(packageName)
                } else {
                    showRevokeAllPermissions()
                }
            }
        }
        appPermissionViewModel.allAppPermissionsGranted.observe(viewLifecycleOwner) { isAllGranted
            ->
            allowAllPreference?.isChecked = isAllGranted
        }
    }

    private fun showRevokeAllPermissions() {
        childFragmentManager.setFragmentResultListener(DISCONNECT_CANCELED_EVENT, this) { _, _ ->
            allowAllPreference?.isChecked = true
        }

        childFragmentManager.setFragmentResultListener(DISCONNECT_ALL_EVENT, this) { _, bundle ->
            appPermissionViewModel.revokeAllPermissions(packageName)
            if (bundle.containsKey(KEY_DELETE_DATA) && bundle.getBoolean(KEY_DELETE_DATA)) {
                appPermissionViewModel.deleteAppData(packageName, appName)
            }
        }

        DisconnectDialogFragment(appName).show(childFragmentManager, DisconnectDialogFragment.TAG)
    }

    private fun updatePermissions(permissions: List<HealthPermission>) {
        mReadPermissionCategory?.removeAll()
        mWritePermissionCategory?.removeAll()
        permissionMap.clear()

        permissions.forEach { permission ->
            val category =
                if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                    mReadPermissionCategory
                } else {
                    mWritePermissionCategory
                }

            val preference =
                HealthSwitchPreference(requireContext()).also {
                    val healthCategory = fromHealthPermissionType(permission.healthPermissionType)
                    it.setIcon(healthCategory.icon())
                    it.setTitle(fromPermissionType(permission.healthPermissionType).uppercaseLabel)
                    it.logNameActive = AppAccessElement.PERMISSION_SWITCH_ACTIVE
                    it.logNameInactive = AppAccessElement.PERMISSION_SWITCH_INACTIVE
                    it.setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        appPermissionViewModel.updatePermission(packageName, permission, checked)
                        true
                    }
                }
            permissionMap[permission] = preference
            category?.addPreference(preference)
        }

        mReadPermissionCategory?.apply { isVisible = (preferenceCount != 0) }
        mWritePermissionCategory?.apply { isVisible = (preferenceCount != 0) }
    }

    private fun setupFooter() {
        appPermissionViewModel.atLeastOnePermissionGranted.observe(viewLifecycleOwner) {
            isAtLeastOneGranted ->
            updateFooter(isAtLeastOneGranted)
        }
    }

    private fun updateFooter(isAtLeastOneGranted: Boolean) {
        var title =
            getString(R.string.other_android_permissions) +
                PARAGRAPH_SEPARATOR +
                getString(R.string.manage_permissions_rationale, appName)

        if (isAtLeastOneGranted) {
            val dataAccessDate = appPermissionViewModel.loadAccessDate(packageName)
            dataAccessDate?.let {
                val formattedDate = dateFormatter.formatLongDate(dataAccessDate)
                title =
                    getString(R.string.manage_permissions_time_frame, appName, formattedDate) +
                        PARAGRAPH_SEPARATOR +
                        title
            }
        }

        mConnectedAppFooter?.title = title
        if (healthPermissionReader.isRationalIntentDeclared(packageName)) {
            mConnectedAppFooter?.setLearnMoreText(getString(R.string.manage_permissions_learn_more))
            logger.logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
            mConnectedAppFooter?.setLearnMoreAction {
                logger.logInteraction(AppAccessElement.PRIVACY_POLICY_LINK)
                val startRationaleIntent =
                    healthPermissionReader.getApplicationRationaleIntent(packageName)
                startActivity(startRationaleIntent)
            }
        }
    }
}
