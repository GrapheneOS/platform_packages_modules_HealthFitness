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
import static com.android.server.healthconnect.device.tracker.StepSensorEventListener.MIN_STEPS_PER_MINUTE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
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
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
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
    private FitnessTestUtils mFitnessTestUtils;
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
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);

        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
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
    public void onSensorChangedAfterBoot_highCadence_writesStepsSinceBoot() throws Exception {
        int stepCount = 100;
        long endTimestampNanos = MINUTES.toNanos(2);
        long expectedStartTimestampNanos = 0;

        triggerStepEvent(stepCount, endTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), stepCount, expectedStartTimestampNanos, endTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedAfterBoot_lowCadence_writesStepsWithEstimatedStart()
            throws Exception {
        int stepCount = 10;
        long endTimestampNanos = MINUTES.toNanos(10);
        long expectedStartTimestampNanos = endTimestampNanos - getDurationNanosForSteps(stepCount);

        triggerStepEvent(stepCount, endTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), stepCount, expectedStartTimestampNanos, endTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_writesTwice() throws Exception {
        // The first event is always written instantly and the second is written through the
        // scheduled future
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondEndTimestampNanos = firstEndTimestampNanos + secondTimestampDelayNanos;
        long expectedSecondStartTimestampNanos = firstEndTimestampNanos;

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(2);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
        // We convert the cumulative step count to deltas and write in the deltas.
        assertRecord(
                records.get(1),
                secondStepCount - firstStepCount,
                expectedSecondStartTimestampNanos,
                secondEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedThrice_writesTwice() throws Exception {
        // The first event is always written instantly and the second and third events are merged
        // and written through the scheduled future
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        int secondStepCount = firstStepCount + 10;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondEndTimestampNanos = firstEndTimestampNanos + secondTimestampDelayNanos;
        int thirdStepCount = secondStepCount + 5;
        long thirdTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long thirdEndTimestampNanos = secondEndTimestampNanos + thirdTimestampDelayNanos;
        long expectedThirdStartTimestampNanos = firstEndTimestampNanos;

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondEndTimestampNanos);
        sleep(thirdTimestampDelayNanos);
        triggerStepEvent(thirdStepCount, thirdEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(2);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
        assertRecord(
                records.get(1),
                thirdStepCount - firstStepCount,
                expectedThirdStartTimestampNanos,
                thirdEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_bothStepCountsAreDuplicate_ignoresSecondEvent()
            throws Exception {
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondEndTimestampNanos = firstEndTimestampNanos + secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(firstStepCount, secondEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_secondStepCountIsLower_ignoresSecondEvent() throws Exception {
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        int secondStepCount = firstStepCount - 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondEndTimestampNanos = firstEndTimestampNanos + secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_bothTimestampsAreDuplicate_ignoresSecondEvent()
            throws Exception {
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, firstEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedTwice_secondTimestampsIsLower_ignoresSecondEvent() throws Exception {
        int firstStepCount = 10;
        long firstEndTimestampNanos = MINUTES.toNanos(10);
        long expectedFirstStartTimestampNanos =
                firstEndTimestampNanos - getDurationNanosForSteps(firstStepCount);
        int secondStepCount = firstStepCount + 5;
        long secondTimestampDelayNanos = MILLISECONDS.toNanos(100);
        long secondEndTimestampNanos = firstEndTimestampNanos - secondTimestampDelayNanos;

        triggerStepEvent(firstStepCount, firstEndTimestampNanos);
        sleep(secondTimestampDelayNanos);
        triggerStepEvent(secondStepCount, secondEndTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(
                records.get(0),
                firstStepCount,
                expectedFirstStartTimestampNanos,
                firstEndTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChangedSoonAfterBoot_writesStartTimestampAsBootTime() throws Exception {
        int stepCount = 10;
        long endTimestampNanos = SECONDS.toNanos(10);
        long expectedStartTimestampNanos = 0;

        triggerStepEvent(stepCount, endTimestampNanos);
        awaitPassiveSensorTasksComplete();
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        assertRecord(records.get(0), stepCount, expectedStartTimestampNanos, endTimestampNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onSensorChanged_pendingBatchWriteFutureNotEmpty() throws Exception {
        int stepDelta = 10;
        long endTimestampNanos = MINUTES.toNanos(10);

        triggerStepEvent(stepDelta, endTimestampNanos);
        // Wait for the scheduled task to instantly process the event and schedule the next write
        mThreadScheduler
                .mPassiveTrackerExecutor
                .submit(
                        () -> {
                            // This runnable will execute after the task triggered by
                            // triggerStepEvent() has been processed by the executor.
                        })
                .get();

        assertThat(mStepSensorEventListener.mPendingBatchWriteFuture).isPresent();
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void afterOnSensorChanged_noNewEventsReceived_stopsSchedulingWrites() throws Exception {
        int timeoutBuffer = 100;
        int stepDelta = 10;
        long endTimestampNanos = MINUTES.toNanos(10);
        triggerStepEvent(stepDelta, endTimestampNanos);
        // Wait for the future task to be scheduled
        mThreadScheduler.mPassiveTrackerExecutor.submit(() -> {}).get();
        Optional<ScheduledFuture<?>> pendingFutureOptional =
                mStepSensorEventListener.mPendingBatchWriteFuture;
        assertThat(pendingFutureOptional).isPresent();
        ScheduledFuture<?> pendingFuture = pendingFutureOptional.get();

        // Wait for the future task to execute and complete.
        // The task itself is responsible for clearing mPendingBatchWriteFuture through
        // #writeBatchAndScheduleNextWrite.
        pendingFuture.get(
                mStepSensorEventListener.getBatchingDurationMillis() + timeoutBuffer,
                TimeUnit.MILLISECONDS);

        assertThat(mStepSensorEventListener.mPendingBatchWriteFuture).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void eventSoonAfterPreviousEvent_returnsStartAsEndOfPreviousEvent() {
        int stepDelta = 1;
        long endOfLastDataPointNanos = SECONDS.toNanos(100);
        long endOfCurrentDataPointNanos = endOfLastDataPointNanos + SECONDS.toNanos(50);

        long startTime =
                StepSensorEventListener.estimateStartTime(
                        stepDelta,
                        endOfLastDataPointNanos,
                        endOfCurrentDataPointNanos,
                        /* isDelayedTask= */ true);

        assertThat(startTime).isEqualTo(endOfLastDataPointNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void eventOneMinuteAfterPreviousEvent_returnsStartAsEndOfPreviousEvent() {
        int stepDelta = 1;
        long endOfLastDataPointNanos = SECONDS.toNanos(100);
        long endOfCurrentDataPointNanos = endOfLastDataPointNanos + MINUTES.toNanos(1);

        long startTime =
                StepSensorEventListener.estimateStartTime(
                        stepDelta,
                        endOfLastDataPointNanos,
                        endOfCurrentDataPointNanos,
                        /* isDelayedTask= */ true);

        assertThat(startTime).isEqualTo(endOfLastDataPointNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void eventTwoMinutesAfterPreviousEvent_highCadence_returnsStartAsEndOfPreviousEvent() {
        int stepDelta = 90;
        long endOfLastDataPointNanos = SECONDS.toNanos(100);
        long endOfCurrentDataPointNanos = endOfLastDataPointNanos + MINUTES.toNanos(2);

        long startTime =
                StepSensorEventListener.estimateStartTime(
                        stepDelta,
                        endOfLastDataPointNanos,
                        endOfCurrentDataPointNanos,
                        /* isDelayedTask= */ false);

        assertThat(startTime).isEqualTo(endOfLastDataPointNanos);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void eventTwoMinutesAfterPreviousEvent_lowCadence_returnsEstimatedStartTime() {
        int stepDelta = 1;
        long endOfLastDataPointNanos = SECONDS.toNanos(100);
        long endOfCurrentDataPointNanos = endOfLastDataPointNanos + MINUTES.toNanos(2);

        long startTime =
                StepSensorEventListener.estimateStartTime(
                        stepDelta,
                        endOfLastDataPointNanos,
                        endOfCurrentDataPointNanos,
                        /* isDelayedTask= */ false);

        assertThat(startTime)
                .isEqualTo(endOfCurrentDataPointNanos - getDurationNanosForSteps(stepDelta));
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void eventTwoMinutesAfterPreviousEvent_lowCadence_returnsLongerEstimatedStartTime() {
        int stepDelta = 45; // 30 steps per minute cadence means this would take 1 minute 30 seconds
        long endOfLastDataPointNanos = SECONDS.toNanos(100);
        long endOfCurrentDataPointNanos = endOfLastDataPointNanos + MINUTES.toNanos(2);

        long startTime =
                StepSensorEventListener.estimateStartTime(
                        stepDelta,
                        endOfLastDataPointNanos,
                        endOfCurrentDataPointNanos,
                        /* isDelayedTask= */ false);

        assertThat(startTime).isEqualTo(endOfCurrentDataPointNanos - SECONDS.toNanos(90));
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void onAccuracyChanged_doesNotThrow() throws Exception {
        mStepSensorEventListener.onAccuracyChanged(
                createSensor(), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    @Test
    @EnableFlags({Flags.FLAG_STEP_TRACKING_ENABLED, Flags.FLAG_STEP_TRACKING_ENABLED_DB})
    public void reset_clearsPendingDataAndCancelsFuture() throws Exception {
        triggerStepEvent(10, MINUTES.toNanos(1));
        // Wait for the initial processing to complete and schedule the next write
        mThreadScheduler.mPassiveTrackerExecutor.submit(() -> {}).get();
        assertThat(mStepSensorEventListener.mPendingBatchWriteFuture).isPresent();

        mStepSensorEventListener.reset();

        assertThat(mStepSensorEventListener.mPendingData.sensorValue()).isEqualTo(0);
        assertThat(mStepSensorEventListener.mPendingData.sensorTimestampNanos()).isEqualTo(0);
        assertThat(mStepSensorEventListener.mPendingBatchWriteFuture).isEmpty();
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

    private static void assertRecord(
            RecordInternal<?> record,
            int stepCount,
            long startTimestampNanos,
            long endTimestampNanos) {
        StepsRecordInternal stepsRecord = (StepsRecordInternal) record;
        assertThat(stepsRecord.getCount()).isEqualTo(stepCount);
        assertThat(stepsRecord.getStartTimeInMillis())
                .isEqualTo(getTimestampAfterBoot(startTimestampNanos));
        assertThat(stepsRecord.getEndTimeInMillis())
                .isEqualTo(getTimestampAfterBoot(endTimestampNanos));
    }

    private static long getTimestampAfterBoot(long timestampSinceBootNanos) {
        return NANOSECONDS.toMillis(BOOT_TIME_NANOS + timestampSinceBootNanos);
    }

    /**
     * Returns the time it would take to travel a specified number of steps based on the minimum
     * cadence.
     */
    private long getDurationNanosForSteps(int stepCount) {
        double minutesPerStep = (1 / MIN_STEPS_PER_MINUTE);
        return (long) (stepCount * minutesPerStep * MINUTES.toNanos(1));
    }
}
