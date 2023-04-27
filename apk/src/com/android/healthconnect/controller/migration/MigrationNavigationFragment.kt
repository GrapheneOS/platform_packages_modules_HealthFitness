package com.android.healthconnect.controller.migration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.api.MigrationState
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

    private fun updateFragment(migrationState: MigrationState) {
        when (migrationState) {
            MigrationState.ALLOWED_NOT_STARTED,
            MigrationState.ALLOWED_PAUSED -> {
                showMigrationPausedFragment()
            }
            MigrationState.APP_UPGRADE_REQUIRED -> {
                showAppUpdateRequiredFragment()
            }
            MigrationState.MODULE_UPGRADE_REQUIRED -> {
                showModuleUpdateRequiredFragment()
            }
            MigrationState.IN_PROGRESS -> {
                showInProgressFragment()
            }
            else -> {
                // Other states should not lead here
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

    private fun showMigrationPausedFragment() {
        findNavController()
            .navigate(R.id.action_migrationNavigationFragment_to_migrationPausedFragment)
    }
}
