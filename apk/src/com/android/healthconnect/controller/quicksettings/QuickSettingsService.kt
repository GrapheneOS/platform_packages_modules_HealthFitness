/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.quicksettings

import android.content.Intent
import android.service.quicksettings.TileService
import com.android.healthconnect.controller.MainActivity

/** Service for Quick Settings Tile. Tapping on the icon opens Health Connect. */
class QuickSettingsService : TileService() {

    override fun onClick() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        openAppIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivityAndCollapse(openAppIntent)
    }
}
