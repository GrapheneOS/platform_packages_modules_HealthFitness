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

package com.android.healthconnect.controller.tests.route.api

import android.health.connect.datatypes.ExerciseSessionRecord
import com.android.healthconnect.controller.route.api.ILoadExerciseRouteUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeLoadExerciseRouteUseCase :
    FakeUseCase<String, ExerciseSessionRecord?>(dispatcher = Dispatchers.Unconfined),
    ILoadExerciseRouteUseCase {
    private var exerciseSessionRecord: ExerciseSessionRecord? = null

    fun updateExerciseSession(record: ExerciseSessionRecord?) {
        exerciseSessionRecord = record
    }

    override suspend fun successValue(input: String): ExerciseSessionRecord? {
        return exerciseSessionRecord
    }

    override fun reset() {
        exerciseSessionRecord = null
    }
}
