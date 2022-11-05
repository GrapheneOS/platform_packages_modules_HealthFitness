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

package android.healthconnect;

import static android.healthconnect.Constants.DEFAULT_INT;
import static android.healthconnect.HealthDataCategory.ACTIVITY;
import static android.healthconnect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.healthconnect.HealthDataCategory.CYCLE_TRACKING;
import static android.healthconnect.HealthDataCategory.VITALS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

// TODO(b/255340973): consider generate this class.
/**
 * Permissions for accessing the HealthConnect APIs.
 *
 * <p>Apps must support {@link android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} with {@link
 * android.healthconnect.HealthConnectManager#CATEGORY_HEALTH_PERMISSIONS} category to be granted
 * read/write health data permissions.
 */
public final class HealthPermissions {
    /**
     * Allows an application to grant/revoke health-related permissions.
     *
     * <p>Protection level: signature.
     *
     * @hide
     */
    @SystemApi
    public static final String MANAGE_HEALTH_PERMISSIONS =
            "android.permission.MANAGE_HEALTH_PERMISSIONS";
    /**
     * Used for runtime permissions which grant access to Health Connect data.
     *
     * @hide
     */
    @SystemApi
    public static final String HEALTH_PERMISSION_GROUP = "android.permission-group.HEALTH";
    /**
     * Allows an application to read the user's active calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_ACTIVE_CALORIES_BURNED =
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED";
    /**
     * Allows an application to read the user's distance data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_DISTANCE = "android.permission.health.READ_DISTANCE";
    /**
     * Allows an application to read the user's elevation gained data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_ELEVATION_GAINED =
            "android.permission.health.READ_ELEVATION_GAINED";
    /**
     * Allows an application to read the user's exercise data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_EXERCISE = "android.permission.health.READ_EXERCISE";
    /**
     * Allows an application to read the user's floors climbed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_FLOORS_CLIMBED =
            "android.permission.health.READ_FLOORS_CLIMBED";
    /**
     * Allows an application to read the user's steps data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_STEPS = "android.permission.health.READ_STEPS";
    /**
     * Allows an application to read the user's total calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_TOTAL_CALORIES_BURNED =
            "android.permission.health.READ_TOTAL_CALORIES_BURNED";
    /**
     * Allows an application to read the user's vo2 maximum data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_VO2_MAX = "android.permission.health.READ_VO2_MAX";
    /**
     * Allows an application to read the user's wheelchair pushes data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_WHEELCHAIR_PUSHES =
            "android.permission.health.READ_WHEELCHAIR_PUSHES";
    /**
     * Allows an application to read the user's power data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_POWER = "android.permission.health.READ_POWER";
    /**
     * Allows an application to read the user's speed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SPEED = "android.permission.health.READ_SPEED";
    /**
     * Allows an application to read the user's basal metabolic rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BASAL_METABOLIC_RATE =
            "android.permission.health.READ_BASAL_METABOLIC_RATE";
    /**
     * Allows an application to read the user's body fat data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_FAT = "android.permission.health.READ_BODY_FAT";
    /**
     * Allows an application to read the user's body water mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_WATER_MASS =
            "android.permission.health.READ_BODY_WATER_MASS";
    /**
     * Allows an application to read the user's bone mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BONE_MASS = "android.permission.health.READ_BONE_MASS";
    /**
     * Allows an application to read the user's height data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEIGHT = "android.permission.health.READ_HEIGHT";
    /**
     * Allows an application to read the user's hip circumference data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HIP_CIRCUMFERENCE =
            "android.permission.health.READ_HIP_CIRCUMFERENCE";
    /**
     * Allows an application to read the user's lean body mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_LEAN_BODY_MASS =
            "android.permission.health.READ_LEAN_BODY_MASS";
    /**
     * Allows an application to read the user's waist circumference data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_WAIST_CIRCUMFERENCE =
            "android.permission.health.READ_WAIST_CIRCUMFERENCE";
    /**
     * Allows an application to read the user's weight data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_WEIGHT = "android.permission.health.READ_WEIGHT";
    /**
     * Allows an application to read the user's cervical mucus data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_CERVICAL_MUCUS =
            "android.permission.health.READ_CERVICAL_MUCUS";
    /**
     * Allows an application to read the user's menstruation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_MENSTRUATION = "android.permission.health.READ_MENSTRUATION";
    /**
     * Allows an application to read the user's ovulation test data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_OVULATION_TEST =
            "android.permission.health.READ_OVULATION_TEST";
    /**
     * Allows an application to read the user's sexual activity data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SEXUAL_ACTIVITY =
            "android.permission.health.READ_SEXUAL_ACTIVITY";
    /**
     * Allows an application to read the user's hydration data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HYDRATION = "android.permission.health.READ_HYDRATION";
    /**
     * Allows an application to read the user's nutrition data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_NUTRITION = "android.permission.health.READ_NUTRITION";
    /**
     * Allows an application to read the user's sleep data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SLEEP = "android.permission.health.READ_SLEEP";
    /**
     * Allows an application to read the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BASAL_BODY_TEMPERATURE =
            "android.permission.health.READ_BASAL_BODY_TEMPERATURE";
    /**
     * Allows an application to read the user's blood glucose data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BLOOD_GLUCOSE = "android.permission.health.READ_BLOOD_GLUCOSE";
    /**
     * Allows an application to read the user's blood pressure data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BLOOD_PRESSURE =
            "android.permission.health.READ_BLOOD_PRESSURE";
    /**
     * Allows an application to read the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_TEMPERATURE =
            "android.permission.health.READ_BODY_TEMPERATURE";
    /**
     * Allows an application to read the user's heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEART_RATE = "android.permission.health.READ_HEART_RATE";
    /**
     * Allows an application to read the user's heart rate variability data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEART_RATE_VARIABILITY =
            "android.permission.health.READ_HEART_RATE_VARIABILITY";
    /**
     * Allows an application to read the user's oxygen saturation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_OXYGEN_SATURATION =
            "android.permission.health.READ_OXYGEN_SATURATION";
    /**
     * Allows an application to read the user's respiratory rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_RESPIRATORY_RATE =
            "android.permission.health.READ_RESPIRATORY_RATE";
    /**
     * Allows an application to read the user's resting heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_RESTING_HEART_RATE =
            "android.permission.health.READ_RESTING_HEART_RATE";
    /**
     * Allows an application to write the user's calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_ACTIVE_CALORIES_BURNED =
            "android.permission.health.WRITE_ACTIVE_CALORIES_BURNED";
    /**
     * Allows an application to write the user's distance data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_DISTANCE = "android.permission.health.WRITE_DISTANCE";
    /**
     * Allows an application to write the user's elevation gained data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_ELEVATION_GAINED =
            "android.permission.health.WRITE_ELEVATION_GAINED";
    /**
     * Allows an application to write the user's exercise data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_EXERCISE = "android.permission.health.WRITE_EXERCISE";
    /**
     * Allows an application to write the user's floors climbed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_FLOORS_CLIMBED =
            "android.permission.health.WRITE_FLOORS_CLIMBED";
    /**
     * Allows an application to write the user's steps data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_STEPS = "android.permission.health.WRITE_STEPS";
    /**
     * Allows an application to write the user's total calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_TOTAL_CALORIES_BURNED =
            "android.permission.health.WRITE_TOTAL_CALORIES_BURNED";
    /**
     * Allows an application to write the user's vo2 maximum data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_VO2_MAX = "android.permission.health.WRITE_VO2_MAX";
    /**
     * Allows an application to write the user's wheelchair pushes data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_WHEELCHAIR_PUSHES =
            "android.permission.health.WRITE_WHEELCHAIR_PUSHES";
    /**
     * Allows an application to write the user's power data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_POWER = "android.permission.health.WRITE_POWER";
    /**
     * Allows an application to write the user's speed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SPEED = "android.permission.health.WRITE_SPEED";
    /**
     * Allows an application to write the user's basal metabolic rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BASAL_METABOLIC_RATE =
            "android.permission.health.WRITE_BASAL_METABOLIC_RATE";
    /**
     * Allows an application to write the user's body fat data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_FAT = "android.permission.health.WRITE_BODY_FAT";
    /**
     * Allows an application to write the user's body water mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_WATER_MASS =
            "android.permission.health.WRITE_BODY_WATER_MASS";
    /**
     * Allows an application to write the user's bone mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BONE_MASS = "android.permission.health.WRITE_BONE_MASS";
    /**
     * Allows an application to write the user's height data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEIGHT = "android.permission.health.WRITE_HEIGHT";
    /**
     * Allows an application to write the user's hip circumference data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HIP_CIRCUMFERENCE =
            "android.permission.health.WRITE_HIP_CIRCUMFERENCE";
    /**
     * Allows an application to write the user's lean body mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_LEAN_BODY_MASS =
            "android.permission.health.WRITE_LEAN_BODY_MASS";
    /**
     * Allows an application to write the user's waist circumference data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_WAIST_CIRCUMFERENCE =
            "android.permission.health.WRITE_WAIST_CIRCUMFERENCE";
    /**
     * Allows an application to write the user's weight data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_WEIGHT = "android.permission.health.WRITE_WEIGHT";
    /**
     * Allows an application to write the user's cervical mucus data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_CERVICAL_MUCUS =
            "android.permission.health.WRITE_CERVICAL_MUCUS";
    /**
     * Allows an application to write the user's menstruation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_MENSTRUATION = "android.permission.health.WRITE_MENSTRUATION";
    /**
     * Allows an application to write the user's ovulation test data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_OVULATION_TEST =
            "android.permission.health.WRITE_OVULATION_TEST";
    /**
     * Allows an application to write the user's sexual activity data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SEXUAL_ACTIVITY =
            "android.permission.health.WRITE_SEXUAL_ACTIVITY";
    /**
     * Allows an application to write the user's hydration data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HYDRATION = "android.permission.health.WRITE_HYDRATION";
    /**
     * Allows an application to write the user's nutrition data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_NUTRITION = "android.permission.health.WRITE_NUTRITION";
    /**
     * Allows an application to write the user's sleep data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SLEEP = "android.permission.health.WRITE_SLEEP";
    /**
     * Allows an application to write the user's basal body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BASAL_BODY_TEMPERATURE =
            "android.permission.health.WRITE_BASAL_BODY_TEMPERATURE";
    /**
     * Allows an application to write the user's blood glucose data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BLOOD_GLUCOSE =
            "android.permission.health.WRITE_BLOOD_GLUCOSE";
    /**
     * Allows an application to write the user's blood pressure data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BLOOD_PRESSURE =
            "android.permission.health.WRITE_BLOOD_PRESSURE";
    /**
     * Allows an application to write the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_TEMPERATURE =
            "android.permission.health.WRITE_BODY_TEMPERATURE";
    /**
     * Allows an application to write the user's heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEART_RATE = "android.permission.health.WRITE_HEART_RATE";
    /**
     * Allows an application to write the user's heart rate variability data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEART_RATE_VARIABILITY =
            "android.permission.health.WRITE_HEART_RATE_VARIABILITY";
    /**
     * Allows an application to write the user's oxygen saturation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_OXYGEN_SATURATION =
            "android.permission.health.WRITE_OXYGEN_SATURATION";
    /**
     * Allows an application to write the user's respiratory rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_RESPIRATORY_RATE =
            "android.permission.health.WRITE_RESPIRATORY_RATE";
    /**
     * Allows an application to write the user's resting heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_RESTING_HEART_RATE =
            "android.permission.health.WRITE_RESTING_HEART_RATE";

    private static final Set<String> sWritePermissionsSet =
            new ArraySet<>(
                    Set.of(
                            WRITE_ACTIVE_CALORIES_BURNED,
                            WRITE_DISTANCE,
                            WRITE_ELEVATION_GAINED,
                            WRITE_EXERCISE,
                            WRITE_FLOORS_CLIMBED,
                            WRITE_STEPS,
                            WRITE_TOTAL_CALORIES_BURNED,
                            WRITE_VO2_MAX,
                            WRITE_WHEELCHAIR_PUSHES,
                            WRITE_POWER,
                            WRITE_SPEED,
                            WRITE_BASAL_METABOLIC_RATE,
                            WRITE_BODY_FAT,
                            WRITE_BODY_WATER_MASS,
                            WRITE_BONE_MASS,
                            WRITE_HEIGHT,
                            WRITE_HIP_CIRCUMFERENCE,
                            WRITE_LEAN_BODY_MASS,
                            WRITE_WAIST_CIRCUMFERENCE,
                            WRITE_WEIGHT,
                            WRITE_CERVICAL_MUCUS,
                            WRITE_MENSTRUATION,
                            WRITE_OVULATION_TEST,
                            WRITE_SEXUAL_ACTIVITY,
                            WRITE_HYDRATION,
                            WRITE_NUTRITION,
                            WRITE_SLEEP,
                            WRITE_BASAL_BODY_TEMPERATURE,
                            WRITE_BLOOD_GLUCOSE,
                            WRITE_BLOOD_PRESSURE,
                            WRITE_BODY_TEMPERATURE,
                            WRITE_HEART_RATE,
                            WRITE_HEART_RATE_VARIABILITY,
                            WRITE_OXYGEN_SATURATION,
                            WRITE_RESPIRATORY_RATE,
                            WRITE_RESTING_HEART_RATE));

    private static final Map<String, Integer> sWriteHealthPermissionToHealthDataCategoryMap =
            new ArrayMap<>();

    private static final Map<Integer, String[]> sDataCategoryToWritePermissionsMap =
            new ArrayMap<>();

    private HealthPermissions() {}

    /**
     * @return true if {@code permissionName} is a write-permission
     * @hide
     */
    public static boolean isWritePermission(@NonNull String permissionName) {
        Objects.requireNonNull(permissionName);

        return sWritePermissionsSet.contains(permissionName);
    }

    /**
     * @return {@link HealthDataCategory} for {@code permissionName}. -1 if permission category for
     *     {@code permissionName} is not found
     * @hide
     */
    @HealthDataCategory.Type
    public static int getHealthDataCategory(@Nullable String permissionName) {
        if (sWriteHealthPermissionToHealthDataCategoryMap.isEmpty()) {
            populateWriteHealthPermissionToHealthDataCategoryMap();
        }

        return sWriteHealthPermissionToHealthDataCategoryMap.getOrDefault(
                permissionName, DEFAULT_INT);
    }

    /**
     * @return {@link HealthDataCategory} for {@code permissionName}. -1 if permission category for
     *     {@code permissionName} is not found
     * @hide
     */
    public static String[] getWriteHealthPermissionsFor(@HealthDataCategory.Type int dataCategory) {
        if (sDataCategoryToWritePermissionsMap.isEmpty()) {
            populateWriteHealthPermissionToHealthDataCategoryMap();
        }

        return sDataCategoryToWritePermissionsMap.getOrDefault(dataCategory, new String[] {});
    }

    private static synchronized void populateWriteHealthPermissionToHealthDataCategoryMap() {
        if (!sWriteHealthPermissionToHealthDataCategoryMap.isEmpty()) {
            return;
        }

        // Write permissions
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_ACTIVE_CALORIES_BURNED, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_DISTANCE, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_ELEVATION_GAINED, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_EXERCISE, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_FLOORS_CLIMBED, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_STEPS, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_TOTAL_CALORIES_BURNED, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_VO2_MAX, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_WHEELCHAIR_PUSHES, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_POWER, ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_SPEED, ACTIVITY);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BASAL_METABOLIC_RATE, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BODY_FAT, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BODY_WATER_MASS, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BONE_MASS, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_HEIGHT, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HIP_CIRCUMFERENCE, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_LEAN_BODY_MASS, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_WAIST_CIRCUMFERENCE, BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_WEIGHT, BODY_MEASUREMENTS);

        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_CERVICAL_MUCUS, CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_MENSTRUATION, CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_OVULATION_TEST, CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_SEXUAL_ACTIVITY, CYCLE_TRACKING);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HYDRATION, HealthDataCategory.NUTRITION);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_NUTRITION, HealthDataCategory.NUTRITION);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_SLEEP, HealthDataCategory.SLEEP);

        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BASAL_BODY_TEMPERATURE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BLOOD_GLUCOSE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BLOOD_PRESSURE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_BODY_TEMPERATURE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_HEART_RATE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_HEART_RATE_VARIABILITY, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_OXYGEN_SATURATION, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_RESPIRATORY_RATE, VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_RESTING_HEART_RATE, VITALS);

        sDataCategoryToWritePermissionsMap.put(
                ACTIVITY,
                new String[] {
                    WRITE_ACTIVE_CALORIES_BURNED,
                    WRITE_DISTANCE,
                    WRITE_ELEVATION_GAINED,
                    WRITE_EXERCISE,
                    WRITE_FLOORS_CLIMBED,
                    WRITE_STEPS,
                    WRITE_TOTAL_CALORIES_BURNED,
                    WRITE_VO2_MAX,
                    WRITE_WHEELCHAIR_PUSHES,
                    WRITE_POWER,
                    WRITE_SPEED
                });

        sDataCategoryToWritePermissionsMap.put(
                BODY_MEASUREMENTS,
                new String[] {
                    WRITE_BASAL_METABOLIC_RATE,
                    WRITE_BODY_FAT,
                    WRITE_BODY_WATER_MASS,
                    WRITE_BONE_MASS,
                    WRITE_HEIGHT,
                    WRITE_HIP_CIRCUMFERENCE,
                    WRITE_LEAN_BODY_MASS,
                    WRITE_WAIST_CIRCUMFERENCE,
                    WRITE_WEIGHT
                });

        sDataCategoryToWritePermissionsMap.put(
                CYCLE_TRACKING,
                new String[] {
                    WRITE_CERVICAL_MUCUS,
                    WRITE_MENSTRUATION,
                    WRITE_OVULATION_TEST,
                    WRITE_SEXUAL_ACTIVITY
                });

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.NUTRITION, new String[] {WRITE_HYDRATION, WRITE_NUTRITION});

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.SLEEP, new String[] {WRITE_SLEEP});

        sDataCategoryToWritePermissionsMap.put(
                VITALS,
                new String[] {
                    WRITE_BASAL_BODY_TEMPERATURE,
                    WRITE_BLOOD_GLUCOSE,
                    WRITE_BLOOD_PRESSURE,
                    WRITE_BODY_TEMPERATURE,
                    WRITE_HEART_RATE,
                    WRITE_HEART_RATE_VARIABILITY,
                    WRITE_OXYGEN_SATURATION,
                    WRITE_RESPIRATORY_RATE,
                    WRITE_RESTING_HEART_RATE
                });
    }
}
