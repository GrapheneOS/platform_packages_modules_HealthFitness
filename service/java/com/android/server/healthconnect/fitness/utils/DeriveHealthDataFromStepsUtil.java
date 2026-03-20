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

package com.android.server.healthconnect.fitness.utils;

import com.android.internal.annotations.VisibleForTesting;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to derive distance and active calories from step count.
 *
 * @hide
 */
public final class DeriveHealthDataFromStepsUtil {

    public static final double DEFAULT_WEIGHT_KG = 73.0;

    private static final double STRIDE_WALKING_A2 = 0.1057;
    private static final double STRIDE_WALKING_A1 = 0.3484;
    private static final double STRIDE_WALKING_A0 = -0.2162;
    private static final double MIN_WALKING_CADENCE = 1.4;
    private static final double MAX_WALKING_CADENCE = 2.8;
    private static final double DEFAULT_HEIGHT_METERS = 1.7;
    private static final double MIN_HEIGHT_METERS = 0.5;
    private static final double MAX_HEIGHT_METERS = 2.5;

    private static final double WALKING_MET = 3.5;
    private static final double ACSM_WALKING_HORIZONTAL_COEFFICIENT = 0.1;
    private static final double BMR_MET_COST = 1.0;
    // The caloric equivalent of 1 ml of oxygen consumed (kcal/ml O2)
    private static final double KCAL_PER_MILLILITER_OXYGEN = 0.00501;

    /** Calculates the estimated distance in meters from steps and cadence, using default height. */
    public static double calculateDistanceMeters(int steps, double cadence) {
        return steps * estimateStride(cadence, DEFAULT_HEIGHT_METERS);
    }

    /** Calculates active calories using both MET and ACSM formulas and returns the maximum. */
    public static double calculateMaxActiveCalories(
            double distanceMeters, Duration duration, double weightKg) {
        // Calculate MET-based calories
        double metCalories = calculateMetActiveCalories(duration, WALKING_MET, weightKg);

        // Calculate ACSM-based calories
        double acsmCalories = calculateAcsmActiveCalories(distanceMeters, duration, weightKg);

        return Math.max(metCalories, acsmCalories);
    }

    private static double estimateStride(double cadence, double height) {
        double clampedCadence =
                Math.max(MIN_WALKING_CADENCE, Math.min(cadence, MAX_WALKING_CADENCE));
        double clampedHeight = Math.max(MIN_HEIGHT_METERS, Math.min(height, MAX_HEIGHT_METERS));

        return STRIDE_WALKING_A2 * clampedHeight * clampedCadence
                + STRIDE_WALKING_A1 * clampedHeight
                + STRIDE_WALKING_A0;
    }

    /**
     * Calculates active calories burned using MET (Metabolic Equivalent of Task) formula, using the
     * provided weight.
     *
     * <p>It calculates the calories the user would burn using standard MET multipliers based on the
     * activity type (e.g., walking vs. running).
     *
     * <p>Formula: MET_Calories = WeightKg * DurationInHours * MET_VALUE
     *
     * <p>Adaptation: To exclude BMR (Basal Metabolic Rate) expenditure and calculate only active
     * calories, we subtract 1 (the BMR_MET_COST) from the MET_VALUE constant.
     */
    @VisibleForTesting
    static double calculateMetActiveCalories(Duration duration, double metValue, double weightKg) {
        double durationHours = (double) duration.toMillis() / TimeUnit.HOURS.toMillis(1);
        return weightKg * durationHours * (metValue - BMR_MET_COST);
    }

    /**
     * Calculates active calories burned using ACSM (American College of Sports Medicine) formula,
     * using the provided weight.
     *
     * <p>It calculates calories based on the user's actual speed and distance (which, for pedometer
     * data, is derived from the step count and stride length).
     *
     * <p>Speed (m/min) = DistanceMeters / DurationMinutes
     *
     * <p>It computes Oxygen Consumption (VO2) based on the speed. <br>
     * Walking VO2: (Speed * 0.1) + 3.5 <br>
     * Running VO2: (Speed * 0.2) + 3.5
     *
     * <p>Formula: ACSM_Calories = VO2 * DurationMinutes * WeightKg * KCAL_PER_MILLILITER_OXYGEN
     *
     * <p>Adaptation: To exclude BMR expenditure and calculate only active calories, we do not add
     * the resting component (+ 3.5) in the VO2 calculation.
     */
    @VisibleForTesting
    static double calculateAcsmActiveCalories(
            double distanceMeters, Duration duration, double weightKg) {
        double durationMinutes = (double) duration.toMillis() / TimeUnit.MINUTES.toMillis(1);
        if (durationMinutes <= 0) {
            return 0.0;
        }

        double speedMetersPerMin = distanceMeters / durationMinutes;
        double vo2Active = speedMetersPerMin * ACSM_WALKING_HORIZONTAL_COEFFICIENT;
        return vo2Active * durationMinutes * weightKg * KCAL_PER_MILLILITER_OXYGEN;
    }
}
