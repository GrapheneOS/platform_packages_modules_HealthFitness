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
import android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.ExerciseLap
import android.health.connect.datatypes.ExerciseSegment
import android.health.connect.datatypes.ExerciseSessionRecord
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
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.NutritionRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.PlannedExerciseBlock
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.PowerRecord.PowerRecordSample
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord
import android.health.connect.datatypes.SexualActivityRecord
import android.health.connect.datatypes.SkinTemperatureRecord
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
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData.Companion.generateExerciseRouteFromLocations
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import java.time.Instant
import kotlin.reflect.KClass

class InsertOrUpdateRecords {

    companion object {

        private fun getStringValue(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): String {
            return mFieldNameToFieldInput[fieldName]?.getFieldValue().toString()
        }

        private fun getIntegerValue(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Int {
            return getStringValue(mFieldNameToFieldInput, fieldName).toInt()
        }

        private fun getLongValue(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Long {
            return getStringValue(mFieldNameToFieldInput, fieldName).toLong()
        }

        private fun getDoubleValue(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Double {
            return getStringValue(mFieldNameToFieldInput, fieldName).toDouble()
        }

        private fun getStartTime(mFieldNameToFieldInput: HashMap<String, InputFieldView>): Instant {
            return mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant
        }

        private fun getEndTime(mFieldNameToFieldInput: HashMap<String, InputFieldView>): Instant {
            return mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant
        }

        private fun getTime(mFieldNameToFieldInput: HashMap<String, InputFieldView>): Instant {
            return mFieldNameToFieldInput["time"]?.getFieldValue() as Instant
        }

        private fun getMass(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Mass {
            return Mass.fromGrams(getDoubleValue(mFieldNameToFieldInput, fieldName))
        }

        private fun getEnergy(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Energy {
            return Energy.fromCalories(getDoubleValue(mFieldNameToFieldInput, fieldName))
        }

        private fun getTemperature(
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            fieldName: String,
        ): Temperature {
            return Temperature.fromCelsius(getDoubleValue(mFieldNameToFieldInput, fieldName))
        }

        fun createRecordObject(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            context: Context,
            recordUuid: String,
        ): Record {
            return createRecordObjectHelper(
                recordClass,
                mFieldNameToFieldInput,
                getMetaData(context, recordUuid),
            )
        }

        fun createRecordObject(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            context: Context,
        ): Record {
            return createRecordObjectHelper(
                recordClass,
                mFieldNameToFieldInput,
                getMetaData(context),
            )
        }

        private fun createRecordObjectHelper(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            metaData: Metadata,
        ): Record {
            return when (recordClass) {
                StepsRecord::class ->
                    StepsRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            getLongValue(mFieldNameToFieldInput, "mCount"),
                        )
                        .build()

                DistanceRecord::class ->
                    DistanceRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            Length.fromMeters(getDoubleValue(mFieldNameToFieldInput, "mDistance")),
                        )
                        .build()

                ActiveCaloriesBurnedRecord::class ->
                    ActiveCaloriesBurnedRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            Energy.fromCalories(getDoubleValue(mFieldNameToFieldInput, "mEnergy")),
                        )
                        .build()

                ElevationGainedRecord::class ->
                    ElevationGainedRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            Length.fromMeters(getDoubleValue(mFieldNameToFieldInput, "mElevation")),
                        )
                        .build()

                BasalMetabolicRateRecord::class ->
                    BasalMetabolicRateRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            Power.fromWatts(
                                getDoubleValue(mFieldNameToFieldInput, "mBasalMetabolicRate")
                            ),
                        )
                        .build()

                SpeedRecord::class ->
                    SpeedRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mSpeedRecordSamples"]?.getFieldValue()
                                as List<SpeedRecordSample>,
                        )
                        .build()

                HeartRateRecord::class ->
                    HeartRateRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mHeartRateSamples"]?.getFieldValue()
                                as List<HeartRateSample>,
                        )
                        .build()

                PowerRecord::class ->
                    PowerRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mPowerRecordSamples"]?.getFieldValue()
                                as List<PowerRecordSample>,
                        )
                        .build()

                CyclingPedalingCadenceRecord::class ->
                    CyclingPedalingCadenceRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mCyclingPedalingCadenceRecordSamples"]
                                ?.getFieldValue() as List<CyclingPedalingCadenceRecordSample>,
                        )
                        .build()

                FloorsClimbedRecord::class ->
                    FloorsClimbedRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            getDoubleValue(mFieldNameToFieldInput, "mFloors"),
                        )
                        .build()

                TotalCaloriesBurnedRecord::class ->
                    TotalCaloriesBurnedRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            Energy.fromCalories(getDoubleValue(mFieldNameToFieldInput, "mEnergy")),
                        )
                        .build()

                WheelchairPushesRecord::class ->
                    WheelchairPushesRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            getLongValue(mFieldNameToFieldInput, "mCount"),
                        )
                        .build()

                Vo2MaxRecord::class ->
                    Vo2MaxRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mMeasurementMethod"),
                            getDoubleValue(
                                mFieldNameToFieldInput,
                                "mVo2MillilitersPerMinuteKilogram",
                            ),
                        )
                        .build()

                BodyFatRecord::class ->
                    BodyFatRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            Percentage.fromValue(
                                getDoubleValue(mFieldNameToFieldInput, "mPercentage")
                            ),
                        )
                        .build()

                BodyWaterMassRecord::class ->
                    BodyWaterMassRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getMass(mFieldNameToFieldInput, "mBodyWaterMass"),
                        )
                        .build()

                BoneMassRecord::class ->
                    BoneMassRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getMass(mFieldNameToFieldInput, "mMass"),
                        )
                        .build()

                HeightRecord::class ->
                    HeightRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            Length.fromMeters(getDoubleValue(mFieldNameToFieldInput, "mHeight")),
                        )
                        .build()

                LeanBodyMassRecord::class ->
                    LeanBodyMassRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getMass(mFieldNameToFieldInput, "mMass"),
                        )
                        .build()

                WeightRecord::class ->
                    WeightRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getMass(mFieldNameToFieldInput, "mWeight"),
                        )
                        .build()

                CervicalMucusRecord::class ->
                    CervicalMucusRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mSensation"),
                            getIntegerValue(mFieldNameToFieldInput, "mAppearance"),
                        )
                        .build()

                MenstruationFlowRecord::class ->
                    MenstruationFlowRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mFlow"),
                        )
                        .build()

                OvulationTestRecord::class ->
                    OvulationTestRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mResult"),
                        )
                        .build()

                SexualActivityRecord::class ->
                    SexualActivityRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mProtectionUsed"),
                        )
                        .build()

                HydrationRecord::class ->
                    HydrationRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            Volume.fromLiters(getDoubleValue(mFieldNameToFieldInput, "mVolume")),
                        )
                        .build()

                IntermenstrualBleedingRecord::class ->
                    IntermenstrualBleedingRecord.Builder(metaData, getTime(mFieldNameToFieldInput))
                        .build()

                BasalBodyTemperatureRecord::class ->
                    BasalBodyTemperatureRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(
                                mFieldNameToFieldInput,
                                "mBodyTemperatureMeasurementLocation",
                            ),
                            Temperature.fromCelsius(
                                getDoubleValue(mFieldNameToFieldInput, "mTemperature")
                            ),
                        )
                        .build()

                BloodGlucoseRecord::class ->
                    BloodGlucoseRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mSpecimenSource"),
                            BloodGlucose.fromMillimolesPerLiter(
                                getDoubleValue(mFieldNameToFieldInput, "mLevel")
                            ),
                            getIntegerValue(mFieldNameToFieldInput, "mRelationToMeal"),
                            getIntegerValue(mFieldNameToFieldInput, "mMealType"),
                        )
                        .build()

                BloodPressureRecord::class ->
                    BloodPressureRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mMeasurementLocation"),
                            Pressure.fromMillimetersOfMercury(
                                getDoubleValue(mFieldNameToFieldInput, "mSystolic")
                            ),
                            Pressure.fromMillimetersOfMercury(
                                getDoubleValue(mFieldNameToFieldInput, "mDiastolic")
                            ),
                            getIntegerValue(mFieldNameToFieldInput, "mBodyPosition"),
                        )
                        .build()

                BodyTemperatureRecord::class ->
                    BodyTemperatureRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getIntegerValue(mFieldNameToFieldInput, "mMeasurementLocation"),
                            Temperature.fromCelsius(
                                getDoubleValue(mFieldNameToFieldInput, "mTemperature")
                            ),
                        )
                        .build()

                HeartRateVariabilityRmssdRecord::class ->
                    HeartRateVariabilityRmssdRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getDoubleValue(mFieldNameToFieldInput, "mHeartRateVariabilityMillis"),
                        )
                        .build()

                OxygenSaturationRecord::class ->
                    OxygenSaturationRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            Percentage.fromValue(
                                getDoubleValue(mFieldNameToFieldInput, "mPercentage")
                            ),
                        )
                        .build()

                RespiratoryRateRecord::class ->
                    RespiratoryRateRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getDoubleValue(mFieldNameToFieldInput, "mRate"),
                        )
                        .build()

                RestingHeartRateRecord::class ->
                    RestingHeartRateRecord.Builder(
                            metaData,
                            getTime(mFieldNameToFieldInput),
                            getLongValue(mFieldNameToFieldInput, "mBeatsPerMinute"),
                        )
                        .build()

                SkinTemperatureRecord::class ->
                    SkinTemperatureRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                        )
                        .apply {
                            OxygenSaturationRecord.Builder(
                                    metaData,
                                    getTime(mFieldNameToFieldInput),
                                    Percentage.fromValue(
                                        getDoubleValue(mFieldNameToFieldInput, "mPercentage")
                                    ),
                                )
                                .build()
                            if (!mFieldNameToFieldInput["mDeltas"]!!.isEmpty()) {
                                setDeltas(
                                    mFieldNameToFieldInput["mDeltas"]?.getFieldValue()
                                        as List<SkinTemperatureRecord.Delta>
                                )
                            }
                            if (!mFieldNameToFieldInput["mBaseline"]!!.isEmpty()) {
                                setBaseline(getTemperature(mFieldNameToFieldInput, "mBaseline"))
                            }
                            if (!mFieldNameToFieldInput["mMeasurementLocation"]!!.isEmpty()) {
                                setMeasurementLocation(
                                    getIntegerValue(mFieldNameToFieldInput, "mMeasurementLocation")
                                )
                            }
                        }
                        .build()

                SleepSessionRecord::class ->
                    SleepSessionRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                        )
                        .apply {
                            if (!mFieldNameToFieldInput["mNotes"]!!.isEmpty()) {
                                setNotes(getStringValue(mFieldNameToFieldInput, "mNotes"))
                            }
                            if (!mFieldNameToFieldInput["mTitle"]!!.isEmpty()) {
                                setTitle(getStringValue(mFieldNameToFieldInput, "mTitle"))
                            }
                            if (!mFieldNameToFieldInput["mStages"]!!.isEmpty()) {
                                setStages(
                                    mFieldNameToFieldInput["mStages"]?.getFieldValue()
                                        as List<SleepSessionRecord.Stage>
                                )
                            }
                        }
                        .build()

                StepsCadenceRecord::class ->
                    StepsCadenceRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mStepsCadenceRecordSamples"]?.getFieldValue()
                                as List<StepsCadenceRecordSample>,
                        )
                        .build()

                MenstruationPeriodRecord::class ->
                    MenstruationPeriodRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                        )
                        .build()

                ExerciseSessionRecord::class -> {
                    val startTime = mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant

                    ExerciseSessionRecord.Builder(
                            metaData,
                            startTime,
                            mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                            mFieldNameToFieldInput["mExerciseType"]
                                ?.getFieldValue()
                                .toString()
                                .toInt(),
                        )
                        .apply {
                            if (!mFieldNameToFieldInput["mNotes"]!!.isEmpty()) {
                                setNotes(getStringValue(mFieldNameToFieldInput, "mNotes"))
                            }
                            if (!mFieldNameToFieldInput["mTitle"]!!.isEmpty()) {
                                setTitle(getStringValue(mFieldNameToFieldInput, "mTitle"))
                            }
                            if (!mFieldNameToFieldInput["mExerciseRoute"]!!.isEmpty()) {
                                val exerciseRoutes =
                                    mFieldNameToFieldInput["mExerciseRoute"]?.getFieldValue()
                                        as List<ExerciseRoutesTestData.ExerciseRouteLocationData>
                                setRoute(
                                    generateExerciseRouteFromLocations(
                                        exerciseRoutes,
                                        startTime.toEpochMilli(),
                                    )
                                )
                            }
                            if (!mFieldNameToFieldInput["mSegments"]!!.isEmpty()) {
                                setSegments(
                                    mFieldNameToFieldInput["mSegments"]?.getFieldValue()
                                        as List<ExerciseSegment>
                                )
                            }
                            if (!mFieldNameToFieldInput["mLaps"]!!.isEmpty()) {
                                setLaps(
                                    mFieldNameToFieldInput["mLaps"]?.getFieldValue()
                                        as List<ExerciseLap>
                                )
                            }
                        }
                        .build()
                }

                PlannedExerciseSessionRecord::class -> {
                    val startTime = mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant
                    PlannedExerciseSessionRecord.Builder(
                            metaData,
                            mFieldNameToFieldInput["mPlannedExerciseType"]
                                ?.getFieldValue()
                                .toString()
                                .toInt(),
                            startTime,
                            mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                        )
                        .apply {
                            if (!mFieldNameToFieldInput["mNotes"]!!.isEmpty()) {
                                setNotes(getStringValue(mFieldNameToFieldInput, "mNotes"))
                            }
                            if (!mFieldNameToFieldInput["mTitle"]!!.isEmpty()) {
                                setTitle(getStringValue(mFieldNameToFieldInput, "mTitle"))
                            }
                            if (!mFieldNameToFieldInput["mBlocks"]!!.isEmpty()) {
                                setBlocks(
                                    mFieldNameToFieldInput["mBlocks"]?.getFieldValue()
                                        as List<PlannedExerciseBlock>
                                )
                            }
                        }
                        .build()
                }

                NutritionRecord::class ->
                    NutritionRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                        )
                        .apply {
                            if (!mFieldNameToFieldInput["mBiotin"]!!.isEmpty()) {
                                setBiotin(getMass(mFieldNameToFieldInput, "mBiotin"))
                            }
                            if (!mFieldNameToFieldInput["mCaffeine"]!!.isEmpty()) {
                                setCaffeine(getMass(mFieldNameToFieldInput, "mCaffeine"))
                            }
                            if (!mFieldNameToFieldInput["mCalcium"]!!.isEmpty()) {
                                setCalcium(getMass(mFieldNameToFieldInput, "mCalcium"))
                            }
                            if (!mFieldNameToFieldInput["mChloride"]!!.isEmpty()) {
                                setChloride(getMass(mFieldNameToFieldInput, "mChloride"))
                            }
                            if (!mFieldNameToFieldInput["mCholesterol"]!!.isEmpty()) {
                                setCholesterol(getMass(mFieldNameToFieldInput, "mCholesterol"))
                            }
                            if (!mFieldNameToFieldInput["mChromium"]!!.isEmpty()) {
                                setChromium(getMass(mFieldNameToFieldInput, "mChromium"))
                            }
                            if (!mFieldNameToFieldInput["mCopper"]!!.isEmpty()) {
                                setCopper(getMass(mFieldNameToFieldInput, "mCopper"))
                            }
                            if (!mFieldNameToFieldInput["mDietaryFiber"]!!.isEmpty()) {
                                setDietaryFiber(getMass(mFieldNameToFieldInput, "mDietaryFiber"))
                            }
                            if (!mFieldNameToFieldInput["mEnergy"]!!.isEmpty()) {
                                setEnergy(getEnergy(mFieldNameToFieldInput, "mEnergy"))
                            }
                            if (!mFieldNameToFieldInput["mEnergyFromFat"]!!.isEmpty()) {
                                setEnergyFromFat(
                                    getEnergy(mFieldNameToFieldInput, "mEnergyFromFat")
                                )
                            }
                            if (!mFieldNameToFieldInput["mFolate"]!!.isEmpty()) {
                                setFolate(getMass(mFieldNameToFieldInput, "mFolate"))
                            }
                            if (!mFieldNameToFieldInput["mFolicAcid"]!!.isEmpty()) {
                                setFolicAcid(getMass(mFieldNameToFieldInput, "mFolicAcid"))
                            }
                            if (!mFieldNameToFieldInput["mIodine"]!!.isEmpty()) {
                                setIodine(getMass(mFieldNameToFieldInput, "mIodine"))
                            }
                            if (!mFieldNameToFieldInput["mIron"]!!.isEmpty()) {
                                setIron(getMass(mFieldNameToFieldInput, "mIron"))
                            }
                            if (!mFieldNameToFieldInput["mMagnesium"]!!.isEmpty()) {
                                setMagnesium(getMass(mFieldNameToFieldInput, "mMagnesium"))
                            }
                            if (!mFieldNameToFieldInput["mManganese"]!!.isEmpty()) {
                                setManganese(getMass(mFieldNameToFieldInput, "mManganese"))
                            }
                            if (!mFieldNameToFieldInput["mMolybdenum"]!!.isEmpty()) {
                                setMolybdenum(getMass(mFieldNameToFieldInput, "mMolybdenum"))
                            }
                            if (!mFieldNameToFieldInput["mMonounsaturatedFat"]!!.isEmpty()) {
                                setMonounsaturatedFat(
                                    getMass(mFieldNameToFieldInput, "mMonounsaturatedFat")
                                )
                            }
                            if (!mFieldNameToFieldInput["mNiacin"]!!.isEmpty()) {
                                setNiacin(getMass(mFieldNameToFieldInput, "mNiacin"))
                            }
                            if (!mFieldNameToFieldInput["mPantothenicAcid"]!!.isEmpty()) {
                                setPantothenicAcid(
                                    getMass(mFieldNameToFieldInput, "mPantothenicAcid")
                                )
                            }
                            if (!mFieldNameToFieldInput["mPhosphorus"]!!.isEmpty()) {
                                setPhosphorus(getMass(mFieldNameToFieldInput, "mPhosphorus"))
                            }
                            if (!mFieldNameToFieldInput["mPolyunsaturatedFat"]!!.isEmpty()) {
                                setPolyunsaturatedFat(
                                    getMass(mFieldNameToFieldInput, "mPolyunsaturatedFat")
                                )
                            }
                            if (!mFieldNameToFieldInput["mPotassium"]!!.isEmpty()) {
                                setPotassium(getMass(mFieldNameToFieldInput, "mPotassium"))
                            }
                            if (!mFieldNameToFieldInput["mProtein"]!!.isEmpty()) {
                                setProtein(getMass(mFieldNameToFieldInput, "mProtein"))
                            }
                            if (!mFieldNameToFieldInput["mRiboflavin"]!!.isEmpty()) {
                                setRiboflavin(getMass(mFieldNameToFieldInput, "mRiboflavin"))
                            }
                            if (!mFieldNameToFieldInput["mSaturatedFat"]!!.isEmpty()) {
                                setSaturatedFat(getMass(mFieldNameToFieldInput, "mSaturatedFat"))
                            }
                            if (!mFieldNameToFieldInput["mSelenium"]!!.isEmpty()) {
                                setSelenium(getMass(mFieldNameToFieldInput, "mSelenium"))
                            }
                            if (!mFieldNameToFieldInput["mSodium"]!!.isEmpty()) {
                                setSodium(getMass(mFieldNameToFieldInput, "mSodium"))
                            }
                            if (!mFieldNameToFieldInput["mSugar"]!!.isEmpty()) {
                                setSugar(getMass(mFieldNameToFieldInput, "mSugar"))
                            }
                            if (!mFieldNameToFieldInput["mThiamin"]!!.isEmpty()) {
                                setThiamin(getMass(mFieldNameToFieldInput, "mThiamin"))
                            }
                            if (!mFieldNameToFieldInput["mTotalCarbohydrate"]!!.isEmpty()) {
                                setTotalCarbohydrate(
                                    getMass(mFieldNameToFieldInput, "mTotalCarbohydrate")
                                )
                            }
                            if (!mFieldNameToFieldInput["mTotalFat"]!!.isEmpty()) {
                                setTotalFat(getMass(mFieldNameToFieldInput, "mTotalFat"))
                            }
                            if (!mFieldNameToFieldInput["mTransFat"]!!.isEmpty()) {
                                setTransFat(getMass(mFieldNameToFieldInput, "mTransFat"))
                            }
                            if (!mFieldNameToFieldInput["mUnsaturatedFat"]!!.isEmpty()) {
                                setUnsaturatedFat(
                                    getMass(mFieldNameToFieldInput, "mUnsaturatedFat")
                                )
                            }
                            if (!mFieldNameToFieldInput["mVitaminA"]!!.isEmpty()) {
                                setVitaminA(getMass(mFieldNameToFieldInput, "mVitaminA"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminB12"]!!.isEmpty()) {
                                setVitaminB12(getMass(mFieldNameToFieldInput, "mVitaminB12"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminB6"]!!.isEmpty()) {
                                setVitaminB6(getMass(mFieldNameToFieldInput, "mVitaminB6"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminC"]!!.isEmpty()) {
                                setVitaminC(getMass(mFieldNameToFieldInput, "mVitaminC"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminD"]!!.isEmpty()) {
                                setVitaminD(getMass(mFieldNameToFieldInput, "mVitaminD"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminE"]!!.isEmpty()) {
                                setVitaminE(getMass(mFieldNameToFieldInput, "mVitaminE"))
                            }
                            if (!mFieldNameToFieldInput["mVitaminK"]!!.isEmpty()) {
                                setVitaminK(getMass(mFieldNameToFieldInput, "mVitaminK"))
                            }
                            if (!mFieldNameToFieldInput["mZinc"]!!.isEmpty()) {
                                setZinc(getMass(mFieldNameToFieldInput, "mZinc"))
                            }
                        }
                        .build()

                MindfulnessSessionRecord::class ->
                    MindfulnessSessionRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mMindfulnessSessionType"]
                                ?.getFieldValue()
                                .toString()
                                .toInt(),
                        )
                        .apply {
                            mFieldNameToFieldInput["mTitle"]
                                ?.takeIf { !it.isEmpty() }
                                ?.let { setTitle(it.getFieldValue().toString()) }
                            mFieldNameToFieldInput["mNotes"]
                                ?.takeIf { !it.isEmpty() }
                                ?.let { setNotes(it.getFieldValue().toString()) }
                        }
                        .build()

                ActivityIntensityRecord::class ->
                    ActivityIntensityRecord.Builder(
                            metaData,
                            getStartTime(mFieldNameToFieldInput),
                            getEndTime(mFieldNameToFieldInput),
                            mFieldNameToFieldInput["mActivityIntensityType"]
                                ?.getFieldValue()
                                .toString()
                                .toInt(),
                        )
                        .build()

                else -> throw NotImplementedError("Record type not implemented")
            }
        }
    }
}
