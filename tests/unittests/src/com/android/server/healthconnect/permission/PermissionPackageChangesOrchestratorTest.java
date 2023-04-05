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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PermissionPackageChangesOrchestratorTest {
    private static final String SELF_PACKAGE_NAME = "com.android.healthconnect.unittests";
    private static final UserHandle CURRENT_USER = Process.myUserHandle();
    private int mCurrentUid;
    private PermissionPackageChangesOrchestrator mOrchestrator;
    private Context mContext;

    @Mock private HealthConnectPermissionHelper mHelper;
    @Mock private HealthPermissionIntentAppsTracker mTracker;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mCurrentUid = mContext.getPackageManager().getPackageUid(SELF_PACKAGE_NAME, 0);
        mOrchestrator =
                new PermissionPackageChangesOrchestrator(mTracker, mFirstGrantTimeManager, mHelper);
        setIntentWasRemoved(/* isIntentRemoved= */ false);
    }

    @Test
    public void testPackageAdded_callsTrackerToUpdateState_noGrantTimeOrPermsCalls() {
        mOrchestrator.onReceive(mContext, buildPackageIntent(Intent.ACTION_PACKAGE_ADDED));
        verify(mTracker)
                .updateStateAndGetIfIntentWasRemoved(eq(SELF_PACKAGE_NAME), eq(CURRENT_USER));
        verify(mHelper, never())
                .revokeAllHealthPermissions(eq(SELF_PACKAGE_NAME), anyString(), eq(CURRENT_USER));
        verify(mFirstGrantTimeManager, never())
                .onPackageRemoved(eq(SELF_PACKAGE_NAME), eq(mCurrentUid), eq(CURRENT_USER));
    }

    @Test
    public void testPackageChanged_intentWasRemoved_revokesPerms() {
        setIntentWasRemoved(/* isIntentRemoved= */ true);
        mOrchestrator.onReceive(mContext, buildPackageIntent(Intent.ACTION_PACKAGE_CHANGED));
        verify(mTracker)
                .updateStateAndGetIfIntentWasRemoved(eq(SELF_PACKAGE_NAME), eq(CURRENT_USER));
        verify(mHelper)
                .revokeAllHealthPermissions(eq(SELF_PACKAGE_NAME), anyString(), eq(CURRENT_USER));
    }

    @Test
    public void testPackageRemoved_resetsGrantTime() {
        mOrchestrator.onReceive(
                mContext,
                buildPackageIntent(Intent.ACTION_PACKAGE_REMOVED, /* isReplaced= */ false));
        verify(mFirstGrantTimeManager)
                .onPackageRemoved(eq(SELF_PACKAGE_NAME), eq(mCurrentUid), eq(CURRENT_USER));
    }

    @Test
    public void testPackageReplaced_noGrantTimeResetsOrPermsRevokes() {
        mOrchestrator.onReceive(
                mContext,
                buildPackageIntent(Intent.ACTION_PACKAGE_REMOVED, /* isReplaced= */ true));
        verify(mFirstGrantTimeManager, never())
                .onPackageRemoved(eq(SELF_PACKAGE_NAME), eq(mCurrentUid), eq(CURRENT_USER));
        verify(mHelper, never())
                .revokeAllHealthPermissions(eq(SELF_PACKAGE_NAME), anyString(), eq(CURRENT_USER));
    }

    @Test
    public void testPackageReplaced_intentNotSupported_revokesPerms() {
        setIntentWasRemoved(/* isIntentRemoved= */ true);
        mOrchestrator.onReceive(
                mContext,
                buildPackageIntent(Intent.ACTION_PACKAGE_REMOVED, /* isReplaced= */ true));
        verify(mHelper)
                .revokeAllHealthPermissions(eq(SELF_PACKAGE_NAME), anyString(), eq(CURRENT_USER));
    }

    private Intent buildPackageIntent(String action) {
        return buildPackageIntent(action, /* isReplaced= */ false);
    }

    private Intent buildPackageIntent(String action, boolean isReplaced) {
        return new Intent()
                .setAction(action)
                .putExtra(Intent.EXTRA_REPLACING, isReplaced)
                .setData(Uri.parse("package:" + SELF_PACKAGE_NAME))
                .putExtra(Intent.EXTRA_UID, mCurrentUid);
    }

    private void setIntentWasRemoved(boolean isIntentRemoved) {
        when(mTracker.updateStateAndGetIfIntentWasRemoved(SELF_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(isIntentRemoved);
    }
}
