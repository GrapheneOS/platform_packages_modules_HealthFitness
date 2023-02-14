/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.controller.route

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.health.connect.HealthConnectManager.EXTRA_EXERCISE_ROUTE
import android.health.connect.HealthConnectManager.EXTRA_SESSION_ID
import android.health.connect.datatypes.ExerciseSessionRecord
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.map.MapView
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint

/** Request route activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class RouteRequestActivity : Hilt_RouteRequestActivity() {

    companion object {
        private const val TAG = "RouteRequestActivity"
    }

    @VisibleForTesting lateinit var dialog: AlertDialog

    private val viewModel: ExerciseRouteViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra(EXTRA_SESSION_ID) || intent.getStringExtra(EXTRA_SESSION_ID) == null) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
            return
        }
        viewModel.getExerciseWithRoute(intent.getStringExtra(EXTRA_SESSION_ID)!!)
        viewModel.exerciseSession.observe(this) { session -> setupDialog(session) }
    }

    private fun setupDialog(sessionRecord: ExerciseSessionRecord?) {
        if (sessionRecord?.route == null || sessionRecord.route!!.routeLocations.isEmpty()) {
            Log.e(TAG, "No route or empty route, finishing.")
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            setResult(Activity.RESULT_CANCELED, result)
            finish()
            return
        }

        val session = sessionRecord!!
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        // TODO(pakoch): Add a reference to the intent sender.
        val title = applicationContext.getString(R.string.request_route_header_title, "Test app")
        val sessionDetails =
            applicationContext.getString(
                R.string.date_owner_format,
                LocalDateTimeFormatter(applicationContext).formatLongDate(session.startTime), "app")
        val view = layoutInflater.inflate(R.layout.route_request_dialog, null)
        view
            .findViewById<ImageView>(R.id.dialog_icon)
            .setImageDrawable(getDrawable(R.drawable.health_connect_icon))
        view.findViewById<TextView>(R.id.dialog_title).text = title
        view.findViewById<MapView>(R.id.map_view).setRoute(session.route!!)
        view.findViewById<TextView>(R.id.session_title).text = session.title
        view.findViewById<TextView>(R.id.date_app).text = sessionDetails

        view.findViewById<Button>(R.id.route_dont_allow_button).setOnClickListener {
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, sessionId)
            setResult(Activity.RESULT_CANCELED, result)
            finish()
        }

        view.findViewById<Button>(R.id.route_allow_button).setOnClickListener {
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            result.putExtra(EXTRA_EXERCISE_ROUTE, session.route)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.show()
    }
}
