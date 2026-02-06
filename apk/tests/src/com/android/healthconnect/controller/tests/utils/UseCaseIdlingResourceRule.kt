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

package com.android.healthconnect.controller.tests.utils

import androidx.test.espresso.IdlingRegistry
import com.android.healthconnect.controller.shared.usecase.UseCaseIdlingResource
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A [org.junit.rules.TestRule] that registers and unregisters the [UseCaseIdlingResource] for each
 * test.
 */
class UseCaseIdlingResourceRule : TestWatcher() {
    override fun starting(description: Description?) {
        IdlingRegistry.getInstance().register(UseCaseIdlingResource.countingIdlingResource)
    }

    override fun finished(description: Description?) {
        IdlingRegistry.getInstance().unregister(UseCaseIdlingResource.countingIdlingResource)
    }
}
