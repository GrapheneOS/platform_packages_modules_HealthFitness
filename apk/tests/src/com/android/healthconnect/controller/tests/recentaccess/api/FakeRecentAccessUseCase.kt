/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.recentaccess.api

import android.health.connect.accesslog.AccessLog
import com.android.healthconnect.controller.recentaccess.api.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeRecentAccessUseCase :
    FakeUseCase<Unit, List<AccessLog>>(dispatcher = Dispatchers.Unconfined),
    ILoadRecentAccessUseCase {
    private var list: List<AccessLog> = emptyList()

    fun updateList(list: List<AccessLog>) {
        this.list = list
    }

    fun addToList(newLogs: List<AccessLog>) {
        this.list = list + newLogs
    }

    override suspend fun successValue(input: Unit): List<AccessLog> {
        return list
    }

    override fun reset() {
        this.list = emptyList()
    }
}
