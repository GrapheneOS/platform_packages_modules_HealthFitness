/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.utils

import android.health.connect.datatypes.BasalMetabolicRateRecord
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation
import android.health.connect.datatypes.BodyTemperatureRecord
import android.health.connect.datatypes.BodyWaterMassRecord
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseSegmentType
import android.health.connect.datatypes.ExerciseSessionType
import android.health.connect.datatypes.FhirResource
import android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION
import android.health.connect.datatypes.FhirVersion
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HydrationRecord
import android.health.connect.datatypes.IntermenstrualBleedingRecord
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.MedicalResource
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.Velocity
import android.health.connect.datatypes.units.Volume
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.healthconnect.controller.dataentries.units.PowerConverter
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.randomInstant
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthconnect.controller.utils.toLocalDateTime
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random
import org.mockito.Mockito

val NOW: Instant = Instant.parse("2022-10-20T07:06:05.432Z")
val MIDNIGHT: Instant = Instant.parse("2022-10-20T00:00:00.000Z")

fun getHeartRateRecord(heartRateValues: List<Long>, startTime: Instant = NOW): HeartRateRecord {
    return HeartRateRecord.Builder(
            getMetaData(),
            startTime,
            startTime.plusSeconds(2),
            heartRateValues.map { HeartRateRecord.HeartRateSample(it, NOW) },
        )
        .build()
}

fun getStepsRecord(steps: Long, time: Instant = NOW): StepsRecord {
    return StepsRecord.Builder(getMetaData(), time, time.plusSeconds(2), steps).build()
}

fun getStepsCadenceRecord(time: Instant = NOW): StepsCadenceRecord {
    return StepsCadenceRecord.Builder(
            getMetaData(),
            time,
            time.plusSeconds(2),
            listOf(
                StepsCadenceRecord.StepsCadenceRecordSample(12.5, time),
                StepsCadenceRecord.StepsCadenceRecordSample(12.3, time.plusSeconds(1)),
            ),
        )
        .build()
}

fun getStepsRecordWithUniqueIds(steps: Long, time: Instant = NOW): StepsRecord {
    return StepsRecord.Builder(getMetaDataWithUniqueIds(), time, time.plusSeconds(2), steps).build()
}

fun getBasalMetabolicRateRecord(calories: Long): BasalMetabolicRateRecord {
    val watts = PowerConverter.convertWattsFromCalories(calories)
    return BasalMetabolicRateRecord.Builder(getMetaData(), NOW, Power.fromWatts(watts)).build()
}

fun getDistanceRecord(distance: Length, time: Instant = NOW): DistanceRecord {
    return DistanceRecord.Builder(getMetaData(), time, time.plusSeconds(2), distance).build()
}

fun getTotalCaloriesBurnedRecord(calories: Energy, time: Instant = NOW): TotalCaloriesBurnedRecord {
    return TotalCaloriesBurnedRecord.Builder(getMetaData(), time, time.plusSeconds(2), calories)
        .build()
}

fun getSleepSessionRecord(startTime: Instant = NOW): SleepSessionRecord {
    val endTime = startTime.toLocalDateTime().plusHours(8).toInstant()
    return SleepSessionRecord.Builder(getMetaData(), startTime, endTime).build()
}

fun getSleepSessionRecord(startTime: Instant, endTime: Instant): SleepSessionRecord {
    return SleepSessionRecord.Builder(getMetaData(), startTime, endTime).build()
}

fun getWeightRecord(time: Instant = NOW, weight: Mass): WeightRecord {
    return WeightRecord.Builder(getMetaData(), time, weight).build()
}

fun getIntermenstrualBleedingRecord(time: Instant): IntermenstrualBleedingRecord {
    return IntermenstrualBleedingRecord.Builder(getMetaData(), time).build()
}

fun getBodyTemperatureRecord(
    time: Instant,
    location: Int,
    temperature: Temperature,
): BodyTemperatureRecord {
    return BodyTemperatureRecord.Builder(getMetaData(), time, location, temperature).build()
}

fun getOxygenSaturationRecord(time: Instant, percentage: Percentage): OxygenSaturationRecord {
    return OxygenSaturationRecord.Builder(getMetaData(), time, percentage).build()
}

fun getHydrationRecord(startTime: Instant, endTime: Instant, volume: Volume): HydrationRecord {
    return HydrationRecord.Builder(getMetaData(), startTime, endTime, volume).build()
}

fun getBodyWaterMassRecord(time: Instant, bodyWaterMass: Mass): BodyWaterMassRecord {
    return BodyWaterMassRecord.Builder(getMetaData(), time, bodyWaterMass).build()
}

fun getRandomRecord(fitnessPermissionType: FitnessPermissionType, date: LocalDate): Record {
    return when (fitnessPermissionType) {
        FitnessPermissionType.STEPS ->
            getStepsRecord(Random.nextLong(0, 5000), date.randomInstant())
        FitnessPermissionType.DISTANCE ->
            getDistanceRecord(
                Length.fromMeters(Random.nextDouble(0.0, 5000.0)),
                date.randomInstant(),
            )
        FitnessPermissionType.TOTAL_CALORIES_BURNED ->
            getTotalCaloriesBurnedRecord(
                Energy.fromCalories(Random.nextDouble(1500.0, 5000.0)),
                date.randomInstant(),
            )
        FitnessPermissionType.SLEEP -> getSleepSessionRecord(date.randomInstant())
        else ->
            throw IllegalArgumentException(
                "HealthPermissionType $fitnessPermissionType not supported"
            )
    }
}

fun getSamplePlannedExerciseSessionRecord(): PlannedExerciseSessionRecord {
    val exerciseBlock1 =
        getPlannedExerciseBlock(
            repetitions = 1,
            description = "Warm up",
            exerciseSteps =
                listOf(
                    getPlannedExerciseStep(
                        exerciseSegmentType = ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                        completionGoal =
                            ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
                        performanceGoals =
                            listOf(
                                ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                ExercisePerformanceGoal.SpeedGoal(
                                    Velocity.fromMetersPerSecond(25.0),
                                    Velocity.fromMetersPerSecond(15.0),
                                ),
                            ),
                    )
                ),
        )
    val exerciseBlock2 =
        getPlannedExerciseBlock(
            repetitions = 1,
            description = "Main set",
            exerciseSteps =
                listOf(
                    getPlannedExerciseStep(
                        exerciseSegmentType = ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                        completionGoal =
                            ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(4000.0)),
                        performanceGoals =
                            listOf(
                                ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                ExercisePerformanceGoal.SpeedGoal(
                                    Velocity.fromMetersPerSecond(50.0),
                                    Velocity.fromMetersPerSecond(25.0),
                                ),
                            ),
                    )
                ),
        )
    val exerciseBlocks = listOf(exerciseBlock1, exerciseBlock2)

    return getPlannedExerciseSessionRecord(
        title = "Morning Run",
        note = "Morning quick run by the park",
        exerciseBlocks = exerciseBlocks,
    )
}

fun getPlannedExerciseSessionRecord(
    title: String?,
    note: String?,
    exerciseBlocks: List<PlannedExerciseBlock>,
): PlannedExerciseSessionRecord {
    return basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
        .setTitle(title)
        .setNotes(note)
        .setBlocks(exerciseBlocks)
        .build()
}

private fun basePlannedExerciseSession(exerciseType: Int): PlannedExerciseSessionRecord.Builder {
    val builder: PlannedExerciseSessionRecord.Builder =
        PlannedExerciseSessionRecord.Builder(
            getMetaData(),
            exerciseType,
            NOW,
            NOW.plusSeconds(3600),
        )
    builder.setNotes("Sample training plan notes")
    builder.setTitle("Training plan title")
    builder.setStartZoneOffset(ZoneOffset.UTC)
    builder.setEndZoneOffset(ZoneOffset.UTC)
    return builder
}

fun getPlannedExerciseBlock(
    repetitions: Int,
    description: String?,
    exerciseSteps: List<PlannedExerciseStep>,
): PlannedExerciseBlock {
    return PlannedExerciseBlock.Builder(repetitions)
        .setDescription(description)
        .setSteps(exerciseSteps)
        .build()
}

fun getPlannedExerciseStep(
    exerciseSegmentType: Int,
    completionGoal: ExerciseCompletionGoal,
    performanceGoals: List<ExercisePerformanceGoal>,
): PlannedExerciseStep {
    return PlannedExerciseStep.Builder(
            exerciseSegmentType,
            PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
            completionGoal,
        )
        .setPerformanceGoals(performanceGoals)
        .build()
}

fun getMetaData(): Metadata {
    return getMetaData(TEST_APP_PACKAGE_NAME)
}

fun getMetaDataWithUniqueIds(): Metadata {
    return getMetaDataWithUniqueIds(TEST_APP_PACKAGE_NAME)
}

fun getMetaData(packageName: String): Metadata {
    val device: Device =
        Device.Builder().setManufacturer("google").setModel("Pixel4a").setType(2).build()
    val dataOrigin = DataOrigin.Builder().setPackageName(packageName).build()
    return Metadata.Builder()
        .setId("test_id")
        .setDevice(device)
        .setDataOrigin(dataOrigin)
        .setClientRecordId("BMR" + Math.random().toString())
        .build()
}

fun getMetaDataWithUniqueIds(packageName: String): Metadata {
    val device: Device =
        Device.Builder().setManufacturer("google").setModel("Pixel4a").setType(2).build()
    val dataOrigin = DataOrigin.Builder().setPackageName(packageName).build()
    return Metadata.Builder()
        .setId(getUniqueId())
        .setDevice(device)
        .setDataOrigin(dataOrigin)
        .setClientRecordId("BMR" + Math.random().toString())
        .build()
}

fun getUniqueId(): String {
    return UUID.randomUUID().toString()
}

fun getDataOrigin(packageName: String): DataOrigin =
    DataOrigin.Builder().setPackageName(packageName).build()

fun getSleepSessionRecords(inputDates: List<Pair<Instant, Instant>>): List<SleepSessionRecord> {
    val result = arrayListOf<SleepSessionRecord>()
    inputDates.forEach { (startTime, endTime) ->
        result.add(SleepSessionRecord.Builder(getMetaData(), startTime, endTime).build())
    }

    return result
}

fun verifySleepSessionListsEqual(actual: List<Record>, expected: List<SleepSessionRecord>) {
    assertThat(actual.size).isEqualTo(expected.size)
    for ((index, element) in actual.withIndex()) {
        assertThat(element is SleepSessionRecord).isTrue()
        val expectedElement = expected[index]
        val actualElement = element as SleepSessionRecord

        assertThat(actualElement.startTime).isEqualTo(expectedElement.startTime)
        assertThat(actualElement.endTime).isEqualTo(expectedElement.endTime)
        assertThat(actualElement.notes).isEqualTo(expectedElement.notes)
        assertThat(actualElement.title).isEqualTo(expectedElement.title)
        assertThat(actualElement.stages).isEqualTo(expectedElement.stages)
    }
}

fun verifyOxygenSaturationListsEqual(actual: List<Record>, expected: List<OxygenSaturationRecord>) {
    assertThat(actual.size).isEqualTo(expected.size)
    for ((index, element) in actual.withIndex()) {
        assertThat(element is OxygenSaturationRecord).isTrue()
        val expectedElement = expected[index]
        val actualElement = element as OxygenSaturationRecord

        assertThat(actualElement.time).isEqualTo(expectedElement.time)
        assertThat(actualElement.percentage).isEqualTo(expectedElement.percentage)
    }
}

fun verifyHydrationListsEqual(actual: List<Record>, expected: List<HydrationRecord>) {
    assertThat(actual.size).isEqualTo(expected.size)
    for ((index, element) in actual.withIndex()) {
        assertThat(element is HydrationRecord).isTrue()
        val expectedElement = expected[index]
        val actualElement = element as HydrationRecord

        assertThat(actualElement.startTime).isEqualTo(expectedElement.startTime)
        assertThat(actualElement.endTime).isEqualTo(expectedElement.endTime)
        assertThat(actualElement.volume).isEqualTo(expectedElement.volume)
    }
}

fun verifyBodyWaterMassListsEqual(actual: List<Record>, expected: List<Record>) {
    assertThat(actual.size).isEqualTo(expected.size)
    for ((index, element) in actual.withIndex()) {
        assertThat(element is BodyWaterMassRecord).isTrue()
        val expectedElement = expected[index] as BodyWaterMassRecord
        val actualElement = element as BodyWaterMassRecord

        assertThat(actualElement.time).isEqualTo(expectedElement.time)
        assertThat(actualElement.bodyWaterMass).isEqualTo(expectedElement.bodyWaterMass)
    }
}

// test data constants - start

val START_TIME = Instant.parse("2023-06-12T22:30:00Z")

// pre-defined Instants within a day, week, and month of the START_TIME Instant
val INSTANT_DAY: Instant = Instant.parse("2023-06-11T23:30:00Z")
val INSTANT_DAY2: Instant = Instant.parse("2023-06-12T02:00:00Z")
val INSTANT_WEEK: Instant = Instant.parse("2023-06-14T11:15:00Z")
val INSTANT_MONTH1: Instant = Instant.parse("2023-06-26T23:10:00Z")
val INSTANT_MONTH2: Instant = Instant.parse("2023-06-30T11:30:00Z")
val INSTANT_MONTH3: Instant = Instant.parse("2023-07-01T07:45:00Z")
val INSTANT_MONTH4: Instant = Instant.parse("2023-07-01T19:15:00Z")
val INSTANT_MONTH5: Instant = Instant.parse("2023-07-05T03:45:00Z")
val INSTANT_MONTH6: Instant = Instant.parse("2023-07-07T07:05:00Z")

val SLEEP_DAY_0H20 =
    getSleepSessionRecord(
        Instant.parse("2023-06-12T21:00:00Z"),
        Instant.parse("2023-06-12T21:20:00Z"),
    )
val SLEEP_DAY_1H45 =
    getSleepSessionRecord(
        Instant.parse("2023-06-12T16:00:00Z"),
        Instant.parse("2023-06-12T17:45:00Z"),
    )
val SLEEP_DAY_9H15 =
    getSleepSessionRecord(
        Instant.parse("2023-06-12T22:30:00Z"),
        Instant.parse("2023-06-13T07:45:00Z"),
    )
val SLEEP_WEEK_9H15 =
    getSleepSessionRecord(
        Instant.parse("2023-06-14T22:30:00Z"),
        Instant.parse("2023-06-15T07:45:00Z"),
    )
val SLEEP_WEEK_33H15 =
    getSleepSessionRecord(
        Instant.parse("2023-06-11T22:30:00Z"),
        Instant.parse("2023-06-13T07:45:00Z"),
    )
val SLEEP_MONTH_81H15 =
    getSleepSessionRecord(
        Instant.parse("2023-07-09T22:30:00Z"),
        Instant.parse("2023-07-13T07:45:00Z"),
    )

val HYDRATION_MONTH: HydrationRecord =
    getHydrationRecord(INSTANT_MONTH1, INSTANT_MONTH2, Volume.fromLiters(2.0))
val HYDRATION_MONTH2: HydrationRecord =
    getHydrationRecord(INSTANT_MONTH3, INSTANT_MONTH4, Volume.fromLiters(0.3))
val HYDRATION_MONTH3: HydrationRecord =
    getHydrationRecord(INSTANT_MONTH5, INSTANT_MONTH6, Volume.fromLiters(1.5))

val OXYGENSATURATION_DAY: OxygenSaturationRecord =
    getOxygenSaturationRecord(INSTANT_DAY, Percentage.fromValue(98.0))
val OXYGENSATURATION_DAY2: OxygenSaturationRecord =
    getOxygenSaturationRecord(INSTANT_DAY2, Percentage.fromValue(95.0))

val DISTANCE_STARTDATE_1500: DistanceRecord =
    getDistanceRecord(Length.fromMeters(1500.0), START_TIME)

val WEIGHT_DAY_100: WeightRecord = getWeightRecord(INSTANT_DAY, Mass.fromGrams(100000.0))
val WEIGHT_WEEK_100: WeightRecord = getWeightRecord(INSTANT_WEEK, Mass.fromGrams(100000.0))
val WEIGHT_MONTH_100: WeightRecord = getWeightRecord(INSTANT_MONTH3, Mass.fromGrams(100000.0))
val WEIGHT_STARTDATE_100: WeightRecord = getWeightRecord(START_TIME, Mass.fromGrams(100000.0))

val INTERMENSTRUAL_BLEEDING_DAY: IntermenstrualBleedingRecord =
    getIntermenstrualBleedingRecord(INSTANT_DAY)

val BODYTEMPERATURE_MONTH: BodyTemperatureRecord =
    getBodyTemperatureRecord(
        INSTANT_MONTH3,
        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
        Temperature.fromCelsius(100.0),
    )

val BODYWATERMASS_WEEK: BodyWaterMassRecord =
    getBodyWaterMassRecord(INSTANT_WEEK, Mass.fromGrams(1000.0))

// records using today's date, yesterday's date, and the date two days ago - for header testing
fun getMixedRecordsAcrossTwoDays(timeSource: TimeSource): List<Record> {
    val instantToday: Instant = timeSource.currentLocalDateTime().toInstant()
    val instantYesterday: Instant = timeSource.currentLocalDateTime().minusDays(1).toInstant()
    return listOf(
        getHydrationRecord(instantToday, instantToday.plusSeconds(900), Volume.fromLiters(2.0)),
        getSleepSessionRecord(instantToday, instantToday.plusSeconds(1800)),
        getDistanceRecord(Length.fromMeters(2500.0), instantYesterday),
        getOxygenSaturationRecord(instantYesterday, Percentage.fromValue(99.0)),
    )
}

fun getMixedRecordsAcrossThreeDays(timeSource: TimeSource): List<Record> {
    val instantTwoDaysAgo: Instant = timeSource.currentLocalDateTime().minusDays(2).toInstant()
    return getMixedRecordsAcrossTwoDays(timeSource)
        .plus(
            listOf(
                getWeightRecord(instantTwoDaysAgo, Mass.fromGrams(95000.0)),
                getDistanceRecord(Length.fromMeters(2000.0), instantTwoDaysAgo),
            )
        )
}

// test data constants - end

// Enables or disables animations in a test
fun toggleAnimation(isEnabled: Boolean) {
    with(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())) {
        executeShellCommand(
            "settings put global transition_animation_scale ${if (isEnabled) 1 else 0}"
        )
        executeShellCommand("settings put global window_animation_scale ${if (isEnabled) 1 else 0}")
        executeShellCommand(
            "settings put global animator_duration_scale ${if (isEnabled) 1 else 0}"
        )
    }
}

// Used for matching arguments for [RequestPermissionViewModel]
fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

/** Utility function to turn an array of permission strings to a list of [HealthPermission]s */
fun Array<String>.toPermissionsList(): List<HealthPermission> {
    return this.map { HealthPermission.fromPermissionString(it) }.toList()
}

// region apps

const val TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app"
const val TEST_APP_PACKAGE_NAME_2 = "android.healthconnect.controller.test.app2"
const val TEST_APP_PACKAGE_NAME_3 = "package.name.3"
const val UNSUPPORTED_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app3"
const val OLD_PERMISSIONS_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app4"
const val MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app5"
const val BODY_SENSORS_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app6"
const val WEAR_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app7"
const val BODY_SENSORS_AND_HEALTH_TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app8"
const val TEST_APP_NAME = "Health Connect test app"
const val TEST_APP_NAME_2 = "Health Connect test app 2"
const val TEST_APP_NAME_3 = "Health Connect test app 3"
const val OLD_APP_NAME = "Old permissions test app"
const val MEDICAL_APP_NAME = "Medical permissions HC app"
const val BODY_SENSORS_TEST_APP_NAME = "Body Sensors Test App"

val TEST_APP =
    AppMetadata(packageName = TEST_APP_PACKAGE_NAME, appName = TEST_APP_NAME, icon = null)
val TEST_APP_2 =
    AppMetadata(packageName = TEST_APP_PACKAGE_NAME_2, appName = TEST_APP_NAME_2, icon = null)
val TEST_APP_3 =
    AppMetadata(packageName = TEST_APP_PACKAGE_NAME_3, appName = TEST_APP_NAME_3, icon = null)
val OLD_TEST_APP =
    AppMetadata(
        packageName = OLD_PERMISSIONS_TEST_APP_PACKAGE_NAME,
        appName = OLD_APP_NAME,
        icon = null,
    )
// endregion

// PHR
val TEST_DATASOURCE_ID = getUniqueId()
val TEST_FHIR_VERSION: FhirVersion = FhirVersion.parseFhirVersion("4.0.1")
val TEST_FHIR_RESOURCE_IMMUNIZATION: FhirResource =
    FhirResource.Builder(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            "Immunization1",
            "{\"resourceType\":\"Immunization\",\"id\":\"immunization-1\",\"status\":\"completed\",\"vaccineCode\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/cvx\",\"code\":\"115\"},{\"system\":\"http://hl7.org/fhir/sid/ndc\",\"code\":\"58160-842-11\"}],\"text\":\"Tdap\"},\"patient\":{\"reference\":\"Patient/patient_1\",\"display\":\"Example, Anne\"},\"occurrenceDateTime\":\"2018-05-21\"}",
        )
        .build()
val TEST_FHIR_RESOURCE_IMMUNIZATION_2: FhirResource =
    FhirResource.Builder(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            "Immunization2",
            "{\"resourceType\":\"Immunization\",\"id\":\"immunization-2\",\"status\":\"completed\",\"vaccineCode\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/cvx\",\"code\":\"115\"},{\"system\":\"http://hl7.org/fhir/sid/ndc\",\"code\":\"58160-842-11\"}],\"text\":\"Tdap\"},\"patient\":{\"reference\":\"Patient/patient_1\",\"display\":\"Example, Anne\"},\"occurrenceDateTime\":\"2018-05-21\"}",
        )
        .build()
val TEST_FHIR_RESOURCE_IMMUNIZATION_3: FhirResource =
    FhirResource.Builder(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            "Immunization3",
            "{\"resourceType\":\"Immunization\",\"id\":\"immunization-3\",\"status\":\"completed\",\"vaccineCode\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/cvx\",\"code\":\"115\"},{\"system\":\"http://hl7.org/fhir/sid/ndc\",\"code\":\"58160-842-11\"}],\"text\":\"Tdap\"},\"patient\":{\"reference\":\"Patient/patient_1\",\"display\":\"Example, Anne\"},\"occurrenceDateTime\":\"2018-05-21\"}",
        )
        .build()
val TEST_FHIR_RESOURCE_IMMUNIZATION_LONG: FhirResource =
    FhirResource.Builder(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            "Immunization11",
            "{\"resourceType\":\"Immunization\",\"id\":\"immunization-1\",\"status\":\"completed\",\"vaccineCode\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/cvx\",\"code\":\"115\"},{\"system\":\"http://hl7.org/fhir/sid/ndc\",\"code\":\"58160-842-11\"}],\"text\":\"Tdap\"},\"patient\":{\"reference\":\"Patient/patient_1\",\"display\":\"Example, Anne\"},\"encounter\":{\"reference\":\"Encounter/encounter_unk\",\"display\":\"GP Visit\"},\"occurrenceDateTime\":\"2018-05-21\",\"primarySource\":true,\"manufacturer\":{\"display\":\"Sanofi Pasteur\"},\"lotNumber\":\"1\",\"site\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActSite\",\"code\":\"LA\",\"display\":\"Left Arm\"}],\"text\":\"Left Arm\"},\"route\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-RouteOfAdministration\",\"code\":\"IM\",\"display\":\"Injection, intramuscular\"}],\"text\":\"Injection, intramuscular\"},\"doseQuantity\":{\"value\":0.5,\"unit\":\"mL\"},\"performer\":[{\"function\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0443\",\"code\":\"AP\",\"display\":\"Administering Provider\"}],\"text\":\"Administering Provider\"},\"actor\":{\"reference\":\"Practitioner/practitioner_1\",\"type\":\"Practitioner\",\"display\":\"Dr Maria Hernandez\"}}]}",
        )
        .build()
val TEST_FHIR_RESOURCE_INVALID_JSON: FhirResource =
    FhirResource.Builder(
            FHIR_RESOURCE_TYPE_IMMUNIZATION,
            "invalid_json",
            "{\"resourceType\" : \"Immunization\", {{{\"id\"\" : \"Immunization-3\"}",
        )
        .build()
val TEST_MEDICAL_RESOURCE_IMMUNIZATION: MedicalResource =
    MedicalResource.Builder(
            MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
            TEST_DATASOURCE_ID,
            TEST_FHIR_VERSION,
            TEST_FHIR_RESOURCE_IMMUNIZATION,
        )
        .build()
val TEST_MEDICAL_RESOURCE_IMMUNIZATION_2: MedicalResource =
    MedicalResource.Builder(
            MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
            TEST_DATASOURCE_ID,
            TEST_FHIR_VERSION,
            TEST_FHIR_RESOURCE_IMMUNIZATION_2,
        )
        .build()
val TEST_MEDICAL_RESOURCE_IMMUNIZATION_3: MedicalResource =
    MedicalResource.Builder(
            MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
            TEST_DATASOURCE_ID,
            TEST_FHIR_VERSION,
            TEST_FHIR_RESOURCE_IMMUNIZATION_3,
        )
        .build()
val TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG: MedicalResource =
    MedicalResource.Builder(
            MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
            TEST_DATASOURCE_ID,
            TEST_FHIR_VERSION,
            TEST_FHIR_RESOURCE_IMMUNIZATION_LONG,
        )
        .build()
val TEST_MEDICAL_RESOURCE_INVALID_JSON: MedicalResource =
    MedicalResource.Builder(
            MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
            TEST_DATASOURCE_ID,
            TEST_FHIR_VERSION,
            TEST_FHIR_RESOURCE_INVALID_JSON,
        )
        .build()
val TEST_MEDICAL_DATA_SOURCE: MedicalDataSource =
    MedicalDataSource.Builder(
            /* id= */ TEST_DATASOURCE_ID,
            TEST_APP_PACKAGE_NAME,
            /* fhirBaseUri= */ Uri.parse("fhir.base.uri"),
            /* displayName= */ "App A Data Source",
            /* fhirVersion= */ TEST_FHIR_VERSION,
        )
        .build()
val TEST_MEDICAL_DATA_SOURCE_2: MedicalDataSource =
    MedicalDataSource.Builder(
            /* id= */ getUniqueId(),
            TEST_APP_PACKAGE_NAME,
            /* fhirBaseUri= */ Uri.parse("fhir.base.uri"),
            /* displayName= */ "App A Data Source 2",
            /* fhirVersion= */ TEST_FHIR_VERSION,
        )
        .build()
val TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP: MedicalDataSource =
    MedicalDataSource.Builder(
            /* id= */ getUniqueId(),
            TEST_APP_PACKAGE_NAME_2,
            /* fhirBaseUri= */ Uri.parse("fhir.base.uri"),
            /* displayName= */ "App B Data Source",
            /* fhirVersion= */ TEST_FHIR_VERSION,
        )
        .build()
// endregion
