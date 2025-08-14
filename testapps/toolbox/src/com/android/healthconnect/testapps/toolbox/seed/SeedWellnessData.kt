/**
 * Copyright (C) 2024 The Android Open Source Project
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
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.health.connect.datatypes.units.Mass
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.insertRecords
import java.time.Duration.ofDays
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.runBlocking

class SeedWellnessData(private val context: Context, private val manager: HealthConnectManager) {
    companion object {
        val VALID_MINDFULNESS_SESSION_TYPE =
            setOf(
                MINDFULNESS_SESSION_TYPE_OTHER,
                MINDFULNESS_SESSION_TYPE_MEDITATION,
                MINDFULNESS_SESSION_TYPE_BREATHING,
                MINDFULNESS_SESSION_TYPE_MOVEMENT,
                MINDFULNESS_SESSION_TYPE_MUSIC,
                MINDFULNESS_SESSION_TYPE_UNGUIDED,
                MINDFULNESS_SESSION_TYPE_UNKNOWN,
            )

        val VALID_NICOTINE_INTAKE_TYPE =
            setOf(NICOTINE_INTAKE_TYPE_CIGARETTE, NICOTINE_INTAKE_TYPE_VAPE)
    }

    private val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
    private val yesterday = start.minus(ofDays(1))
    private val lastWeek = start.minus(ofDays(7))
    private val lastMonth = start.minus(ofDays(31))

    fun seedWellnessData() {
        runBlocking {
            try {
                seedMindfulnessSessionRecord()
                seedNicotineIntakeRecord()
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    private suspend fun seedMindfulnessSessionRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getMindfulnessSessionRecord(start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getMindfulnessSessionRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getMindfulnessSessionRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getMindfulnessSessionRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedNicotineIntakeRecord() {
        val records =
            (1L..3).map { timeOffSet -> getNicotineIntakeRecord(start.plus(ofMinutes(timeOffSet))) }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getNicotineIntakeRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getNicotineIntakeRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getNicotineIntakeRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private fun getMindfulnessSessionRecord(time: Instant): MindfulnessSessionRecord {
        return MindfulnessSessionRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                VALID_MINDFULNESS_SESSION_TYPE.random(),
            )
            .build()
    }

    private fun getNicotineIntakeRecord(time: Instant): NicotineIntakeRecord {
        return NicotineIntakeRecord.Builder(
                getMetaData(context),
                time,
                time.plusSeconds(30),
                Random.nextInt(100),
                VALID_NICOTINE_INTAKE_TYPE.random(),
            )
            .setNicotineIntake(Mass.fromGrams(Random.nextDouble(0.0, 0.01)))
            .build()
    }
}
