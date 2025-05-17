/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.testing.cts;

import android.app.UiAutomation;
import android.health.connect.HealthConnectException;

/**
 * A specialization of {@link TestOutcomeReceiver} for the case when {@link HealthConnectException}
 * may be thrown.
 *
 * @param <T> the type of object being received,
 */
public final class HealthConnectReceiver<T> extends TestOutcomeReceiver<T, HealthConnectException> {
    public HealthConnectReceiver() {}

    /** Use this constructor to set the caller of this receiver. This is for debugging purposes. */
    public HealthConnectReceiver(String caller) {
        this.mCaller = caller;
    }

    /**
     * Helper for calling an API method that returns its response via an {@link
     * android.os.OutcomeReceiver}.
     */
    public static <R> R callAndGetResponse(CallableForOutcome<R, HealthConnectException> callable)
            throws InterruptedException {
        return callAndGetResponse(HealthConnectException.class, callable);
    }

    /**
     * Helper for calling an API method that returns its response via an {@link
     * android.os.OutcomeReceiver} while holding permissions via {@link
     * UiAutomation#adoptShellPermissionIdentity(String...)}.
     */
    public static <R> R callAndGetResponseWithShellPermissionIdentity(
            CallableForOutcome<R, HealthConnectException> callable, String... permissions)
            throws InterruptedException {
        return callAndGetResponseWithShellPermissionIdentity(
                HealthConnectException.class, callable, permissions);
    }
}
