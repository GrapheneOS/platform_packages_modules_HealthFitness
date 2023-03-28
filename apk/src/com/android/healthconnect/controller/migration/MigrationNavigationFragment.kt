package com.android.healthconnect.controller.migration

import android.health.connect.HealthConnectDataState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(Fragment::class)
class MigrationNavigationFragment : Hilt_MigrationNavigationFragment() {

    private val migrationViewModel: MigrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_migration_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        migrationViewModel.migrationState.observe(viewLifecycleOwner) { migrationState ->
            updateFragment(migrationState)
        }
    }

    private fun updateFragment(migrationState: @DataMigrationState Int) {
        when (migrationState) {
            // TODO (b/273745755) Expose real UI states
            HealthConnectDataState.MIGRATION_STATE_IDLE -> {
                // do nothing
            }
            HealthConnectDataState.MIGRATION_STATE_ALLOWED -> {
                // start migration
                showMoreSpaceNeededFragment()
            }
            HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS -> {
                showInProgressFragment()
            }
            HealthConnectDataState.MIGRATION_STATE_APP_UPGRADE_REQUIRED -> {
                showAppUpdateRequiredFragment()
            }
            HealthConnectDataState.MIGRATION_STATE_MODULE_UPGRADE_REQUIRED -> {
                showModuleUpdateRequiredFragment()
            }
        }
    }

    private fun showInProgressFragment() {
        findNavController()
            .navigate(R.id.action_migrationNavigationFragment_to_migrationInProgressFragment)
    }

    private fun showAppUpdateRequiredFragment() {
        findNavController()
            .navigate(R.id.action_migrationNavigationFragment_to_migrationAppUpdateNeededFragment)
    }

    private fun showModuleUpdateRequiredFragment() {
        findNavController()
            .navigate(
                R.id.action_migrationNavigationFragment_to_migrationModuleUpdateNeededFragment)
    }

    private fun showMoreSpaceNeededFragment() {
        findNavController()
            .navigate(R.id.action_migrationNavigationFragment_to_migrationMoreSpaceNeededFragment)
    }

    private fun showMigrationPausedFragment() {
        findNavController()
            .navigate(R.id.action_migrationNavigationFragment_to_migrationPausedFragment)
    }
}
