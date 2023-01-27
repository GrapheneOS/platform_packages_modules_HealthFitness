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
import android.health.connect.datatypes.BasalMetabolicRateRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeartRateRecord.HeartRateSample
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.PowerRecord.PowerRecordSample
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.SpeedRecord.SpeedRecordSample
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Power
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

        fun createRecordObjectHelper(
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
                HeightRecord::class -> {
                    record =
                        HeightRecord.Builder(
                                metaData,
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mLength"]
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
