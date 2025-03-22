/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.permission;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.health.connect.HealthPermissions;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class HealthConnectPermissionHelperTest {
    private static final String TEST_PACKAGE_NAME = "com.android.healthconnect.testapp";
    private static final String HC_PACKAGE_NAME = "com.android.healthconnect";
    private static final UserHandle CURRENT_USER = Process.myUserHandle();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private HealthConnectPermissionHelper mPermissionHelper;
    private final HealthConnectMappings mHealthConnectMappings = new HealthConnectMappings();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private HealthPermissionIntentAppsTracker mTracker;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    @Mock private AppInfoHelper mAppInfoHelper;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        mPermissionHelper =
                new HealthConnectPermissionHelper(
                        mContext,
                        mPackageManager,
                        mTracker,
                        mFirstGrantTimeManager,
                        mHealthDataCategoryPriorityHelper,
                        mAppInfoHelper,
                        mHealthConnectMappings);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        PermissionGroupInfo info = new PermissionGroupInfo();
        info.packageName = HC_PACKAGE_NAME;
        when(mPackageManager.getPermissionGroupInfo(
                        eq(android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP),
                        eq(0)))
                .thenReturn(info);
        setUpHealthPermissions();
    }

    @Test
    @DisableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_flagDisabled_shouldEnforce() {
        assertTrue(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_watchDevice_shouldNotEnforce()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);

        assertFalse(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_noPackageFound_shouldEnforce()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenThrow(new PackageManager.NameNotFoundException("Cannot find the app name"));

        assertTrue(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_noHealthPermissions_shouldNotEnforce()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.requestedPermissions = new String[] {permission.ACTIVITY_RECOGNITION};
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertFalse(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_requestNotFromSplitPermission_shouldEnforce()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.requestedPermissions = new String[] {HealthPermissions.READ_HEART_RATE};
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_USER_SET);

        assertTrue(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_exceptionDuringGetPermissionFlags_shouldEnforce()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.requestedPermissions = new String[] {HealthPermissions.READ_HEART_RATE};
        // There is a second package lookup during the permission flag query.
        // Simulate the package being uninstalled before this second lookup.
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo)
                .thenThrow(new PackageManager.NameNotFoundException("Cannot find the app name"));
        verify(mPackageManager, never()).getPermissionFlags(any(), any(), any());

        assertTrue(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_hrFromSplitPermission_shouldNotEnforce()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        /* targetSdkVersion= */ 34,
                        /* requestedPermissions= */ new String[] {
                            HealthPermissions.READ_HEART_RATE
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        assertFalse(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            shouldEnforcePermissionUsageIntent_hrFromSplitPermission_targetSdk22_shouldNotEnforce()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        /* targetSdkVersion= */ 22,
                        /* requestedPermissions= */ new String[] {
                            HealthPermissions.READ_HEART_RATE
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVIEW_REQUIRED);

        assertFalse(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            shouldEnforcePermissionUsageIntent_onePermissionNotFromSplitPermission_shouldEnforce()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.requestedPermissions =
                new String[] {
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
                };
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        TEST_PACKAGE_NAME,
                        CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_USER_SET);

        assertTrue(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void shouldEnforcePermissionUsageIntent_multipleSplitPermissions_shouldNotEnforce()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        /* targetSdkVersion= */ 34,
                        /* requestedPermissions= */ new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        TEST_PACKAGE_NAME,
                        CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        assertFalse(
                mPermissionHelper.shouldEnforcePermissionUsageIntent(
                        TEST_PACKAGE_NAME, CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_noIntentSupport_doesNotGrantPermission()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {HealthPermissions.READ_SKIN_TEMPERATURE});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThrows(
                SecurityException.class,
                () ->
                        mPermissionHelper.grantHealthPermission(
                                TEST_PACKAGE_NAME,
                                HealthPermissions.READ_SKIN_TEMPERATURE,
                                CURRENT_USER));

        assertNoPermissionGranted();
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_noIntentSupport_wear_grantsPermission()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(Build.VERSION_CODES.VANILLA_ICE_CREAM, new String[] {});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_supportsRationaleIntent_grantsPermission()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {HealthPermissions.READ_HEART_RATE});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_noIntentSupport_nonSplittablePermission_doesNotGrantPermission()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(Build.VERSION_CODES.VANILLA_ICE_CREAM, new String[] {});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        verify(mPackageManager, never()).getPermissionFlags(any(), any(), any());

        // Skin Temperature is not a permission that can be granted via the
        // split permission platform upgrade.
        assertThrows(
                SecurityException.class,
                () ->
                        mPermissionHelper.grantHealthPermission(
                                TEST_PACKAGE_NAME,
                                HealthPermissions.READ_SKIN_TEMPERATURE,
                                CURRENT_USER));

        assertNoPermissionGranted();
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_noIntentSupport_splittablePermission_baklavaTargetSdk_doesNotGrantPermission()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(Build.VERSION_CODES.BAKLAVA, new String[] {});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        verify(mPackageManager, never()).getPermissionFlags(any(), any(), any());

        assertThrows(
                SecurityException.class,
                () ->
                        mPermissionHelper.grantHealthPermission(
                                TEST_PACKAGE_NAME,
                                HealthPermissions.READ_HEART_RATE,
                                CURRENT_USER));

        assertNoPermissionGranted();
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_noIntentSupport_readHrExplicitlyRequested_doesNotGrantPermission()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(Build.VERSION_CODES.VANILLA_ICE_CREAM, new String[] {});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_USER_SET);

        assertThrows(
                SecurityException.class,
                () ->
                        mPermissionHelper.grantHealthPermission(
                                TEST_PACKAGE_NAME,
                                HealthPermissions.READ_HEART_RATE,
                                CURRENT_USER));

        assertNoPermissionGranted();
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_noIntentSupport_readBackgroundExplicitlyRequested_doesNotGrantPermission()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(Build.VERSION_CODES.VANILLA_ICE_CREAM, new String[] {});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        TEST_PACKAGE_NAME,
                        CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_USER_SET);

        assertThrows(
                SecurityException.class,
                () ->
                        mPermissionHelper.grantHealthPermission(
                                TEST_PACKAGE_NAME,
                                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                                CURRENT_USER));

        assertNoPermissionGranted();
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_noIntentSupport_readHrSplitPermission_grantsPermission()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEART_RATE});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_noIntentSupport_readBackgroundSplitPermission_grantsPermission()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        TEST_PACKAGE_NAME,
                        CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHeartRate_supportsIntent_outdatedTargetSdk_alsoGrantBodySensors()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME), eq(permission.BODY_SENSORS), eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHeartRate_noIntentSupport_outdatedTargetSdk_alsoGrantBodySensors()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME), eq(permission.BODY_SENSORS), eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_readHeartRate_postSplitTargetSdk_noBodySensorsSync()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_readHeartRate_bodySensorNotRequested_noBodySensorsSync()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEART_RATE});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void grantHealthPermission_readHeartRate_olderBuildVersion_noBodySensorsSync()
            throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEART_RATE, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHealthDataInBackground_supportsIntent_outdatedTargetSdk_alsoGrantBodySensorsBackground()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS_BACKGROUND
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHealthDataInBackground_noIntentSupport_outdatedTargetSdk_alsoGrantBodySensorsBackground()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(false);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS_BACKGROUND
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        TEST_PACKAGE_NAME,
                        CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHealthDataInBackground_postSplitTargetSdk_noBodySensorsBackgroundSync()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS_BACKGROUND), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHealthDataInBackground_bodySensorsBackgroundNotRequested_noBodySensorsBackgroundSync()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS_BACKGROUND), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            grantHealthPermission_readHealthDataInBackground_olderPlatformVersion_noBodySensorsBackgroundSync()
                    throws PackageManager.NameNotFoundException {
        when(mTracker.supportsPermissionUsageIntent(eq(TEST_PACKAGE_NAME), eq(CURRENT_USER)))
                .thenReturn(true);
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.grantHealthPermission(
                TEST_PACKAGE_NAME, HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND, CURRENT_USER);

        verify(mPackageManager)
                .grantRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .grantRuntimePermission(any(), eq(permission.BODY_SENSORS_BACKGROUND), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeHealthPermission_readHeartRate_outdatedTargetSdk_bodySensorsRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEART_RATE,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeHealthPermission_readHeartRate_postSplitTargetSdk_bodySensorsNotRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA, new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEART_RATE,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeHealthPermission_readHeartRate_bodySensorsNotRequested_bodySensorsNotRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
        when(mPackageManager.getPermissionFlags(
                        HealthPermissions.READ_HEART_RATE, TEST_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEART_RATE,
                /* reason= */ null,
                CURRENT_USER);

        int expectedFlag =
                PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED
                        | PackageManager.FLAG_PERMISSION_USER_SET;
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(expectedFlag),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeHealthPermission_readHeartRate_olderPlatformVersion_bodySensorsNotRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEART_RATE,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeHealthPermission_readHealthDataInBackground_outdatedTargetSdk_bodySensorsBackgroundRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeHealthPermission_readHealthDataInBackground_postSplitTargetSdk_bodySensorsBackgroundNotRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeHealthPermission_readHealthDataInBackground_bodySensorsBackgroundNotRequested_bodySensorsBackgroundNotRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeHealthPermission_readHealthDataInBackground_olderPlatformVersion_bodySensorsBackgroundNotRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {permission.BODY_SENSORS_BACKGROUND});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        mPermissionHelper.revokeHealthPermission(
                TEST_PACKAGE_NAME,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                /* reason= */ null,
                CURRENT_USER);

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    public void revokeAllHealthPermissions_noPermissionsGranted() throws Exception {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE, HealthPermissions.READ_STEPS
                        },
                        new int[] {0, 0});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .isEmpty();
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, "Test reason", CURRENT_USER))
                .isFalse();
    }

    @Test
    public void revokeAllHealthPermissions_revokesHealthPermissions()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEART_RATE),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeAllHealthPermissions_noPermissionsGranted_bodySensorsRequested_bodySensorsRevoked()
                    throws Exception {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS},
                        new int[] {0, 0});
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .isEmpty();
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, "Test reason", CURRENT_USER))
                .isTrue();
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeAllHealthPermissions_outdatedTargetSdk_requestsBodySensors_bodySensorsRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(HealthPermissions.READ_HEART_RATE);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeAllHealthPermissions_postSplitTargetSdk_noBodySensorsRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeAllHealthPermissions_bodySensorNotRequested_noBodySensorsRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(HealthPermissions.READ_HEART_RATE);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeAllHealthPermissions_olderPlatform_noBodySensorsRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE, permission.BODY_SENSORS,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(HealthPermissions.READ_HEART_RATE);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeAllHealthPermissions_outdatedTargetSdk_requestsBodySensorsBackground_bodySensorsBackgroundRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS_BACKGROUND,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeAllHealthPermissions_postSplitTargetSdk_noBodySensorsBackgroundRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.BAKLAVA,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS_BACKGROUND,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeAllHealthPermissions_bodySensorBackgroundNotRequested_noBodySensorsBackgroundRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(HealthPermissions.READ_HEART_RATE);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void revokeAllHealthPermissions_olderPlatform_noBodySensorsBackgroundRevoked()
            throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            permission.BODY_SENSORS_BACKGROUND,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager, never())
                .revokeRuntimePermission(any(), eq(permission.BODY_SENSORS), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS), any(), anyInt(), anyInt(), any());
        verify(mPackageManager, never())
                .revokeRuntimePermission(
                        any(), eq(permission.BODY_SENSORS_BACKGROUND), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND), any(), anyInt(), anyInt(), any());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @EnableFlags({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void
            revokeAllHealthPermissions_outdatedTargetSdk_requestsBodySensorsAndBackground_allBodySensorsPermissionsRevoked()
                    throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo =
                getMockPackageInfo(
                        Build.VERSION_CODES.VANILLA_ICE_CREAM,
                        new String[] {
                            HealthPermissions.READ_HEART_RATE,
                            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                            permission.BODY_SENSORS,
                            permission.BODY_SENSORS_BACKGROUND,
                        },
                        new int[] {
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                            PackageInfo.REQUESTED_PERMISSION_GRANTED,
                        });
        when(mPackageManager.getPackageInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);

        assertThat(mPermissionHelper.getGrantedHealthPermissions(TEST_PACKAGE_NAME, CURRENT_USER))
                .containsExactly(
                        HealthPermissions.READ_HEART_RATE,
                        HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND);
        assertThat(
                        mPermissionHelper.revokeAllHealthPermissions(
                                TEST_PACKAGE_NAME, /* reason= */ null, CURRENT_USER))
                .isTrue();

        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
        verify(mPackageManager)
                .revokeRuntimePermission(
                        eq(TEST_PACKAGE_NAME),
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(CURRENT_USER),
                        any());
        verify(mPackageManager)
                .updatePermissionFlags(
                        eq(permission.BODY_SENSORS_BACKGROUND),
                        eq(TEST_PACKAGE_NAME),
                        anyInt(),
                        eq(PackageManager.FLAG_PERMISSION_USER_SET),
                        eq(CURRENT_USER));
    }

    private void setUpHealthPermissions() throws PackageManager.NameNotFoundException {
        PackageInfo mockPackageInfo = new PackageInfo();
        // For now add a few of the HealthPermissions just for the test.
        mockPackageInfo.permissions =
                new PermissionInfo[] {
                    createPermissionInfo(HealthPermissions.READ_HEART_RATE),
                    createPermissionInfo(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                    createPermissionInfo(HealthPermissions.READ_SKIN_TEMPERATURE),
                    createPermissionInfo(HealthPermissions.READ_OXYGEN_SATURATION),
                };
        when(mPackageManager.getPackageInfo(eq(HC_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
    }

    private PackageInfo getMockPackageInfo(int targetSdkVersion, String[] requestedPermissions) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.targetSdkVersion = targetSdkVersion;
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.requestedPermissions = requestedPermissions;
        mockPackageInfo.applicationInfo = appInfo;
        return mockPackageInfo;
    }

    private PackageInfo getMockPackageInfo(
            int targetSdkVersion, String[] requestedPermissions, int[] requestedPermissionsFlags) {
        PackageInfo mockPackageInfo = getMockPackageInfo(targetSdkVersion, requestedPermissions);
        mockPackageInfo.requestedPermissionsFlags = requestedPermissionsFlags;
        return mockPackageInfo;
    }

    private PermissionInfo createPermissionInfo(String permissionName) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.name = permissionName;
        permissionInfo.group = HealthPermissions.HEALTH_PERMISSION_GROUP;
        return permissionInfo;
    }

    private void assertNoPermissionGranted() {
        verify(mPackageManager, never()).grantRuntimePermission(any(), any(), any());
        verify(mPackageManager, never())
                .updatePermissionFlags(any(), any(), anyInt(), anyInt(), any());
    }
}
