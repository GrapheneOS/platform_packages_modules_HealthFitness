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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;

/**
 * Listener that receives SensorManager pedometer events.
 *
 * @hide
 */
class StepSensorEventListener implements SensorEventListener {

    private final HealthConnectThreadScheduler mThreadScheduler;
    private final DeviceRecordHelper mDeviceRecordHelper;

    // TODO(b/413650602): Check if we ever want to cache the current device.
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    StepSensorEventListener(
            HealthConnectThreadScheduler threadScheduler,
            DeviceRecordHelper deviceRecordHelper,
            DeviceDataSourcesHelper deviceDataSourcesHelper) {
        this.mThreadScheduler = threadScheduler;
        this.mDeviceRecordHelper = deviceRecordHelper;
        this.mDeviceDataSourcesHelper = deviceDataSourcesHelper;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Implementation goes here. Do nothing for now.
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy ignored.
    }
}
