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
package android.healthconnect.cts;

import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.healthconnect.testing.cts.PermissionUtils.grantAllHealthPermissions;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermission;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermissions;
import static android.healthconnect.testing.cts.PermissionUtils.revokeAllHealthPermissions;
import static android.healthconnect.testing.cts.TestUtils.createMatchmakingIntent;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Intent;
import android.health.connect.MatchmakingRequest;
import android.health.connect.MatchmakingResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(FLAG_MATCHMAKING)
public class HealthConnectMatchmakingTest {

    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");

    private static final TestAppProxy APP_B_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.B");

    private static final TestAppProxy APP_WITH_WRITE_PERMS_ONLY =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.writePermsOnly");

    private static final String EXTRA_RECORD_TYPES = "android.health.connect.extra.RECORD_TYPES";
    private static final String EXTRA_INCLUDED_DATA_SOURCES =
            "android.health.connect.extra.INCLUDED_DATA_SOURCES";
    private static final String EXTRA_EXCLUDED_DATA_SOURCES =
            "android.health.connect.extra.EXCLUDED_DATA_SOURCES";

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        grantAllHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());
        grantAllHealthPermissions(APP_B_WITH_READ_WRITE_PERMS.getPackageName());
        grantAllHealthPermissions(APP_WITH_WRITE_PERMS_ONLY.getPackageName());
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllDataFromHealthConnect();
        grantAllHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());
        grantAllHealthPermissions(APP_B_WITH_READ_WRITE_PERMS.getPackageName());
        grantAllHealthPermissions(APP_WITH_WRITE_PERMS_ONLY.getPackageName());
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_hasMatchingApp_returnTrue() throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage("%s.isMatchmakingPossible(empty_set)", APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_hasMatchingApp_returnTrue() throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage("%s.isMatchmakingPossible(empty_set)", APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_callingAppHasNoReadPermission_returnFalse() throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage("%s.isMatchmakingPossible(empty_set)", APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_callingAppHasNoReadPermission_returnFalse()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage("%s.isMatchmakingPossible(empty_set)", APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_sleepRequested_noReadSleepPermission_returnFalse()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), READ_STEPS);
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(SleepSessionRecord.class);

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_sleepRequested_noReadSleepPermission_returnFalse()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), READ_STEPS);
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(SleepSessionRecord.class);

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_sleepRequested_hasReadSleepPermission_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), READ_SLEEP);
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_sleepRequested_hasReadSleepPermission_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), READ_SLEEP);
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response = MatchmakingHelper.getMatchmakingResponse();

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_multipleRequested_noReadPermission_returnFalse() throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(READ_STEPS, READ_EXERCISE, WRITE_DISTANCE));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(DistanceRecord.class, SleepSessionRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([DistanceRecord, SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_multipleRequested_noReadPermission_returnFalse()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(READ_STEPS, READ_EXERCISE, WRITE_DISTANCE));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(DistanceRecord.class, SleepSessionRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([DistanceRecord, SleepSessionRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_multipleRequested_readPermissionForOne_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(READ_STEPS, READ_EXERCISE, READ_SLEEP, WRITE_DISTANCE));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(SleepSessionRecord.class, StepsRecord.class, HeartRateRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord, StepsRecord,"
                                + " HeartRateRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_multipleRequested_readPermissionForOne_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(READ_STEPS, READ_EXERCISE, READ_SLEEP, WRITE_DISTANCE));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(SleepSessionRecord.class, StepsRecord.class, HeartRateRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord, StepsRecord,"
                                + " HeartRateRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    @Test
    public void testMatchingApps_multipleRequested_readPermissionForAll_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(
                        READ_STEPS,
                        READ_EXERCISE,
                        READ_SLEEP,
                        WRITE_DISTANCE,
                        READ_HEART_RATE,
                        READ_SLEEP));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(SleepSessionRecord.class, StepsRecord.class, HeartRateRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord, StepsRecord,"
                                + " HeartRateRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_multipleRequested_readPermissionForAll_returnTrue()
            throws Exception {
        revokeAllHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");
        grantHealthPermissions(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                List.of(
                        READ_STEPS,
                        READ_EXERCISE,
                        READ_SLEEP,
                        WRITE_DISTANCE,
                        READ_HEART_RATE,
                        READ_SLEEP));
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingHelper.getMatchmakingResponse(
                        Set.of(SleepSessionRecord.class, StepsRecord.class, HeartRateRecord.class));

        assertWithMessage(
                        "%s.isMatchmakingPossible([SleepSessionRecord, StepsRecord,"
                                + " HeartRateRecord])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_includedSourcesNotVisible_matchingSources_returnTrue()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        // APP_A_WITH_READ_WRITE_PERMS does not have visibility over this package name
        // So it will not be taken into account for matchmaking
        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithIncludedDataSources(
                        Set.of(new DataOrigin.Builder().setPackageName("test.package").build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(includedDataSources=[test.package])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_includedSourcesVisible_matchingSources_returnTrue()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithIncludedDataSources(
                        Set.of(
                                new DataOrigin.Builder()
                                        .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                        .build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(includedDataSources=["
                                + APP_WITH_WRITE_PERMS_ONLY.getPackageName()
                                + "])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_includedSourcesVisible_noMatchingSources_returnFalse()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        grantAllHealthPermissions(APP_WITH_WRITE_PERMS_ONLY.getPackageName());
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        // APP_WITH_WRITE_PERMS already has all matching permissions granted
        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithIncludedDataSources(
                        Set.of(
                                new DataOrigin.Builder()
                                        .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                        .build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(includedDataSources=["
                                + APP_WITH_WRITE_PERMS_ONLY.getPackageName()
                                + "])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_excludedSourcesNotVisible_matchingSources_returnTrue()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        // APP_A_WITH_READ_WRITE_PERMS does not have visibility over this package name
        // So it will not be taken into account for exclusion from matchmaking
        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithExcludedDataSources(
                        Set.of(new DataOrigin.Builder().setPackageName("test.package").build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(excludedDataSources=[test.package])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_excludedSourcesVisible_matchingSources_returnTrue()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        // Exclude APP_WITH_WRITE_PERMS_ONLY from Matchmaking
        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithExcludedDataSources(
                        Set.of(
                                new DataOrigin.Builder()
                                        .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                        .build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(excludedDataSources=["
                                + APP_WITH_WRITE_PERMS_ONLY.getPackageName()
                                + "])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isTrue();
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testMatchingDataSources_excludedSourcesVisible_noMatchingSources_returnTrue()
            throws Exception {
        // APP_A_WITH_READ_WRITE_PERMS has all permissions granted
        revokeAllHealthPermissions(
                APP_WITH_WRITE_PERMS_ONLY.getPackageName(), "HealthConnectDeviceTest");
        revokeAllHealthPermissions(
                APP_B_WITH_READ_WRITE_PERMS.getPackageName(), "HealthConnectDeviceTest");

        // APP_WITH_WRITE_PERMS already has all matching permissions granted
        MatchmakingResponse response =
                MatchmakingWithDevicesHelper.getMatchmakingResponseWithExcludedDataSources(
                        Set.of(
                                new DataOrigin.Builder()
                                        .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                        .build(),
                                new DataOrigin.Builder()
                                        .setPackageName(
                                                APP_B_WITH_READ_WRITE_PERMS.getPackageName())
                                        .build()));
        assertWithMessage(
                        "%s.isMatchmakingPossible(excludedDataSources=["
                                + APP_WITH_WRITE_PERMS_ONLY.getPackageName()
                                + ", "
                                + APP_B_WITH_READ_WRITE_PERMS.getPackageName()
                                + "])",
                        APP_A_WITH_READ_WRITE_PERMS)
                .that(response.isMatchmakingPossible())
                .isFalse();
    }

    @RequiresFlagsEnabled(FLAG_MATCHMAKING)
    @Test
    public void testCreateMatchingAppsIntent_success() {
        Intent matchingAppsIntent = createMatchmakingIntent(Set.of());
        assertThat(matchingAppsIntent.getAction()).isNotNull();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_RECORD_TYPES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_RECORD_TYPES)).isEmpty();
    }

    @RequiresFlagsEnabled({
        FLAG_MATCHMAKING,
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testCreateEmptyMatchingAppsIntent_withDevices_success() {
        Intent matchingAppsIntent = createMatchmakingIntent(Set.of());
        assertThat(matchingAppsIntent.getAction()).isNotNull();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_RECORD_TYPES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_RECORD_TYPES)).isEmpty();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_INCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_INCLUDED_DATA_SOURCES)).isEmpty();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_EXCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_EXCLUDED_DATA_SOURCES)).isEmpty();
    }

    @RequiresFlagsEnabled(FLAG_MATCHMAKING)
    @Test
    public void testCreateMatchingAppsIntent_multipleRecordsPassed_success() {
        Intent matchingAppsIntent =
                createMatchmakingIntent(Set.of(StepsRecord.class, SleepSessionRecord.class));
        assertThat(matchingAppsIntent.getAction()).isNotNull();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_RECORD_TYPES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_RECORD_TYPES))
                .asList()
                .containsExactly(
                        StepsRecord.class.getCanonicalName(),
                        SleepSessionRecord.class.getCanonicalName());
    }

    @RequiresFlagsEnabled({
        FLAG_MATCHMAKING,
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testCreateMatchingAppsIntent_multipleIncludeDataSources_success() {
        Intent matchingAppsIntent =
                createMatchmakingIntent(
                        Set.of(), Set.of("include.package.1", "include.package.2"), Set.of());
        assertThat(matchingAppsIntent.getAction()).isNotNull();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_RECORD_TYPES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_RECORD_TYPES)).isEmpty();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_INCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_INCLUDED_DATA_SOURCES))
                .asList()
                .containsExactly("include.package.1", "include.package.2");
        assertThat(matchingAppsIntent.hasExtra(EXTRA_EXCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_EXCLUDED_DATA_SOURCES)).isEmpty();
    }

    @RequiresFlagsEnabled({
        FLAG_MATCHMAKING,
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    @Test
    public void testCreateMatchingAppsIntent_multipleExcludeDataSources_success() {
        Intent matchingAppsIntent =
                createMatchmakingIntent(
                        Set.of(), Set.of(), Set.of("exclude.package.1", "exclude.package.2"));
        assertThat(matchingAppsIntent.getAction()).isNotNull();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_RECORD_TYPES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_RECORD_TYPES)).isEmpty();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_INCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_INCLUDED_DATA_SOURCES)).isEmpty();
        assertThat(matchingAppsIntent.hasExtra(EXTRA_EXCLUDED_DATA_SOURCES)).isTrue();
        assertThat(matchingAppsIntent.getStringArrayExtra(EXTRA_EXCLUDED_DATA_SOURCES))
                .asList()
                .containsExactly("exclude.package.1", "exclude.package.2");
    }

    private static class MatchmakingHelper {
        private static MatchmakingResponse getMatchmakingResponse() throws Exception {
            MatchmakingRequest request = new MatchmakingRequest.Builder().build();
            return APP_A_WITH_READ_WRITE_PERMS.isMatchmakingPossible(request);
        }

        private static MatchmakingResponse getMatchmakingResponse(
                Class<? extends android.health.connect.datatypes.Record> recordType)
                throws Exception {
            MatchmakingRequest request =
                    new MatchmakingRequest.Builder().addRecordType(recordType).build();
            return APP_A_WITH_READ_WRITE_PERMS.isMatchmakingPossible(request);
        }

        private static MatchmakingResponse getMatchmakingResponse(
                Set<Class<? extends Record>> recordTypes) throws Exception {
            MatchmakingRequest request =
                    new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
            return APP_A_WITH_READ_WRITE_PERMS.isMatchmakingPossible(request);
        }
    }

    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    private static class MatchmakingWithDevicesHelper {
        private static MatchmakingResponse getMatchmakingResponseWithIncludedDataSources(
                Set<DataOrigin> includedDataSources) throws Exception {
            MatchmakingRequest request =
                    new MatchmakingRequest.Builder()
                            .setIncludedDataSources(includedDataSources)
                            .build();
            return APP_A_WITH_READ_WRITE_PERMS.isMatchmakingPossible(request);
        }

        private static MatchmakingResponse getMatchmakingResponseWithExcludedDataSources(
                Set<DataOrigin> excludedDataSources) throws Exception {
            MatchmakingRequest request =
                    new MatchmakingRequest.Builder()
                            .setExcludedDataSources(excludedDataSources)
                            .build();
            return APP_A_WITH_READ_WRITE_PERMS.isMatchmakingPossible(request);
        }
    }
}
