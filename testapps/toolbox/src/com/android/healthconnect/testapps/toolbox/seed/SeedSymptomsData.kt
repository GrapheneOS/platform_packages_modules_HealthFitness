/**
 * Copyright (C) 2025 The Android Open Source Project
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
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.runBlocking

class SeedSymptomsData(private val context: Context, private val manager: HealthConnectManager) {
    fun seedSymptomsData() {
        runBlocking {
            val records = mutableListOf<SymptomRecord>()
            val now = Instant.now()
            for (i in 0..2) {
                // Interval record
                records.add(
                    SymptomRecord.Builder(
                            SymptomRecord.SYMPTOM_TYPE_COUGH,
                            now.minus((i * 2).toLong(), ChronoUnit.DAYS),
                            now.minus((i * 2).toLong(), ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
                            GeneralUtils.getMetaData(context),
                        )
                        .setSeverity(Random.nextInt(0, 4))
                        .setNotes("This is a seeded cough symptom.")
                        .build()
                )
                // Instant record
                records.add(
                    SymptomRecord.Builder(
                            SymptomRecord.SYMPTOM_TYPE_SNORE,
                            now.minus((i * 2).toLong(), ChronoUnit.DAYS),
                            GeneralUtils.getMetaData(context),
                        )
                        .setSeverity(Random.nextInt(0, 4))
                        .setNotes("This is a seeded snore symptom.")
                        .build()
                )
                // LocalDate record
                records.add(
                    SymptomRecord.Builder(
                            SymptomRecord.SYMPTOM_TYPE_COUGH,
                            LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(i.toLong()),
                            GeneralUtils.getMetaData(context),
                        )
                        .setSeverity(Random.nextInt(0, 4))
                        .setNotes("This is a seeded cough symptom with LocalDate.")
                        .build()
                )
            }
            GeneralUtils.insertRecords(records, manager)
        }
    }
}
