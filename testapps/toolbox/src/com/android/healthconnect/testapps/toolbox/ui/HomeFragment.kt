/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.health.connect.HealthConnectManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.HEALTH_PERMISSIONS
import com.android.healthconnect.testapps.toolbox.PerformanceTestingFragment
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.HomeActivity
import com.android.healthconnect.testapps.toolbox.seed.SeedData
import com.android.healthconnect.testapps.toolbox.viewmodels.HomeFragmentViewModel
import com.android.healthconnect.testapps.toolbox.viewmodels.PerformanceTestingViewModel
import kotlin.system.exitProcess

/** Home fragment for Health Connect Toolbox. */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mNavigationController: NavController
    private val performanceTestingViewModel: PerformanceTestingViewModel by viewModels()
    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels()

    private val manager by lazy {
        requireContext().getSystemService(HealthConnectManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Starting API Level 30 If permission is denied more than once, user doesn't see the dialog
        // asking permissions again unless they grant the permission from settings.
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionMap: Map<String, Boolean> ->
                requestPermissionResultHandler(permissionMap)
            }
    }

    private fun requestPermissionResultHandler(permissionMap: Map<String, Boolean>) {
        var numberOfPermissionsMissing = HEALTH_PERMISSIONS.size
        for (value in permissionMap.values) {
            if (value) {
                numberOfPermissionsMissing--
            }
        }

        if (numberOfPermissionsMissing == 0) {
            Toast.makeText(
                    this.requireContext(),
                    R.string.all_permissions_success,
                    Toast.LENGTH_SHORT,
                )
                .show()
        } else {
            Toast.makeText(
                    this.requireContext(),
                    getString(
                        R.string.number_of_permissions_not_granted,
                        numberOfPermissionsMissing,
                    ),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val performanceTesting = PerformanceTestingFragment()
        childFragmentManager
            .beginTransaction()
            .add(performanceTesting, "PERFORMANCE_TESTING_FRAGMENT")
            .commit()
        view.requireViewById<Button>(R.id.launch_health_connect_button).setOnClickListener {
            launchHealthConnect()
        }
        view.requireViewById<Button>(R.id.permissions_request_button).setOnClickListener {
            goToPermissionsRequestScreen()
        }
        view.requireViewById<Button>(R.id.request_route_button).setOnClickListener {
            goToRequestRoute()
        }
        view.requireViewById<Button>(R.id.insert_update_data_button).setOnClickListener {
            goToCategoryListPage()
        }
        view.requireViewById<Button>(R.id.seed_random_data_button).setOnClickListener {
            seedDataButtonPressed()
        }
        view.requireViewById<Button>(R.id.seed_all_data_button).setOnClickListener {
            seedAllDataButtonPressed()
        }
        view.requireViewById<Button>(R.id.seed_performance_read_data_button).setOnClickListener {
            performanceTestingViewModel.beginReadingData()
        }
        view.requireViewById<Button>(R.id.seed_performance_insert_data_button).setOnClickListener {
            performanceTestingViewModel.beginInsertingData(false)
        }
        view.requireViewById<Button>(R.id.toggle_permission_intent_filter).setOnClickListener {
            togglePermissionIntentFilter()
        }
        view.requireViewById<Button>(R.id.read_data_in_background_button).setOnClickListener {
            goToReadDataInBackgroundPage()
        }
        view.requireViewById<Button>(R.id.read_data_in_foreground_button).setOnClickListener {
            goToReadDataInForegroundPage()
        }
        view.findViewById<Button>(R.id.phr_options_button).setOnClickListener { goToPhrOptions() }

        view.requireViewById<Button>(R.id.exit_process_button).setOnClickListener {
            exitProcess(status = 0)
        }
        view.findViewById<Button>(R.id.new_screens_button).setOnClickListener {
            startActivity(Intent(requireActivity(), HomeActivity::class.java))
        }
        view.requireViewById<Button>(R.id.backup_restore_button).setOnClickListener {
            goToBackupRestorePage()
        }
        view.requireViewById<Button>(R.id.access_log_button).setOnClickListener {
            accessLogButtonPressed()
        }
        mNavigationController = findNavController()

        homeFragmentViewModel.seedAllDataState.observe(viewLifecycleOwner) { state ->
            if (state is HomeFragmentViewModel.SeedAllDataState.Error) {
                Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_seed_data_success, Toast.LENGTH_SHORT).show()
            }
        }

        setUpMatchmaking(view)
    }

    private fun setUpMatchmaking(view: View) {
        val matchmakingButton = view.requireViewById<Button>(R.id.matchmaking_button)
        homeFragmentViewModel.loadMatchmakingStatus(manager)
        homeFragmentViewModel.canConnectMatchingApps.observe(viewLifecycleOwner) { canConnect ->
            matchmakingButton.isVisible = canConnect
        }

        val matchingAppsIntent = homeFragmentViewModel.createMatchmakingIntent(manager)
        matchmakingButton.setOnClickListener {
            matchmakingActivityResultLauncher.launch(matchingAppsIntent)
        }
    }

    private val matchmakingActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "Matchmaking successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Matchmaking activity cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onResume() {
        super.onResume()
        homeFragmentViewModel.loadMatchmakingStatus(manager)
    }

    private fun launchHealthConnect() {
        val intent = Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun accessLogButtonPressed() {
        try {
            // Foreground 15-minute write quota = 1000
            SeedData(requireContext(), manager).seedRandomDataToGenerateAccessLog(1_000)
            Toast.makeText(
                    this.requireContext(),
                    R.string.toast_seed_data_success,
                    Toast.LENGTH_SHORT,
                )
                .show()
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), ex.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun seedDataButtonPressed() {
        try {
            SeedData(requireContext(), manager).seedData()
            Toast.makeText(
                    this.requireContext(),
                    R.string.toast_seed_data_success,
                    Toast.LENGTH_SHORT,
                )
                .show()
        } catch (ex: Exception) {
            Toast.makeText(this.requireContext(), ex.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun seedAllDataButtonPressed() {
        homeFragmentViewModel.seedAllDataViewModel(requireContext(), manager)
    }

    private fun togglePermissionIntentFilter() {
        val pm = requireActivity().applicationContext.packageManager
        val packageName = requireActivity().packageName
        val compName = ComponentName(packageName, "$packageName.AliasMainActivity")
        val componentState = pm.getComponentEnabledSetting(compName)
        var desiredState = COMPONENT_ENABLED_STATE_ENABLED
        if (
            componentState == COMPONENT_ENABLED_STATE_DEFAULT ||
                componentState == COMPONENT_ENABLED_STATE_ENABLED
        ) {
            desiredState = COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(compName, desiredState, PackageManager.DONT_KILL_APP)

        val toastText =
            if (desiredState == COMPONENT_ENABLED_STATE_ENABLED)
                R.string.toast_permission_filter_enabled
            else R.string.toast_permission_filter_disabled

        Toast.makeText(this.requireContext(), toastText, Toast.LENGTH_SHORT).show()
    }

    private fun goToPermissionsRequestScreen() {
        mNavigationController.navigate(R.id.action_home_screen_to_permissionsRequestScreen)
    }

    private fun goToRequestRoute() {
        mNavigationController.navigate(R.id.action_homeFragment_to_routeRequest)
    }

    private fun goToCategoryListPage() {
        mNavigationController.navigate(R.id.action_homeFragment_to_categoryList)
    }

    private fun goToReadDataInBackgroundPage() {
        mNavigationController.navigate(R.id.action_homeFragment_to_readDataInBackground)
    }

    private fun goToReadDataInForegroundPage() {
        mNavigationController.navigate(R.id.action_homeFragment_to_readDataInForeground)
    }

    private fun goToPhrOptions() {
        mNavigationController.navigate(R.id.action_homeFragment_to_phrOptions)
    }

    private fun goToBackupRestorePage() {
        mNavigationController.navigate(R.id.action_home_screen_to_backupRestoreScreen)
    }
}
