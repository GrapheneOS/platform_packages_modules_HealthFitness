/*
 * Copyright (C) 2022 The Android Open Source Project
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
 */

package com.android.healthconnect.controller.recentaccess

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel.RecentAccessState
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.RecentAccessElement
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.tryLaunchAppOnboardingActivity
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Recent access fragment showing a timeline of apps that have recently accessed Health Connect. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class RecentAccessFragment : Hilt_RecentAccessFragment() {

    companion object {
        private const val RECENT_ACCESS_TODAY_KEY = "recent_access_today"
        private const val RECENT_ACCESS_YESTERDAY_KEY = "recent_access_yesterday"
        private const val RECENT_ACCESS_NO_DATA_KEY = "no_data"
    }

    init {
        this.setPageName(PageName.RECENT_ACCESS_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var timeSource: TimeSource
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val viewModel: RecentAccessViewModel by viewModels()
    private lateinit var contentParent: FrameLayout
    private lateinit var fab: ExtendedFloatingActionButton
    private var recyclerView: RecyclerView? = null
    private var isFabImpressionLogged: Boolean = false

    private val mRecentAccessTodayPreferenceGroup: PreferenceGroup by pref(RECENT_ACCESS_TODAY_KEY)

    private val mRecentAccessYesterdayPreferenceGroup: PreferenceGroup by
        pref(RECENT_ACCESS_YESTERDAY_KEY)

    private val mRecentAccessNoDataPreference: Preference by pref(RECENT_ACCESS_NO_DATA_KEY)

    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.recent_access_preference_screen, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        contentParent = requireActivity().findViewById(android.R.id.content)
        val fabLayout =
            inflater.inflate(R.layout.widget_floating_action_button, contentParent, false)
        fab = fabLayout.findViewById(R.id.extended_fab)

        recyclerView = rootView.findViewById(androidx.preference.R.id.recycler_view)
        val bottomPadding =
            resources.getDimensionPixelSize(R.dimen.recent_access_fab_bottom_padding)
        recyclerView?.setPadding(0, 0, 0, bottomPadding)

        return rootView
    }

    /**
     * Updates the visibility and state of the FAB (Floating Action Button) based on the current
     * navigation destination and the state of recent access apps.
     *
     * The FAB is shown only when:
     * - The current screen is the Recent Access screen.
     * - There is data to display in the Recent Access screen.
     *
     * If the FAB is visible, its impression is logged. If the FAB is not visible, it's removed from
     * the view hierarchy and its impression is not logged.
     */
    private fun updateFabState() {
        val navController = findNavController()
        val isRecentAccessScreen = navController.currentDestination?.id == R.id.recentAccessFragment
        val recentAccessState = viewModel.recentAccessApps.value
        val hasData =
            recentAccessState is RecentAccessState.WithData &&
                recentAccessState.recentAccessEntries.isNotEmpty()

        if (isRecentAccessScreen && hasData) {
            if (fab.parent == null) {
                contentParent.addView(fab)
            }
            fab.isVisible = true
            if (!isFabImpressionLogged) {
                logger.logImpression(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
                isFabImpressionLogged = true
            }
        } else {
            fab.isVisible = false
            if (fab.parent != null) {
                contentParent.removeView(fab)
            }
            isFabImpressionLogged = false
        }
    }

    override fun onPause() {
        super.onPause()
        fab.isVisible = false
        if (fab.parent != null) {
            contentParent.removeView(fab)
        }
        destinationChangedListener?.let {
            findNavController().removeOnDestinationChangedListener(it)
            destinationChangedListener = null
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadRecentAccessApps()
        updateFabState()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        destinationChangedListener =
            NavController.OnDestinationChangedListener { _, _, _ -> updateFabState() }
        navController.addOnDestinationChangedListener(destinationChangedListener!!)

        viewModel.recentAccessApps.observe(viewLifecycleOwner) { state ->
            updateFabState()
            when (state) {
                is RecentAccessState.Loading -> {
                    setLoading(true)
                }
                is RecentAccessState.Error -> {
                    setError(true)
                }
                is RecentAccessState.WithData -> {
                    setLoading(false)
                    updateRecentApps(state.recentAccessEntries)
                }
            }
        }

        updateFabState()
    }

    private fun updateRecentApps(recentAppsList: List<RecentAccessEntry>) {
        mRecentAccessTodayPreferenceGroup.removeAll()
        mRecentAccessYesterdayPreferenceGroup.removeAll()
        mRecentAccessNoDataPreference.isVisible = false

        if (recentAppsList.isEmpty()) {
            mRecentAccessYesterdayPreferenceGroup.isVisible = false
            mRecentAccessTodayPreferenceGroup.isVisible = false
            mRecentAccessNoDataPreference.isVisible = true
            mRecentAccessNoDataPreference.isSelectable = false
        } else {
            mRecentAccessTodayPreferenceGroup.isVisible = recentAppsList[0].isToday
            mRecentAccessYesterdayPreferenceGroup.isVisible = !recentAppsList.last().isToday

            fab.setOnClickListener {
                logger.logInteraction(RecentAccessElement.MANAGE_PERMISSIONS_FAB)
                if (findNavController().currentDestination?.id == R.id.recentAccessFragment) {
                    findNavController()
                        .navigate(R.id.action_recentAccessFragment_to_connectedAppsFragment)
                }
            }

            recentAppsList.forEachIndexed { index, recentApp ->
                val isLastUsage =
                    (index == recentAppsList.size - 1) ||
                        (recentApp.isToday &&
                            index < recentAppsList.size - 1 &&
                            !recentAppsList[index + 1].isToday)
                val newPreference =
                    RecentAccessPreference(requireContext(), recentApp, timeSource, true).also {
                        it.setOnPreferenceClickListener {
                            if (recentApp.isInactive) {
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.recent_access_inactive_app),
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            } else {
                                if (
                                    findNavController().currentDestination?.id ==
                                        R.id.recentAccessFragment
                                ) {
                                    navigateToAppInfoOrOnboarding(recentApp)
                                }
                            }
                            true
                        }
                    }

                if (recentApp.isToday) {
                    mRecentAccessTodayPreferenceGroup.addPreference(newPreference)
                    if (!(isLastUsage || SettingsThemeHelper.isExpressiveTheme(requireContext()))) {
                        mRecentAccessTodayPreferenceGroup.addPreference(
                            DividerPreference(requireContext())
                        )
                    }
                } else {
                    mRecentAccessYesterdayPreferenceGroup.addPreference(newPreference)
                    if (!(isLastUsage || SettingsThemeHelper.isExpressiveTheme(requireContext()))) {
                        mRecentAccessYesterdayPreferenceGroup.addPreference(
                            DividerPreference(requireContext())
                        )
                    }
                }
            }
        }
        updateFabState()
    }

    private fun navigateToAppInfoOrOnboarding(recentApp: RecentAccessEntry) {
        val appPermissionsType = recentApp.appPermissionsType
        val navigationId =
            when (appPermissionsType) {
                AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                    R.id.action_recentAccessFragment_to_fitnessAppFragment
                AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                    R.id.action_recentAccessFragment_to_medicalAppFragment
                AppPermissionsType.COMBINED_PERMISSIONS ->
                    R.id.action_recentAccessFragment_to_combinedPermissionsFragment
            }

        if (
            recentApp.shouldLaunchAppOnboardingIfAvailable &&
                tryLaunchAppOnboardingActivity(
                    healthPermissionReader,
                    recentApp.metadata.packageName,
                )
        ) {
            return
        }
        findNavController()
            .navigate(
                navigationId,
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to recentApp.metadata.packageName,
                    Constants.EXTRA_APP_NAME to recentApp.metadata.appName,
                ),
            )
    }
}
