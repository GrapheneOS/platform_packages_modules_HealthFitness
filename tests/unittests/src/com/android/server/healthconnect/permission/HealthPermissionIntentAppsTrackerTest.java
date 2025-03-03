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

package com.android.server.healthconnect.permission;

import static com.android.healthfitness.flags.Flags.FLAG_PERMISSION_TRACKER_FIX_MAPPING_INIT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class HealthPermissionIntentAppsTrackerTest {
    private static final String SELF_PACKAGE_NAME = "com.android.healthconnect.unittests";
    private static final UserHandle CURRENT_USER = Process.myUserHandle();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(eq(UserManager.class))).thenReturn(mUserManager);
        when(mUserManager.getUserHandles(anyBoolean())).thenReturn(List.of(CURRENT_USER));
    }

    @Test
    public void testCheckPackage_packageSupportsIntent_returnsTrue() {
        setSelfIntentSupport(/* intentSupported= */ true);
        HealthPermissionIntentAppsTracker mTracker =
                new HealthPermissionIntentAppsTracker(mContext);
        mTracker.setupForUser(CURRENT_USER);
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isTrue();
    }

    @Test
    public void testCheckPackage_packageDoesntSupportIntent_returnsFalse() {
        setSelfIntentSupport(/* intentSupported= */ false);
        HealthPermissionIntentAppsTracker mTracker =
                new HealthPermissionIntentAppsTracker(mContext);
        mTracker.setupForUser(CURRENT_USER);
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isFalse();
    }

    @Test
    @EnableFlags({FLAG_PERMISSION_TRACKER_FIX_MAPPING_INIT})
    public void testCheckPackage_userNotInit_packageSupportsIntent_returnsTrue() {
        setSelfIntentSupport(/* intentSupported= */ true);
        HealthPermissionIntentAppsTracker mTracker =
                new HealthPermissionIntentAppsTracker(mContext);
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isTrue();
    }

    @Test
    public void testUpdatePackageState_packageSupportIntentRemoved_returnsRemovedIntentTrue() {
        setSelfIntentSupport(/* intentSupported= */ true);
        HealthPermissionIntentAppsTracker mTracker =
                new HealthPermissionIntentAppsTracker(mContext);
        mTracker.setupForUser(CURRENT_USER);
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isTrue();
        setSelfIntentSupport(/* intentSupported= */ false);
        // This call doesn't update the cache and continues returning true.
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isTrue();
        assertThat(mTracker.updateAndGetSupportsPackageUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isFalse();
        assertThat(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .isFalse();
    }

    private void setSelfIntentSupport(boolean intentSupported) {
        when(mPackageManager.queryIntentActivitiesAsUser(any(), any(), eq(CURRENT_USER)))
                .thenReturn(intentSupported ? List.of(new ResolveInfo()) : Collections.emptyList());
    }
}
