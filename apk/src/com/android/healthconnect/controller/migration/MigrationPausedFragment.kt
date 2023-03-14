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
package com.android.healthconnect.controller.migration

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(Fragment::class)
class MigrationPausedFragment : Hilt_MigrationPausedFragment() {

    companion object {
        private const val TAG = "MigrationPausedFragment"
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.migration_paused, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resumeButton = view.findViewById<Button>(R.id.resume_button)
        resumeButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_migrationPausedFragment_to_migrationApk)
            } catch (exception: Exception) {
                Log.e(TAG, "Migration APK does not exist", exception)
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
