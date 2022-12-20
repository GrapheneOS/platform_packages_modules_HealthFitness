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
import android.healthconnect.HealthConnectManager.EXTRA_EXERCISE_ROUTE
import android.healthconnect.HealthConnectManager.EXTRA_SESSION_ID
import android.healthconnect.datatypes.ExerciseRoute
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.DialogTitle
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.map.MapView
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant

/** Request route activity for Health Connect. */
@AndroidEntryPoint(FragmentActivity::class)
class RouteRequestActivity : Hilt_RouteRequestActivity() {

    @VisibleForTesting lateinit var dialog: AlertDialog

    private val START = Instant.ofEpochMilli(1234567891011)

    // TODO(pakoch): remove this when plugging in the real data.
    private val WARSAW_ROUTE =
        ExerciseRoute(
            listOf(
                ExerciseRoute.Location.Builder(START.plusSeconds(12), 52.26019, 21.02268).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(40), 52.26000, 21.02360).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(48), 52.25973, 21.02356).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(60), 52.25966, 21.02313).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(78), 52.25993, 21.02309).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(79), 52.25972, 21.02271).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(90), 52.25948, 21.02276).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(93), 52.25945, 21.02335).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(94), 52.25960, 21.02338).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(100), 52.25961, 21.02382).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(102), 52.25954, 21.02370).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(105), 52.25945, 21.02362).build(),
                ExerciseRoute.Location.Builder(START.plusSeconds(109), 52.25954, 21.02354).build(),
            ))

    companion object {
        private const val TAG = "RouteRequestActivity"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra(EXTRA_SESSION_ID)) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            setResult(Activity.RESULT_CANCELED, result)
            finish()
        }
        // TODO(pakoch): Add a reference to the intent sender.
        val title = applicationContext.getString(R.string.request_route_header_title, "Test app")
        val view = layoutInflater.inflate(R.layout.route_request_dialog, null)

        view
            .findViewById<ImageView>(R.id.dialog_icon)
            .setImageDrawable(getDrawable(R.drawable.health_connect_icon))
        view.findViewById<DialogTitle>(R.id.dialog_title).text = title
        view.findViewById<MapView>(R.id.map_view).setRoute(WARSAW_ROUTE)

        view.findViewById<Button>(R.id.route_dont_allow_button).setOnClickListener {
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            setResult(Activity.RESULT_CANCELED, result)
            finish()
        }

        view.findViewById<Button>(R.id.route_allow_button).setOnClickListener {
            val result = Intent()
            result.putExtra(EXTRA_SESSION_ID, intent.getStringExtra(EXTRA_SESSION_ID))
            result.putExtra(EXTRA_EXERCISE_ROUTE, WARSAW_ROUTE)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
        dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.show()
    }
}
