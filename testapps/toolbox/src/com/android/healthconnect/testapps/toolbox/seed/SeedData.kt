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
package com.android.healthconnect.testapps.toolbox.seed

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.TemperatureDelta
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.insertRecords
import kotlinx.coroutines.runBlocking
import java.time.Duration.ofDays
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Random
import kotlin.random.Random as ktRandom


class SeedData(private val context: Context, private val manager: HealthConnectManager) {

    companion object {
        const val NUMBER_OF_SERIES_RECORDS_TO_INSERT = 200L
        val VALID_READING_LOCATIONS =
            setOf(
                SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER,
                SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE,
                SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
                SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN)
    }

    fun seedRandomDataToGenerateAccessLog(numOfLogs: Int) {
        runBlocking {
            try {
                (1..numOfLogs).map { seedHeartRateData(1) }
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    fun seedData() {
        runBlocking {
            try {
                seedMenstruationData()
                seedStepsData()
                seedHeartRateData(10)
                seedSkinTemperatureData(10)
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    fun seedAllData(){
        try {
            SeedActivityData(context, manager).seedActivityData()
            SeedBodyMeasurementsData(context, manager).seedBodyMeasurementsData()
            SeedCycleTrackingData(context, manager).seedCycleTrackingData()
            SeedNutritionData(context, manager).seedNutritionData()
            SeedSleepData(context, manager).seedSleepCategoryData()
            SeedVitalsData(context, manager).seedVitalsData()
            SeedWellnessData(context, manager).seedWellnessData()
        }catch (ex: Exception) {
            throw ex
        }
    }

    private suspend fun seedStepsData() {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val records = (1L..50).map { count -> getStepsRecord(count, start.plus(ofMinutes(count))) }

        insertRecords(records, manager)
    }

    private suspend fun seedMenstruationData() {
        val today = Instant.now()
        val periodRecord =
            MenstruationPeriodRecord.Builder(getMetaData(context), today.minus(ofDays(5L)), today).build()
        val flowRecords =
            (-5..0).map { days ->
                MenstruationFlowRecord.Builder(
                        getMetaData(context),
                        today.plus(ofDays(days.toLong())),
                        MenstruationFlowType.FLOW_MEDIUM)
                    .build()
            }
        insertRecords(
            buildList {
                add(periodRecord)
                addAll(flowRecords)
            },
            manager)
    }

    suspend fun seedHeartRateData(numberOfRecordsPerBatch: Long) {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val random = Random()
        val records =
            (1L..numberOfRecordsPerBatch).map { timeOffset ->
                val hrSamples = ArrayList<Pair<Long, Instant>>()
                repeat(10) { i ->
                    hrSamples.add(
                        Pair(getValidHeartRate(random), start.plus(ofMinutes(timeOffset + i))))
                }
                getHeartRateRecord(
                    hrSamples,
                    start.plus(ofMinutes(timeOffset)),
                    start.plus(ofMinutes(timeOffset + 100)))
            }
        insertRecords(records, manager)
    }

    private suspend fun seedSkinTemperatureData(numberOfRecordsPerBatch: Long) {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val records =
            (1L..numberOfRecordsPerBatch).map { timeOffset ->
                val skinTempSamples = ArrayList<SkinTemperatureRecord.Delta>()
                repeat(10) { i ->
                    skinTempSamples.add(
                        SkinTemperatureRecord.Delta(
                            getValidTemperatureDelta(), start.plus(ofMinutes(timeOffset + i))))
                }
                getSkinTemperatureRecord(
                    skinTempSamples,
                    start.plus(ofMinutes(timeOffset)),
                    start.plus(ofMinutes(timeOffset + 100)))
            }
        insertRecords(records, manager)
    }

    private fun getSkinTemperatureRecord(
        deltasList: List<SkinTemperatureRecord.Delta>,
        startTime: Instant,
        endTime: Instant
    ): SkinTemperatureRecord {
        return SkinTemperatureRecord.Builder(getMetaData(context), startTime, endTime)
            .setDeltas(deltasList)
            .setBaseline(Temperature.fromCelsius(25.0))
            .setMeasurementLocation(VALID_READING_LOCATIONS.random())
            .build()
    }

    private fun getValidTemperatureDelta(): TemperatureDelta {
        return TemperatureDelta.fromCelsius((ktRandom.nextInt(-30, 30)).toDouble() / 10)
    }

    private fun getHeartRateRecord(
        heartRateValues: List<Pair<Long, Instant>>,
        start: Instant,
        end: Instant,
    ): HeartRateRecord {
        return HeartRateRecord.Builder(
                getMetaData(context),
                start,
                end,
                heartRateValues.map { HeartRateRecord.HeartRateSample(it.first, it.second) })
            .build()
    }

    private fun getValidHeartRate(random: Random): Long {
        return (random.nextInt(20) + 80).toLong()
    }

    private fun getStepsRecord(count: Long, time: Instant): StepsRecord {
        return StepsRecord.Builder(getMetaData(context), time, time.plusSeconds(30), count).build()
    }
}


