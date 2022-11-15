package com.android.healthconnect.controller.cantseeallyourapps

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Can't see all your apps fragment for Health Connect. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class CantSeeAllYourAppsFragment : Hilt_CantSeeAllYourAppsFragment() {

    override fun onResume() {
        super.onResume()
        setTitle(R.string.cant_see_all_your_apps_title)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.cant_see_all_your_apps_screen, rootKey)
    }
}
