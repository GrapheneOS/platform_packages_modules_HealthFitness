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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiAutomation;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

public class FirstGrantTimeUnitTest {

    private static final String SELF_PACKAGE_NAME = "com.android.healthconnect.unittests";
    private static final UserHandle CURRENT_USER = Process.myUserHandle();

    @Mock private HealthPermissionIntentAppsTracker mTracker;

    private FirstGrantTimeManager mGrantTimeManager;

    private final UiAutomation mUiAutomation =
            InstrumentationRegistry.getInstrumentation().getUiAutomation();

    @Mock private FirstGrantTimeDatastore mDatastore;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getContext();
        MockitoAnnotations.initMocks(this);
        when(mDatastore.readForUser(CURRENT_USER)).thenReturn(new UserGrantTimeState(1));

        mUiAutomation.adoptShellPermissionIdentity(
                "android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS");
        mGrantTimeManager = new FirstGrantTimeManager(context, mTracker, mDatastore);
        mUiAutomation.dropShellPermissionIdentity();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownPackage_throwsException() {
        mGrantTimeManager.getFirstGrantTime("android.unknown_package", CURRENT_USER);
    }

    @Test
    public void testCurrentPackage_intentNotSupported_grantTimeIsNull() {
        when(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(false);
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER)).isNull();
    }

    @Test
    public void testCurrentPackage_intentSupported_grantTimeIsNotNull() {
        when(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(true);
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isNotNull();
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isGreaterThan(Instant.now().minusSeconds((long) 1e3));
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isLessThan(Instant.now().plusSeconds((long) 1e3));
        verify(mDatastore).writeForUser(ArgumentMatchers.any(), ArgumentMatchers.eq(CURRENT_USER));
        verify(mDatastore).readForUser(CURRENT_USER);
    }
}
