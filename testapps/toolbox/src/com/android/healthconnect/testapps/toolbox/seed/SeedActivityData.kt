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
package com.android.healthconnect.testapps.toolbox.seed

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE
import android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.ExerciseLap
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseSegment
import android.health.connect.datatypes.ExerciseSegmentType
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType
import android.health.connect.datatypes.FloorsClimbedRecord
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_COOLDOWN
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_REST
import android.health.connect.datatypes.PlannedExerciseStep.EXERCISE_CATEGORY_WARMUP
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.PowerRecord.PowerRecordSample
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.SpeedRecord.SpeedRecordSample
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_COOPER_TEST
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_HEART_RATE_RATIO
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_METABOLIC_CART
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_OTHER
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST
import android.health.connect.datatypes.WheelchairPushesRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Velocity
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData.Companion.generateExerciseRouteFromLocations
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData.Companion.routeDataMap
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.insertRecords
import kotlinx.coroutines.runBlocking
import java.time.Duration.ofDays
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class SeedActivityData(private val context: Context, private val manager: HealthConnectManager) {

    companion object {
        val VALID_VO2_MEASUREMENT_METHOD =
            setOf(
                MEASUREMENT_METHOD_OTHER,
                MEASUREMENT_METHOD_METABOLIC_CART,
                MEASUREMENT_METHOD_HEART_RATE_RATIO,
                MEASUREMENT_METHOD_COOPER_TEST,
                MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST,
                MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
            )
    }

    private val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
    private val yesterday = start.minus(ofDays(1))
    private val lastWeek = start.minus(ofDays(7))
    private val lastMonth = start.minus(ofDays(31))

    fun seedActivityData() {
        runBlocking {
            try {
                seedActivityIntensityData()
                seedStepsData()
                seedDistanceData()
                seedElevationGainedRecord()
                seedActiveCaloriesBurnedData()
                seedExerciseSessionData()
                seedPlannedExerciseSessionRecord()
                seedSpeedRecord()
                seedPowerRecord()
                seedCyclingPedalingCadenceRecord()
                seedFloorsClimbedRecord()
                seedTotalCaloriesBurnedRecord()
                seedWheelchairPushesRecord()
                seedVo2MaxRecord()
                seedStepsCadenceRecord()
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    private suspend fun seedActivityIntensityData() {
        val records =
            listOf(start, yesterday, lastWeek, lastMonth).flatMap { baseTime ->
                List(3) {
                    val startTime = baseTime.plus(ofMinutes(5 * it.toLong()))

                    ActivityIntensityRecord.Builder(
                            getMetaData(context),
                            startTime,
                            startTime.plus(ofMinutes(3)),
                            if (Random.nextBoolean()) ACTIVITY_INTENSITY_TYPE_MODERATE
                            else ACTIVITY_INTENSITY_TYPE_VIGOROUS,
                        )
                        .build()
                }
            }

        insertRecords(records, manager)
    }

    private suspend fun seedStepsData() {
        val records = (1L..50).map { count -> getStepsRecord(count, start.plus(ofMinutes(count))) }
        val yesterdayRecords =
            (1L..3).map { count -> getStepsRecord(count, yesterday.plus(ofMinutes(count))) }
        val lastWeekRecords =
            (1L..3).map { count -> getStepsRecord(count, lastWeek.plus(ofMinutes(count))) }
        val lastMonthRecords =
            (1L..3).map { count -> getStepsRecord(count, lastMonth.plus(ofMinutes(count))) }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedDistanceData() {
        val records =
            (1L..50).map { timeOffSet ->
                getDistanceData(getValidLengthData(500, 5000), start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getDistanceData(
                    getValidLengthData(500, 5000),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getDistanceData(getValidLengthData(500, 5000), lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getDistanceData(
                    getValidLengthData(500, 5000),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedElevationGainedRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getElevationGainedRecord(
                    getValidLengthData(500, 5000),
                    start.plus(ofMinutes(timeOffSet)),
                )
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getElevationGainedRecord(
                    getValidLengthData(500, 5000),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getElevationGainedRecord(
                    getValidLengthData(500, 5000),
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getElevationGainedRecord(
                    getValidLengthData(500, 5000),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedActiveCaloriesBurnedData() {
        val records =
            (1L..15).map { timeOffSet ->
                getActiveCaloriesBurnedRecord(getValidEnergy(), start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getActiveCaloriesBurnedRecord(
                    getValidEnergy(),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getActiveCaloriesBurnedRecord(
                    getValidEnergy(),
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getActiveCaloriesBurnedRecord(
                    getValidEnergy(),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedExerciseSessionData() {
        val records =
            (1L..3).map { timeOffSet ->
                val exerciseSegments = ArrayList<ExerciseSegment>()
                repeat(5) { i ->
                    exerciseSegments.add(
                        ExerciseSegment.Builder(
                                start.plus(ofMinutes(timeOffSet + i)),
                                start.plus(ofMinutes(timeOffSet + i + 1)),
                                getValidSegmentType(),
                            )
                            .build()
                    )
                }
                val exerciseLaps = ArrayList<ExerciseLap>()
                repeat(5) { i ->
                    exerciseLaps.add(
                        ExerciseLap.Builder(
                                start.plus(ofMinutes(timeOffSet + i)),
                                start.plus(ofMinutes(timeOffSet + i + 1)),
                            )
                            .setLength(getValidLengthData(50, 1050))
                            .build()
                    )
                }

                getExerciseSessionRecord(
                    exerciseSegments,
                    exerciseLaps,
                    start.plus(ofMinutes(timeOffSet)),
                )
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val exerciseSegments = ArrayList<ExerciseSegment>()
                repeat(5) { i ->
                    exerciseSegments.add(
                        ExerciseSegment.Builder(
                                yesterday.plus(ofMinutes(timeOffSet + i)),
                                yesterday.plus(ofMinutes(timeOffSet + i + 1)),
                                getValidSegmentType(),
                            )
                            .build()
                    )
                }
                val exerciseLaps = ArrayList<ExerciseLap>()
                repeat(5) { i ->
                    exerciseLaps.add(
                        ExerciseLap.Builder(
                                yesterday.plus(ofMinutes(timeOffSet + i)),
                                yesterday.plus(ofMinutes(timeOffSet + i + 1)),
                            )
                            .setLength(getValidLengthData(50, 1050))
                            .build()
                    )
                }
                getExerciseSessionRecord(
                    exerciseSegments,
                    exerciseLaps,
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val exerciseSegments = ArrayList<ExerciseSegment>()
                repeat(5) { i ->
                    exerciseSegments.add(
                        ExerciseSegment.Builder(
                                lastWeek.plus(ofMinutes(timeOffSet + i)),
                                lastWeek.plus(ofMinutes(timeOffSet + i + 1)),
                                getValidSegmentType(),
                            )
                            .build()
                    )
                }
                val exerciseLaps = ArrayList<ExerciseLap>()
                repeat(5) { i ->
                    exerciseLaps.add(
                        ExerciseLap.Builder(
                                lastWeek.plus(ofMinutes(timeOffSet + i)),
                                lastWeek.plus(ofMinutes(timeOffSet + i + 1)),
                            )
                            .setLength(getValidLengthData(50, 1050))
                            .build()
                    )
                }
                getExerciseSessionRecord(
                    exerciseSegments,
                    exerciseLaps,
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val exerciseSegments = ArrayList<ExerciseSegment>()
                repeat(5) { i ->
                    exerciseSegments.add(
                        ExerciseSegment.Builder(
                                lastMonth.plus(ofMinutes(timeOffSet + i)),
                                lastMonth.plus(ofMinutes(timeOffSet + i + 1)),
                                getValidSegmentType(),
                            )
                            .build()
                    )
                }
                val exerciseLaps = ArrayList<ExerciseLap>()
                repeat(5) { i ->
                    exerciseLaps.add(
                        ExerciseLap.Builder(
                                lastMonth.plus(ofMinutes(timeOffSet + i)),
                                lastMonth.plus(ofMinutes(timeOffSet + i + 1)),
                            )
                            .setLength(getValidLengthData(50, 1050))
                            .build()
                    )
                }
                getExerciseSessionRecord(
                    exerciseSegments,
                    exerciseLaps,
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedPlannedExerciseSessionRecord() {
        val tomorrow = start.plus(ofDays(1))
        val records =
            (1L..3).map { timeOffSet ->
                val plannedExerciseBlocks = ArrayList<PlannedExerciseBlock>()
                plannedExerciseBlocks.add(getValidPlannedExerciseBlockData())
                plannedExerciseBlocks.add(getCardioPlannedExerciseBlockData())
                getPlannedExerciseSessionRecord(
                    plannedExerciseBlocks,
                    start.plus(ofMinutes(timeOffSet)),
                )
            }
        val tomorrowRecords =
            (1L..3).map { timeOffSet ->
                val plannedExerciseBlocks = ArrayList<PlannedExerciseBlock>()
                repeat(10) { plannedExerciseBlocks.add(getValidPlannedExerciseBlockData()) }
                getPlannedExerciseSessionRecord(
                    plannedExerciseBlocks,
                    tomorrow.plus(ofMinutes(timeOffSet)),
                )
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val plannedExerciseBlocks = ArrayList<PlannedExerciseBlock>()
                repeat(10) { plannedExerciseBlocks.add(getValidPlannedExerciseBlockData()) }
                getPlannedExerciseSessionRecord(
                    plannedExerciseBlocks,
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val plannedExerciseBlocks = ArrayList<PlannedExerciseBlock>()
                repeat(10) { plannedExerciseBlocks.add(getValidPlannedExerciseBlockData()) }
                getPlannedExerciseSessionRecord(
                    plannedExerciseBlocks,
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val plannedExerciseBlocks = ArrayList<PlannedExerciseBlock>()
                repeat(10) { plannedExerciseBlocks.add(getValidPlannedExerciseBlockData()) }
                getPlannedExerciseSessionRecord(
                    plannedExerciseBlocks,
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(tomorrowRecords, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedSpeedRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                val speedRecordSample = ArrayList<SpeedRecordSample>()
                repeat(10) { i ->
                    speedRecordSample.add(
                        SpeedRecordSample(
                            getValidSpeedData(),
                            start.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getSpeedRecord(speedRecordSample, start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val speedRecordSample = ArrayList<SpeedRecordSample>()
                repeat(10) { i ->
                    speedRecordSample.add(
                        SpeedRecordSample(
                            getValidSpeedData(),
                            yesterday.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getSpeedRecord(speedRecordSample, yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val speedRecordSample = ArrayList<SpeedRecordSample>()
                repeat(10) { i ->
                    speedRecordSample.add(
                        SpeedRecordSample(
                            getValidSpeedData(),
                            lastWeek.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getSpeedRecord(speedRecordSample, lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val speedRecordSample = ArrayList<SpeedRecordSample>()
                repeat(10) { i ->
                    speedRecordSample.add(
                        SpeedRecordSample(
                            getValidSpeedData(),
                            lastMonth.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getSpeedRecord(speedRecordSample, lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedPowerRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                val powerRecordSample = ArrayList<PowerRecordSample>()
                repeat(10) { i ->
                    powerRecordSample.add(
                        PowerRecordSample(
                            getValidPowerData(),
                            start.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getPowerRecord(powerRecordSample, start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val powerRecordSample = ArrayList<PowerRecordSample>()
                repeat(10) { i ->
                    powerRecordSample.add(
                        PowerRecordSample(
                            getValidPowerData(),
                            yesterday.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getPowerRecord(powerRecordSample, yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val powerRecordSample = ArrayList<PowerRecordSample>()
                repeat(10) { i ->
                    powerRecordSample.add(
                        PowerRecordSample(
                            getValidPowerData(),
                            lastWeek.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getPowerRecord(powerRecordSample, lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val powerRecordSample = ArrayList<PowerRecordSample>()
                repeat(10) { i ->
                    powerRecordSample.add(
                        PowerRecordSample(
                            getValidPowerData(),
                            lastMonth.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getPowerRecord(powerRecordSample, lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedCyclingPedalingCadenceRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                val cyclingCadenceSample = ArrayList<CyclingPedalingCadenceRecordSample>()
                repeat(10) { i ->
                    cyclingCadenceSample.add(
                        CyclingPedalingCadenceRecordSample(
                            getValidDoubleData(60, 100),
                            start.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getCyclingPedalingCadenceRecord(
                    cyclingCadenceSample,
                    start.plus(ofMinutes(timeOffSet)),
                )
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val cyclingCadenceSample = ArrayList<CyclingPedalingCadenceRecordSample>()
                repeat(10) { i ->
                    cyclingCadenceSample.add(
                        CyclingPedalingCadenceRecordSample(
                            getValidDoubleData(60, 100),
                            yesterday.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getCyclingPedalingCadenceRecord(
                    cyclingCadenceSample,
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val cyclingCadenceSample = ArrayList<CyclingPedalingCadenceRecordSample>()
                repeat(10) { i ->
                    cyclingCadenceSample.add(
                        CyclingPedalingCadenceRecordSample(
                            getValidDoubleData(60, 100),
                            lastWeek.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getCyclingPedalingCadenceRecord(
                    cyclingCadenceSample,
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val cyclingCadenceSample = ArrayList<CyclingPedalingCadenceRecordSample>()
                repeat(10) { i ->
                    cyclingCadenceSample.add(
                        CyclingPedalingCadenceRecordSample(
                            getValidDoubleData(60, 100),
                            lastMonth.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }

                getCyclingPedalingCadenceRecord(
                    cyclingCadenceSample,
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedFloorsClimbedRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getFloorsClimbedRecord(getValidDoubleData(1, 10), start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getFloorsClimbedRecord(
                    getValidDoubleData(1, 10),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getFloorsClimbedRecord(
                    getValidDoubleData(1, 10),
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getFloorsClimbedRecord(
                    getValidDoubleData(1, 10),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedTotalCaloriesBurnedRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getTotalCaloriesBurnedRecord(getValidEnergy(), start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getTotalCaloriesBurnedRecord(
                    getValidEnergy(),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getTotalCaloriesBurnedRecord(getValidEnergy(), lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getTotalCaloriesBurnedRecord(
                    getValidEnergy(),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedWheelchairPushesRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getWheelchairPushesRecord(timeOffSet, start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getWheelchairPushesRecord(timeOffSet, yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getWheelchairPushesRecord(timeOffSet, lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getWheelchairPushesRecord(timeOffSet, lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedVo2MaxRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getVo2MaxRecord(
                    getValidVo2MeasurementMethod(),
                    getValidDoubleData(25, 40),
                    start.plus(ofMinutes(timeOffSet)),
                )
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getVo2MaxRecord(
                    getValidVo2MeasurementMethod(),
                    getValidDoubleData(25, 40),
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getVo2MaxRecord(
                    getValidVo2MeasurementMethod(),
                    getValidDoubleData(25, 40),
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getVo2MaxRecord(
                    getValidVo2MeasurementMethod(),
                    getValidDoubleData(25, 40),
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedStepsCadenceRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                val stepsCadenceRecordSample = ArrayList<StepsCadenceRecordSample>()
                repeat(10) { i ->
                    stepsCadenceRecordSample.add(
                        StepsCadenceRecordSample(
                            getValidDoubleData(160, 180),
                            start.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getStepsCadenceRecord(stepsCadenceRecordSample, start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                val stepsCadenceRecordSample = ArrayList<StepsCadenceRecordSample>()
                repeat(10) { i ->
                    stepsCadenceRecordSample.add(
                        StepsCadenceRecordSample(
                            getValidDoubleData(160, 180),
                            yesterday.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getStepsCadenceRecord(
                    stepsCadenceRecordSample,
                    yesterday.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                val stepsCadenceRecordSample = ArrayList<StepsCadenceRecordSample>()
                repeat(10) { i ->
                    stepsCadenceRecordSample.add(
                        StepsCadenceRecordSample(
                            getValidDoubleData(160, 180),
                            lastWeek.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getStepsCadenceRecord(
                    stepsCadenceRecordSample,
                    lastWeek.plus(ofMinutes(timeOffSet)),
                )
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                val stepsCadenceRecordSample = ArrayList<StepsCadenceRecordSample>()
                repeat(10) { i ->
                    stepsCadenceRecordSample.add(
                        StepsCadenceRecordSample(
                            getValidDoubleData(160, 180),
                            lastMonth.plus(ofMinutes(timeOffSet + i)),
                        )
                    )
                }
                getStepsCadenceRecord(
                    stepsCadenceRecordSample,
                    lastMonth.plus(ofMinutes(timeOffSet)),
                )
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private fun getStepsRecord(count: Long, time: Instant): StepsRecord {
        return StepsRecord.Builder(getMetaData(context), time, time.plusSeconds(30), count).build()
    }

    private fun getDistanceData(length: Length, time: Instant): DistanceRecord {
        return DistanceRecord.Builder(getMetaData(context), time, time.plusSeconds(30), length)
            .build()
    }

    private fun getValidLengthData(min: Int, max: Int): Length {
        return Length.fromMeters((Random.nextInt(min, max)).toDouble())
    }

    private fun getElevationGainedRecord(distance: Length, time: Instant): ElevationGainedRecord {
        return ElevationGainedRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                distance,
            )
            .build()
    }

    private fun getActiveCaloriesBurnedRecord(
        energy: Energy,
        time: Instant,
    ): ActiveCaloriesBurnedRecord {
        return ActiveCaloriesBurnedRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                energy,
            )
            .build()
    }

    private fun getValidEnergy(): Energy {
        return Energy.fromCalories((Random.nextInt(500, 5000)).toDouble())
    }

    private fun getExerciseSessionRecord(
        exerciseSegments: List<ExerciseSegment>,
        laps: List<ExerciseLap>,
        time: Instant,
    ): ExerciseSessionRecord {
        return ExerciseSessionRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(1000),
                ExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
            )
            .setSegments(exerciseSegments)
            .setLaps(laps)
            .setRoute(
                generateExerciseRouteFromLocations(
                    getValidExerciseRouteLocation(),
                    time.toEpochMilli(),
                )
            )
            .build()
    }

    private fun getValidExerciseRouteLocation():
        List<ExerciseRoutesTestData.ExerciseRouteLocationData> {
        return routeDataMap.values.random()
    }

    private fun getValidSegmentType(): Int {
        return ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STRETCHING
    }

    private fun getPlannedExerciseSessionRecord(
        plannedExerciseBlocks: List<PlannedExerciseBlock>,
        time: Instant,
    ): PlannedExerciseSessionRecord {
        return PlannedExerciseSessionRecord.Builder(
                getMetaData(context),
                ExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
                time,
                time.plusSeconds(30),
            )
            .setBlocks(plannedExerciseBlocks)
            .setExerciseType(ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING)
            .setNotes("Gym training notes")
            .build()
    }

    private fun getValidPlannedExerciseBlockData(): PlannedExerciseBlock {
        val warmupSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
                    EXERCISE_CATEGORY_WARMUP,
                    ExerciseCompletionGoal.RepetitionsGoal(Random.nextInt(10, 20)),
                )
                .build()
        val activeSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
                    EXERCISE_CATEGORY_ACTIVE,
                    ExerciseCompletionGoal.RepetitionsGoal(Random.nextInt(20, 30)),
                )
                .build()
        val cooldownSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
                    EXERCISE_CATEGORY_COOLDOWN,
                    ExerciseCompletionGoal.RepetitionsGoal(Random.nextInt(20, 30)),
                )
                .build()
        val rest =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_STRETCHING,
                    EXERCISE_CATEGORY_REST,
                    ExerciseCompletionGoal.DurationGoal(ofSeconds(30)),
                )
                .build()
        return PlannedExerciseBlock.Builder(Random.nextInt(1, 5))
            .addStep(warmupSet)
            .addStep(activeSet)
            .addStep(cooldownSet)
            .addStep(rest)
            .build()
    }

    private fun getCardioPlannedExerciseBlockData(): PlannedExerciseBlock {
        val warmupSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
                    EXERCISE_CATEGORY_WARMUP,
                    ExerciseCompletionGoal.DurationGoal(ofMinutes(5)),
                )
                .addPerformanceGoal(
                    ExercisePerformanceGoal.SpeedGoal(
                        Velocity.fromMetersPerSecond(100.0),
                        Velocity.fromMetersPerSecond(200.0),
                    )
                )
                .build()
        val activeSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
                    EXERCISE_CATEGORY_WARMUP,
                    ExerciseCompletionGoal.DurationGoal(ofMinutes(45)),
                )
                .addPerformanceGoal(ExercisePerformanceGoal.HeartRateGoal(90, 110))
                .build()
        val cooldownSet =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
                    EXERCISE_CATEGORY_WARMUP,
                    ExerciseCompletionGoal.DurationGoal(ofMinutes(10)),
                )
                .build()
        val rest =
            PlannedExerciseStep.Builder(
                    ExerciseSessionType.EXERCISE_SESSION_TYPE_STRETCHING,
                    EXERCISE_CATEGORY_REST,
                    ExerciseCompletionGoal.DurationGoal(ofSeconds(180)),
                )
                .build()
        return PlannedExerciseBlock.Builder(Random.nextInt(1, 5))
            .addStep(warmupSet)
            .addStep(activeSet)
            .addStep(cooldownSet)
            .addStep(rest)
            .build()
    }

    private fun getValidPlannedExerciseStepData(): PlannedExerciseStep {
        return PlannedExerciseStep.Builder(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
                EXERCISE_CATEGORY_ACTIVE,
                getValidCompletionGoal(),
            )
            .build()
    }

    private fun getValidCompletionGoal(): ExerciseCompletionGoal {
        return ExerciseCompletionGoal.StepsGoal(Random.nextInt(100, 500))
    }

    private fun getSpeedRecord(speed: List<SpeedRecordSample>, time: Instant): SpeedRecord {
        return SpeedRecord.Builder(getMetaData(context), time, time.plusSeconds(1000), speed)
            .build()
    }

    private fun getValidSpeedData(): Velocity {
        return Velocity.fromMetersPerSecond(Random.nextInt(1, 10).toDouble())
    }

    private fun getPowerRecord(power: List<PowerRecordSample>, time: Instant): PowerRecord {
        return PowerRecord.Builder(getMetaData(context), time, time.plusSeconds(1000), power)
            .build()
    }

    private fun getValidPowerData(): Power {
        return Power.fromWatts(Random.nextInt(150, 400).toDouble())
    }

    private fun getCyclingPedalingCadenceRecord(
        cyclingCadenceSample: List<CyclingPedalingCadenceRecordSample>,
        time: Instant,
    ): CyclingPedalingCadenceRecord {
        return CyclingPedalingCadenceRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(1000),
                cyclingCadenceSample,
            )
            .build()
    }

    private fun getFloorsClimbedRecord(floors: Double, time: Instant): FloorsClimbedRecord {
        return FloorsClimbedRecord.Builder(getMetaData(context), time, time.plusSeconds(30), floors)
            .build()
    }

    private fun getTotalCaloriesBurnedRecord(
        energy: Energy,
        time: Instant,
    ): TotalCaloriesBurnedRecord {
        return TotalCaloriesBurnedRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                energy,
            )
            .build()
    }

    private fun getWheelchairPushesRecord(count: Long, time: Instant): WheelchairPushesRecord {
        return WheelchairPushesRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                count,
            )
            .build()
    }

    private fun getVo2MaxRecord(
        measurementMethod: Int,
        vo2Max: Double,
        time: Instant,
    ): Vo2MaxRecord {
        return Vo2MaxRecord.Builder(getMetaData(context), time, measurementMethod, vo2Max).build()
    }

    private fun getValidVo2MeasurementMethod(): Int {
        return VALID_VO2_MEASUREMENT_METHOD.random()
    }

    private fun getStepsCadenceRecord(
        stepsCadenceRecordSample: List<StepsCadenceRecordSample>,
        time: Instant,
    ): StepsCadenceRecord {
        return StepsCadenceRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(1000),
                stepsCadenceRecordSample,
            )
            .build()
    }

    private fun getValidDoubleData(min: Int, max: Int): Double {
        return Random.nextInt(min, max).toDouble()
    }
}
