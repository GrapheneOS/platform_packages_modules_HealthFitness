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
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.BasalBodyTemperatureRecord
import android.health.connect.datatypes.BasalMetabolicRateRecord
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BodyFatRecord
import android.health.connect.datatypes.BodyTemperatureRecord
import android.health.connect.datatypes.BodyWaterMassRecord
import android.health.connect.datatypes.BoneMassRecord
import android.health.connect.datatypes.CervicalMucusRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.FloorsClimbedRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.HydrationRecord
import android.health.connect.datatypes.IntermenstrualBleedingRecord
import android.health.connect.datatypes.LeanBodyMassRecord
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord
import android.health.connect.datatypes.SexualActivityRecord
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.WheelchairPushesRecord
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry

class DataEntryFormatter(
    private val powerFormatter: PowerFormatter = PowerFormatter(),
    private val stepsFormatter: StepsFormatter = StepsFormatter(),
    private val heartRateFormatter: HeartRateFormatter = HeartRateFormatter(),
    private val distanceFormatter: DistanceFormatter = DistanceFormatter(),
    private val elevationGainedFormatter: ElevationGainedFormatter = ElevationGainedFormatter(),
    private val floorsClimbedFormatter: FloorsClimbedFormatter = FloorsClimbedFormatter(),
    private val stepsCadenceFormatter: StepsCadenceFormatter = StepsCadenceFormatter(),
    private val totalCaloriesBurnedFormatter: TotalCaloriesBurnedFormatter =
        TotalCaloriesBurnedFormatter(),
    private val vo2MaxFormatter: Vo2MaxFormatter = Vo2MaxFormatter(),
    private val wheelchairPushesFormatter: WheelchairPushesFormatter = WheelchairPushesFormatter(),
    private val speedFormatter: SpeedFormatter = SpeedFormatter(),
    private val cyclingPedalingCadenceFormatter: CyclingPedalingCadenceFormatter =
        CyclingPedalingCadenceFormatter(),
    private val basalMetabolicRateFormatter: BasalMetabolicRateFormatter =
        BasalMetabolicRateFormatter(),
    private val bodyFatFormatter: BodyFatFormatter = BodyFatFormatter(),
    private val bodyWaterMassFormatter: BodyWaterMassFormatter = BodyWaterMassFormatter(),
    private val boneMassFormatter: BoneMassFormatter = BoneMassFormatter(),
    private val heightFormatter: HeightFormatter = HeightFormatter(),
    private val leanBodyMassFormatter: LeanBodyMassFormatter = LeanBodyMassFormatter(),
    private val weightFormatter: WeightFormatter = WeightFormatter(),
    private val cervicalMucusFormatter: CervicalMucusFormatter = CervicalMucusFormatter(),
    private val hydrationFormatter: HydrationFormatter = HydrationFormatter(),
    private val nutritionFormatter: NutritionFormatter = NutritionFormatter(),
    private val activeCaloriesBurnedFormatter: ActiveCaloriesBurnedFormatter =
        ActiveCaloriesBurnedFormatter(),
    private val menstruationFlowFormatter: MenstruationFlowFormatter = MenstruationFlowFormatter(),
    private val menstruationPeriodFormatter: MenstruationPeriodFormatter =
        MenstruationPeriodFormatter(),
    private val ovulationTestFormatter: OvulationTestFormatter = OvulationTestFormatter(),
    private val sexualActivityFormatter: SexualActivityFormatter = SexualActivityFormatter(),
    private val intermenstrualBleedingFormatter: IntermenstrualBleedingFormatter =
        IntermenstrualBleedingFormatter(),
    private val basalBodyTemperatureFormatter: BasalBodyTemperatureFormatter =
        BasalBodyTemperatureFormatter(),
    private val bloodGlucoseFormatter: BloodGlucoseFormatter = BloodGlucoseFormatter(),
    private val bloodPressureFormatter: BloodPressureFormatter = BloodPressureFormatter(),
    private val bodyTemperatureFormatter: BodyTemperatureFormatter = BodyTemperatureFormatter(),
    private val heartRateVariabilityRmssdFormatter: HeartRateVariabilityRmssdFormatter =
        HeartRateVariabilityRmssdFormatter(),
    private val oxygenSaturationFormatter: OxygenSaturationFormatter = OxygenSaturationFormatter(),
    private val respiratoryRateFormatter: RespiratoryRateFormatter = RespiratoryRateFormatter(),
    private val restingHeartRateFormatter: RestingHeartRateFormatter = RestingHeartRateFormatter(),
    private val skinTemperatureFormatter: SkinTemperatureFormatter = SkinTemperatureFormatter(),
    private val activityIntensityFormatter: ActivityIntensityFormatter =
        ActivityIntensityFormatter(),
    private val exerciseSessionFormatter: ExerciseSessionFormatter = ExerciseSessionFormatter(),
    private val plannedExerciseSessionFormatter: PlannedExerciseSessionFormatter =
        PlannedExerciseSessionFormatter(),
    private val sleepSessionFormatter: SleepSessionFormatter = SleepSessionFormatter(),
    private val mindfulnessSessionFormatter: MindfulnessSessionFormatter =
        MindfulnessSessionFormatter(),
    private val nicotineIntakeFormatter: NicotineIntakeFormatter = NicotineIntakeFormatter(),
    private val symptomFormatter: SymptomFormatter = SymptomFormatter(),
) {

    fun format(record: Record, context: Context): FormattedEntry {
        return when (record) {

            // ACTIVITY
            is ActiveCaloriesBurnedRecord -> activeCaloriesBurnedFormatter.format(record, context)
            is ActivityIntensityRecord -> activityIntensityFormatter.format(record, context)
            is DistanceRecord -> distanceFormatter.format(record, context)
            is ElevationGainedRecord -> elevationGainedFormatter.format(record, context)
            is FloorsClimbedRecord -> floorsClimbedFormatter.format(record, context)
            is StepsRecord -> stepsFormatter.format(record, context)
            is StepsCadenceRecord -> stepsCadenceFormatter.format(record, context)
            is TotalCaloriesBurnedRecord -> totalCaloriesBurnedFormatter.format(record, context)
            is Vo2MaxRecord -> vo2MaxFormatter.format(record, context)
            is WheelchairPushesRecord -> wheelchairPushesFormatter.format(record, context)
            is PowerRecord -> powerFormatter.format(record, context)
            is SpeedRecord -> speedFormatter.format(record, context)
            is CyclingPedalingCadenceRecord ->
                cyclingPedalingCadenceFormatter.format(record, context)
            is ExerciseSessionRecord -> exerciseSessionFormatter.format(record, context)
            is PlannedExerciseSessionRecord ->
                plannedExerciseSessionFormatter.format(record, context)

            // BODY_MEASUREMENTS
            is BasalMetabolicRateRecord -> basalMetabolicRateFormatter.format(record, context)
            is BodyFatRecord -> bodyFatFormatter.format(record)
            is BodyWaterMassRecord -> bodyWaterMassFormatter.format(record, context)
            is BoneMassRecord -> boneMassFormatter.format(record, context)
            is HeightRecord -> heightFormatter.format(record, context)
            is LeanBodyMassRecord -> leanBodyMassFormatter.format(record, context)
            is WeightRecord -> weightFormatter.format(record, context)

            // CYCLE_TRACKING
            is CervicalMucusRecord -> cervicalMucusFormatter.format(record, context)
            is MenstruationFlowRecord -> menstruationFlowFormatter.format(record, context)
            is MenstruationPeriodRecord -> menstruationPeriodFormatter.format(record, context)
            is OvulationTestRecord -> ovulationTestFormatter.format(record, context)
            is SexualActivityRecord -> sexualActivityFormatter.format(record, context)
            is IntermenstrualBleedingRecord ->
                intermenstrualBleedingFormatter.format(record, context)

            // NUTRITION
            is HydrationRecord -> hydrationFormatter.format(record, context)
            is NutritionRecord -> nutritionFormatter.format(record, context)

            // SLEEP
            is SleepSessionRecord -> sleepSessionFormatter.format(record, context)

            // VITALS
            is BasalBodyTemperatureRecord -> basalBodyTemperatureFormatter.format(record, context)
            is BloodGlucoseRecord -> bloodGlucoseFormatter.format(record, context)
            is BloodPressureRecord -> bloodPressureFormatter.format(record, context)
            is BodyTemperatureRecord -> bodyTemperatureFormatter.format(record, context)
            is HeartRateRecord -> heartRateFormatter.format(record, context)
            is HeartRateVariabilityRmssdRecord ->
                heartRateVariabilityRmssdFormatter.format(record, context)
            is OxygenSaturationRecord -> oxygenSaturationFormatter.format(record)
            is RespiratoryRateRecord -> respiratoryRateFormatter.format(record, context)
            is RestingHeartRateRecord -> restingHeartRateFormatter.format(record, context)
            is SkinTemperatureRecord -> skinTemperatureFormatter.format(record, context)

            // WELLNESS
            is MindfulnessSessionRecord -> mindfulnessSessionFormatter.format(record, context)
            is NicotineIntakeRecord -> nicotineIntakeFormatter.format(record, context)
            is SymptomRecord -> symptomFormatter.format(record, context)

            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }
}
