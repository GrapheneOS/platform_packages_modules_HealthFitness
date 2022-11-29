/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.utils

import android.app.Activity
import android.content.Context
import android.healthconnect.HealthConnectManager
import android.healthconnect.datatypes.ActiveCaloriesBurnedRecord
import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.ElevationGainedRecord
import android.healthconnect.datatypes.ExerciseEventRecord
import android.healthconnect.datatypes.ExerciseLapRecord
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.HeartRateRecord.HeartRateSample
import android.healthconnect.datatypes.PowerRecord
import android.healthconnect.datatypes.PowerRecord.PowerRecordSample
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.SpeedRecord
import android.healthconnect.datatypes.SpeedRecord.SpeedRecordSample
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.units.Energy
import android.healthconnect.datatypes.units.Length
import android.healthconnect.datatypes.units.Power
import android.widget.Toast
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.insertRecords
import java.time.Instant
import kotlin.reflect.KClass

class InsertOrUpdateRecords {

    companion object {
        fun insertOrUpdateRecord(
            recordClass: KClass<out Record>,
            mFieldNameToFieldInput: HashMap<String, InputFieldView>,
            activity: Activity,
            context: Context,
        ) {
            val manager: HealthConnectManager =
                context.getSystemService(HealthConnectManager::class.java)!!
            val records = ArrayList<Record>()
            when (recordClass) {
                StepsRecord::class -> {
                    records.add(
                        StepsRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mCount"]
                                    ?.getFieldValue()
                                    .toString()
                                    .toLong())
                            .build())
                }
                DistanceRecord::class -> {
                    records.add(
                        DistanceRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mDistance"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build())
                }
                ActiveCaloriesBurnedRecord::class -> {
                    records.add(
                        ActiveCaloriesBurnedRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Energy.fromJoules(
                                    mFieldNameToFieldInput["mEnergy"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build())
                }
                ElevationGainedRecord::class -> {
                    records.add(
                        ElevationGainedRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mElevation"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build())
                }
                ExerciseLapRecord::class -> {
                    records.add(
                        ExerciseLapRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant)
                            .setLength(
                                Length.fromMeters(
                                    mFieldNameToFieldInput["mLength"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build())
                }
                ExerciseEventRecord::class -> {
                    records.add(
                        ExerciseEventRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mEventType"]?.getFieldValue() as Int)
                            .build())
                }
                BasalMetabolicRateRecord::class -> {
                    records.add(
                        BasalMetabolicRateRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["time"]?.getFieldValue() as Instant,
                                Power.fromWatts(
                                    mFieldNameToFieldInput["mBasalMetabolicRate"]
                                        ?.getFieldValue()
                                        .toString()
                                        .toDouble()))
                            .build())
                }
                SpeedRecord::class -> {
                    records.add(
                        SpeedRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mSpeedRecordSamples"]?.getFieldValue()
                                    as List<SpeedRecordSample>)
                            .build())
                }
                HeartRateRecord::class -> {
                    records.add(
                        HeartRateRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mHeartRateSamples"]?.getFieldValue()
                                    as List<HeartRateSample>)
                            .build())
                }
                PowerRecord::class -> {
                    records.add(
                        PowerRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mPowerRecordSamples"]?.getFieldValue()
                                    as List<PowerRecordSample>)
                            .build())
                }
                CyclingPedalingCadenceRecord::class -> {
                    records.add(
                        CyclingPedalingCadenceRecord.Builder(
                                getMetaData(context),
                                mFieldNameToFieldInput["startTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["endTime"]?.getFieldValue() as Instant,
                                mFieldNameToFieldInput["mCyclingPedalingCadenceRecordSamples"]
                                    ?.getFieldValue() as List<CyclingPedalingCadenceRecordSample>)
                            .build())
                }
                else -> {
                    activity.runOnUiThread {
                        Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (records.size > 0) {
                insertRecords(activity, context, records, manager)
            }
        }
    }
}
