package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.OnboardingElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.annotations.VisibleForTesting
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {

    /** Companion object for OnboardingActivity. */
    companion object {
        @VisibleForTesting const val USER_ACTIVITY_TRACKER = "USER_ACTIVITY_TRACKER"
        @VisibleForTesting const val ONBOARDING_SHOWN_PREF_KEY = "ONBOARDING_SHOWN_PREF_KEY"

        fun maybeRedirectToOnboardingActivity(activity: Activity): Boolean {
            val sharedPreference =
                activity.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
            val previouslyOpened = sharedPreference.getBoolean(ONBOARDING_SHOWN_PREF_KEY, false)
            if (!previouslyOpened) {
                return true
            }
            return false
        }
    }

    @Inject lateinit var logger: HealthConnectLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_screen)

        logger.setPageId(PageName.ONBOARDING_PAGE)

        val sharedPreference = getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val goBackButton = findViewById<Button>(R.id.go_back_button)
        val getStartedButton = findViewById<Button>(R.id.get_started_button)
        logger.logImpression(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
        logger.logImpression(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)

        goBackButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        getStartedButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)
            val editor = sharedPreference.edit()
            editor.putBoolean(ONBOARDING_SHOWN_PREF_KEY, true)
            editor.apply()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        logger.logPageImpression()
    }
}
