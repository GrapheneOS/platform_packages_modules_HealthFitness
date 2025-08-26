/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.healthconnect.controller.permissions.request

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(BottomSheetDialogFragment::class)
class PermissionsBottomSheetDialogFragment : Hilt_PermissionsBottomSheetDialogFragment() {
    @Inject lateinit var logger: HealthConnectLogger
    private val viewModel: RequestPermissionViewModel by activityViewModels()

    private var currentContentFragment: Fragment? = null

    companion object {
        const val TAG = "PermissionsBottomSheet"
        const val HALF_EXPANDED_RATIO = 0.8

        fun newInstance(): PermissionsBottomSheetDialogFragment {
            return PermissionsBottomSheetDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_dialog, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            bottomSheet?.let { frameLayout ->
                frameLayout.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_background)

                val behavior = BottomSheetBehavior.from(frameLayout)
                behavior.peekHeight =
                    (resources.displayMetrics.heightPixels * HALF_EXPANDED_RATIO).toInt()
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            updateBottomSheetHeightBasedOnState(
                                bottomSheet,
                                newState,
                                behavior,
                                frameLayout,
                            )
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                    }
                )
                val initialState = BottomSheetBehavior.STATE_COLLAPSED
                behavior.state = initialState

                frameLayout.post {
                    updateBottomSheetHeightBasedOnState(
                        frameLayout,
                        initialState,
                        behavior,
                        frameLayout,
                    )
                }
            }
        }
        return dialog
    }

    private fun updateBottomSheetHeightBasedOnState(
        bottomSheetView: View,
        newState: Int,
        behavior: BottomSheetBehavior<FrameLayout>,
        frameLayout: FrameLayout,
    ) {
        frameLayout.let { wrapper ->
            val screenHeight = resources.displayMetrics.heightPixels
            val newHeight: Int =
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        (bottomSheetView.parent as? View)?.height
                            ?: (screenHeight - behavior.expandedOffset)
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        behavior.peekHeight
                    }

                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        (screenHeight * HALF_EXPANDED_RATIO).toInt()
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {
                        0
                    }

                    else -> {
                        return
                    }
                }

            val params = wrapper.layoutParams
            if (params.height != newHeight) {
                params.height = newHeight
                wrapper.layoutParams = params
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModelObservers()
    }

    private fun setupViewModelObservers() {
        viewModel.permissionsActivityState.observe(viewLifecycleOwner) { screenState ->
            when (screenState) {
                is PermissionsActivityState.ShowMedical -> {
                    if (screenState.isWriteOnly) {
                        showContentFragment(MedicalWritePermissionFragment())
                    } else {
                        showContentFragment(MedicalPermissionsFragment())
                    }
                }
                is PermissionsActivityState.ShowFitness -> {
                    showContentFragment(FitnessPermissionsFragment())
                }
                is PermissionsActivityState.ShowAdditional -> {
                    if (screenState.singlePermission) {
                        showContentFragment(SingleAdditionalPermissionFragment())
                    } else {
                        showContentFragment(CombinedAdditionalPermissionsFragment())
                    }
                }
                is PermissionsActivityState.FinishRequest -> {
                    handlePermissionFlowCompletion()
                }
                is PermissionsActivityState.NoPermissions -> {
                    handlePermissionFlowCompletion()
                }
            }
        }
    }

    private fun handlePermissionFlowCompletion() {
        setActivityResultsFromViewModel()
        dismiss()
        activity?.finish()
    }

    private fun showContentFragment(fragment: Fragment) {
        if (currentContentFragment?.javaClass == fragment.javaClass) {
            return
        }
        currentContentFragment = fragment
        childFragmentManager
            .beginTransaction()
            .replace(R.id.bottom_sheet_fragment_container, fragment)
            .commit()
    }

    private fun setActivityResultsFromViewModel() {
        if (activity == null || activity?.isFinishing == true) {
            return
        }
        val results = viewModel.getPermissionGrants()
        val grants =
            results.values.map { state ->
                if (state == PermissionState.GRANTED) {
                    PackageManager.PERMISSION_GRANTED
                } else {
                    PackageManager.PERMISSION_DENIED
                }
            }
        val permissionStrings = results.keys.map { it.toString() }

        val resultIntent =
            Intent().apply {
                putExtra(
                    PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES,
                    permissionStrings.toTypedArray(),
                )
                putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS, grants.toIntArray())
            }
        activity?.setResult(RESULT_OK, resultIntent)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity?.setResult(android.app.Activity.RESULT_CANCELED)
        activity?.finish()
    }
}
