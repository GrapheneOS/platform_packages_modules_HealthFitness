/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.utils

import android.app.Activity
import android.health.connect.datatypes.Record
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.dialog.ProgressDialogFragment
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.ToolbarElement
import com.android.healthfitness.flags.AconfigFlagHelper
import com.android.healthfitness.flags.Flags.launchOnboardingActivity
import dagger.hilt.android.EntryPointAccessors

private lateinit var deviceInfoUtils: DeviceInfoUtils

/** Sets fragment title on the collapsing layout, delegating to host if needed. */
fun Fragment.setTitle(@StringRes title: Int) {
    (requireActivity() as Activity).setTitle(title)
}

fun Fragment.setupMenu(
    @MenuRes menuRes: Int,
    viewLifecycleOwner: LifecycleOwner,
    logger: HealthConnectLogger? = null,
    onMenuItemSelected: (MenuItem) -> Boolean,
) {
    setupMenu(menuRes, viewLifecycleOwner, logger, null, onMenuItemSelected)
}

fun Fragment.setupMenu(
    @MenuRes menuRes: Int,
    viewLifecycleOwner: LifecycleOwner,
    logger: HealthConnectLogger? = null,
    onPrepareMenu: ((Menu) -> Unit)? = null,
    onMenuItemSelected: (MenuItem) -> Boolean,
) {

    val hiltEntryPoint =
        EntryPointAccessors.fromApplication(
            requireContext().applicationContext,
            DeviceInfoUtilsEntryPoint::class.java,
        )

    deviceInfoUtils = hiltEntryPoint.deviceInfoUtils()

    val menuProvider =
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(menuRes, menu)
                menu.findItem(R.id.menu_send_feedback).isVisible =
                    deviceInfoUtils.isSendFeedbackAvailable(requireContext())
            }

            override fun onPrepareMenu(menu: Menu) {
                onPrepareMenu?.invoke(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_send_feedback -> {
                        deviceInfoUtils.openSendFeedbackActivity(requireActivity())
                        true
                    }
                    R.id.menu_help -> {
                        // TODO (b/270864219) might be able to move impression out of this method
                        logger?.logImpression(ToolbarElement.TOOLBAR_HELP_BUTTON)
                        logger?.logInteraction(ToolbarElement.TOOLBAR_HELP_BUTTON)
                        deviceInfoUtils.openHCGetStartedLink(requireActivity())
                        true
                    }
                    else -> onMenuItemSelected.invoke(menuItem)
                }
            }
        }

    (requireActivity() as MenuHost).addMenuProvider(
        menuProvider,
        viewLifecycleOwner,
        Lifecycle.State.RESUMED,
    )
}

fun Fragment.setupSharedMenu(
    viewLifecycleOwner: LifecycleOwner,
    logger: HealthConnectLogger? = null,
    @MenuRes menuRes: Int = R.menu.send_feedback_and_help,
    onMenuItemSelected: (MenuItem) -> Boolean = { false },
) {
    setupMenu(menuRes, viewLifecycleOwner, logger, onMenuItemSelected)
}

fun Fragment.showLoadingDialog() {
    ProgressDialogFragment().show(childFragmentManager, ProgressDialogFragment.TAG)
}

fun Fragment.dismissLoadingDialog() {
    val dialog = childFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG)
    if (dialog != null && dialog is ProgressDialogFragment) {
        dialog.dismiss()
    }
}

/**
 * If {@code packageName} has exported an onboarding activity, and the package has not yet been
 * granted any permissions, the onboarding activity will be launched.
 *
 * @return {@code true} if the activity was successfully launched. Otherwise, {@code false}.
 */
fun Fragment.tryLaunchAppOnboardingActivity(
    healthPermissionReader: HealthPermissionReader,
    packageName: String,
): Boolean {
    if (launchOnboardingActivity()) {
        val maybeOnboardingIntent =
            healthPermissionReader.getOnboardingActivityIntent(requireContext(), packageName)
        if (maybeOnboardingIntent != null) {
            activity?.startActivity(maybeOnboardingIntent)
            return true
        }
    }
    return false
}

/**
 * If `packageName` has exported a device onboarding activity, the device onboarding activity will
 * be launched.
 *
 * @return `true` if the activity was successfully launched. Otherwise, `false`.
 */
fun Fragment.tryLaunchDeviceOnboardingActivity(
    healthPermissionReader: HealthPermissionReader,
    packageName: String,
    deviceId: String,
    recordTypes: ArrayList<Class<out Record>> = arrayListOf(),
): Boolean {
    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) return false

    val maybeOnboardingIntent =
        healthPermissionReader.getDeviceOnboardingActivityIntent(
            requireContext(),
            packageName,
            deviceId,
            recordTypes,
        )
    if (maybeOnboardingIntent != null) {
        activity?.startActivity(maybeOnboardingIntent)
        return true
    }

    return false
}

/**
 * If `packageName` has exported a device management activity, the device management activity will
 * be launched.
 *
 * @return `true` if the activity was successfully launched. Otherwise, `false`.
 */
fun Fragment.tryLaunchDeviceManagementActivity(
    healthPermissionReader: HealthPermissionReader,
    packageName: String,
    deviceId: String,
    recordTypes: ArrayList<Class<out Record>> = arrayListOf(),
): Boolean {
    if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) return false

    val maybeManagementIntent =
        healthPermissionReader.getDeviceManagementActivityIntent(
            requireContext(),
            packageName,
            deviceId,
            recordTypes,
        )
    if (maybeManagementIntent != null) {
        activity?.startActivity(maybeManagementIntent)
        return true
    }

    return false
}

/** Returns a [Lazy] delegate to load the PreferenceFragment's preferences. */
inline fun <P : Preference> HealthPreferenceFragment.pref(key: String): Lazy<P> {
    return lazy { findPreference(key)!! }
}

/** Returns a [Lazy] delegate to load the PreferenceFragment's preferences. */
inline fun <P : Preference> PreferenceFragmentCompat.pref(key: String): Lazy<P> {
    return lazy { findPreference(key)!! }
}

/**
 * Shows a DialogFragment, created by the dialogProvider, if one with the same tag isn't already
 * visible.
 *
 * @param T The type of DialogFragment to show.
 * @param tag The tag to associate with the DialogFragment.
 * @param dialogProvider A lambda that creates and returns an instance of the DialogFragment.
 */
inline fun <reified T : DialogFragment> FragmentManager.showDialogIfNotExists(
    tag: String,
    dialogProvider: () -> T,
) {
    if (this.findFragmentByTag(tag) == null) {
        val dialog = dialogProvider()
        dialog.show(this, tag)
    }
}

fun NavController.navigateSafe(
    @IdRes fromDestinationId: Int,
    @IdRes actionId: Int,
    args: Bundle? = null,
) {
    if (currentDestination?.id == fromDestinationId) {
        try {
            navigate(actionId, args)
        } catch (e: IllegalArgumentException) {
            Log.e("HCNavigation", "Navigation failed despite source check", e)
        }
    } else {
        Log.e("HCNavigation", "Unexpected source destination, navigation cancelled.")
    }
}
