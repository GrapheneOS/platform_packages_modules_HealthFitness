/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.deletion

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** A {@link DialogFragment} for choosing the deletion time range. */
@AndroidEntryPoint(DialogFragment::class)
class TimeRangeDialogFragment : Hilt_TimeRangeDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = layoutInflater.inflate(R.layout.time_range_picker, null)
        return AlertDialogBuilder(this)
            .setTitle(R.string.time_range_title)
            .setIcon(R.attr.deleteSettingsIcon)
            .setView(view)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.time_range_next_button)
            .create()
    }
    companion object {
        const val TAG = "TimeRangeDialogFragment"
    }
}
