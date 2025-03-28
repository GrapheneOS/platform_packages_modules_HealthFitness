/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.healthconnect.testapps.toolbox.ui

import android.health.connect.HealthConnectManager
import android.health.connect.backuprestore.GetChangesForBackupResponse
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.showMessageDialog
import com.android.healthconnect.testapps.toolbox.viewmodels.BackupRestoreFragmentViewModel
import kotlin.getValue

class BackupRestoreFragment : Fragment(R.layout.fragment_backup_restore) {

    private val backupRestoreFragmentViewModel: BackupRestoreFragmentViewModel by viewModels()
    val manager by lazy { requireContext().getSystemService(HealthConnectManager::class.java) }
    private var response: GetChangesForBackupResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.read_backup_data).setOnClickListener {
            backupRestoreFragmentViewModel.storeBackupResponse(manager, ::showMessage)
        }
        view.findViewById<Button>(R.id.write_restore_data).setOnClickListener {
            backupRestoreFragmentViewModel.restore(manager, ::showMessage)
        }
    }

    private fun showMessage(message: String) {
        requireContext().showMessageDialog(message)
    }

    companion object {
        const val TAG = "BackupRestoreFragment"
    }
}
