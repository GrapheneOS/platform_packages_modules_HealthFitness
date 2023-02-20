package com.android.healthconnect.controller.permissions.connectedapps

import android.content.Context
import android.text.TextUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.settingslib.widget.AppPreference

class HealthAppPreference(context: Context, private val appMetadata: AppMetadata) :
    AppPreference(context), ComparablePreference {

    init {
        title = appMetadata.appName
        icon = appMetadata.icon
    }

    override fun isSameItem(preference: Preference): Boolean {
        return preference is HealthAppPreference &&
            TextUtils.equals(appMetadata.appName, preference.appMetadata.appName)
    }

    override fun hasSameContents(preference: Preference): Boolean {
        return preference is HealthAppPreference && appMetadata == preference.appMetadata
    }

    override fun onBindViewHolder(view: PreferenceViewHolder?) {
        super.onBindViewHolder(view)
    }
}

/** Allows comparison with a [Preference] to determine if it has been changed. */
internal interface ComparablePreference {
    /** Returns true if given Preference represents an item of the same kind. */
    fun isSameItem(preference: Preference): Boolean

    /** Returns true if given Preference contains the same data. */
    fun hasSameContents(preference: Preference): Boolean
}
