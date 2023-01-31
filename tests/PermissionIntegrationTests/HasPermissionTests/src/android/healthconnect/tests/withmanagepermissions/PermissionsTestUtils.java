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

package android.healthconnect.tests.withmanagepermissions;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;

public class PermissionsTestUtils {

    /**
     * Skips test if the test app doesn't hold {@link HealthPermissions.MANAGE_HEALTH_PERMISSIONS}.
     * Should be called from the test or test setup methods.
     *
     * <p>{@link HealthPermissions.MANAGE_HEALTH_PERMISSIONS} is protected at signature level, and
     * should be granted automatically if it's possible to hold. This method simply check if test
     * package holds the permission.
     */
    public static void assumeHoldManageHealthPermissionsPermission(Context context) {
        assumeTrue(
                "Skipping test - test cannot hold "
                        + HealthPermissions.MANAGE_HEALTH_PERMISSIONS
                        + " for the build-under-test. This is likely because the build uses "
                        + "mainline prebuilts and therefore does not have a compatible signature "
                        + "with this test app. See the test class Javadoc for more info.",
                context.checkSelfPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
                        == PackageManager.PERMISSION_GRANTED);
    }
}
