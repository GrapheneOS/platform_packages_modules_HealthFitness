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
package com.android.server.healthconnect.telemetry;

import static android.health.connect.Constants.DEFAULT_INT;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.UserHandle;
import android.util.Slog;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsLogger;

/**
 * JobService for Health Connect telemetry around data quality.
 *
 * @hide
 */
public final class TelemetryJobService extends JobService {
    private static final String TAG = "DataQualityTelemetryJobService";
    @Nullable private static volatile UserHandle sUserHandle;
    public static final String EXTRA_USER_ID = "user_id";

    public static void setCurrentUser(UserHandle userHandle) {
        sUserHandle = userHandle;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        int userId = params.getExtras().getInt(EXTRA_USER_ID, /* defaultValue= */ DEFAULT_INT);
        if (userId == DEFAULT_INT || sUserHandle == null) {
            Slog.w(TAG, "onStartJob called for unknown user");
            return false;
        }
        if (userId != requireNonNull(sUserHandle).getIdentifier()) {
            Slog.w(TAG, "onStartJob called for a non-active user " + userId);
            return false;
        }

        HealthConnectInjector healthConnectInjector = HealthConnectInjector.getInstance();
        HealthConnectThreadScheduler threadScheduler = healthConnectInjector.getThreadScheduler();
        LatencyMetricsLogger latencyMetricsLogger = healthConnectInjector.getLatencyMetricsLogger();

        threadScheduler.scheduleInternalTask(
                () -> {
                    DataQualityTelemetryJobScheduler.execute(latencyMetricsLogger);
                    jobFinished(params, false);
                });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
