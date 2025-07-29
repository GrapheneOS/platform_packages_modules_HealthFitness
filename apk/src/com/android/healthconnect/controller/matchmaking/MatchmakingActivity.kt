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

import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.HealthConnectBottomSheetDialogFragment
import com.android.healthconnect.controller.shared.dialog.HealthConnectBottomSheetDialogFragment.BottomSheetCallback
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.activity.EmbeddingUtils.maybeRedirectIntoTwoPaneSettings
import com.android.healthfitness.flags.Flags.matchmaking
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(FragmentActivity::class)
class MatchmakingActivity : Hilt_MatchmakingActivity(), BottomSheetCallback {
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils

    companion object {
        private const val TAG = "MatchmakingActivity"
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
        }

        if (maybeRedirectIntoTwoPaneSettings(this)) {
            finishWithCancelResult()
        }

        setContentView(R.layout.activity_matchmaking)

        if (savedInstanceState == null) {
            HealthConnectBottomSheetDialogFragment.newInstance(MatchmakingFragment::class.java)
                .show(supportFragmentManager, "MatchmakingBottomSheet")
        }
    }

    override fun onPrimaryButtonClicked() {
        // TODO: Grant permissions
        finish()
    }

    override fun onSecondaryButtonClicked() {
        // TODO: Revoke permissions
        finish()
    }

    override fun onDialogCancel() {
        finishWithCancelResult()
    }

    private fun finishWithCancelResult() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
