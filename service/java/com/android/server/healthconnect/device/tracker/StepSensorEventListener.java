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

/**
 * Listener that receives SensorManager pedometer events.
 *
 * @hide
 */
class StepSensorEventListener implements SensorEventListener {

    private static final String TAG = "HealthConnectStepSensorEventListener";
    @VisibleForTesting static final long BOOT_TIME_NANOS = computeBootTimeNanos();

    private final Context mContext;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final DeviceRecordHelper mDeviceRecordHelper;

    // TODO(b/413650602): Check if we ever want to cache the current device.
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    // Sensor manager step count resets on device boot, which is also when the Health Connect
    // process starts.
    // TODO(b/397400522): Check if we need to handle user switching.
    private int mLastSensorValue = 0;

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
            // TODO(b/397400522): Add support for buffering instead of converting and writing every
            // individual data point.
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

        int sensorValue = (int) event.values[0]; // Cumulative step count since boot.
        int stepDelta =
                sensorValue - mLastSensorValue; // Convert from cumulative steps to a step delta.
        long eventEndTimeNanos = calculateRealEventTimestampNanos(event.timestamp);

        if (sensorValue <= mLastSensorValue) {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Ignoring same or lower sensor value");
            }
            return;
        }

        // Update field to keep track of step deltas.
        mLastSensorValue = sensorValue;

        executeOrScheduleWrite(stepDelta, eventEndTimeNanos);
    }

    private void executeOrScheduleWrite(float stepDelta, long eventEndTimeNanos) {
        // TODO(b/397400522): Implement scheduled writes.
        // Gets us off the main thread ASAP.
        mThreadScheduler.schedulePassiveTrackerTask(
                () -> writeSteps(getStepsRecordInternal(stepDelta, eventEndTimeNanos)));
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
