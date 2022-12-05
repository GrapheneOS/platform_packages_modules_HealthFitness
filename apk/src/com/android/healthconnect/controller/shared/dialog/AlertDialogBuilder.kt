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
package com.android.healthconnect.controller.shared.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.DialogTitle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.AttributeResolver

/** {@link AlertDialog.Builder} wrapper for applying theming attributes. */
class AlertDialogBuilder(fragment: Fragment) {

    private var context: Context = fragment.requireContext()
    private var alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
    private var customTitleLayout: View =
        LayoutInflater.from(context).inflate(R.layout.dialog_title, null)

    fun setIcon(@AttrRes iconId: Int): AlertDialogBuilder {
        val iconView: ImageView = customTitleLayout.findViewById(R.id.dialog_icon)
        val iconDrawable = AttributeResolver.getNullableDrawable(context, iconId)
        iconDrawable?.let {
            iconView.setImageDrawable(it)
            alertDialogBuilder.setCustomTitle(customTitleLayout)
        }

        return this
    }

    /** Sets the title in the custom title layout using the given resource id. */
    fun setTitle(@StringRes titleId: Int): AlertDialogBuilder {
        val titleView: DialogTitle = customTitleLayout.findViewById(R.id.dialog_title)
        titleView.setText(titleId)
        alertDialogBuilder.setCustomTitle(customTitleLayout)
        return this
    }

    /** Sets the title in the custom title layout. */
    fun setTitle(titleString: String): AlertDialogBuilder {
        val titleView: DialogTitle = customTitleLayout.findViewById(R.id.dialog_title)
        titleView.text = titleString
        alertDialogBuilder.setCustomTitle(customTitleLayout)
        return this
    }

    /** Sets the message to be displayed in the dialog using the given resource id. */
    fun setMessage(@StringRes messageId: Int): AlertDialogBuilder {
        alertDialogBuilder.setMessage(messageId)
        return this
    }

    /** Sets the message to be displayed in the dialog. */
    fun setMessage(message: CharSequence?): AlertDialogBuilder {
        alertDialogBuilder.setMessage(message)
        return this
    }

    fun setView(view: View): AlertDialogBuilder {
        alertDialogBuilder.setView(view)
        return this
    }

    fun setPositiveButton(
        @StringRes textId: Int,
        onClickListener: DialogInterface.OnClickListener? = null
    ): AlertDialogBuilder {
        alertDialogBuilder.setPositiveButton(textId, onClickListener)
        return this
    }

    fun setNegativeButton(
        @StringRes textId: Int,
        onClickListener: DialogInterface.OnClickListener? = null
    ): AlertDialogBuilder {
        alertDialogBuilder.setNegativeButton(textId, onClickListener)
        return this
    }

    fun setMessage(message: String): AlertDialogBuilder {
        alertDialogBuilder.setMessage(message)
        return this
    }

    fun create(): AlertDialog {
        val dialog = alertDialogBuilder.create()
        setDialogGravityFromTheme(dialog)
        return dialog
    }

    private fun setDialogGravityFromTheme(dialog: AlertDialog) {
        val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.dialogGravity))
        try {
            if (typedArray.hasValue(0)) {
                requireNotNull(dialog.window).setGravity(typedArray.getInteger(0, CENTER))
            }
        } finally {
            typedArray.recycle()
        }
    }
}
