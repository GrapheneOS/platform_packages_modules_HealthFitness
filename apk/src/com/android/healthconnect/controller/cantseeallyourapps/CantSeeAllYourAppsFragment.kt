package com.android.healthconnect.controller.cantseeallyourapps

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Can't see all your apps fragment for Health Connect. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class CantSeeAllYourAppsFragment : Hilt_CantSeeAllYourAppsFragment() {

    companion object {
        const val CHECK_FOR_UPDATES = "check_for_updates"
        private const val SEE_ALL_COMPATIBLE_APPS = "see_all_compatible_apps"
        private const val SEND_FEEDBACK = "send_feedback"
    }

    private val mCheckForUpdates: Preference? by lazy {
        preferenceScreen.findPreference(CHECK_FOR_UPDATES)
    }

    private val mSeeAllCompatibleApps: Preference? by lazy {
        preferenceScreen.findPreference(SEE_ALL_COMPATIBLE_APPS)
    }

    private val mSendFeedback: Preference? by lazy {
        preferenceScreen.findPreference(SEND_FEEDBACK)
    }

    override fun onResume() {
        super.onResume()
        setTitle(R.string.cant_see_all_your_apps_title)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.cant_see_all_your_apps_screen, rootKey)

        mCheckForUpdates?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connected_apps_to_updated_apps)
            true
        }

        mSeeAllCompatibleApps?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connected_apps_to_play_store)
            true
        }

        mSendFeedback?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_BUG_REPORT)
            getActivity()?.startActivityForResult(intent, 0)
            true
        }
    }
}
