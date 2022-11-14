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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_PARAMETERS
import dagger.hilt.android.AndroidEntryPoint

/**
 * Invisible fragment that handles every deletion flow with the deletion dialogs.
 *
 * <p>This fragment needs to be added to every page that performs deletion. Then the deletion flow
 * can be started via {@link StartDeletionEvent}.
 *
 * <p>It can be added to the parent fragment without attaching to a view via the following snippet:
 *
 * <pre> if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
 * ```
 *      childFragmentManager
 *          .commitNow {
 *              add({@link DeletionFragment}(), FRAGMENT_TAG_DELETION)
 *          }
 * ```
 * } </pre>
 */
@AndroidEntryPoint(Fragment::class)
class DeletionFragment : Hilt_DeletionFragment() {

    var deletionParameters = Deletion()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deletion, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(DELETION_PARAMETERS, deletionParameters)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState?.let { bundle ->
            (bundle.getParcelable(DELETION_PARAMETERS) as Deletion?)?.let {
                deletionParameters = it
            }
        }
    }

    private fun showConfirmationDialog() {
        // TODO
    }

    private fun showFirstDialog() {
        if (deletionParameters.showTimeRangePickerDialog) {
            TimeRangeDialogFragment.create(deletionParameters)
                .also { it.setClickListener { dialog, which -> showConfirmationDialog() } }
                .show(childFragmentManager, TimeRangeDialogFragment.TAG)
        }
    }

    fun startDataDeletion(deletionParameters: Deletion) {
        this.deletionParameters = deletionParameters

        showFirstDialog()
    }
}
