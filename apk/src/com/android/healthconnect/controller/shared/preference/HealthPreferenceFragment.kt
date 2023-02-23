package com.android.healthconnect.controller.shared.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.shared.HealthPreferenceComparisonCallback

/** A base fragment that represents a page in Health Connect. */
abstract class HealthPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceComparisonCallback = HealthPreferenceComparisonCallback()
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen?): RecyclerView.Adapter<*> {
        val adapter = super.onCreateAdapter(preferenceScreen)
        /* By default, the PreferenceGroupAdapter does setHasStableIds(true). Since each Preference
         * is internally allocated with an auto-incremented ID, it does not allow us to gracefully
         * update only changed preferences based on HealthPreferenceComparisonCallback. In order to
         * allow the list to track the changes, we need to ignore the Preference IDs. */
        adapter.setHasStableIds(false)
        return adapter
    }
}
