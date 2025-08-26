/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.shared

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.test.espresso.IdlingResource
import com.android.healthconnect.controller.shared.dialog.HealthConnectBottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** An [IdlingResource] that waits for the bottom sheet to be in a stable state. */
class BottomSheetIdlingResource(private val activity: FragmentActivity, private val tag: String) :
    IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var isIdle = false

    override fun getName(): String {
        return BottomSheetIdlingResource::class.java.name
    }

    override fun isIdleNow(): Boolean {
        if (bottomSheetBehavior == null) {
            val fragment = activity.supportFragmentManager.findFragmentByTag(tag)
            if (fragment is HealthConnectBottomSheetDialogFragment) {
                val dialog = fragment.dialog as? BottomSheetDialog
                dialog?.let {
                    val bottomSheet =
                        it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    if (bottomSheet != null) {
                        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                        bottomSheetBehavior?.addBottomSheetCallback(
                            object : BottomSheetBehavior.BottomSheetCallback() {
                                override fun onStateChanged(bottomSheet: View, newState: Int) {
                                    isIdle =
                                        (newState != BottomSheetBehavior.STATE_DRAGGING &&
                                            newState != BottomSheetBehavior.STATE_SETTLING)
                                    if (isIdle) {
                                        resourceCallback?.onTransitionToIdle()
                                    }
                                }

                                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                            }
                        )
                    }
                }
            }
        }

        isIdle =
            bottomSheetBehavior != null &&
                bottomSheetBehavior?.state != BottomSheetBehavior.STATE_DRAGGING &&
                bottomSheetBehavior?.state != BottomSheetBehavior.STATE_SETTLING

        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }
}
