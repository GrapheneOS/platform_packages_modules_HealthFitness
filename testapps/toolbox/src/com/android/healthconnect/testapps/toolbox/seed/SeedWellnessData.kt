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
import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_ABSINTHE
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BRANDY
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CHUHAI
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CIDER
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_COCKTAIL
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_GIN
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_HIGHBALL
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_LAGER
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_MEAD
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_RUM
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SAKE
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SHOCHU
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SOJU
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_TEQUILA
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_VODKA
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WHISKEY
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE
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
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.util.Log
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.insertRecords
import java.time.Duration.ofDays
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

        val VALID_ALCOHOL_CONSUMPTION_TYPE =
            setOf(
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_VODKA,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_GIN,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WHISKEY,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_RUM,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_TEQUILA,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_LAGER,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CIDER,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SAKE,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SHOCHU,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SOJU,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_MEAD,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_ABSINTHE,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BRANDY,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_COCKTAIL,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CHUHAI,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_HIGHBALL,
            )

        val VALID_NICOTINE_INTAKE_TYPE =
            setOf(NICOTINE_INTAKE_TYPE_CIGARETTE, NICOTINE_INTAKE_TYPE_VAPE)

        val ALCOHOL_CONSUMPTION_NOTES =
            listOf("Pub quiz night", "Pub crawl", "Celebration", "Birthday", null)
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
                seedAlcoholConsumptionRecord()
            } catch (ex: Exception) {
                Log.e("SeedWellnessData", "Error when seeding wellness data $ex")
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

    private suspend fun seedAlcoholConsumptionRecord() {
        val records = getAlcoholConsumptionRecords(start)
        val yesterdayRecords = getAlcoholConsumptionRecords(yesterday)
        val lastWeekRecords = getAlcoholConsumptionRecords(lastWeek)
        val lastMonthRecords = getAlcoholConsumptionRecords(lastMonth)

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

    private fun getAlcoholConsumptionRecords(startTime: Instant): List<AlcoholConsumptionRecord> {
        return listOf(
            getAlcoholConsumptionRecordInstant(startTime.plus(ofMinutes(5))),
            getAlcoholConsumptionRecordInterval(
                startTime.plus(ofMinutes(10)),
                startTime.plus(ofMinutes(20)),
            ),
            getAlcoholConsumptionRecordDate(
                LocalDate.ofInstant(startTime, ZoneOffset.systemDefault())
            ),
        )
    }

    private fun getAlcoholConsumptionRecordInstant(time: Instant): AlcoholConsumptionRecord {
        return AlcoholConsumptionRecord.Builder(
                getMetaData(context),
                time,
                VALID_ALCOHOL_CONSUMPTION_TYPE.random(),
            )
            .setAlcoholByVolume(Percentage.fromValue(Random.nextDouble(1.0, 100.0)))
            .setServingVolume(Volume.fromLiters(Random.nextDouble(1.0, 10.0)))
            .setNotes(ALCOHOL_CONSUMPTION_NOTES.random())
            .build()
    }

    private fun getAlcoholConsumptionRecordInterval(
        startTime: Instant,
        endTime: Instant,
    ): AlcoholConsumptionRecord {
        return AlcoholConsumptionRecord.Builder(
                getMetaData(context),
                startTime,
                endTime,
                VALID_ALCOHOL_CONSUMPTION_TYPE.random(),
            )
            .setAlcoholByVolume(Percentage.fromValue(Random.nextDouble(1.0, 100.0)))
            .setServingVolume(Volume.fromLiters(Random.nextDouble(1.0, 10.0)))
            .setNotes(ALCOHOL_CONSUMPTION_NOTES.random())
            .build()
    }

    private fun getAlcoholConsumptionRecordDate(date: LocalDate): AlcoholConsumptionRecord {
        return AlcoholConsumptionRecord.Builder(
                getMetaData(context),
                date,
                VALID_ALCOHOL_CONSUMPTION_TYPE.random(),
            )
            .setAlcoholByVolume(Percentage.fromValue(Random.nextDouble(1.0, 100.0)))
            .setServingVolume(Volume.fromLiters(Random.nextDouble(1.0, 10.0)))
            .setNotes(ALCOHOL_CONSUMPTION_NOTES.random())
            .build()
    }
}
