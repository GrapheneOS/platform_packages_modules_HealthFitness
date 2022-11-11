package com.android.healthconnect.controller.shared.dialog

import android.content.Context
import android.util.Log
import android.view.Gravity.CENTER
import android.view.Gravity.TOP
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.DialogTitle
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.AttributeResolver

/** {@link AlertDialog.Builder} wrapper for applying theming attributes. */
class AlertDialogBuilder(dialogFragment: DialogFragment) {

    private var context: Context = dialogFragment.requireContext()
    private var alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
    private var customTitleLayout: View =
        LayoutInflater.from(context).inflate(R.layout.dialog_title, null)

    init {
        alertDialogBuilder.setCustomTitle(customTitleLayout)
    }

    fun setIcon(@AttrRes iconId: Int): AlertDialogBuilder {
        val iconView: ImageView = customTitleLayout.findViewById(R.id.dialog_icon)
        val iconDrawable = AttributeResolver.getNullableDrawable(context, iconId)
        iconDrawable?.let { iconView.setImageDrawable(it) }
        return this
    }

    /** Sets the title in the custom title layout */
    fun setTitle(@StringRes titleId: Int): AlertDialogBuilder {
        val titleView: DialogTitle = customTitleLayout.findViewById(R.id.dialog_title)
        titleView.setText(titleId)
        return this
    }

    fun setView(view: View): AlertDialogBuilder {
        alertDialogBuilder.setView(view)
        return this
    }

    fun setPositiveButton(@StringRes textId: Int): AlertDialogBuilder {
        alertDialogBuilder.setPositiveButton(textId) { dialog, which -> // TODO
        }
        return this
    }

    fun setNegativeButton(@StringRes textId: Int): AlertDialogBuilder {
        alertDialogBuilder.setNegativeButton(textId) { dialog, which -> // TODO
        }
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
