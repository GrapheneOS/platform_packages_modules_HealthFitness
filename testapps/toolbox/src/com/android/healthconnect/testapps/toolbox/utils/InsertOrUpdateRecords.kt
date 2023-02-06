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
package com.android.healthconnect.testapps.toolbox.utils

import android.content.Context
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
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
import android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.FloorsClimbedRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeartRateRecord.HeartRateSample
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.HydrationRecord
import android.health.connect.datatypes.IntermenstrualBleedingRecord
import android.health.connect.datatypes.LeanBodyMassRecord
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.PowerRecord.PowerRecordSample
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord
import android.health.connect.datatypes.SexualActivityRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.SpeedRecord.SpeedRecordSample
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.WheelchairPushesRecord
import android.health.connect.datatypes.units.BloodGlucose
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Pressure
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.Volume
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import java.time.Instant
import kotlin.reflect.KClass

class InsertOrUpdateRecords {

    companion object {
        fun createRecordObject(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            context: Context,
            recordUuid: String,
        ): Record {
            return createRecordObjectHelper(
                recordClass, mFieldNameToFieldInput, getMetaData(context, recordUuid))
        }

        fun createRecordObject(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            context: Context,
        ): Record {
            return createRecordObjectHelper(
                recordClass, mFieldNameToFieldInput, getMetaData(context))
        }

        private fun createRecordObjectHelper(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            metaData: Metadata,
        ): Record {

            val record: Record
            when (recordClass) {
                StepsRecord::class -> {
                    record =
                        StepsRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mCount"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toLong())
                            .build()
                }
                DistanceRecord::class -> {
                    record =
                        DistanceRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mDistance"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                ActiveCaloriesBurnedRecord::class -> {
                    record =
                        ActiveCaloriesBurnedRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Energy.fromJoules(
                                    mFieldNameToFieldInput["mEnergy"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                ElevationGainedRecord::class -> {
                    record =
                        ElevationGainedRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mElevation"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                BasalMetabolicRateRecord::class -> {
                    record =
                        BasalMetabolicRateRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Power.fromWatts(
                                    mFieldNameToFieldInput["mBasalMetabolicRate"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                SpeedRecord::class -> {
                    record =
                        SpeedRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mSpeedRecordSamples"]?.getFieldValue()
                                    as List<SpeedRecordSample>)
                            .build()
                }
                HeartRateRecord::class -> {
                    record =
                        HeartRateRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mHeartRateSamples"]?.getFieldValue()
                                    as List<HeartRateSample>)
                            .build()
                }
                PowerRecord::class -> {
                    record =
                        PowerRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mPowerRecordSamples"]?.getFieldValue()
                                    as List<PowerRecordSample>)
                            .build()
                }
                CyclingPedalingCadenceRecord::class -> {
                    record =
                        CyclingPedalingCadenceRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mCyclingPedalingCadenceRecordSamples"]
                                    ?.getFieldValue() as List<CyclingPedalingCadenceRecordSample>)
                            .build()
                }
                FloorsClimbedRecord::class -> {
                    record =
                        FloorsClimbedRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mFloors"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                TotalCaloriesBurnedRecord::class -> {
                    record =
                        TotalCaloriesBurnedRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Energy.fromJoules(
                                    mFieldNameToFieldInput["mEnergy"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                WheelchairPushesRecord::class -> {
                    record =
                        WheelchairPushesRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mCount"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toLong())
                            .build()
                }
                Vo2MaxRecord::class -> {
                    record =
                        Vo2MaxRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mMeasurementMethod"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                mFieldNameToFieldInput["mVo2MillilitersPerMinuteKilogram"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toDouble())
                            .build()
                }
                BodyFatRecord::class -> {
                    record =
                        BodyFatRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Percentage.fromValue(
                                    mFieldNameToFieldInput["mPercentage"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                BodyWaterMassRecord::class -> {
                    record =
                        BodyWaterMassRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mBodyWaterMass"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                BoneMassRecord::class -> {
                    record =
                        BoneMassRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mMass"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                HeightRecord::class -> {
                    record =
                        HeightRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mHeight"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                LeanBodyMassRecord::class -> {
                    record =
                        LeanBodyMassRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mMass"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                WeightRecord::class -> {
                    record =
                        WeightRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mWeight"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                CervicalMucusRecord::class -> {
                    record =
                        CervicalMucusRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mSensation"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                mFieldNameToFieldInput["mAppearance"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                MenstruationFlowRecord::class -> {
                    record =
                        MenstruationFlowRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mFlow"]?.getFieldValue().toString().toInt())
                            .build()
                }
                OvulationTestRecord::class -> {
                    record =
                        OvulationTestRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mResult"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                SexualActivityRecord::class -> {
                    record =
                        SexualActivityRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mProtectionUsed"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                HydrationRecord::class -> {
                    record =
                        HydrationRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Volume.fromMilliliters(
                                    mFieldNameToFieldInput["mVolume"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                IntermenstrualBleedingRecord::class -> {
                    record =
                        IntermenstrualBleedingRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant)
                            .build()
                }
                BasalBodyTemperatureRecord::class -> {
                    record =
                        BasalBodyTemperatureRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mBodyTemperatureMeasurementLocation"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                Temperature.fromCelsius(
                                    mFieldNameToFieldInput["mTemperature"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                BloodGlucoseRecord::class -> {
                    record =
                        BloodGlucoseRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mSpecimenSource"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                BloodGlucose.fromMillimolesPerLiter(
                                    mFieldNameToFieldInput["mLevel"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()),
                                mFieldNameToFieldInput["mRelationToMeal"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                mFieldNameToFieldInput["mMealType"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                BloodPressureRecord::class -> {
                    record =
                        BloodPressureRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mMeasurementLocation"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                Pressure.fromMillimetersOfMercury(
                                    mFieldNameToFieldInput["mSystolic"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()),
                                Pressure.fromMillimetersOfMercury(
                                    mFieldNameToFieldInput["mDiastolic"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()),
                                mFieldNameToFieldInput["mBodyPosition"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt())
                            .build()
                }
                BodyTemperatureRecord::class -> {
                    record =
                        BodyTemperatureRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mMeasurementLocation"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toInt(),
                                Temperature.fromCelsius(
                                    mFieldNameToFieldInput["mTemperature"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                HeartRateVariabilityRmssdRecord::class -> {
                    record =
                        HeartRateVariabilityRmssdRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mHeartRateVariabilityMillis"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toDouble())
                            .build()
                }
                OxygenSaturationRecord::class -> {
                    record =
                        OxygenSaturationRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Percentage.fromValue(
                                    mFieldNameToFieldInput["mPercentage"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                RespiratoryRateRecord::class -> {
                    record =
                        RespiratoryRateRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mRate"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toDouble())
                            .build()
                }
                RestingHeartRateRecord::class -> {
                    record =
                        RestingHeartRateRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mBeatsPerMinute"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toLong())
                            .build()
                }
                SleepSessionRecord::class -> {
                    record =
                        SleepSessionRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant)
                            .setNotes(mFieldNameToFieldInput["mNotes"]?.getFieldValue().toString())
                            .setTitle(mFieldNameToFieldInput["mTitle"]?.getFieldValue().toString())
                            .setStages(
                                mFieldNameToFieldInput["mStages"]?.getFieldValue()
                                    as List<SleepSessionRecord.Stage>)
                            .build()
                }
                StepsCadenceRecord::class -> {
                    record =
                        StepsCadenceRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mStepsCadenceRecordSamples"]
                                    ?.getFieldValue() as List<StepsCadenceRecordSample>)
                            .build()
                }
                MenstruationPeriodRecord::class -> {
                    record =
                        MenstruationPeriodRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant)
                            .build()
                }
                NutritionRecord::class -> {
                    record =
                        NutritionRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant)
                            .setBiotin(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mBiotin"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setCaffeine(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mCaffeine"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setCalcium(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mCalcium"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setChloride(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mChloride"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setCholesterol(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mCholesterol"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setChromium(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mChromium"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setCopper(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mCopper"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setDietaryFiber(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mDietaryFiber"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setEnergy(
                                Energy.fromJoules(
                                    mFieldNameToFieldInput["mEnergy"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setEnergyFromFat(
                                Energy.fromJoules(
                                    mFieldNameToFieldInput["mEnergyFromFat"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setFolate(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mFolate"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setFolicAcid(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mFolicAcid"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setIodine(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mIodine"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setIron(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mIron"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setMagnesium(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mMagnesium"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .setManganese(
                                Mass.fromKilograms(
                                    mFieldNameToFieldInput["mManganese"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build()
                }
                else -> {
                    throw NotImplementedError("Record type not implemented")
                }
            }
            return record
        }
    }
}
