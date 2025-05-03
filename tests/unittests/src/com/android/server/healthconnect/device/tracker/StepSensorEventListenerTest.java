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

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Unit tests for {@link StepSensorEventListener} */
@RunWith(AndroidJUnit4.class)
public class StepSensorEventListenerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private HealthConnectThreadScheduler mThreadScheduler;
    private DeviceRecordHelper mDeviceRecordHelper;
    private DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        mDeviceRecordHelper = healthConnectInjector.getDeviceRecordHelper();
        mDeviceDataSourcesHelper = healthConnectInjector.getDeviceDataSourcesHelper();
    }

    @Test
    public void onSensorChanged_doesNotThrow() throws Exception {
        StepSensorEventListener eventListener =
                new StepSensorEventListener(
                        mThreadScheduler, mDeviceRecordHelper, mDeviceDataSourcesHelper);
        eventListener.onSensorChanged(
                createStepSensorEvent(/* value= */ 1, /* timestamp= */ 1234567890));
    }

    @Test
    public void onAccuracyChanged_doesNotThrow() throws Exception {
        StepSensorEventListener eventListener =
                new StepSensorEventListener(
                        mThreadScheduler, mDeviceRecordHelper, mDeviceDataSourcesHelper);
        eventListener.onAccuracyChanged(createSensor(), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    private static SensorEvent createStepSensorEvent(int value, long timestamp) throws Exception {
        final Constructor<SensorEvent> constructor =
                SensorEvent.class.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        final SensorEvent event = constructor.newInstance(1);
        event.sensor = createSensor();
        event.values[0] = value;
        event.timestamp = timestamp;
        return event;
    }

    private static Sensor createSensor() throws Exception {
        Constructor<Sensor> constr = Sensor.class.getDeclaredConstructor();
        constr.setAccessible(true);
        Sensor sensor = constr.newInstance();
        setSensorType(sensor, Sensor.TYPE_STEP_COUNTER, "Step sensor");
        return sensor;
    }

    private static void setSensorType(Sensor sensor, int type, String strType) throws Exception {
        Method setter = Sensor.class.getDeclaredMethod("setType", Integer.TYPE);
        setter.setAccessible(true);
        setter.invoke(sensor, type);
        if (strType != null) {
            Field f = sensor.getClass().getDeclaredField("mStringType");
            f.setAccessible(true);
            f.set(sensor, strType);
        }
    }
}
