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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.Nullable;
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
    @VisibleForTesting static final double MIN_STEPS_PER_MINUTE = 30;

    private final Context mContext;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final DeviceRecordHelper mDeviceRecordHelper;

    // TODO(b/413650602): Check if we ever want to cache the current device.
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    /** Class to hold a cumulative step data point and associated timestamp since boot time. */
    @VisibleForTesting
    record SensorData(int sensorValue, long sensorTimestampNanos) {}

    // Sensor manager step count resets on device boot, which is also when the Health Connect
    // process starts.
    @VisibleForTesting @Nullable SensorData mLastSavedData = null;

    @VisibleForTesting @Nullable SensorData mPendingData = null;

    @VisibleForTesting Optional<ScheduledFuture<?>> mPendingBatchWriteFuture = Optional.empty();

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

    /**
     * Clears internal state, pending data and cancels any scheduled tasks.
     *
     * <p>This should only be called after {@code SensorManager#unregisterListener}.
     */
    public void reset() {
        mLastSavedData = null;
        mPendingData = null;
        if (mPendingBatchWriteFuture.isPresent()) {
            mPendingBatchWriteFuture.get().cancel(/* mayInterruptIfRunning= */ false);
            mPendingBatchWriteFuture = Optional.empty();
        }
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
                    if (mLastSavedData == null) {
                        // Set baseline cumulative step count after initial tracking start or after
                        // a #reset due to a user switch or unsubscribe.
                        if (android.health.connect.Constants.DEBUG) {
                            Slog.d(TAG, "First event since tracking enabled, setting baseline.");
                        }

                        mLastSavedData =
                                new SensorData(sensorValueCumulative, sensorEventTimestampNanos);

                        return;
                    }

                    mPendingData = new SensorData(sensorValueCumulative, sensorEventTimestampNanos);

                    if (mPendingBatchWriteFuture.isEmpty()
                            || mPendingBatchWriteFuture.get().isDone()) {
                        // This will write in the first received event immediately and then future
                        // events every 60 seconds (BATCHING_DURATION_NANOS) until there are no
                        // steps for 60 seconds
                        writeBatchAndScheduleNextWrite(/* isDelayedTask= */ false);
                    }
                });
    }

    private boolean isOldOrInvalidValue(int sensorValueCumulative, long sensorEventTimestampNanos) {
        if (sensorValueCumulative < 0 || sensorEventTimestampNanos < 0) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as value or timestamp is negative");
            }
            return true;
        }

        if (mPendingData != null && sensorValueCumulative <= mPendingData.sensorValue) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as value is same or lower than pending value");
            }
            return true;
        }

        if (mPendingData != null
                && sensorEventTimestampNanos <= mPendingData.sensorTimestampNanos) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(
                        TAG,
                        "Ignoring event as sensor timestamp is same or lower than pending value");
            }
            return true;
        }

        if (mLastSavedData != null && sensorValueCumulative <= mLastSavedData.sensorValue) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as value is same or lower than saved value");
            }
            return true;
        }

        if (mLastSavedData != null
                && sensorEventTimestampNanos <= mLastSavedData.sensorTimestampNanos) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring event as sensor timestamp is same or lower than saved value");
            }
            return true;
        }

        return false;
    }

    private void writeBatchAndScheduleNextWrite(boolean isDelayedTask) {
        if (mLastSavedData == null || mPendingData == null) {
            Slog.w(TAG, "Last saved or pending data is null, aborting");
            return;
        }

        int stepDelta =
                mPendingData.sensorValue
                        - mLastSavedData
                                .sensorValue; // Convert from cumulative steps to a step delta

        if (stepDelta == 0) {
            mPendingBatchWriteFuture = Optional.empty();
            // Don't execute or schedule another write as there have not been any changes
            return;
        }

        Instant bootTime = computeBootTime();
        Instant realStartTimestamp =
                calculateRealEventTimestamp(
                        bootTime,
                        estimateStartTime(
                                stepDelta,
                                mLastSavedData.sensorTimestampNanos,
                                mPendingData.sensorTimestampNanos,
                                isDelayedTask));
        Instant realEndTimestamp =
                calculateRealEventTimestamp(bootTime, mPendingData.sensorTimestampNanos);
        writeSteps(getStepsRecordInternal(stepDelta, realStartTimestamp, realEndTimestamp));
        mLastSavedData = mPendingData;

        mPendingBatchWriteFuture =
                mThreadScheduler.schedulePassiveTrackerTask(
                        () -> writeBatchAndScheduleNextWrite(/* isDelayedTask= */ true),
                        getBatchingDurationMillis());
        if (mPendingBatchWriteFuture.isEmpty()) {
            Slog.e(TAG, "Failed to schedule a write");
        }
    }

    /**
     * Estimates start time in nanos since device boot.
     *
     * <p>The earliest possible and default start time is the end timestamp of the last saved data
     * point. If a low step cadence is detected and write was not triggered from a delayed task, we
     * extrapolate the start timestamp from a minimum cadence. If the write was triggered from a
     * delayed task, the extrapolated start timestamp will be the end of the last event timestamp.
     *
     * <p>We only receive steps when AP is awake. If the device is asleep and the user takes steps,
     * we won't know the start timestamp of the steps event. If the step cadence is low (a small
     * number of steps over a large duration), we can trim and estimate the start timestamp by
     * applying a {@link MIN_STEPS_PER_MINUTE}.
     */
    // TODO(b/418989797): Use the last device wake time as a potential start timestamp.
    @VisibleForTesting
    static long estimateStartTime(
            int stepDelta,
            long endOfLastDataPointNanos,
            long endOfCurrentDataPointNanos,
            boolean isDelayedTask) {
        if (isDelayedTask) {
            // As these are continuous steps, the start time should always be the end of the last
            // event
            return endOfLastDataPointNanos;
        }

        long startTimestampNanos = endOfLastDataPointNanos;

        // Check if the interval is too large, and trim it down to avoid large deltas with few steps
        float cadence = getCadence(stepDelta, startTimestampNanos, endOfCurrentDataPointNanos);
        if (cadence < MIN_STEPS_PER_MINUTE) {
            double estimatedDurationMinutes = stepDelta / MIN_STEPS_PER_MINUTE;
            long estimatedDurationNanos = (long) (estimatedDurationMinutes * MINUTES.toNanos(1));

            // Ensure the trimmed start time is not earlier than the end of the last data point
            startTimestampNanos =
                    Math.max(
                            endOfCurrentDataPointNanos - estimatedDurationNanos,
                            startTimestampNanos);
        }

        return startTimestampNanos;
    }

    /**
     * Returns the cadence in steps/minute.
     *
     * @return the cadence as a float, or a negative value if start is not before end.
     */
    private static float getCadence(int delta, long startNanos, long endNanos) {
        long duration = endNanos - startNanos;
        if (duration <= 0) {
            Slog.e(TAG, "Invalid start and end timestamps");
            return -1;
        }

        float durationMinutes = (float) duration / MINUTES.toNanos(1);
        return delta / durationMinutes;
    }

    @VisibleForTesting
    long getBatchingDurationMillis() {
        return BATCHING_DURATION_MILLIS;
    }

    private StepsRecordInternal getStepsRecordInternal(
            float stepCount, Instant eventStartTimestamp, Instant eventEndTimestamp) {
        StepsRecordInternal record = new StepsRecordInternal();
        record.setCount((int) stepCount);
        record.setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);

        record.setStartTime(eventStartTimestamp.toEpochMilli());
        record.setEndTime(eventEndTimestamp.toEpochMilli());
        record.setStartZoneOffset(
                ZoneOffset.systemDefault()
                        .getRules()
                        .getOffset(eventStartTimestamp)
                        .getTotalSeconds());
        record.setEndZoneOffset(
                ZoneOffset.systemDefault()
                        .getRules()
                        .getOffset(eventEndTimestamp)
                        .getTotalSeconds());
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

    @VisibleForTesting
    Instant computeBootTime() {
        return Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime());
    }

    private Instant calculateRealEventTimestamp(
            Instant bootTime, long eventTimestampNanosSinceBoot) {
        // TODO(b/397400522): Add validation to ensure this is a valid timestamp.
        return bootTime.plusNanos(eventTimestampNanosSinceBoot);
    }
}
