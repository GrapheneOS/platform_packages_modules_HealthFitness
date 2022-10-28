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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.healthconnect.HealthPermissions;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/*
 * Configuration test to check that all health permissions are defined.
 */
@RunWith(AndroidJUnit4.class)
public class HealthPermissionsPresenceTest {

    private static final Set<String> HEALTH_PERMISSIONS =
            Set.of(
                    HealthPermissions.READ_ACTIVE_CALORIES_BURNED,
                    HealthPermissions.READ_DISTANCE,
                    HealthPermissions.READ_ELEVATION_GAINED,
                    HealthPermissions.READ_EXERCISE,
                    HealthPermissions.READ_FLOORS_CLIMBED,
                    HealthPermissions.READ_STEPS,
                    HealthPermissions.READ_TOTAL_CALORIES_BURNED,
                    HealthPermissions.READ_VO2_MAX,
                    HealthPermissions.READ_WHEELCHAIR_PUSHES,
                    HealthPermissions.READ_POWER,
                    HealthPermissions.READ_SPEED,
                    HealthPermissions.READ_BASAL_METABOLIC_RATE,
                    HealthPermissions.READ_BODY_FAT,
                    HealthPermissions.READ_BODY_WATER_MASS,
                    HealthPermissions.READ_BONE_MASS,
                    HealthPermissions.READ_HEIGHT,
                    HealthPermissions.READ_HIP_CIRCUMFERENCE,
                    HealthPermissions.READ_LEAN_BODY_MASS,
                    HealthPermissions.READ_WAIST_CIRCUMFERENCE,
                    HealthPermissions.READ_WEIGHT,
                    HealthPermissions.READ_CERVICAL_MUCUS,
                    HealthPermissions.READ_MENSTRUATION,
                    HealthPermissions.READ_OVULATION_TEST,
                    HealthPermissions.READ_SEXUAL_ACTIVITY,
                    HealthPermissions.READ_HYDRATION,
                    HealthPermissions.READ_NUTRITION,
                    HealthPermissions.READ_SLEEP,
                    HealthPermissions.READ_BASAL_BODY_TEMPERATURE,
                    HealthPermissions.READ_BLOOD_GLUCOSE,
                    HealthPermissions.READ_BLOOD_PRESSURE,
                    HealthPermissions.READ_BODY_TEMPERATURE,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_HEART_RATE_VARIABILITY,
                    HealthPermissions.READ_OXYGEN_SATURATION,
                    HealthPermissions.READ_RESPIRATORY_RATE,
                    HealthPermissions.READ_RESTING_HEART_RATE,
                    HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED,
                    HealthPermissions.WRITE_DISTANCE,
                    HealthPermissions.WRITE_ELEVATION_GAINED,
                    HealthPermissions.WRITE_EXERCISE,
                    HealthPermissions.WRITE_FLOORS_CLIMBED,
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.WRITE_TOTAL_CALORIES_BURNED,
                    HealthPermissions.WRITE_VO2_MAX,
                    HealthPermissions.WRITE_WHEELCHAIR_PUSHES,
                    HealthPermissions.WRITE_POWER,
                    HealthPermissions.WRITE_SPEED,
                    HealthPermissions.WRITE_BASAL_METABOLIC_RATE,
                    HealthPermissions.WRITE_BODY_FAT,
                    HealthPermissions.WRITE_BODY_WATER_MASS,
                    HealthPermissions.WRITE_BONE_MASS,
                    HealthPermissions.WRITE_HEIGHT,
                    HealthPermissions.WRITE_HIP_CIRCUMFERENCE,
                    HealthPermissions.WRITE_LEAN_BODY_MASS,
                    HealthPermissions.WRITE_WAIST_CIRCUMFERENCE,
                    HealthPermissions.WRITE_WEIGHT,
                    HealthPermissions.WRITE_CERVICAL_MUCUS,
                    HealthPermissions.WRITE_MENSTRUATION,
                    HealthPermissions.WRITE_OVULATION_TEST,
                    HealthPermissions.WRITE_SEXUAL_ACTIVITY,
                    HealthPermissions.WRITE_HYDRATION,
                    HealthPermissions.WRITE_NUTRITION,
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.WRITE_BASAL_BODY_TEMPERATURE,
                    HealthPermissions.WRITE_BLOOD_GLUCOSE,
                    HealthPermissions.WRITE_BLOOD_PRESSURE,
                    HealthPermissions.WRITE_BODY_TEMPERATURE,
                    HealthPermissions.WRITE_HEART_RATE,
                    HealthPermissions.WRITE_HEART_RATE_VARIABILITY,
                    HealthPermissions.WRITE_OXYGEN_SATURATION,
                    HealthPermissions.WRITE_RESPIRATORY_RATE,
                    HealthPermissions.WRITE_RESTING_HEART_RATE);

    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mPackageManager = InstrumentationRegistry.getTargetContext().getPackageManager();
    }

    @Test
    public void testHealthPermissionGroup_isDefined() throws Exception {
        PermissionGroupInfo info =
                mPackageManager.getPermissionGroupInfo(
                        "android.permission-group.HEALTH", /* flags= */ 0);

        assertThat(info).isNotNull();
    }

    @Test
    public void testHealthPermissions_isDefined() throws Exception {
        for (String permissionName : HEALTH_PERMISSIONS) {
            assertHealthPermissionIsDefined(permissionName);
        }
    }

    private void assertHealthPermissionIsDefined(String permissionName) throws Exception {
        PermissionInfo info =
                mPackageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
        assertThat(info.getProtection()).isEqualTo(PermissionInfo.PROTECTION_DANGEROUS);
    }
}
