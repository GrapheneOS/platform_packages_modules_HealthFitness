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
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.connectedApp.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
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

        @JvmStatic
        fun newInstance(packageName: String) =
            ConnectedAppFragment().apply { mPackageName = packageName }
    }

    private var mPackageName: String = ""
    private val viewModel: AppPermissionViewModel by viewModels()

    private val header: PermissionHeaderPreference? by lazy {
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
        if (mPackageName.isEmpty() && requireArguments().containsKey(EXTRA_PACKAGE_NAME)) {
            mPackageName =
                requireArguments().getString(EXTRA_PACKAGE_NAME)
                    ?: throw IllegalArgumentException("PackageName can't be null!")
        }
        viewModel.loadAppInfo(mPackageName)
        viewModel.loadForPackage(mPackageName)

        allowAllPreference?.addOnSwitchChangeListener { preference, grantAll ->
            if (preference.isPressed) {
                if (grantAll) {
                    viewModel.grantAllPermissions(mPackageName)
                } else {
                    viewModel.revokeAllPermissions(mPackageName)
                }
            }
        }

        viewModel.appPermissions.observe(viewLifecycleOwner) { permissions ->
            updatePermissions(permissions)
        }
        mDeleteAllDataPreference?.setOnPreferenceClickListener {
            // TODO handle case when appName not found?
            val appName = viewModel.appInfo.value?.appName
            val deletionType = DeletionType.DeletionTypeAppData(mPackageName, appName ?: "")
            childFragmentManager.setFragmentResult(
                START_DELETION_EVENT, bundleOf(DELETION_TYPE to deletionType))
            true
        }

        viewModel.allAppPermissionsGranted.observe(viewLifecycleOwner) { isAllGranted ->
            allowAllPreference?.isChecked = isAllGranted
        }
        viewModel.atLeastOnePermissionGranted.observe(viewLifecycleOwner) { isAtLeastOneGranted ->
            updateFooter(isAtLeastOneGranted)
        }
        viewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            header?.apply {
                setIcon(appMetadata.icon)
                setTitle(appMetadata.appName)
            }
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
                    it.setOnPreferenceChangeListener { _, newValue ->
                        val checked = newValue as Boolean
                        viewModel.updatePermission(
                            mPackageName, permissionStatus.healthPermission, checked)
                        true
                    }
                })
        }
    }

    private fun updateFooter(isAtLeastOneGranted: Boolean) {
        val appName = viewModel.appInfo.value?.appName
        var title =
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
