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

package com.android.healthconnect.controller.recentaccess

import com.android.healthconnect.controller.R
import com.google.common.collect.ImmutableSet
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadRecentAccessUseCase @Inject constructor() {
    /** Returns a list of apps that have recently accessed Health Connect */
    // TODO this will call the getAccessLogs API and return a list of
    // RecentAccessApps sorted in descending order by their instantTime field
    suspend operator fun invoke(): List<RecentAccessApp> {
        val recentApps = arrayListOf<RecentAccessApp>()

        // currently we just generate a list of predefined RecentAccessApps
        for (i in 19 downTo 10) {
            val instant = Instant.parse("2022-10-20T${i}:12:13.00Z")
            recentApps.add(
                RecentAccessApp(
                    R.string.recent_app_1, R.drawable.ic_vitals, instant, ImmutableSet.of("Read")))
        }

        return recentApps
    }
}
