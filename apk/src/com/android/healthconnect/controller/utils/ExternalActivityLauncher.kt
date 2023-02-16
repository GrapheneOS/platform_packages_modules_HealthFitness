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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.settingslib.HelpUtils

/** Utility class to launch help center articles. */
object ExternalActivityLauncher {

    private const val TAG = "ExternalActivityLauncher"
    fun openHCGetStartedLink(activity: FragmentActivity) {
        val helpUrlString = activity.getString(R.string.hc_get_started_link)
        val fullUri = HelpUtils.uriWithAddedParameters(activity, Uri.parse(helpUrlString))
        val intent =
            Intent(Intent.ACTION_VIEW, fullUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Unable to open help center URL.", e)
        }
    }

    fun openSendFeedbackActivity(activity: FragmentActivity) {
        val intent = Intent(Intent.ACTION_BUG_REPORT)
        activity.startActivityForResult(intent, 0)
    }
}
