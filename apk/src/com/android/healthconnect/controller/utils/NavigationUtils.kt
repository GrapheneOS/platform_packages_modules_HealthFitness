package com.android.healthconnect.controller.utils

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

class NavigationUtils @Inject constructor() {

    fun navigate(fragment: Fragment, action: Int) {
        fragment.findNavController().navigate(action)
    }

    fun startActivity(fragment: Fragment, intent: Intent) {
        fragment.startActivity(intent)
    }
}
