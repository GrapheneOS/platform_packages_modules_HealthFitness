package com.android.healthconnect.controller.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.OnboardingElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {
    @Inject lateinit var logger: HealthConnectLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_screen)

        logger.setPageId(PageName.ONBOARDING_PAGE)

        val sharedPreference = getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
        val goBackButton = findViewById<Button>(R.id.go_back_button)
        val getStartedButton = findViewById<Button>(R.id.get_started_button)
        logger.logImpression(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
        logger.logImpression(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)

        goBackButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
            finish()
        }
        getStartedButton.setOnClickListener {
            logger.logInteraction(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)
            val editor = sharedPreference.edit()
            editor.putBoolean(getString(R.string.previously_opened), true)
            editor.apply()
            val nextIntentToOpen: Intent? = getIntentExtra()
            nextIntentToOpen?.let { startActivity(getIntentExtra()) }
            finish()
        }
    }

    private fun getIntentExtra(): Intent? {
        return intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    }
}
