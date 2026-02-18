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

package com.android.healthconnect.controller.tests.autodelete.api

import com.android.healthconnect.controller.autodelete.api.ILoadAutoDeleteUseCase
import com.android.healthconnect.controller.autodelete.api.IUpdateAutoDeleteUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeLoadAutoDeleteUseCase :
    FakeUseCase<Unit, Int>(dispatcher = Dispatchers.Unconfined), ILoadAutoDeleteUseCase {
    private var autoDeleteRange = 0

    fun setAutoDeleteRange(range: Int) {
        autoDeleteRange = range
    }

    override suspend fun successValue(input: Unit): Int {
        return autoDeleteRange
    }

    override fun reset() {
        super.reset()
        autoDeleteRange = 0
    }
}

class FakeUpdateAutoDeleteUseCase :
    FakeUseCase<Int, Unit>(dispatcher = Dispatchers.Unconfined), IUpdateAutoDeleteUseCase {
    override suspend fun successValue(input: Int) {
        return
    }
}
