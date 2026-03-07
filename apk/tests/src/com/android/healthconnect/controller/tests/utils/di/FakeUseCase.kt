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

package com.android.healthconnect.controller.tests.utils.di

import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class FakeUseCase<Input, Output>(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) : BaseUseCase<Input, Output>(dispatcher) {

    var numberOfInvocations = 0
        protected set

    private var shouldForceFail = false
    private var failureException: Throwable = DEFAULT_USE_CASE_EXCEPTION

    protected abstract suspend fun successValue(input: Input): Output

    final override suspend fun execute(input: Input): Output {
        numberOfInvocations += 1
        if (shouldForceFail) {
            throw failureException
        } else {
            return successValue(input)
        }
    }

    open fun reset() {
        numberOfInvocations = 0
        shouldForceFail = false
        failureException = DEFAULT_USE_CASE_EXCEPTION
    }

    fun setForceFail(forceFail: Boolean, exception: Throwable = DEFAULT_USE_CASE_EXCEPTION) {
        this.shouldForceFail = forceFail
        if (forceFail) {
            this.failureException = exception
        }
    }
}

const val DEFAULT_USE_CASE_EXCEPTION_MESSAGE = "Forced failure in FakeUseCase"
val DEFAULT_USE_CASE_EXCEPTION = IllegalStateException(DEFAULT_USE_CASE_EXCEPTION_MESSAGE)
