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
 *
 *
 */

package com.android.healthconnect.controller.tests.data.formatters

import android.content.Context
import android.health.connect.datatypes.ExercisePerformanceGoal.AmrapGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.CadenceGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.HeartRateGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.PowerGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.RateOfPerceivedExertionGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.SpeedGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.UnknownGoal
import android.health.connect.datatypes.ExercisePerformanceGoal.WeightGoal
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN
import android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Velocity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.ExercisePerformanceGoalFormatter
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.DistanceUnit
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.units.WeightUnit
import com.google.common.truth.Truth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExercisePerformanceGoalFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: ExercisePerformanceGoalFormatter
    @Inject lateinit var unitPreferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatGoal_weightPerformanceGoal() = runBlocking {
        unitPreferences.setWeightUnit(WeightUnit.POUND)
        Truth.assertThat(
                formatter.formatGoal(
                    WeightGoal(Mass.fromGrams(1000.0)),
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    WeightGoal(Mass.fromGrams(1000.0)),
                    title = "2.2 lb",
                    titleA11y = "2.2 pounds",
                )
            )
    }

    @Test
    fun formatGoal_powerPerformanceGoal() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    PowerGoal(Power.fromWatts(30.0), Power.fromWatts(100.0)),
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    PowerGoal(Power.fromWatts(30.0), Power.fromWatts(100.0)),
                    title = "30 W - 100 W",
                    titleA11y = "30 watts - 100 watts",
                )
            )
    }

    @Test
    fun formatGoal_amrapPerformanceGoal() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    AmrapGoal.INSTANCE,
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    AmrapGoal.INSTANCE,
                    title = "As many reps as possible",
                    titleA11y = "As many reps as possible",
                )
            )
    }

    @Test
    fun formatGoal_cadencePerformanceGoal() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    CadenceGoal(50.0, 60.0),
                    EXERCISE_SEGMENT_TYPE_BIKING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    CadenceGoal(50.0, 60.0),
                    title = "50 rpm - 60 rpm",
                    titleA11y = "50 revolutions per minute - 60 revolutions per minute",
                )
            )
    }

    @Test
    fun formatGoal_cadencePerformanceGoal_activityTypesWithCadenceMotion() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    CadenceGoal(50.0, 60.0),
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    CadenceGoal(50.0, 60.0),
                    title = "50 steps/min - 60 steps/min",
                    titleA11y = "50 steps per minute - 60 steps per minute",
                )
            )
    }

    @Test
    fun formatGoal_metricSystem_speedPerformanceGoal() = runBlocking {
        unitPreferences.setDistanceUnit(DistanceUnit.KILOMETERS)
        Truth.assertThat(
                formatter.formatGoal(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(15.0),
                        Velocity.fromMetersPerSecond(25.0),
                    ),
                    EXERCISE_SEGMENT_TYPE_BIKING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(15.0),
                        Velocity.fromMetersPerSecond(25.0),
                    ),
                    title = "54 km/h - 90 km/h",
                    titleA11y = "54 kilometres per hour - 90 kilometres per hour",
                )
            )
    }

    @Test
    fun formatGoal_exerciseSegmentTypeWithPace_speedPerformanceGoal_metricSystem() = runBlocking {
        unitPreferences.setDistanceUnit(DistanceUnit.KILOMETERS)
        Truth.assertThat(
                formatter.formatGoal(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(10.0),
                        Velocity.fromMetersPerSecond(20.0),
                    ),
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(10.0),
                        Velocity.fromMetersPerSecond(20.0),
                    ),
                    title = "00:50 min/km - 01:40 min/km",
                    titleA11y = "00:50 minute per kilometre - 01:40 minute per kilometre",
                )
            )
    }

    @Test
    fun formatGoal_swimmingExerciseSegmentType_speedPerformanceGoal_metricSystem_localeUK() =
        runBlocking {
            unitPreferences.setDistanceUnit(DistanceUnit.KILOMETERS)
            Truth.assertThat(
                    formatter.formatGoal(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(50.0),
                            Velocity.fromMetersPerSecond(100.0),
                        ),
                        EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                    )
                )
                .isEqualTo(
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(50.0),
                            Velocity.fromMetersPerSecond(100.0),
                        ),
                        title = "00:01 min/100 metres - 00:02 min/100 metres",
                        titleA11y = "00:01 minute per 100 metres - 00:02 minute per 100 metres",
                    )
                )
        }

    @Test
    fun formatGoal_swimmingExerciseSegmentType_speedPerformanceGoal_imperialSystem_localeUS() =
        runBlocking {
            Locale.setDefault(Locale.US)
            unitPreferences.setDistanceUnit(DistanceUnit.MILES)
            Truth.assertThat(
                    formatter.formatGoal(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(25.0),
                            Velocity.fromMetersPerSecond(50.0),
                        ),
                        EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                    )
                )
                .isEqualTo(
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(25.0),
                            Velocity.fromMetersPerSecond(50.0),
                        ),
                        title = "00:01 min/100 yards - 00:03 min/100 yards",
                        titleA11y = "00:01 minute per 100 yards - 00:03 minute per 100 yards",
                    )
                )
        }

    @Test
    fun formatGoal_speedPerformanceGoal_imperialSystem() = runBlocking {
        unitPreferences.setDistanceUnit(DistanceUnit.MILES)
        Truth.assertThat(
                formatter.formatGoal(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(25.0),
                        Velocity.fromMetersPerSecond(15.0),
                    ),
                    EXERCISE_SEGMENT_TYPE_BIKING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(25.0),
                        Velocity.fromMetersPerSecond(15.0),
                    ),
                    title = "55.923 mph - 33.554 mph",
                    titleA11y = "55.923 miles per hour - 33.554 miles per hour",
                )
            )
    }

    @Test
    fun formatGoal_exerciseSegmentTypeWithPace_speedPerformanceGoal_imperialSystem() = runBlocking {
        unitPreferences.setDistanceUnit(DistanceUnit.MILES)
        Truth.assertThat(
                formatter.formatGoal(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(10.0),
                        Velocity.fromMetersPerSecond(20.0),
                    ),
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    SpeedGoal(
                        Velocity.fromMetersPerSecond(10.0),
                        Velocity.fromMetersPerSecond(20.0),
                    ),
                    title = "01:20 min/mile - 02:40 min/mile",
                    titleA11y = "01:20 minute per mile - 02:40 minute per mile",
                )
            )
    }

    @Test
    fun formatGoal_exerciseSegmentTypeWithPace_speedPerformanceGoal_withZeroValue_metricSystem() =
        runBlocking {
            unitPreferences.setDistanceUnit(DistanceUnit.KILOMETERS)
            Truth.assertThat(
                    formatter.formatGoal(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(0.0),
                            Velocity.fromMetersPerSecond(0.0),
                        ),
                        EXERCISE_SEGMENT_TYPE_RUNNING,
                    )
                )
                .isEqualTo(
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(0.0),
                            Velocity.fromMetersPerSecond(0.0),
                        ),
                        title = "00:00 min/km - 00:00 min/km",
                        titleA11y = "00:00 minute per kilometre - 00:00 minute per kilometre",
                    )
                )
        }

    @Test
    fun formatGoal_swimmingExerciseSegmentType_speedPerformanceGoal_withUnRealisticValue_metricSystem() =
        runBlocking {
            unitPreferences.setDistanceUnit(DistanceUnit.KILOMETERS)
            Truth.assertThat(
                    formatter.formatGoal(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(0.01),
                            Velocity.fromMetersPerSecond(0.02),
                        ),
                        EXERCISE_SEGMENT_TYPE_RUNNING,
                    )
                )
                .isEqualTo(
                    FormattedEntry.ExercisePerformanceGoalEntry(
                        SpeedGoal(
                            Velocity.fromMetersPerSecond(0.01),
                            Velocity.fromMetersPerSecond(0.02),
                        ),
                        title = "--:-- - --:--",
                        titleA11y = "--:-- - --:--",
                    )
                )
        }

    @Test
    fun formatGoal_heartRatePerformanceGoal() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    HeartRateGoal(100, 150),
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    HeartRateGoal(100, 150),
                    title = "100 bpm - 150 bpm",
                    titleA11y = "100 beats per minute - 150 beats per minute",
                )
            )
    }

    @Test
    fun formatGoal_rateOfPerceivedExertionGoalPerformanceGoal() = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    RateOfPerceivedExertionGoal(4),
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    RateOfPerceivedExertionGoal(4),
                    title = "Effort level: 4/10",
                    titleA11y = "Effort level: 4/10",
                )
            )
    }

    @Test
    fun formatGoal_unknownPerformanceGoal(): Unit = runBlocking {
        Truth.assertThat(
                formatter.formatGoal(
                    UnknownGoal.INSTANCE,
                    EXERCISE_SEGMENT_TYPE_UNKNOWN,
                )
            )
            .isEqualTo(
                FormattedEntry.ExercisePerformanceGoalEntry(
                    UnknownGoal.INSTANCE,
                    title = "",
                    titleA11y = "",
                )
            )
    }
}
