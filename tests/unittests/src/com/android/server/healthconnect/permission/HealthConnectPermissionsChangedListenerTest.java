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

package com.android.server.healthconnect.permission;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class HealthConnectPermissionsChangedListenerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private HealthConnectPermissionsChangedListener mHealthConnectPermissionsChangedListener;
    @Mock FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock Context mContext;
    @Mock Context mProfileContext;
    @Mock PackageManager mPackageManager;
    @Mock UserManager mUserManager;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getUser()).thenReturn(Process.myUserHandle());

        mHealthConnectPermissionsChangedListener =
                new HealthConnectPermissionsChangedListener(mContext, mFirstGrantTimeManager);
    }

    @Test
    public void registerPermissionsChangeListener_testListenerRegistered() {
        mHealthConnectPermissionsChangedListener.registerPermissionsChangeListener();

        verify(mPackageManager, times(1))
                .addOnPermissionsChangeListener(eq(mHealthConnectPermissionsChangedListener));
    }

    @Test
    public void onPermissionsChanged_testUpdateFirstGrantTimesFromPermissionStateInvoked() {
        UserHandle currentUser = Process.myUserHandle();
        int uid = Process.myUid();
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mHealthConnectPermissionsChangedListener.onPermissionsChanged(uid);

        verify(mFirstGrantTimeManager, times(1))
                .updateFirstGrantTimesFromPermissionState(eq(currentUser), eq(uid), eq(false));
    }

    @Test
    public void onPermissionsChanged_ignoresProfiles() {
        UserHandle profile = UserHandle.of(999);
        int uid = profile.getUid(/* appId= */ 1);
        when(mContext.createContextAsUser(profile, /* flags= */ 0)).thenReturn(mProfileContext);
        when(mProfileContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isProfile()).thenReturn(true);

        mHealthConnectPermissionsChangedListener.onPermissionsChanged(uid);

        verifyNoInteractions(mFirstGrantTimeManager);
    }
}
