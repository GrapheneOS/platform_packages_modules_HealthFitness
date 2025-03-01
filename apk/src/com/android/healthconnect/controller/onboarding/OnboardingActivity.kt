/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.ONBOARDING_SHOWN_PREF_KEY
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.OnboardingElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled
import com.android.settingslib.collapsingtoolbar.EdgeToEdgeUtils
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {

    /** Companion object for OnboardingActivity. */
    companion object {
        private const val TARGET_ACTIVITY_INTENT = "ONBOARDING_TARGET_ACTIVITY_INTENT"

        /** A utility function designed solely for testing purposes. */
        fun disableOnboarding(context: Context) {
            val editor =
                context.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE).edit()
            editor.putBoolean(ONBOARDING_SHOWN_PREF_KEY, true)
            editor.commit()
        }

        fun shouldRedirectToOnboardingActivity(activity: Activity): Boolean {
            val sharedPreference =
                activity.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
            val previouslyOpened = sharedPreference.getBoolean(ONBOARDING_SHOWN_PREF_KEY, false)
            return !previouslyOpened
        }

        fun createIntent(context: Context, targetIntent: Intent? = null): Intent {
            val flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION

            return Intent(context, OnboardingActivity::class.java).apply {
                addFlags(flags)
                if (targetIntent != null) {
                    putExtra(TARGET_ACTIVITY_INTENT, targetIntent)
                }
            }
        }
    }

    @Inject lateinit var logger: HealthConnectLogger

    private var targetIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.enable(this)
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_HealthConnect_Expressive)
        }
        super.onCreate(savedInstanceState)
        // This flag ensures a non system app cannot show an overlay on Health Connect. b/313425281
        window.addSystemFlags(
            WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
        )
        if (intent.hasExtra(TARGET_ACTIVITY_INTENT)) {
            targetIntent = intent.getParcelableExtra(TARGET_ACTIVITY_INTENT)
        }

        logger.setPageId(PageName.ONBOARDING_PAGE)

        setupOnboardingScreen()
    }

    override fun onResume() {
        super.onResume()
        logger.logPageImpression()
    }

    private fun setupOnboardingScreen() {
        setContentView(R.layout.onboarding_screen)

        val onboardingDescription = findViewById<TextView>(R.id.onboarding_description)
        val withHealthConnectTitle =
            findViewById<TextView>(R.id.onboarding_description_with_health_connect)
        if (isPersonalHealthRecordEnabled()) {
            onboardingDescription.setText(R.string.onboarding_description_health_records)
            withHealthConnectTitle.visibility = View.VISIBLE
            logger.logImpression(OnboardingElement.ONBOARDING_MESSAGE_WITH_PHR)
        } else {
            onboardingDescription.setText(R.string.onboarding_description)
            withHealthConnectTitle.visibility = View.GONE
        }

        setupButtonArea()
    }

    private fun setupButtonArea() {
        val buttonArea = findViewById<FrameLayout>(R.id.button_area)
        val buttonLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(this)) {
                R.layout.widget_setup_bottom_button_bar_expressive
            } else {
                R.layout.widget_setup_bottom_button_bar_legacy
            }
        buttonArea.addView(LayoutInflater.from(this).inflate(buttonLayoutId, buttonArea, false))

        val goBackButton = findViewById<Button>(R.id.secondary_button)
        goBackButton.text = getString(R.string.onboarding_go_back_button_text)
        val getStartedButton = findViewById<Button>(R.id.primary_button_full)
        getStartedButton.text = getString(R.string.onboarding_get_started_button_text)
        logger.logImpression(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
        logger.logImpression(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)

        goBackButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val sharedPreference = getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        getStartedButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)
            val editor = sharedPreference.edit()
            editor.putBoolean(ONBOARDING_SHOWN_PREF_KEY, true)
            editor.apply()
            if (targetIntent == null) {
                setResult(Activity.RESULT_OK)
            } else {
                startActivity(targetIntent)
            }
            finish()
        }
    }
}
