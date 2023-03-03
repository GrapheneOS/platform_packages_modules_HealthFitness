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
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.HealthConnectManager.EXTRA_EXERCISE_ROUTE
import android.health.connect.HealthConnectManager.EXTRA_SESSION_ID
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.route.ExerciseRouteViewModel.SessionWithAttribution
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.map.MapView
import com.android.healthconnect.controller.utils.FeatureUtils
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/** Request route activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class RouteRequestActivity : Hilt_RouteRequestActivity() {

    companion object {
        private const val TAG = "RouteRequestActivity"
    }

    @Inject lateinit var appInfoReader: AppInfoReader
    @Inject lateinit var featureUtils: FeatureUtils

    @VisibleForTesting lateinit var dialog: AlertDialog
    @VisibleForTesting lateinit var infoDialog: AlertDialog

    private val viewModel: ExerciseRouteViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!featureUtils.isExerciseRouteEnabled()) {
            Log.e(TAG, "Exercise routes not available, finishing.")
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
            return
        }

        if (!intent.hasExtra(EXTRA_SESSION_ID) ||
            intent.getStringExtra(EXTRA_SESSION_ID) == null ||
            !intent.hasExtra(EXTRA_PACKAGE_NAME) ||
            intent.getStringExtra(EXTRA_PACKAGE_NAME) == null) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            setResult(Activity.RESULT_CANCELED, Intent())
            finish()
            return
        }
        viewModel.getExerciseWithRoute(intent.getStringExtra(EXTRA_SESSION_ID)!!)
        viewModel.exerciseSession.observe(this) { session -> setupRequestDialog(session) }
    }

    private fun setupRequestDialog(data: SessionWithAttribution?) {
        if ((data == null) ||
            (data.session?.route == null) ||
            data.session?.route!!.routeLocations.isEmpty()) {
            Log.e(TAG, "No route or empty route, finishing.")
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            setResult(Activity.RESULT_CANCELED, result)
            finish()
            return
        }

        val session = data.session!!
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        val sessionDetails =
            applicationContext.getString(
                R.string.date_owner_format,
                LocalDateTimeFormatter(applicationContext).formatLongDate(session.startTime),
                data.appInfo.appName)
        val view = layoutInflater.inflate(R.layout.route_request_dialog, null)
        runBlocking {
            val requester =
                appInfoReader.getAppMetadata(intent.getStringExtra(EXTRA_PACKAGE_NAME)!!)
            val title =
                applicationContext.getString(R.string.request_route_header_title, requester.appName)
            view.findViewById<TextView>(R.id.dialog_title).text = title
        }
        view
            .findViewById<ImageView>(R.id.dialog_icon)
            .setImageDrawable(getDrawable(R.drawable.health_connect_icon))
        view.findViewById<MapView>(R.id.map_view).setRoute(session.route!!)
        view.findViewById<TextView>(R.id.session_title).text = session.title
        view.findViewById<TextView>(R.id.date_app).text = sessionDetails

        view.findViewById<LinearLayout>(R.id.more_info).setOnClickListener {
            dialog.hide()
            setupInfoDialog()
            infoDialog.show()
        }

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
        dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        dialog.show()
    }

    private fun setupInfoDialog() {
        val view = layoutInflater.inflate(R.layout.route_sharing_info_dialog, null)
        view.findViewById<TextView>(R.id.dialog_title).text =
            applicationContext.getString(R.string.request_route_info_header_title)
        view
            .findViewById<ImageView>(R.id.dialog_icon)
            .setImageDrawable(getDrawable(R.drawable.quantum_gm_ic_privacy_tip_vd_theme_24))
        infoDialog =
            AlertDialog.Builder(this)
                .setNegativeButton(R.string.back_button) { _, _ -> dialog.show() }
                .setView(view)
                .setCancelable(false)
                .create()
    }
}
