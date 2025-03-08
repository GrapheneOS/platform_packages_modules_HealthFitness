/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.RecordTypeIdentifier;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.testing.TestUtils;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class HealthDataCategoryPriorityHelperTest {

    private static final String APP_PACKAGE_NAME = "android.healthconnect.mocked.app";
    private static final String APP_PACKAGE_NAME_2 = "android.healthconnect.mocked.app2";
    private static final String APP_PACKAGE_NAME_3 = "android.healthconnect.mocked.app3";
    private static final String APP_PACKAGE_NAME_4 = "android.healthconnect.mocked.app4";

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Rule
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(HealthConnectManager.class)
                    .build();

    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private PreferenceHelper mPreferenceHelper;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    private long mAppPackageId;
    private long mAppPackageId2;
    private long mAppPackageId3;
    private long mAppPackageId4;

    private AppInfoHelper mAppInfoHelper;
    private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private HealthConnectThreadScheduler mThreadScheduler;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setPackageInfoUtils(mPackageInfoUtils)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        transactionTestUtils.insertApp(APP_PACKAGE_NAME);
        transactionTestUtils.insertApp(APP_PACKAGE_NAME_2);
        transactionTestUtils.insertApp(APP_PACKAGE_NAME_3);
        transactionTestUtils.insertApp(APP_PACKAGE_NAME_4);

        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAppPackageId = mAppInfoHelper.getAppInfoId(APP_PACKAGE_NAME);
        mAppPackageId2 = mAppInfoHelper.getAppInfoId(APP_PACKAGE_NAME_2);
        mAppPackageId3 = mAppInfoHelper.getAppInfoId(APP_PACKAGE_NAME_3);
        mAppPackageId4 = mAppInfoHelper.getAppInfoId(APP_PACKAGE_NAME_4);

        mHealthDataCategoryPriorityHelper =
                healthConnectInjector.getHealthDataCategoryPriorityHelper();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.waitForAllScheduledTasksToComplete(mThreadScheduler);
    }

    @Test
    public void testAppendToPriorityList_ifAppInList_doesNotAddToList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));

        mHealthDataCategoryPriorityHelper.appendToPriorityList(
                APP_PACKAGE_NAME, HealthDataCategory.BODY_MEASUREMENTS, true);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS, List.of(mAppPackageId, mAppPackageId2));
    }

    @Test
    public void testAppendToPriorityList_activeDefaultApp_addsToTopOfList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));

        HealthDataCategoryPriorityHelper spy = Mockito.spy(mHealthDataCategoryPriorityHelper);
        doReturn(true).when(spy).isDefaultApp(APP_PACKAGE_NAME_4);
        spy.appendToPriorityList(APP_PACKAGE_NAME_4, HealthDataCategory.BODY_MEASUREMENTS, false);

        assertThat(spy.getAppIdPriorityOrder(HealthDataCategory.BODY_MEASUREMENTS))
                .containsExactly(mAppPackageId4, mAppPackageId, mAppPackageId2)
                .inOrder();
    }

    @Test
    public void testAppendToPriorityList_ifNonDefaultApp_addsToBottomOfList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));

        assertThat(mHealthDataCategoryPriorityHelper.isDefaultApp(APP_PACKAGE_NAME_4)).isFalse();

        mHealthDataCategoryPriorityHelper.appendToPriorityList(
                APP_PACKAGE_NAME_4, HealthDataCategory.BODY_MEASUREMENTS, false);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(mAppPackageId, mAppPackageId2, mAppPackageId4));
    }

    @Test
    public void testAppendToPriorityList_inactiveDefaultApp_addsToBottomOfList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));

        HealthDataCategoryPriorityHelper spy = Mockito.spy(mHealthDataCategoryPriorityHelper);
        doReturn(true).when(spy).isDefaultApp(APP_PACKAGE_NAME_4);
        spy.appendToPriorityList(APP_PACKAGE_NAME_4, HealthDataCategory.BODY_MEASUREMENTS, true);

        assertThat(spy.getAppIdPriorityOrder(HealthDataCategory.BODY_MEASUREMENTS))
                .containsExactly(mAppPackageId, mAppPackageId2, mAppPackageId4)
                .inOrder();
    }

    @Test
    public void testMaybeRemoveAppFromPriorityList_ifWritePermissionsForApp_doesNotRemoveApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionGranted();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(
                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS, List.of(mAppPackageId, mAppPackageId2));
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId3, mAppPackageId));
    }

    @Test
    public void testMaybeRemoveAppFromPriorityList_ifDataForApp_doesNotRemoveApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionNotGranted();

        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME);

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isTrue();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(
                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS, List.of(mAppPackageId, mAppPackageId2));
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId3, mAppPackageId));
    }

    @Test
    public void testMaybeRemoveAppFromPriorityList_ifNoDataForApp_removesApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionNotGranted();

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isFalse();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(
                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY);

        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.ACTIVITY, List.of(mAppPackageId3));
    }

    @Test
    public void
            testMaybeRemoveAppFromPriorityList_allCategories_ifWritePermissionsForApp_doesNotRemoveApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionGranted();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(APP_PACKAGE_NAME);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId3, mAppPackageId));
    }

    @Test
    public void testMaybeRemoveAppFromPriorityList_allCategories_ifDataForApp_doesNotRemoveApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionNotGranted();

        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME);

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isTrue();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(APP_PACKAGE_NAME);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId3, mAppPackageId));
    }

    @Test
    public void testMaybeRemoveAppFromPriorityList_allCategories_ifNoDataForApp_removesApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));
        setupPackageInfoWithWritePermissionNotGranted();

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isFalse();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(APP_PACKAGE_NAME);

        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.ACTIVITY, List.of(mAppPackageId3));
    }

    @Test
    public void
            testMaybeRemoveAppWithoutWritePermissionsFromPriorityList_ifDataForApp_doesNotRemoveApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));

        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME);

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isTrue();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppWithoutWritePermissionsFromPriorityList(
                APP_PACKAGE_NAME);

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId3, mAppPackageId));
    }

    @Test
    public void
            testMaybeRemoveAppWithoutWritePermissionsFromPriorityList_ifNoDataForApp_removesApp() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_3, APP_PACKAGE_NAME));

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isFalse();

        mHealthDataCategoryPriorityHelper.maybeRemoveAppWithoutWritePermissionsFromPriorityList(
                APP_PACKAGE_NAME);

        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.ACTIVITY, List.of(mAppPackageId3));
    }

    @Test
    public void testGetPriorityOrder_callsReSyncPriority() {
        HealthDataCategoryPriorityHelper spy = Mockito.spy(mHealthDataCategoryPriorityHelper);
        doNothing().when(spy).reSyncHealthDataPriorityTable();

        spy.syncAndGetPriorityOrder(HealthDataCategory.ACTIVITY);
        verify(spy, times(1)).reSyncHealthDataPriorityTable();
    }

    @Test
    public void testSetPriority_additionalPackages_addsToPriorityList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2));

        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME_4,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME));

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(mAppPackageId4, mAppPackageId3, mAppPackageId2, mAppPackageId));
    }

    @Test
    public void testSetPriority_fewerPackages_removesFromPriorityList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));

        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(APP_PACKAGE_NAME_2, APP_PACKAGE_NAME));

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS, List.of(mAppPackageId2, mAppPackageId));
    }

    @Test
    public void testSetPriority_samePackages_reordersPriorityList() {
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));

        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_4));

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(mAppPackageId3, mAppPackageId2, mAppPackageId, mAppPackageId4));
    }

    @Test
    public void testReSyncHealthDataPriorityTable_emptyListAndNoContributingApps_remainsEmpty() {
        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();

        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.ACTIVITY);
        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.SLEEP);
    }

    @Test
    public void testReSyncHealthDataPriorityTable_emptyList_populateContributingApps() {
        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId2, mAppPackageId3));
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.SLEEP, List.of(mAppPackageId, mAppPackageId2, mAppPackageId3));
    }

    @Test
    public void testReSyncHealthDataPriorityTable_oneEmptyList_leaveNonEmptyListsUnchanged() {
        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_2));

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId2, mAppPackageId3));
        // Not populated by contributing apps.
        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.SLEEP, List.of(mAppPackageId2));
    }

    @Test
    public void testReSyncHealthDataPriorityTable_newWritePermission_doesNotUpdateTable() {
        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_4, APP_PACKAGE_NAME_2));

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();
        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.VITALS);
    }

    @Test
    public void testReSyncHealthDataPriorityTable_someAppsNoDataNoPermission_removesApps() {
        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_4, APP_PACKAGE_NAME_2));

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();
        // This would have been empty hence not populating with contributing apps.
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY, List.of(mAppPackageId2, mAppPackageId3));
        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.BODY_MEASUREMENTS);
        // This was non-empty hence not populated with contributing apps.
        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.SLEEP, List.of(mAppPackageId2));
    }

    @Test
    public void testReSyncHealthDataPriorityTable_noDataNorPermission_removesApps() {
        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_4, APP_PACKAGE_NAME_2));

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();

        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.ACTIVITY);
        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.BODY_MEASUREMENTS);
        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.SLEEP, List.of(mAppPackageId2));
    }

    @Test
    public void testNewReSyncHealthDataPriorityTable_ifDataForApps_doesNotRemoveApps() {
        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_4, APP_PACKAGE_NAME_2));

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));

        mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();

        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.ACTIVITY, List.of(mAppPackageId));
        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.BODY_MEASUREMENTS);
        assertAppIdPriorityOrderIsEqualTo(HealthDataCategory.SLEEP, List.of(mAppPackageId2));
    }

    @Test
    public void testMaybeAddInactiveAppsToPriorityList_ifPreferenceNotSet_addsToList() {
        when(mPreferenceHelper.getPreference(any())).thenReturn(null);

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));
        // Inactive apps {
        //   Activity -> {APP_PACKAGE_NAME, APP_PACKAGE_NAME_2, APP_PACKAGE_NAME_3}
        //   Vitals -> {APP_PACKAGE_NAME, APP_PACKAGE_NAME_3}

        // Setup current priority list
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.BODY_MEASUREMENTS,
                List.of(
                        APP_PACKAGE_NAME,
                        APP_PACKAGE_NAME_2,
                        APP_PACKAGE_NAME_3,
                        APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(APP_PACKAGE_NAME_4));
        mHealthDataCategoryPriorityHelper.setPriorityOrder(
                HealthDataCategory.SLEEP, List.of(APP_PACKAGE_NAME_4));

        mHealthDataCategoryPriorityHelper
                .addInactiveAppsWhenFirstMigratingToNewAggregationControl();

        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.ACTIVITY,
                List.of(mAppPackageId4, mAppPackageId, mAppPackageId2, mAppPackageId3));
        assertAppIdPriorityOrderIsEqualTo(
                HealthDataCategory.VITALS, List.of(mAppPackageId, mAppPackageId3));
    }

    @Test
    public void testMaybeAddInactiveAppsToPriorityList_ifPreferenceExists_doesNotAdd() {
        when(mPreferenceHelper.getPreference(any())).thenReturn(String.valueOf(true));

        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME);

        mHealthDataCategoryPriorityHelper
                .addInactiveAppsWhenFirstMigratingToNewAggregationControl();

        verify(mPreferenceHelper, never()).insertOrReplacePreference(any(), any());

        assertAppIdPriorityOrderIsEmpty(HealthDataCategory.ACTIVITY);
    }

    @Test
    public void testAppHasDataInCategory_forAppsWithDataInCategory_returnsTrue() {
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_STEPS), APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION),
                APP_PACKAGE_NAME_3);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_BODY_FAT), APP_PACKAGE_NAME_4);

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.ACTIVITY))
                .isTrue();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_2, HealthDataCategory.ACTIVITY))
                .isTrue();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_3, HealthDataCategory.ACTIVITY))
                .isTrue();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_3, HealthDataCategory.SLEEP))
                .isTrue();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_4, HealthDataCategory.BODY_MEASUREMENTS))
                .isTrue();
    }

    @Test
    public void testAppHasDataInCategory_forAppsWithoutDataInCategory_returnsFalse() {
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_NUTRITION), APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION,
                        RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_HYDRATION,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                        RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW),
                APP_PACKAGE_NAME_3);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW), APP_PACKAGE_NAME_4);

        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_3, HealthDataCategory.ACTIVITY))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_4, HealthDataCategory.NUTRITION))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_2, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_4, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.CYCLE_TRACKING))
                .isFalse();
    }

    @Test
    public void testAppHasDataInCategory_ifContributingPackagesMapEmpty_returnsFalse() {
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.SLEEP))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_4, HealthDataCategory.NUTRITION))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_2, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME_4, HealthDataCategory.VITALS))
                .isFalse();
        assertThat(
                        mHealthDataCategoryPriorityHelper.appHasDataInCategory(
                                APP_PACKAGE_NAME, HealthDataCategory.CYCLE_TRACKING))
                .isFalse();
    }

    @Test
    public void testGetDataCategoriesWithDataForPackage_returnsCorrectCategories() {
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME_3);

        assertThat(
                        mHealthDataCategoryPriorityHelper.getDataCategoriesWithDataForPackage(
                                APP_PACKAGE_NAME))
                .isEqualTo(Set.of(HealthDataCategory.ACTIVITY, HealthDataCategory.VITALS));
        assertThat(
                        mHealthDataCategoryPriorityHelper.getDataCategoriesWithDataForPackage(
                                APP_PACKAGE_NAME_2))
                .isEqualTo(Set.of(HealthDataCategory.ACTIVITY, HealthDataCategory.NUTRITION));
        assertThat(
                        mHealthDataCategoryPriorityHelper.getDataCategoriesWithDataForPackage(
                                APP_PACKAGE_NAME_3))
                .isEqualTo(Set.of(HealthDataCategory.VITALS, HealthDataCategory.ACTIVITY));
        assertThat(
                        mHealthDataCategoryPriorityHelper.getDataCategoriesWithDataForPackage(
                                APP_PACKAGE_NAME_4))
                .isEmpty();
    }

    @Test
    public void testGetAllContributorApps_returnsJustAppsWithData() {
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME_3);

        Map<Integer, Set<String>> expectedResult = new HashMap<>();
        expectedResult.put(
                HealthDataCategory.ACTIVITY,
                Set.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2, APP_PACKAGE_NAME_3));
        expectedResult.put(HealthDataCategory.NUTRITION, Set.of(APP_PACKAGE_NAME_2));
        expectedResult.put(HealthDataCategory.VITALS, Set.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_3));
        assertThat(mHealthDataCategoryPriorityHelper.getAllContributorApps())
                .containsExactlyEntriesIn(expectedResult);
    }

    @Test
    public void testGetAllInactiveApps_returnsApps_withDataAndNoWritePermissions() {
        // Setup contributor apps
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_NUTRITION),
                APP_PACKAGE_NAME_2);
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                Set.of(
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                APP_PACKAGE_NAME_3);

        // Setup apps with write permissions
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = APP_PACKAGE_NAME;
        packageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        packageInfo1.requestedPermissionsFlags =
                new int[] {
                    0,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = APP_PACKAGE_NAME_2;
        packageInfo2.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.READ_BLOOD_GLUCOSE
                };
        packageInfo2.requestedPermissionsFlags =
                new int[] {
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        when(mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(
                        eq(APP_PACKAGE_NAME), any(), any()))
                .thenReturn(packageInfo1);
        when(mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(
                        eq(APP_PACKAGE_NAME_2), any(), any()))
                .thenReturn(packageInfo2);

        Map<Integer, Set<String>> expectedResult = new HashMap<>();
        expectedResult.put(
                HealthDataCategory.ACTIVITY,
                Set.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_2, APP_PACKAGE_NAME_3));
        expectedResult.put(HealthDataCategory.VITALS, Set.of(APP_PACKAGE_NAME, APP_PACKAGE_NAME_3));
        assertThat(mHealthDataCategoryPriorityHelper.getAllInactiveApps())
                .containsExactlyEntriesIn(expectedResult);
    }

    private void assertAppIdPriorityOrderIsEqualTo(int type, List<Long> appIds) {
        assertThat(mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(type))
                .containsExactlyElementsIn(appIds)
                .inOrder();
        // Clear cache and re-read to ensure db contains the same.
        mHealthDataCategoryPriorityHelper.clearCache();
        assertThat(mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(type))
                .containsExactlyElementsIn(appIds)
                .inOrder();
    }

    private void assertAppIdPriorityOrderIsEmpty(int type) {
        assertThat(mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(type)).isEmpty();
        // Clear cache and re-read to ensure db contains the same.
        mHealthDataCategoryPriorityHelper.clearCache();
        assertThat(mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(type)).isEmpty();
    }

    private void setupPackageInfoWithWritePermissionGranted() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = APP_PACKAGE_NAME;
        packageInfo.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        packageInfo.requestedPermissionsFlags =
                new int[] {0, PackageInfo.REQUESTED_PERMISSION_GRANTED, 0, 0};

        when(mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(any(), any(), any()))
                .thenReturn(packageInfo);
    }

    private void setupPackageInfoWithWritePermissionNotGranted() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = APP_PACKAGE_NAME;
        packageInfo.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        when(HealthConnectManager.isHealthPermission(any(), any())).thenReturn(true);
        packageInfo.requestedPermissionsFlags = new int[] {0, 0, 0, 0};
        when(mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(any(), any(), any()))
                .thenReturn(packageInfo);
    }
}
