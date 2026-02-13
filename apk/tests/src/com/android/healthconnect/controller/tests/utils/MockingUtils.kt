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

import android.os.OutcomeReceiver
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.doAnswer
import org.mockito.stubbing.OngoingStubbing

/**
 * Automatically finds the OutcomeReceiver in the arguments and invokes it with the provided Result.
 */
infix fun <T, R> OngoingStubbing<R>.doReturnResult(result: Result<T>) =
    doAnswer { invocation: InvocationOnMock ->
        @Suppress("UNCHECKED_CAST")
        val receiver =
            invocation.arguments.firstOrNull { it is OutcomeReceiver<*, *> }
                as? OutcomeReceiver<T, Throwable>
                ?: throw IllegalArgumentException(
                    "No OutcomeReceiver found in ${invocation.method.name}"
                )

        result.fold(
            onSuccess = { data -> receiver.onResult(data) },
            onFailure = { error -> receiver.onError(error) },
        )
        null
    }
