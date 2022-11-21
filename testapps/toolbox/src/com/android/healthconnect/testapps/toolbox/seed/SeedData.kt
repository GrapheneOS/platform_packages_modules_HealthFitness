/**
 * Copyright (C) 2022 The Android Open Source Project
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
import android.healthconnect.HealthConnectException
import android.healthconnect.HealthConnectManager
import android.healthconnect.InsertRecordsResponse
import android.healthconnect.datatypes.DataOrigin
import android.healthconnect.datatypes.Device
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.Metadata
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.StepsRecord
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.OutcomeReceiver
import android.util.Log
import android.widget.Toast
import java.time.Duration
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Random


class SeedData(private val context: Context) {

    companion object {
        private const val TAG = "SeedData"
    }

    private val manager by lazy {
        context.getSystemService(HealthConnectManager::class.java)
    }

    fun seedData() {
        seedStepsData()
        seedHeartRateData()
    }

    private fun seedStepsData() {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val records = (1L..100L).map { count ->
            getStepsRecord(
                count,
                start.plus(ofMinutes(count))
            )
        }
        insertRecord(records)
    }

    private fun seedHeartRateData() {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val random = Random()
        val records = (0L..100L).map { timeOffset ->
            val hrSamples = ArrayList<Pair<Long, Instant>>()
            repeat(100) { i ->
                hrSamples.add(
                    Pair(
                        getValidHeartRate(random),
                        start.plus(ofMinutes(timeOffset + i))
                    )
                )
            }
            getHeartRateRecord(
                hrSamples,
                start.plus(ofMinutes(timeOffset)),
                start.plus(ofMinutes(timeOffset + 100))
            )
        }
        insertRecord(records)
    }

    private fun <T : Record> insertRecord(records: List<T>) {
        try {
            manager.insertRecords(
                records,
                Runnable::run
            ) { response ->
                Log.i(TAG, "onResult: ${response.records.size}")
                Toast.makeText(
                    context,
                    "${response.records.size} steps records added!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (ex: HealthConnectException) {
            Toast.makeText(
                context,
                "Failed to insert steps records! $ex",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Failed to insert steps records!", ex)
        }
    }

    private fun getHeartRateRecord(
        heartRateValues: List<Pair<Long, Instant>>,
        start: Instant,
        end: Instant
    ): HeartRateRecord {
        return HeartRateRecord.Builder(
            getMetaData(),
            start,
            end,
            heartRateValues.map { HeartRateRecord.HeartRateSample(it.first, it.second) })
            .build()
    }

    private fun getValidHeartRate(random: Random): Long {
        return (random.nextInt(20) + 80).toLong()
    }

    private fun getStepsRecord(count: Long, time: Instant): StepsRecord {
        return StepsRecord.Builder(
            getMetaData(),
            time,
            time.plusSeconds(10),
            count
        ).build()
    }

    private fun getMetaData(): Metadata {
        val device: Device =
            Device.Builder().setManufacturer(MANUFACTURER).setModel(MODEL).setType(1).build()
        val dataOrigin = DataOrigin.Builder().setPackageName(context.packageName).build()
        return Metadata.Builder()
            .setDevice(device)
            .setDataOrigin(dataOrigin)
            .build()
    }
}