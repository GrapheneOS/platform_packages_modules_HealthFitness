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
package com.android.server.healthconnect.device.tracker;

import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.os.SystemClock;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * Listener that receives SensorManager pedometer events.
 *
 * @hide
 */
class StepSensorEventListener implements SensorEventListener {

    private static final String TAG = "HealthConnectStepSensorEventListener";
    private static final long BATCHING_DURATION_MILLIS = SECONDS.toMillis(60);

    @VisibleForTesting static final long BOOT_TIME_NANOS = computeBootTimeNanos();

    private final Context mContext;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final DeviceRecordHelper mDeviceRecordHelper;

    // TODO(b/413650602): Check if we ever want to cache the current device.
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    /** Class to hold a cumulative step data point and associated timestamp since boot time. */
    private record SensorData(int sensorValue, long sensorTimestampNanos) {}

    // Sensor manager step count resets on device boot, which is also when the Health Connect
    // process starts.
    // TODO(b/397400522): Check if we need to handle user switching.
    private SensorData mLastSavedData =
            new SensorData(/* sensorValue= */ 0, /* sensorTimestampNanos= */ 0);
    private SensorData mPendingData =
            new SensorData(/* sensorValue= */ 0, /* sensorTimestampNanos= */ 0);
    private Optional<ScheduledFuture<?>> mPendingBatchWriteFuture = Optional.empty();

    StepSensorEventListener(
            Context context,
            HealthConnectThreadScheduler threadScheduler,
            DeviceRecordHelper deviceRecordHelper,
            DeviceDataSourcesHelper deviceDataSourcesHelper) {
        this.mContext = context;
        this.mThreadScheduler = threadScheduler;
        this.mDeviceRecordHelper = deviceRecordHelper;
        this.mDeviceDataSourcesHelper = deviceDataSourcesHelper;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            processSensorEvent(event);
        } catch (Exception e) {
            Slog.e(TAG, "Error processing sensor event: " + e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy ignored.
    }

    private void processSensorEvent(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            Slog.e(TAG, "Not expecting sensor type: " + event.sensor.getName());
            return;
        }

        int sensorValueCumulative = (int) event.values[0]; // Cumulative step count since boot.
        long sensorEventTimestampNanos = event.timestamp;

        // Schedule that runs immediately
        mThreadScheduler.schedulePassiveTrackerTask(
                () -> {
                    if (isOldOrInvalidValue(sensorValueCumulative, sensorEventTimestampNanos)) {
                        return;
                    }

                    mPendingData = new SensorData(sensorValueCumulative, sensorEventTimestampNanos);

                    if (mPendingBatchWriteFuture.isEmpty()
                            || mPendingBatchWriteFuture.get().isDone()) {
                        // This will write in the first received event immediately and then future
                        // events every 60 seconds (BATCHING_DURATION_NANOS) until there are no
                        // steps for 60 seconds
                        writeBatchAndScheduleNextWrite();
                    }
                });
    }

    private boolean isOldOrInvalidValue(int sensorValueCumulative, long sensorEventTimestampNanos) {
        if (sensorValueCumulative <= mPendingData.sensorValue) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as value is same or lower than pending value");
            }
            return true;
        }

        if (sensorEventTimestampNanos <= mPendingData.sensorTimestampNanos) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(
                        TAG,
                        "Ignoring event as sensor timestamp is same or lower than pending value");
            }
            return true;
        }

        if (sensorValueCumulative <= mLastSavedData.sensorValue) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as value is same or lower than saved value");
            }
            return true;
        }

        if (sensorEventTimestampNanos <= mLastSavedData.sensorTimestampNanos) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as sensor timestamp is same or lower than saved value");
            }
            return true;
        }

        return false;
    }

    private void writeBatchAndScheduleNextWrite() {
        int stepDelta =
                mPendingData.sensorValue
                        - mLastSavedData
                                .sensorValue; // Convert from cumulative steps to a step delta

        if (stepDelta == 0) {
            mPendingBatchWriteFuture = Optional.empty();
            // Don't execute or schedule another write as there have not been any changes
            return;
        }

        long realEventTimestampNanos =
                calculateRealEventTimestampNanos(mPendingData.sensorTimestampNanos);
        writeSteps(getStepsRecordInternal(stepDelta, realEventTimestampNanos));
        mLastSavedData = mPendingData;

        mPendingBatchWriteFuture =
                mThreadScheduler.schedulePassiveTrackerTask(
                        this::writeBatchAndScheduleNextWrite, getBatchingDurationMillis());
        if (mPendingBatchWriteFuture.isEmpty()) {
            Slog.e(TAG, "Failed to schedule a write");
        }
    }

    @VisibleForTesting
    long getBatchingDurationMillis() {
        return BATCHING_DURATION_MILLIS;
    }

    private StepsRecordInternal getStepsRecordInternal(float stepCount, long eventEndTimeNanos) {
        StepsRecordInternal record = new StepsRecordInternal();
        record.setCount((int) stepCount);
        record.setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);

        Instant endTime = Instant.ofEpochSecond(0L, eventEndTimeNanos);
        // TODO(b/397400522): Extrapolate start time if there is a large step delta.
        // startTime should never equal endTime as this isn't supported in Jetpack records.
        Instant startTime = endTime.minusMillis(1);
        record.setStartTime(startTime.toEpochMilli());
        record.setEndTime(endTime.toEpochMilli());
        record.setStartZoneOffset(
                ZoneOffset.systemDefault().getRules().getOffset(startTime).getTotalSeconds());
        record.setEndZoneOffset(
                ZoneOffset.systemDefault().getRules().getOffset(endTime).getTotalSeconds());
        record.setLastModifiedTime(System.currentTimeMillis());

        return record;
    }

    private void writeSteps(StepsRecordInternal stepsRecordInternal) {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Writing steps");
        }

        // Records are written into the DB on the internal background executor in
        // FitnessRecordUpsertHelper#insertRecords.
        mDeviceRecordHelper.insertRecords(
                mDeviceDataSourcesHelper.getCurrentDevice(mContext), List.of(stepsRecordInternal));
    }

    private static long calculateRealEventTimestampNanos(long eventTimestampNanos) {
        long realEventTimestamp = eventTimestampNanos + BOOT_TIME_NANOS;
        // TODO(b/397400522): Add validation to ensure this is a valid timestamp.
        return realEventTimestamp;
    }

    private static long computeBootTimeNanos() {
        return MILLISECONDS.toNanos(System.currentTimeMillis() - SystemClock.elapsedRealtime());
    }
}
