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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.health.connect.HealthDataCategory;
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
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    private Context mContext;
    private PackageManager mPackageManager;
    @Mock private SensorManager mSensorManager;
    private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    @Mock private UserManager mUserManager;
    private AppInfoHelper mAppInfoHelper;
    private HealthConnectInjector mHealthConnectInjector;

    @Before
    public void setup() throws Exception {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        AndroidPackageMocker.addToContext(mContext);
        mPackageManager = mContext.getPackageManager();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        doReturn(mSensorManager).when(mContext).getSystemService(SensorManager.class);
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(createSensor());
        when(mSensorManager.registerListener(any(), any(), anyInt(), anyInt())).thenReturn(true);
        doReturn(TEST_USER).when(mContext).getUser();
        doReturn(true).when(mUserManager).isUserUnlocked();
        doReturn(true).when(mUserManager).isUserUnlocked(TEST_USER);
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setUserManager(mUserManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mAppInfoHelper = mHealthConnectInjector.getAppInfoHelper();
        mHealthDataCategoryPriorityHelper =
                mHealthConnectInjector.getHealthDataCategoryPriorityHelper();
    }

    @After
    public void tearDown() throws Exception {
        reset(mContext, mPackageManager, mSensorManager);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_initialize_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_initialize_doesNotThrow() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();
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
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPackageManager);

        assertThat(packages).isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appGrantedReadStepsPermission_isReturnedInList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPackageManager);

        assertThat(packages).containsExactly(TEST_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appPregrantedReadStepsPermission_notReturnedInList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        setAsPregrantedApp(TEST_PACKAGE_NAME);

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPackageManager);

        assertThat(packages).isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasNoSensor_doesNotSubscribeToSensorManager() {
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(null);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // We don't subscribe because there is no step sensor
        verify(mSensorManager, never()).registerListener(any(), any(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasNoSensor_doesNotRegisterPermissionChangeListener() {
        when(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(null);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mPackageManager, never()).addOnPermissionsChangeListener(any());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void
            appHasPermission_sensorManagerUnavailable_doesNotRegisterPermissionChangeListener() {
        doReturn(null).when(mContext).getSystemService(SensorManager.class);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mPackageManager, never()).addOnPermissionsChangeListener(any());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasSensor_subscribesToSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void afterInitialSubscribes_doesNotCallSubscribeAgain() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();
        InOrder inOrderMock = inOrder(mSensorManager);
        inOrderMock
                .verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());

        manager.initializeOrRefresh();

        inOrderMock
                .verify(mSensorManager, never())
                .registerListener(any(), any(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void ifNotTracking_doesNotCallUnsubscribe() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager, never()).unregisterListener(any(StepSensorEventListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void appHasPermission_deviceHasSensor_flushesSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager).flush(any(StepSensorEventListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void afterSensorManagerSubscription_appLosesPermission_unsubscribeFromSensorManager()
            throws Exception {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();
        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());

        revokeStepsPermissionForAllApps();
        manager.initializeOrRefresh();

        verify(mSensorManager).unregisterListener(any(StepSensorEventListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void deviceIsWearOs_stepTrackingNotStarted() throws Exception {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager, never()).registerListener(any(), any(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void duringInitialization_deviceDataPackageAddedToAppPriorityList() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();

        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();

        assertThat(mAppInfoHelper.getAppInfoMap().get(DEVICE_DATA_PROVIDER_PACKAGE)).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void userIsNotUnlocked_stepsTrackingNotStarted() {
        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();
        doReturn(false).when(mUserManager).isUserUnlocked();

        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        manager.initializeOrRefresh();

        assertThat(mAppInfoHelper.getAppInfoMap()).isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void onInitialize_addsListenerForPermissionChanges() {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mPackageManager)
                .addOnPermissionsChangeListener(
                        any(PackageManager.OnPermissionsChangedListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void onInitialize_appendsDevicePackageToPriorityList_onlyOnce() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        // The first initialize call should add the device package to the priority list.
        manager.initializeOrRefresh();
        // Then explicitly remove it.
        mHealthDataCategoryPriorityHelper.setPriorityOrder(HealthDataCategory.ACTIVITY, List.of());
        manager.initializeOrRefresh();

        assertThat(
                        mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(
                                HealthDataCategory.ACTIVITY))
                .isEmpty();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void onAppPermissionGranted_listenerTriggered_refreshesTrackerStatusAndSubscribes()
            throws Exception {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        ArgumentCaptor<PackageManager.OnPermissionsChangedListener> permissionsListenerCaptor =
                ArgumentCaptor.forClass(PackageManager.OnPermissionsChangedListener.class);
        manager.initializeOrRefresh();
        verify(mPackageManager).addOnPermissionsChangeListener(permissionsListenerCaptor.capture());

        grantAppStepsPermission(TEST_PACKAGE_NAME);
        permissionsListenerCaptor.getValue().onPermissionsChanged(/* uid= */ 0);

        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void onAppPermissionRevoked_listenerTriggered_refreshesTrackerStatusAndUnsubscribes()
            throws Exception {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        ArgumentCaptor<PackageManager.OnPermissionsChangedListener> permissionsListenerCaptor =
                ArgumentCaptor.forClass(PackageManager.OnPermissionsChangedListener.class);
        manager.initializeOrRefresh();
        verify(mPackageManager).addOnPermissionsChangeListener(permissionsListenerCaptor.capture());
        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());

        revokeStepsPermissionForAllApps();
        permissionsListenerCaptor.getValue().onPermissionsChanged(/* uid= */ 0);

        verify(mSensorManager).unregisterListener(any(StepSensorEventListener.class));
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void onAppPermissionRevoked_refreshesTrackerStatusAndResetsSensorListener()
            throws Exception {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        StepSensorEventListener listenerMock = mock(StepSensorEventListener.class);
        TrackerManagerImpl manager =
                (TrackerManagerImpl) mHealthConnectInjector.getTrackerManager();
        manager.mListener = listenerMock;
        ArgumentCaptor<PackageManager.OnPermissionsChangedListener> permissionsListenerCaptor =
                ArgumentCaptor.forClass(PackageManager.OnPermissionsChangedListener.class);
        manager.initializeOrRefresh();
        verify(mPackageManager).addOnPermissionsChangeListener(permissionsListenerCaptor.capture());
        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());

        revokeStepsPermissionForAllApps();
        permissionsListenerCaptor.getValue().onPermissionsChanged(/* uid= */ 0);

        verify(listenerMock).reset();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void withValidSubscription_clearTracker_unsubscribesAndResetsListener() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        StepSensorEventListener listenerMock = mock(StepSensorEventListener.class);
        TrackerManagerImpl manager =
                (TrackerManagerImpl) mHealthConnectInjector.getTrackerManager();
        manager.mListener = listenerMock;
        manager.initializeOrRefresh();
        verify(mSensorManager).registerListener(any(), any(), anyInt(), anyInt());

        manager.clearTracker();

        verify(mSensorManager).unregisterListener(listenerMock);
        verify(listenerMock).reset();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void exceptionThrownWithinPermissionListener_exceptionCaught() throws Exception {
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();
        ArgumentCaptor<PackageManager.OnPermissionsChangedListener> permissionsListenerCaptor =
                ArgumentCaptor.forClass(PackageManager.OnPermissionsChangedListener.class);
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        manager.initializeOrRefresh();
        verify(mPackageManager).addOnPermissionsChangeListener(permissionsListenerCaptor.capture());

        when(mPackageManager.getPermissionFlags(any(), any(), any()))
                .thenThrow(new RuntimeException("Something went wrong"));
        permissionsListenerCaptor.getValue().onPermissionsChanged(/* uid= */ 0);

        // Verify that the method which we forced to throw was actually called.
        verify(mPackageManager, times(2)).getPermissionFlags(any(), any(), any());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabledViaPreference_unsubscribesFromSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        mHealthConnectInjector
                .getPreferenceHelper()
                .insertOrReplacePreference("TRACKING_PREF_1", String.valueOf(false));
        TrackerManagerImpl manager =
                spy((TrackerManagerImpl) mHealthConnectInjector.getTrackerManager());

        manager.initializeOrRefresh();

        verify(manager).unsubscribeFromSensorManager();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabledViaPreference_subscribesToSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        mHealthConnectInjector
                .getPreferenceHelper()
                .insertOrReplacePreference("TRACKING_PREF_1", String.valueOf(true));
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_preferenceAbsent_subscribesToSensorManager() {
        grantAppStepsPermission(TEST_PACKAGE_NAME);
        TrackerManager manager = mHealthConnectInjector.getTrackerManager();

        manager.initializeOrRefresh();

        verify(mSensorManager)
                .registerListener(
                        any(StepSensorEventListener.class), any(Sensor.class), anyInt(), anyInt());
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
        when(mPackageManager.getPermissionFlags(
                        eq(HealthPermissions.READ_STEPS), eq(packageName), any()))
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
