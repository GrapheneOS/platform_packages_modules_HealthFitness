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

import static com.android.server.healthconnect.device.tracker.StepSensorEventListener.BOOT_TIME_NANOS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.testing.unittest.TransactionTestUtils;
import android.os.Build;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link StepSensorEventListener} */
@RunWith(AndroidJUnit4.class)
public class StepSensorEventListenerTest {

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(Build.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private static final String TEST_PACKAGE_NAME = "package.name";

    private HealthConnectThreadScheduler mThreadScheduler;
    private DeviceRecordHelper mDeviceRecordHelper;
    private DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private TransactionTestUtils mTransactionTestUtils;
    private Context mContext;
    private StepSensorEventListener mStepSensorEventListener;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        mDeviceRecordHelper = healthConnectInjector.getDeviceRecordHelper();
        mDeviceDataSourcesHelper = healthConnectInjector.getDeviceDataSourcesHelper();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);

        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        mStepSensorEventListener =
                new StepSensorEventListener(
                        mContext, mThreadScheduler, mDeviceRecordHelper, mDeviceDataSourcesHelper);
    }

    @Test
    public void onSensorChanged_doesNotThrow() throws Exception {
        mStepSensorEventListener.onSensorChanged(
                createStepSensorEvent(/* value= */ 1, /* timestamp= */ 1234567890));
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void onSensorChanged_writesSteps() throws Exception {
        int stepCount = 10;
        long timestampNanos = 1234567890;

        triggerStepEvent(stepCount, timestampNanos);
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), stepCount, timestampNanos);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void onSensorChangedTwice_writesStepDeltas() throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount + 5;
        long secondTimestampNanos = firstTimestampNanos + 10_000_000_000L;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(2);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
        // We convert the cumulative step count to deltas and write in the deltas.
        assertRecord(records.get(1), secondStepCount - firstStepCount, secondTimestampNanos);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void onSensorChangedTwice_bothStepCountsAreDuplicate_onlyWritesStepsOnce()
            throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        long secondTimestampNanos = firstTimestampNanos + 10_000_000_000L;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        triggerStepEvent(firstStepCount, secondTimestampNanos);
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void onSensorChangedTwice_secondStepCountIsLower_ignoresSecondEvent() throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount - 5;
        long secondTimestampNanos = firstTimestampNanos + 10_000_000_000L;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    public void onAccuracyChanged_doesNotThrow() throws Exception {
        mStepSensorEventListener.onAccuracyChanged(
                createSensor(), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    private void triggerStepEvent(int stepCount, long timestampNanos) throws Exception {
        mStepSensorEventListener.onSensorChanged(
                createStepSensorEvent(/* value= */ stepCount, /* timestamp= */ timestampNanos));
        awaitAllExecutorsIdle();
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

    /**
     * Waits until all executors are idle. For now this just waits for a fixed duration with {@link
     * Thread#sleep(long)}.
     */
    private static void awaitAllExecutorsIdle() throws InterruptedException {
        Thread.sleep(500);
    }

    private static void assertRecord(RecordInternal<?> record, int stepCount, long timestampNanos) {
        StepsRecordInternal stepsRecord = (StepsRecordInternal) record;
        // Start timestamp is always 1ms before the end timestamp.
        assertThat(stepsRecord.getStartTimeInMillis())
                .isEqualTo(getCurrentTimestampOfEvent(timestampNanos) - 1);
        assertThat(stepsRecord.getEndTimeInMillis())
                .isEqualTo(getCurrentTimestampOfEvent(timestampNanos));
        assertThat(stepsRecord.getCount()).isEqualTo(stepCount);
    }

    private static long getCurrentTimestampOfEvent(long timestampSinceBootNanos) {
        return TimeUnit.NANOSECONDS.toMillis(BOOT_TIME_NANOS + timestampSinceBootNanos);
    }
}
