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

package com.android.healthconnect.controller.shared.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.increaseViewTouchTargetSize
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(BottomSheetDialogFragment::class)
class HealthConnectBottomSheetDialogFragment : Hilt_HealthConnectBottomSheetDialogFragment() {

    @Inject lateinit var logger: HealthConnectLogger

    private lateinit var contentFragment: Fragment
    private var callback: BottomSheetCallback? = null

    interface BottomSheetCallback {
        fun onPrimaryButtonClicked()

        fun onSecondaryButtonClicked()

        fun onDialogCancel()
    }

    companion object {
        private const val FRAGMENT_CLASS_KEY = "fragment_class"
        const val HALF_EXPANDED_RATIO = 0.8

        fun newInstance(
            fragmentClass: Class<out Fragment>
        ): HealthConnectBottomSheetDialogFragment {
            return HealthConnectBottomSheetDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(FRAGMENT_CLASS_KEY, fragmentClass) }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback =
            try {
                context as BottomSheetCallback
            } catch (exception: ClassCastException) {
                throw ClassCastException("$context must implement BottomSheetCallback")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentClass = arguments?.getSerializable(FRAGMENT_CLASS_KEY) as? Class<out Fragment>
        if (fragmentClass != null) {
            contentFragment = fragmentClass.getDeclaredConstructor().newInstance()
        } else {
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.health_connect_bottom_sheet, container, false)
        val buttonLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                R.layout.widget_setup_bottom_button_bar_expressive
            } else {
                R.layout.widget_setup_bottom_button_bar_legacy
            }

        val buttonArea = view.findViewById<FrameLayout>(R.id.bottom_sheet_button_container)
        val buttons = inflater.inflate(buttonLayoutId, buttonArea, false)
        buttonArea.addView(buttons)

        val primaryButton = buttonArea.findViewById<Button>(R.id.primary_button_full)
        val secondaryButton = buttonArea.findViewById<Button>(R.id.secondary_button)
        primaryButton.text = getString(R.string.request_permissions_allow)
        secondaryButton.text = getString(R.string.request_permissions_dont_allow)

        val allowParentView = primaryButton.parent.parent as View
        increaseViewTouchTargetSize(requireContext(), primaryButton, allowParentView)

        val dontAllowParentView = secondaryButton.parent as View
        increaseViewTouchTargetSize(requireContext(), secondaryButton, dontAllowParentView)

        primaryButton.setOnClickListener {
            callback?.onPrimaryButtonClicked()
            dismiss()
        }
        secondaryButton.setOnClickListener {
            callback?.onSecondaryButtonClicked()
            dismiss()
        }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.bottom_sheet_fragment_container, contentFragment)
            .commit()
        return view
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
            }
        }
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback?.onDialogCancel()
    }
}
