/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.tests.withoutmanagepermissions;

import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.InstrumentationRegistry;

import android.healthconnect.HealthConnectManager;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for {@link android.healthconnect.HealthConnectManager} Permission-related APIs.
 *
 * <p><b>Note:</b> These tests operate while <b>not</b> holding {@link
 * android.healthconnect.Constants.MANAGE_HEALTH_PERMISSIONS_NAME}. For tests asserting that the API
 * behaves as expected for holders of the permission, please see {@link
 * android.healthconnect.tests.withmanagepermissions.HealthConnectWithManagePermissionsTest}.
 *
 * <p><b>Build/Install/Run:</b> atest
 * HealthConnectPermissionWithoutManagePermissionsIntegrationTests
 */
@RunWith(AndroidJUnit4.class)
public class HealthConnectWithoutManagePermissionsTest {
    private static final String DEFAULT_APP_PACKAGE = "android.healthconnect.test.app";
    // TODO(b/251787336): Modify these to actual health permissions once they have been defined.
    private static final String DEFAULT_PERM = "android.permission.health.TEST_PERM";

    private Context mContext;
    private HealthConnectManager mHealthConnectManager;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mHealthConnectManager = mContext.getSystemService(HealthConnectManager.class);
    }

    @Test(expected = SecurityException.class)
    public void testGrantHealthPermission_noManageHealthPermissions_throwsSecurityException()
            throws Exception {
        mHealthConnectManager.grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        fail(
                "Expected SecurityException due to not holding"
                        + "android.permission.MANAGE_HEALTH_PERMISSIONS.");
    }

    @Test(expected = SecurityException.class)
    public void testRevokeHealthPermission_noManageHealthPermissions_throwsSecurityException()
            throws Exception {
        mHealthConnectManager.revokeHealthPermission(
                DEFAULT_APP_PACKAGE, DEFAULT_PERM, /* reason= */ null);
        fail(
                "Expected SecurityException due to not holding"
                        + "android.permission.MANAGE_HEALTH_PERMISSIONS.");
    }

    @Test(expected = SecurityException.class)
    public void testGetGrantedHealthPermissions_noManageHealthPermissions_throwsSecurityException()
            throws Exception {
        mHealthConnectManager.getGrantedHealthPermissions(DEFAULT_APP_PACKAGE);
        fail(
                "Expected SecurityException due to not holding"
                        + "android.permission.MANAGE_HEALTH_PERMISSIONS.");
    }

    @Test(expected = SecurityException.class)
    public void testRevokeAllHealthPermissions_noManageHealthPermissions_throwsSecurityException()
            throws Exception {
        mHealthConnectManager.revokeAllHealthPermissions(DEFAULT_APP_PACKAGE, /* reason= */ null);
        fail(
                "Expected SecurityException due to not holding"
                        + "android.permission.MANAGE_HEALTH_PERMISSIONS.");
    }
}
