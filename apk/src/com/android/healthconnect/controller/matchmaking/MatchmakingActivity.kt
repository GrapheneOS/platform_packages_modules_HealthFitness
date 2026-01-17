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

package com.android.healthconnect.controller.matchmaking

import android.health.connect.HealthConnectManager.EXTRA_RECORD_TYPES
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.HealthConnectBottomSheetDialogFragment
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.healthfitness.flags.Flags.matchmaking
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(FragmentActivity::class)
class MatchmakingActivity :
    Hilt_MatchmakingActivity(), HealthConnectBottomSheetDialogFragment.OnCancelListener {
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    private val viewModel: MatchmakingViewModel by viewModels()

    private var bottomSheet: HealthConnectBottomSheetDialogFragment? = null

    private var loadingView: View? = null

    private var errorView: View? = null

    companion object {

        private const val TAG = "MatchmakingActivity"

        private const val BOTTOM_SHEET_TAG = "MatchmakingBottomSheet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (!matchmaking()) {
            finishWithCancelResult()
        }

        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_HealthConnect_MatchMakingActivity_Overlay_Expressive)
        } else {
            setTheme(R.style.Theme_HealthConnect_MatchMakingActivity_Overlay)
        }

        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS)

        // Handles unsupported devices and user profiles.
        if (!deviceInfoUtils.isHealthConnectAvailable(this)) {
            Log.e(TAG, "Health connect is not available for this user or hardware, finishing!")
            finishWithCancelResult()
            return
        }

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            finishWithCancelResult()

            return
        }

        val callingPackage = callingPackage

        if (callingPackage == null) {
            Log.e(TAG, "Calling package is null, finishing!")
            finishWithCancelResult()
            return
        }

        setContentView(R.layout.activity_matchmaking)
        loadingView = findViewById(R.id.loading)
        errorView = findViewById(R.id.error_view)

        if (savedInstanceState == null) {
            val recordTypeNames = intent.getStringArrayExtra(EXTRA_RECORD_TYPES)
            viewModel.loadMatchmakingData(callingPackage, recordTypeNames)
        }

        viewModel.matchmakingState.observe(this) { state ->
            when (state) {
                is MatchmakingViewModel.MatchmakingState.Loading -> {
                    loadingView?.isVisible = true
                    errorView?.isVisible = false
                }

                is MatchmakingViewModel.MatchmakingState.LoadingFailed -> {
                    loadingView?.isVisible = false
                    errorView?.isVisible = true
                    finishWithCancelResult()
                }

                is MatchmakingViewModel.MatchmakingState.WithData -> {
                    loadingView?.isVisible = false
                    errorView?.isVisible = false
                    if (state.matchingApps.isEmpty() && state.matchingDevices.isEmpty()) {
                        finishWithCancelResult()
                    } else {
                        if (savedInstanceState == null) {
                            bottomSheet =
                                HealthConnectBottomSheetDialogFragment.newInstance(
                                    MatchmakingFragment::class.java
                                )

                            bottomSheet?.show(supportFragmentManager, BOTTOM_SHEET_TAG)
                        }
                    }
                }
            }
        }
    }

    override fun onDialogCanceled() {
        val callingPackage = callingPackage
        if (callingPackage != null) {
            viewModel.removeAllPermissionsFromGrantedList(callingPackage)
        }
        finishWithCancelResult()
    }

    private fun finishWithCancelResult() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
