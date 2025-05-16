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

import static android.healthconnect.testing.unittest.TaskUtils.TEST_USER;

import static com.android.healthfitness.flags.Flags.FLAG_STEP_TRACKING_ENABLED;
import static com.android.server.healthconnect.device.DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.health.connect.HealthPermissions;
import android.healthconnect.testing.unittest.mocks.AndroidPackageMocker;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import org.junit.After;
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

/** Unit tests for {@link TrackerManagerImpl} */
@RunWith(AndroidJUnit4.class)
public class TrackerManagerImplTest {

    private static final String TEST_PACKAGE_NAME = "com.test.app";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private HealthConnectPermissionHelper mPermissionHelper;
    @Mock private SensorManager mSensorManager;
    @Mock private UserManager mUserManager;

    private AppInfoHelper mAppInfoHelper;
    private HealthConnectInjector mHealthConnectInjector;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        AndroidPackageMocker.addToContext(mContext);
        mPackageManager = mContext.getPackageManager();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        doReturn(mSensorManager).when(mContext).getSystemService(SensorManager.class);
        doReturn(TEST_USER).when(mContext).getUser();
        doReturn(true).when(mUserManager).isUserUnlocked();
        doReturn(true).when(mUserManager).isUserUnlocked(TEST_USER);
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setHealthConnectPermissionHelper(mPermissionHelper)
                        .setUserManager(mUserManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mAppInfoHelper = mHealthConnectInjector.getAppInfoHelper();
    }

    @After
    public void tearDown() throws Exception {
        reset(mContext, mPackageManager, mSensorManager);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_initialize_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initialize();
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_initialize_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initialize();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void noAppsGrantedReadSteps_noPackagesReturned() {
        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);

        assertThat(packages).isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appGrantedReadStepsPermission_isReturnedInList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);

        assertThat(packages).containsExactly(TEST_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appPregrantedReadStepsPermission_notReturnedInList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        setAsPregrantedApp(TEST_PACKAGE_NAME);

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);

        assertThat(packages).isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasNoSensor_doesNotSubscribeToSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initialize();

        verify(mSensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // We don't subscribe because there is no step sensor
        verify(mSensorManager, never()).registerListener(any(), any(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasSensor_subscribesToSensorManager() throws Exception {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(createSensor());
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initialize();

        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void afterSensorManagerSubscription_appLosesPermission_unsubscribeFromSensorManager()
            throws Exception {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(createSensor());
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initialize();
        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());

        revokeStepsPermissionForAllApps();
        manager.initialize();

        verify(mSensorManager).unregisterListener(any(StepSensorEventListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void deviceIsWearOs_stepTrackingNotStarted() throws Exception {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(createSensor());
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initialize();

        verify(mSensorManager, never()).registerListener(any(), any(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void duringInitialization_deviceDataPackageAddedToAppPriorityList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();

        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initialize();

        assertThat(mAppInfoHelper.getAppInfoMap().get(DEVICE_DATA_PROVIDER_PACKAGE)).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void userIsNotUnlocked_stepsTrackingNotStarted() {
        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();
        doReturn(false).when(mUserManager).isUserUnlocked();

        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initialize();

        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();
    }

    private void grantAppStepsPermission(String packageName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;

        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(new String[] {HealthPermissions.READ_STEPS}),
                        argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(List.of(packageInfo));
    }

    private void revokeStepsPermissionForAllApps() {
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(new String[] {HealthPermissions.READ_STEPS}),
                        argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(List.of());
    }

    private void setAsPregrantedApp(String packageName) {
        when(mPermissionHelper.getHealthPermissionFlags(
                        eq(packageName), any(), eq(HealthPermissions.READ_STEPS)))
                .thenReturn(PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT);
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
