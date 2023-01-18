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

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.settingslib.HelpUtils

/** Utility class to launch help center articles. */
object ExternalActivityLauncher {
    fun openHCGetStartedLink(activity: FragmentActivity) {
        activity.startActivityForResult(
            HelpUtils.getHelpIntent(
                activity,
                activity.getString(R.string.hc_get_started_link),
                /* backupContext= */ ""),
            /*requestCode=*/ 0)
    }

    fun openSendFeedbackActivity(activity: FragmentActivity) {
        val intent = Intent(Intent.ACTION_BUG_REPORT)
        activity.startActivityForResult(intent, 0)
    }
}
