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

package android.healthconnect.testing.shared.recordfactory;

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_FASTING;
import static android.health.connect.datatypes.BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID;
import static android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM;
import static android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_SITTING_DOWN;
import static android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT;
import static android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_DRY;
import static android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation.SENSATION_MEDIUM;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN;
import static android.health.connect.datatypes.MealType.MEAL_TYPE_BREAKFAST;
import static android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_MEDIUM;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION;
import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE;
import static android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult.RESULT_HIGH;
import static android.health.connect.datatypes.SexualActivityRecord.SexualActivityProtectionUsed.PROTECTION_USED_PROTECTED;
import static android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST;
import static android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST;

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.BodyTemperatureRecord;
import android.health.connect.datatypes.BodyWaterMassRecord;
import android.health.connect.datatypes.BoneMassRecord;
import android.health.connect.datatypes.CervicalMucusRecord;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.IntermenstrualBleedingRecord;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RespiratoryRateRecord;
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.Vo2MaxRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.health.connect.datatypes.units.BloodGlucose;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Percentage;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Pressure;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.health.connect.datatypes.units.Velocity;
import android.health.connect.datatypes.units.Volume;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Test infrastructure to enable a fully populated record for use in other tests. */
public class AnotherRecordFactory {

    /** {@return a fully populated record of the given type}. */
    public static Record makePopulatedRecord(Class<? extends Record> recordClass) {
        UUID uuid = UUID.randomUUID();
        Metadata metadata =
                new Metadata.Builder()
                        .setId(uuid.toString())
                        .setClientRecordId("client-record-id")
                        .setClientRecordVersion(567)
                        .setDevice(
                                new Device.Builder()
                                        .setType(DEVICE_TYPE_WATCH)
                                        .setModel("model")
                                        .setManufacturer("manufacturer")
                                        .build())
                        .setDataOrigin(
                                new DataOrigin.Builder().setPackageName("package.name").build())
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                        .setLastModifiedTime(Instant.ofEpochMilli(9012345))
                        .build();
        Instant start = Instant.ofEpochMilli(1_357_924_680L);
        Instant midTime = Instant.ofEpochMilli(2_000_000_000L);
        // Use strange offsets to avoid them mataching real time zones filled in by default.
        ZoneOffset startZoneOffset = ZoneOffset.ofHoursMinutes(-2, -49);
        ZoneOffset endZoneOffset = ZoneOffset.ofHoursMinutes(3, 6);
        Instant end = Instant.ofEpochMilli(2_468_013_579L);

        if (recordClass.equals(StepsRecord.class)) {
            return new StepsRecord.Builder(metadata, start, end, /* count= */ 12_000)
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(ActiveCaloriesBurnedRecord.class)) {
            return new ActiveCaloriesBurnedRecord.Builder(
                            metadata, start, end, Energy.fromCalories(120.1))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(HydrationRecord.class)) {
            return new HydrationRecord.Builder(metadata, start, end, Volume.fromLiters(3.4))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(ElevationGainedRecord.class)) {
            return new ElevationGainedRecord.Builder(metadata, start, end, Length.fromMeters(123.4))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(FloorsClimbedRecord.class)) {
            return new FloorsClimbedRecord.Builder(metadata, start, end, /* floors= */ 4.5)
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(WheelchairPushesRecord.class)) {
            return new WheelchairPushesRecord.Builder(metadata, start, end, /* count= */ 1_234L)
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(DistanceRecord.class)) {
            return new DistanceRecord.Builder(metadata, start, end, Length.fromMeters(23_000L))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(NutritionRecord.class)) {
            return makePopulatedNutritionRecord(
                    metadata, start, end, startZoneOffset, endZoneOffset);
        } else if (recordClass.equals(TotalCaloriesBurnedRecord.class)) {
            return new TotalCaloriesBurnedRecord.Builder(
                            metadata, start, end, Energy.fromCalories(220.5))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(MenstruationPeriodRecord.class)) {
            return new MenstruationPeriodRecord.Builder(metadata, start, end)
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(HeartRateRecord.class)) {
            return new HeartRateRecord.Builder(
                            metadata,
                            start,
                            end,
                            List.of(
                                    new HeartRateRecord.HeartRateSample(70, start),
                                    new HeartRateRecord.HeartRateSample(60, midTime),
                                    new HeartRateRecord.HeartRateSample(100, end)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(CyclingPedalingCadenceRecord.class)) {
            return new CyclingPedalingCadenceRecord.Builder(
                            metadata,
                            start,
                            end,
                            List.of(
                                    new CyclingPedalingCadenceRecord
                                            .CyclingPedalingCadenceRecordSample(75.2, midTime)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(PowerRecord.class)) {
            return new PowerRecord.Builder(
                            metadata,
                            start,
                            end,
                            List.of(
                                    new PowerRecord.PowerRecordSample(Power.fromWatts(60.3), start),
                                    new PowerRecord.PowerRecordSample(
                                            Power.fromWatts(70.3), midTime),
                                    new PowerRecord.PowerRecordSample(Power.fromWatts(80.3), end)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(SpeedRecord.class)) {
            return new SpeedRecord.Builder(
                            metadata,
                            start,
                            end,
                            List.of(
                                    new SpeedRecord.SpeedRecordSample(
                                            Velocity.fromMetersPerSecond(10.4), start),
                                    new SpeedRecord.SpeedRecordSample(
                                            Velocity.fromMetersPerSecond(9.4), midTime),
                                    new SpeedRecord.SpeedRecordSample(
                                            Velocity.fromMetersPerSecond(8.4), end)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(StepsCadenceRecord.class)) {
            return new StepsCadenceRecord.Builder(
                            metadata,
                            start,
                            end,
                            List.of(
                                    new StepsCadenceRecord.StepsCadenceRecordSample(3.4, start),
                                    new StepsCadenceRecord.StepsCadenceRecordSample(3.5, midTime),
                                    new StepsCadenceRecord.StepsCadenceRecordSample(3.6, end)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(BasalMetabolicRateRecord.class)) {
            return new BasalMetabolicRateRecord.Builder(metadata, start, Power.fromWatts(1.2))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BodyFatRecord.class)) {
            return new BodyFatRecord.Builder(metadata, start, Percentage.fromValue(19.1))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(Vo2MaxRecord.class)) {
            return new Vo2MaxRecord.Builder(
                            metadata, start, MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST, 12.3)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(CervicalMucusRecord.class)) {
            return new CervicalMucusRecord.Builder(
                            metadata, start, SENSATION_MEDIUM, APPEARANCE_DRY)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BasalBodyTemperatureRecord.class)) {
            return new BasalBodyTemperatureRecord.Builder(
                            metadata,
                            start,
                            MEASUREMENT_LOCATION_ARMPIT,
                            Temperature.fromCelsius(36.4))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(MenstruationFlowRecord.class)) {
            return new MenstruationFlowRecord.Builder(metadata, start, FLOW_MEDIUM)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(OxygenSaturationRecord.class)) {
            return new OxygenSaturationRecord.Builder(metadata, start, Percentage.fromValue(98.2))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BloodPressureRecord.class)) {
            return new BloodPressureRecord.Builder(
                            metadata,
                            start,
                            BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
                            Pressure.fromMillimetersOfMercury(120.5),
                            Pressure.fromMillimetersOfMercury(79.9),
                            BODY_POSITION_SITTING_DOWN)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(HeightRecord.class)) {
            return new HeightRecord.Builder(metadata, start, Length.fromMeters(1.78))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BloodGlucoseRecord.class)) {
            return new BloodGlucoseRecord.Builder(
                            metadata,
                            start,
                            SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                            BloodGlucose.fromMillimolesPerLiter(1.23),
                            RELATION_TO_MEAL_FASTING,
                            MEAL_TYPE_BREAKFAST)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(WeightRecord.class)) {
            return new WeightRecord.Builder(metadata, start, Mass.fromGrams(79_500.1))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(LeanBodyMassRecord.class)) {
            return new LeanBodyMassRecord.Builder(metadata, start, Mass.fromGrams(62.1))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(SexualActivityRecord.class)) {
            return new SexualActivityRecord.Builder(metadata, start, PROTECTION_USED_PROTECTED)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BodyTemperatureRecord.class)) {
            return new BodyTemperatureRecord.Builder(
                            metadata,
                            start,
                            MEASUREMENT_LOCATION_ARMPIT,
                            Temperature.fromCelsius(36.4))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(OvulationTestRecord.class)) {
            return new OvulationTestRecord.Builder(metadata, start, RESULT_HIGH)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(RespiratoryRateRecord.class)) {
            return new RespiratoryRateRecord.Builder(metadata, start, 60.3)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BoneMassRecord.class)) {
            return new BoneMassRecord.Builder(metadata, start, Mass.fromGrams(15_123.4))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(RestingHeartRateRecord.class)) {
            return new RestingHeartRateRecord.Builder(metadata, start, 55)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(BodyWaterMassRecord.class)) {
            return new BodyWaterMassRecord.Builder(metadata, start, Mass.fromGrams(20_123.4))
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(HeartRateVariabilityRmssdRecord.class)) {
            return new HeartRateVariabilityRmssdRecord.Builder(metadata, start, 100.1)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(IntermenstrualBleedingRecord.class)) {
            return new IntermenstrualBleedingRecord.Builder(metadata, start)
                    .setZoneOffset(startZoneOffset)
                    .build();
        } else if (recordClass.equals(ExerciseSessionRecord.class)) {
            return makeExerciseSessionRecord(metadata, start, startZoneOffset, end, endZoneOffset);
        } else if (recordClass.equals(SleepSessionRecord.class)) {
            return new SleepSessionRecord.Builder(metadata, start, end)
                    .setStages(
                            List.of(
                                    new SleepSessionRecord.Stage(
                                            start,
                                            midTime,
                                            SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT),
                                    new SleepSessionRecord.Stage(
                                            midTime,
                                            end,
                                            SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP)))
                    .setNotes("notes")
                    .setTitle("title")
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(SkinTemperatureRecord.class)) {
            return new SkinTemperatureRecord.Builder(metadata, start, end)
                    .setBaseline(Temperature.fromCelsius(35.4))
                    .setMeasurementLocation(MEASUREMENT_LOCATION_WRIST)
                    .setDeltas(
                            List.of(
                                    new SkinTemperatureRecord.Delta(
                                            TemperatureDelta.fromCelsius(0.0), start),
                                    new SkinTemperatureRecord.Delta(
                                            TemperatureDelta.fromCelsius(0.5), midTime),
                                    new SkinTemperatureRecord.Delta(
                                            TemperatureDelta.fromCelsius(0.3), end)))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(PlannedExerciseSessionRecord.class)) {
            return new PlannedExerciseSessionRecord.Builder(
                            metadata, EXERCISE_SESSION_TYPE_BIKING, start, end)
                    .setNotes("Some notes")
                    .setTitle("A title")
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .setBlocks(
                            List.of(
                                    new PlannedExerciseBlock.Builder(3)
                                            .setDescription("description")
                                            .build(),
                                    new PlannedExerciseBlock.Builder(4)
                                            .setDescription("another description")
                                            .build()))
                    .build();
        } else if (recordClass.equals(MindfulnessSessionRecord.class)) {
            return new MindfulnessSessionRecord.Builder(
                            metadata, start, end, MINDFULNESS_SESSION_TYPE_MEDITATION)
                    .setTitle("title")
                    .setNotes("notes")
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(ActivityIntensityRecord.class)) {
            return new ActivityIntensityRecord.Builder(
                            metadata, start, end, ACTIVITY_INTENSITY_TYPE_MODERATE)
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else if (recordClass.equals(NicotineIntakeRecord.class)) {
            return new NicotineIntakeRecord.Builder(
                            metadata,
                            start,
                            end,
                            /* quantity= */ 20,
                            NICOTINE_INTAKE_TYPE_CIGARETTE)
                    .setNicotineIntake(Mass.fromGrams(5.2))
                    .setStartZoneOffset(startZoneOffset)
                    .setEndZoneOffset(endZoneOffset)
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown record class " + recordClass);
        }
    }

    private static ExerciseSessionRecord makeExerciseSessionRecord(
            Metadata metadata,
            Instant start,
            ZoneOffset startZoneOffset,
            Instant end,
            ZoneOffset endZoneOffset) {
        ExerciseRoute route =
                new ExerciseRoute(
                        List.of(
                                new ExerciseRoute.Location.Builder(
                                                Instant.ofEpochMilli(start.toEpochMilli() + 10),
                                                1.0,
                                                2.0)
                                        .build(),
                                new ExerciseRoute.Location.Builder(
                                                Instant.ofEpochMilli(start.toEpochMilli() + 20),
                                                2.0,
                                                3.0)
                                        .build(),
                                new ExerciseRoute.Location.Builder(
                                                Instant.ofEpochMilli(start.toEpochMilli() + 30),
                                                4.0,
                                                5.0)
                                        .build()));
        String notes = "rain";
        String title = "Morning training";
        List<ExerciseSegment> segmentList =
                List.of(
                        new ExerciseSegment.Builder(start, end, EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)
                                .setRepetitionsCount(10)
                                .build());

        List<ExerciseLap> lapsList =
                List.of(
                        new ExerciseLap.Builder(start, end)
                                .setLength(Length.fromMeters(10))
                                .build());
        return new ExerciseSessionRecord.Builder(
                        metadata, start, end, EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(route)
                .setEndZoneOffset(startZoneOffset)
                .setStartZoneOffset(endZoneOffset)
                .setNotes(notes)
                .setTitle(title)
                .setSegments(segmentList)
                .setLaps(lapsList)
                .build();
    }

    private static NutritionRecord makePopulatedNutritionRecord(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .setUnsaturatedFat(Mass.fromGrams(0.1))
                .setPotassium(Mass.fromGrams(0.1))
                .setThiamin(Mass.fromGrams(0.1))
                .setMealType(MEAL_TYPE_BREAKFAST)
                .setTransFat(Mass.fromGrams(0.1))
                .setManganese(Mass.fromGrams(0.1))
                .setEnergyFromFat(Energy.fromCalories(0.1))
                .setCaffeine(Mass.fromGrams(0.1))
                .setDietaryFiber(Mass.fromGrams(0.1))
                .setSelenium(Mass.fromGrams(0.1))
                .setVitaminB6(Mass.fromGrams(0.1))
                .setProtein(Mass.fromGrams(0.1))
                .setChloride(Mass.fromGrams(0.1))
                .setCholesterol(Mass.fromGrams(0.1))
                .setCopper(Mass.fromGrams(0.1))
                .setIodine(Mass.fromGrams(0.1))
                .setVitaminB12(Mass.fromGrams(0.1))
                .setZinc(Mass.fromGrams(0.1))
                .setRiboflavin(Mass.fromGrams(0.1))
                .setEnergy(Energy.fromCalories(0.1))
                .setMolybdenum(Mass.fromGrams(0.1))
                .setPhosphorus(Mass.fromGrams(0.1))
                .setChromium(Mass.fromGrams(0.1))
                .setTotalFat(Mass.fromGrams(0.1))
                .setCalcium(Mass.fromGrams(0.1))
                .setVitaminC(Mass.fromGrams(0.1))
                .setVitaminE(Mass.fromGrams(0.1))
                .setBiotin(Mass.fromGrams(0.1))
                .setVitaminD(Mass.fromGrams(0.1))
                .setNiacin(Mass.fromGrams(0.1))
                .setMagnesium(Mass.fromGrams(0.1))
                .setTotalCarbohydrate(Mass.fromGrams(0.1))
                .setVitaminK(Mass.fromGrams(0.1))
                .setPolyunsaturatedFat(Mass.fromGrams(0.1))
                .setSaturatedFat(Mass.fromGrams(0.1))
                .setSodium(Mass.fromGrams(0.1))
                .setFolate(Mass.fromGrams(0.1))
                .setMonounsaturatedFat(Mass.fromGrams(0.1))
                .setPantothenicAcid(Mass.fromGrams(0.1))
                .setMealName("Brunch")
                .setIron(Mass.fromGrams(0.1))
                .setVitaminA(Mass.fromGrams(0.1))
                .setFolicAcid(Mass.fromGrams(0.1))
                .setSugar(Mass.fromGrams(0.1))
                .build();
    }
}
