package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(AppCompatActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_screen)
        getSupportActionBar()?.hide()
        var goBackButton = findViewById<Button>(R.id.go_back_button)
        var getStartedButton = findViewById<Button>(R.id.get_started_button)
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
