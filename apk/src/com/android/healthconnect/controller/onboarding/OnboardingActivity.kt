package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_screen)
        val goBackButton = findViewById<Button>(R.id.go_back_button)
        val getStartedButton = findViewById<Button>(R.id.get_started_button)
        goBackButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        getStartedButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
