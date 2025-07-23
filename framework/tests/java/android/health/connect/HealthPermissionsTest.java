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

package android.health.connect;

import static android.health.connect.HealthPermissions.READ_EXERCISE_ROUTE;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_DEVICES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PREGNANCY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PROCEDURES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VISITS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.HealthPermissions.isValidHealthPermission;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(ParameterizedAndroidJunit4.class)
public class HealthPermissionsTest {
    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(Flags.FLAG_DEVICE_RESOURCE);
    }

    private static final String FAIL_MESSAGE =
            "Add new health permission to ALL_EXPECTED_HEALTH_PERMISSIONS and "
                    + " android.healthconnect.cts.HealthPermissionsPresenceTest.HEALTH_PERMISSIONS "
                    + "sets.";

    private static final String PHR_FAIL_MESSAGE =
            "Add new medical permissions to ALL_EXPECTED_HEALTH_PERMISSIONS and "
                    + "apk/HealthPermissionsManifest.xml.";

    private static final String MEDICAL_PERMISSION_IDENTIFIER = "MEDICAL_DATA";

    // Add new health permission to ALL_EXPECTED_HEALTH_PERMISSIONS and
    // {@link android.healthconnect.cts.HealthPermissionsPresenceTest.HEALTH_PERMISSIONS}
    // sets.
    private static final Set<String> ALL_EXPECTED_HEALTH_PERMISSIONS =
            Stream.<Stream<String>>of(
                            Stream.of(
                                    HealthPermissions.READ_HEALTH_DATA_HISTORY,
                                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                                    HealthPermissions.READ_ACTIVE_CALORIES_BURNED,
                                    HealthPermissions.READ_DISTANCE,
                                    HealthPermissions.READ_ELEVATION_GAINED,
                                    HealthPermissions.READ_EXERCISE,
                                    HealthPermissions.READ_EXERCISE_ROUTES,
                                    HealthPermissions.READ_PLANNED_EXERCISE,
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
                                    HealthPermissions.READ_LEAN_BODY_MASS,
                                    HealthPermissions.READ_WEIGHT,
                                    HealthPermissions.READ_CERVICAL_MUCUS,
                                    HealthPermissions.READ_INTERMENSTRUAL_BLEEDING,
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
                                    HealthPermissions.READ_SKIN_TEMPERATURE,
                                    HealthPermissions.READ_MINDFULNESS,
                                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                                    HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS,
                                    HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS,
                                    HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS,
                                    HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                                    HealthPermissions.READ_MEDICAL_DATA_PREGNANCY,
                                    HealthPermissions.READ_MEDICAL_DATA_PROCEDURES,
                                    HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY,
                                    HealthPermissions.READ_MEDICAL_DATA_VACCINES,
                                    HealthPermissions.READ_MEDICAL_DATA_VISITS,
                                    HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS,
                                    HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED,
                                    HealthPermissions.WRITE_DISTANCE,
                                    HealthPermissions.WRITE_ELEVATION_GAINED,
                                    HealthPermissions.WRITE_EXERCISE,
                                    HealthPermissions.WRITE_EXERCISE_ROUTE,
                                    HealthPermissions.WRITE_PLANNED_EXERCISE,
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
                                    HealthPermissions.WRITE_LEAN_BODY_MASS,
                                    HealthPermissions.WRITE_WEIGHT,
                                    HealthPermissions.WRITE_CERVICAL_MUCUS,
                                    HealthPermissions.WRITE_INTERMENSTRUAL_BLEEDING,
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
                                    HealthPermissions.WRITE_RESTING_HEART_RATE,
                                    HealthPermissions.WRITE_SKIN_TEMPERATURE,
                                    HealthPermissions.WRITE_MINDFULNESS,
                                    HealthPermissions.WRITE_MEDICAL_DATA),
                            Flags.activityIntensity()
                                    ? Stream.of(
                                            HealthPermissions.READ_ACTIVITY_INTENSITY,
                                            HealthPermissions.WRITE_ACTIVITY_INTENSITY)
                                    : Stream.of(),
                            Flags.alcoholConsumption()
                                    ? Stream.of(
                                            HealthPermissions.READ_ALCOHOL_CONSUMPTION,
                                            HealthPermissions.WRITE_ALCOHOL_CONSUMPTION)
                                    : Stream.of(),
                            Flags.deviceResource()
                                    ? Stream.of(HealthPermissions.READ_MEDICAL_DATA_DEVICES)
                                    : Stream.of())
                    .flatMap(s -> s)
                    .collect(Collectors.toSet());
    private PackageManager mPackageManager;
    private Context mContext;
    @Mock private PackageInfo mPackageInfo1;

    @Rule public final SetFlagsRule mSetFlagsRule;
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    public HealthPermissionsTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mPackageManager = mContext.getPackageManager();
    }

    @Test
    public void testHealthGroupPermissions_noUnexpectedPermissionsDefined() throws Exception {
        PermissionInfo[] permissionInfos = getHealthPermissionInfos();
        for (PermissionInfo permissionInfo : permissionInfos) {
            if (isValidHealthPermission(permissionInfo)) {
                assertWithMessage(FAIL_MESSAGE)
                        .that(ALL_EXPECTED_HEALTH_PERMISSIONS)
                        .contains(permissionInfo.name);
            }
        }
    }

    @Test
    public void testHealthConnectManager_noUnexpectedPermissionsReturned() throws Exception {
        assertWithMessage(FAIL_MESSAGE)
                .that(HealthConnectManager.getHealthPermissions(mContext))
                .isEqualTo(ALL_EXPECTED_HEALTH_PERMISSIONS);
    }

    @Test
    public void testReadExerciseRoutePerm_hasSignatureProtection() throws Exception {
        assertThat(
                        mPackageManager
                                .getPermissionInfo(READ_EXERCISE_ROUTE, /* flags= */ 0)
                                .getProtection())
                .isEqualTo(PermissionInfo.PROTECTION_SIGNATURE);
    }

    @Test
    public void testReadExerciseRoutePerm_controllerUiHoldsIt() throws Exception {
        String healthControllerPackageName = getControllerUiPackageName();
        assertThat(
                        mContext.getPackageManager()
                                .checkPermission(READ_EXERCISE_ROUTE, healthControllerPackageName))
                .isEqualTo(PackageManager.PERMISSION_GRANTED);
    }

    @Test
    public void testGetDataCategoriesWithWritePermissionsForPackage_returnsCorrectSet() {
        mPackageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        mPackageInfo1.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        Set<Integer> expectedResult =
                Set.of(HealthDataCategory.ACTIVITY, HealthDataCategory.CYCLE_TRACKING);
        Set<Integer> actualResult =
                HealthPermissions.getDataCategoriesWithWritePermissionsForPackage(
                        mPackageInfo1, mContext);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void
            testPackageHasWriteHealthPermissionsForCategory_ifNoWritePermissions_returnsFalse() {
        mPackageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_EXERCISE,
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        mPackageInfo1.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        assertThat(
                        HealthPermissions.getPackageHasWriteHealthPermissionsForCategory(
                                mPackageInfo1, HealthDataCategory.SLEEP, mContext))
                .isFalse();
    }

    @Test
    public void testPackageHasWriteHealthPermissionsForCategory_ifWritePermissions_returnsTrue() {
        mPackageInfo1.requestedPermissions =
                new String[] {
                    HealthPermissions.WRITE_STEPS,
                    HealthPermissions.WRITE_SLEEP,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.WRITE_OVULATION_TEST
                };
        mPackageInfo1.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    0,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };

        assertThat(
                        HealthPermissions.getPackageHasWriteHealthPermissionsForCategory(
                                mPackageInfo1, HealthDataCategory.ACTIVITY, mContext))
                .isTrue();
    }

    @Test
    public void testGetMedicalPermissions_returnsValidPermissions() {
        Set<String> permissions = HealthPermissions.getAllMedicalPermissions();

        HashSet<String> expectedPermissions =
                Stream.of(
                                WRITE_MEDICAL_DATA,
                                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                                READ_MEDICAL_DATA_CONDITIONS,
                                READ_MEDICAL_DATA_LABORATORY_RESULTS,
                                READ_MEDICAL_DATA_MEDICATIONS,
                                READ_MEDICAL_DATA_PERSONAL_DETAILS,
                                READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                                READ_MEDICAL_DATA_PREGNANCY,
                                READ_MEDICAL_DATA_PROCEDURES,
                                READ_MEDICAL_DATA_SOCIAL_HISTORY,
                                READ_MEDICAL_DATA_VACCINES,
                                READ_MEDICAL_DATA_VISITS,
                                READ_MEDICAL_DATA_VITAL_SIGNS)
                        .collect(Collectors.toCollection(HashSet::new));
        if (Flags.deviceResource()) {
            expectedPermissions.add(READ_MEDICAL_DATA_DEVICES);
        }
        assertThat(permissions).containsAtLeastElementsIn(expectedPermissions);
    }

    @Test
    public void testGetMedicalPermissions_returnsAllMedicalPermissions() throws Exception {
        Set<String> permissions = HealthPermissions.getAllMedicalPermissions();

        PermissionInfo[] permissionInfos = getHealthPermissionInfos();
        for (PermissionInfo permissionInfo : permissionInfos) {
            if (isValidHealthPermission(permissionInfo)
                    && permissionInfo.name.contains(MEDICAL_PERMISSION_IDENTIFIER)) {
                assertWithMessage(PHR_FAIL_MESSAGE).that(permissions).contains(permissionInfo.name);
            }
        }
    }

    private PermissionInfo[] getHealthPermissionInfos() throws Exception {
        String healthControllerPackageName = getControllerUiPackageName();

        PackageInfo packageInfo =
                mPackageManager.getPackageInfo(
                        healthControllerPackageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        return packageInfo.permissions;
    }

    private String getControllerUiPackageName() throws Exception {
        return mPackageManager.getPermissionInfo(HealthPermissions.READ_STEPS, /* flags= */ 0)
                .packageName;
    }
}
