/*
 * Copyright (C) 2026 The Android Open Source Project
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
package android.healthconnect.testing.integration;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_PERMISSIONS;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.health.connect.HealthConnectManager;

import androidx.test.core.app.ApplicationProvider;

import java.util.List;

/** Helper class able to call hidden HealthConnectManager APIs */
public class IntegrationTestUtils {
    /** Calls {@link HealthConnectManager#grantHealthPermissions} with shell permission identity. */
    public static List<String> grantHealthPermissions(
            String packageName, List<String> permissionNames) {
        try {
            return runWithShellPermissionIdentity(
                    () ->
                            getHealthConnectManager()
                                    .grantHealthPermissions(packageName, permissionNames),
                    MANAGE_HEALTH_PERMISSIONS);
        } catch (RuntimeException e) {
            // runWithShellPermissionIdentity wraps and rethrows all exceptions as RuntimeException,
            // but we need the original RuntimeException if there is one.
            final Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        }
    }

    private static HealthConnectManager getHealthConnectManager() {
        return getHealthConnectManager(ApplicationProvider.getApplicationContext());
    }

    private static HealthConnectManager getHealthConnectManager(Context context) {
        return requireNonNull(context.getSystemService(HealthConnectManager.class));
    }
}
