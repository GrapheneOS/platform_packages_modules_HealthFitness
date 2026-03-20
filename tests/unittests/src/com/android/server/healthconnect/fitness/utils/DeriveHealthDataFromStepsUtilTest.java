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

import static com.android.server.healthconnect.fitness.utils.DeriveHealthDataFromStepsUtil.DEFAULT_WEIGHT_KG;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;

@RunWith(JUnit4.class)
public class DeriveHealthDataFromStepsUtilTest {

    @Test
    public void testCalculateDistanceMeters_normalCadence() {
        int steps = 1000;
        double cadence = 1.8;

        double distance = DeriveHealthDataFromStepsUtil.calculateDistanceMeters(steps, cadence);

        assertThat(distance).isWithin(0.01).of(699.52);
    }

    @Test
    public void testCalculateDistanceMeters_clampedCadenceHigh() {
        int steps = 1000;
        double cadence = 10.0; // Above 2.8

        double distance = DeriveHealthDataFromStepsUtil.calculateDistanceMeters(steps, cadence);

        assertThat(distance).isWithin(0.01).of(879.21);
    }

    @Test
    public void testCalculateDistanceMeters_clampedCadenceLow() {
        int steps = 1000;
        double cadence = 0.5; // Below 1.4

        double distance = DeriveHealthDataFromStepsUtil.calculateDistanceMeters(steps, cadence);

        assertThat(distance).isWithin(0.01).of(627.65);
    }

    @Test
    public void testCalculateDistanceMeters_zeroSteps_returnsZero() {
        assertThat(DeriveHealthDataFromStepsUtil.calculateDistanceMeters(0, 1.8f)).isEqualTo(0.0);
    }

    @Test
    public void testCalculateMetActiveCalories_normalDuration() {
        Duration duration = Duration.ofMinutes(60);
        double metValue = 3.5;

        double calories =
                DeriveHealthDataFromStepsUtil.calculateMetActiveCalories(
                        duration, metValue, DEFAULT_WEIGHT_KG);

        assertThat(calories).isWithin(0.01).of(182.5);
    }

    @Test
    public void testCalculateMetActiveCalories_zeroDuration_returnsZero() {
        assertThat(
                        DeriveHealthDataFromStepsUtil.calculateMetActiveCalories(
                                Duration.ZERO, 3.5, DEFAULT_WEIGHT_KG))
                .isEqualTo(0.0);
    }

    @Test
    public void testCalculateMetActiveCalories_restingMet_returnsZero() {
        // MET = 1.0 -> Active = 0
        assertThat(
                        DeriveHealthDataFromStepsUtil.calculateMetActiveCalories(
                                Duration.ofMinutes(60), 1.0, DEFAULT_WEIGHT_KG))
                .isEqualTo(0.0);
    }

    @Test
    public void testCalculateAcsmActiveCalories_normalUsage() {
        double distanceMeters = 5000.0;
        Duration duration = Duration.ofMinutes(60);

        double calories =
                DeriveHealthDataFromStepsUtil.calculateAcsmActiveCalories(
                        distanceMeters, duration, DEFAULT_WEIGHT_KG);

        // VO2 = 5000/60 * 0.1 = 8.33
        // calories = 8.33 * 60 * 73 * 0.00501 = 182.865
        assertThat(calories).isWithin(0.01).of(182.86);
    }

    @Test
    public void testCalculateAcsmActiveCalories_zeroDuration_returnsZero() {
        assertThat(
                        DeriveHealthDataFromStepsUtil.calculateAcsmActiveCalories(
                                100.0, Duration.ZERO, DEFAULT_WEIGHT_KG))
                .isEqualTo(0.0);
    }

    @Test
    public void testCalculateMaxActiveCalories_metIsHigher() {
        double distance = 10.0;
        Duration duration = Duration.ofMinutes(60);

        double maxCalories =
                DeriveHealthDataFromStepsUtil.calculateMaxActiveCalories(
                        distance, duration, DEFAULT_WEIGHT_KG);

        assertThat(maxCalories).isWithin(0.1).of(182.5);
    }

    @Test
    public void testCalculateMaxActiveCalories_acsmIsHigher() {
        double distance = 5000.0;
        Duration duration = Duration.ofMinutes(30);

        // MET: 73kg * 0.5h * (3.5 - 1) = 91.25
        // ACSM: 5000m / 30min * 0.1 * 30min * 73kg * (5.01 / 1000) = 182.865
        double maxCalories =
                DeriveHealthDataFromStepsUtil.calculateMaxActiveCalories(
                        distance, duration, DEFAULT_WEIGHT_KG);

        assertThat(maxCalories).isWithin(0.01).of(182.865);
    }
}
