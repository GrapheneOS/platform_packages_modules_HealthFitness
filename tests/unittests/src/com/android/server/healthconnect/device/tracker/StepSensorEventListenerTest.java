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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.testing.unittest.TransactionTestUtils;
import android.healthconnect.testing.unittest.mocks.AndroidPackageMocker;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.device.FakeSerialDeviceDataSourcesHelper;
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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link StepSensorEventListener} */
@RunWith(AndroidJUnit4.class)
public class StepSensorEventListenerTest {

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private static final String TEST_PACKAGE_NAME = "package.name";

    private HealthConnectThreadScheduler mThreadScheduler;
    private TransactionTestUtils mTransactionTestUtils;
    private StepSensorEventListener mStepSensorEventListener;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        Context mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        AndroidPackageMocker.addToContext(mContext);
        DeviceDataSourcesHelper deviceDataSourcesHelper = new FakeSerialDeviceDataSourcesHelper();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .setDeviceDataSourcesHelper(deviceDataSourcesHelper)
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        DeviceRecordHelper mDeviceRecordHelper = healthConnectInjector.getDeviceRecordHelper();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);

        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        mStepSensorEventListener =
                spy(
                        new StepSensorEventListener(
                                mContext,
                                mThreadScheduler,
                                mDeviceRecordHelper,
                                deviceDataSourcesHelper));

        // Reduce the batching delay to speed up the tests
        when(mStepSensorEventListener.getBatchingDurationMillis()).thenReturn(500L);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChanged_doesNotThrow() throws Exception {
        mStepSensorEventListener.onSensorChanged(
                createStepSensorEvent(/* value= */ 1, /* timestamp= */ 1234567890));
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChanged_writesSteps() throws Exception {
        int stepCount = 10;
        long timestampNanos = 1234567890;

        triggerStepEvent(stepCount, timestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), stepCount, timestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_writesTwice() throws Exception {
        // The first event is always written instantly and the second is written through the
        // scheduled future
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondTimestampNanos = firstTimestampNanos + secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(2);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
        // We convert the cumulative step count to deltas and write in the deltas.
        assertRecord(records.get(1), secondStepCount - firstStepCount, secondTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedThrice_writesTwice() throws Exception {
        // The first event is always written instantly and the second and third events are merged
        // and written through the scheduled future
        int firstStepCount = 15;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount + 10;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondTimestampNanos = firstTimestampNanos + secondTimestampDelayNanos;
        int thirdStepCount = secondStepCount + 5;
        long thirdTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long thirdTimestampNanos = secondTimestampNanos + thirdTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        sleep(thirdTimestampDelayNanos);
        triggerStepEvent(thirdStepCount, thirdTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(2);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
        assertRecord(records.get(1), thirdStepCount - firstStepCount, thirdTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_bothStepCountsAreDuplicate_ignoresSecondEvent()
            throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondTimestampNanos = firstTimestampNanos + secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(firstStepCount, secondTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_secondStepCountIsLower_ignoresSecondEvent() throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount - 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondTimestampNanos = firstTimestampNanos + secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_bothTimestampsAreDuplicate_ignoresSecondEvent()
            throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, firstTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_secondTimestampsIsLower_ignoresSecondEvent() throws Exception {
        int firstStepCount = 10;
        long firstTimestampNanos = 1234567890;
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondTimestampNanos = firstTimestampNanos - secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mTransactionTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), firstStepCount, firstTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onAccuracyChanged_doesNotThrow() throws Exception {
        mStepSensorEventListener.onAccuracyChanged(
                createSensor(), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    private void triggerStepEvent(int stepCount, long timestampNanos) throws Exception {
        mStepSensorEventListener.onSensorChanged(
                createStepSensorEvent(/* value= */ stepCount, /* timestamp= */ timestampNanos));
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
     * Waits until the passive tracking executor is idle and shut down.
     *
     * <p>This should only be called once in a test, after all step events have been transmitted.
     */
    // TODO(b/417975987): Consider improving this method with a CountDownLatch.
    private void awaitPassiveSensorTasksComplete() throws InterruptedException {
        // Wait for twice as long as the batching duration because, if an event occurs during a
        // scheduled future, we need to wait for that one to finish and the no-op one that will
        // occur afterwards
        Thread.sleep(mStepSensorEventListener.getBatchingDurationMillis() * 2);

        ScheduledThreadPoolExecutor passiveExecutor = mThreadScheduler.mPassiveTrackerExecutor;
        // Beware that no new tasks are executed once #shutdown is called so any tasks after this
        // and before #resetThreadPools may be lost.
        passiveExecutor.shutdown();
        boolean successful = passiveExecutor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(successful).isTrue();
    }

    private void sleep(long nanos) throws InterruptedException {
        Thread.sleep(NANOSECONDS.toMillis(nanos));
    }

    private static void assertRecord(RecordInternal<?> record, int stepCount, long timestampNanos) {
        StepsRecordInternal stepsRecord = (StepsRecordInternal) record;
        assertThat(stepsRecord.getCount()).isEqualTo(stepCount);
        // Start timestamp is always 1ms before the end timestamp.
        assertThat(stepsRecord.getStartTimeInMillis())
                .isEqualTo(getTimestampAfterBoot(timestampNanos) - 1);
        assertThat(stepsRecord.getEndTimeInMillis())
                .isEqualTo(getTimestampAfterBoot(timestampNanos));
    }

    private static long getTimestampAfterBoot(long timestampSinceBootNanos) {
        return NANOSECONDS.toMillis(BOOT_TIME_NANOS + timestampSinceBootNanos);
    }
}
