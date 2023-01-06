package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.MainActivity
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

/** Onboarding activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class OnboardingActivity : Hilt_OnboardingActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_screen)
        val sharedPreference = getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
        val goBackButton = findViewById<Button>(R.id.go_back_button)
        val getStartedButton = findViewById<Button>(R.id.get_started_button)
        goBackButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }
        getStartedButton.setOnClickListener {
            val editor = sharedPreference.edit()
            editor.putBoolean(getString(R.string.previously_opened), true)
            editor.apply()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
