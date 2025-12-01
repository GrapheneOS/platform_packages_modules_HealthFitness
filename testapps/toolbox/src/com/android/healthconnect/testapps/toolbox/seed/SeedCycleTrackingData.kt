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
import android.health.connect.datatypes.CervicalMucusRecord
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation
import android.health.connect.datatypes.IntermenstrualBleedingRecord
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_FOLLICULAR
import android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_LUTEAL
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult
import android.health.connect.datatypes.SexualActivityRecord
import android.health.connect.datatypes.SexualActivityRecord.SexualActivityProtectionUsed
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

class SeedCycleTrackingData(
    private val context: Context,
    private val manager: HealthConnectManager,
) {

    companion object {
        val VALID_CERVICAL_MUCUS_SENSATION =
            setOf(
                CervicalMucusSensation.SENSATION_LIGHT,
                CervicalMucusSensation.SENSATION_MEDIUM,
                CervicalMucusSensation.SENSATION_HEAVY,
                CervicalMucusSensation.SENSATION_UNKNOWN,
            )
        val VALID_CERVICAL_MUCUS_APPEARANCE =
            setOf(
                CervicalMucusAppearance.APPEARANCE_DRY,
                CervicalMucusAppearance.APPEARANCE_CREAMY,
                CervicalMucusAppearance.APPEARANCE_STICKY,
                CervicalMucusAppearance.APPEARANCE_WATERY,
                CervicalMucusAppearance.APPEARANCE_EGG_WHITE,
                CervicalMucusAppearance.APPEARANCE_UNUSUAL,
                CervicalMucusAppearance.APPEARANCE_UNKNOWN,
            )
        val VALID_MENSTRUATION_FLOW_TYPE =
            setOf(
                MenstruationFlowType.FLOW_LIGHT,
                MenstruationFlowType.FLOW_MEDIUM,
                MenstruationFlowType.FLOW_HEAVY,
                MenstruationFlowType.FLOW_UNKNOWN,
            )
        val VALID_OVULATION_TEST_RESULT =
            setOf(
                OvulationTestResult.RESULT_NEGATIVE,
                OvulationTestResult.RESULT_POSITIVE,
                OvulationTestResult.RESULT_HIGH,
                OvulationTestResult.RESULT_INCONCLUSIVE,
            )
        val VALID_SEXUAL_ACTIVITY_PROTECTION_USED =
            setOf(
                SexualActivityProtectionUsed.PROTECTION_USED_UNKNOWN,
                SexualActivityProtectionUsed.PROTECTION_USED_PROTECTED,
                SexualActivityProtectionUsed.PROTECTION_USED_UNPROTECTED,
            )
        val VALID_MENSTRUAL_CYCLE_PHASES = setOf(PHASE_FOLLICULAR, PHASE_LUTEAL)
    }

    private val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
    private val yesterday = start.minus(ofDays(1))
    private val lastWeek = start.minus(ofDays(7))
    private val lastMonth = start.minus(ofDays(31))

    fun seedCycleTrackingData() {
        runBlocking {
            try {
                seedAllMenstruationData()
                seedCervicalMucusRecord()
                seedMenstrualCyclePhaseRecord()
                seedOvulationTestRecord()
                seedSexualActivityRecord()
                seedIntermenstrualBleedingRecord()
            } catch (ex: Exception) {
                throw ex
            }
        }
    }

    private suspend fun seedAllMenstruationData() {
        val todayPeriodRecord = getMenstruationPeriodRecord(start.minus(ofDays(5L)), start)
        val lastWeekPeriodRecord =
            getMenstruationPeriodRecord(lastWeek.minus(ofDays(1L)), lastWeek.plus(ofDays(4)))
        val lastMonthPeriodRecord =
            getMenstruationPeriodRecord(lastMonth.minus(ofDays(1L)), lastMonth.plus(ofDays(10L)))

        val todayFlowRecords =
            (-5..0).map { days -> getMenstruationFlowRecord(start.plus(ofDays(days.toLong()))) }
        val lastWeekFlowRecords =
            (-1..4).map { days -> getMenstruationFlowRecord(lastWeek.plus(ofDays(days.toLong()))) }
        val lastMonthFlowRecords =
            (-1..10).map { days ->
                getMenstruationFlowRecord(lastMonth.plus(ofDays(days.toLong())))
            }

        insertRecords(
            buildList {
                add(todayPeriodRecord)
                addAll(todayFlowRecords)
            },
            manager,
        )
        insertRecords(
            buildList {
                add(lastWeekPeriodRecord)
                addAll(lastWeekFlowRecords)
            },
            manager,
        )
        insertRecords(
            buildList {
                add(lastMonthPeriodRecord)
                addAll(lastMonthFlowRecords)
            },
            manager,
        )
    }

    private suspend fun seedCervicalMucusRecord() {
        val records =
            (1L..3).map { timeOffSet -> getCervicalMucusRecord(start.plus(ofMinutes(timeOffSet))) }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getCervicalMucusRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getCervicalMucusRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getCervicalMucusRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedOvulationTestRecord() {
        val records =
            (1L..3).map { timeOffSet -> getOvulationTestRecord(start.plus(ofMinutes(timeOffSet))) }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getOvulationTestRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getOvulationTestRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getOvulationTestRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedSexualActivityRecord() {
        val records =
            (1L..3).map { timeOffSet -> getSexualActivityRecord(start.plus(ofMinutes(timeOffSet))) }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getSexualActivityRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getSexualActivityRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getSexualActivityRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private suspend fun seedIntermenstrualBleedingRecord() {
        val records =
            (1L..3).map { timeOffSet ->
                getIntermenstrualBleedingRecord(start.plus(ofMinutes(timeOffSet)))
            }
        val yesterdayRecords =
            (1L..3).map { timeOffSet ->
                getIntermenstrualBleedingRecord(yesterday.plus(ofMinutes(timeOffSet)))
            }
        val lastWeekRecords =
            (1L..3).map { timeOffSet ->
                getIntermenstrualBleedingRecord(lastWeek.plus(ofMinutes(timeOffSet)))
            }
        val lastMonthRecords =
            (1L..3).map { timeOffSet ->
                getIntermenstrualBleedingRecord(lastMonth.plus(ofMinutes(timeOffSet)))
            }

        insertRecords(records, manager)
        insertRecords(yesterdayRecords, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private fun getMenstruationPeriodRecord(
        start: Instant,
        end: Instant,
    ): MenstruationPeriodRecord {
        return MenstruationPeriodRecord.Builder(getMetaData(context), start, end).build()
    }

    private fun getMenstruationFlowRecord(time: Instant): MenstruationFlowRecord {
        return MenstruationFlowRecord.Builder(
                getMetaData(context),
                time,
                VALID_MENSTRUATION_FLOW_TYPE.random(),
            )
            .build()
    }

    private fun getCervicalMucusRecord(time: Instant): CervicalMucusRecord {
        return CervicalMucusRecord.Builder(
                getMetaData(context),
                time,
                VALID_CERVICAL_MUCUS_SENSATION.random(),
                VALID_CERVICAL_MUCUS_APPEARANCE.random(),
            )
            .build()
    }

    private fun getOvulationTestRecord(time: Instant): OvulationTestRecord {
        return OvulationTestRecord.Builder(
                getMetaData(context),
                time,
                VALID_OVULATION_TEST_RESULT.random(),
            )
            .build()
    }

    private fun getSexualActivityRecord(time: Instant): SexualActivityRecord {
        return SexualActivityRecord.Builder(
                getMetaData(context),
                time,
                VALID_SEXUAL_ACTIVITY_PROTECTION_USED.random(),
            )
            .build()
    }

    private fun getIntermenstrualBleedingRecord(time: Instant): IntermenstrualBleedingRecord {
        return IntermenstrualBleedingRecord.Builder(getMetaData(context), time).build()
    }

    private suspend fun seedMenstrualCyclePhaseRecord() {
        val records = getMenstrualCyclePhaseRecords(start)
        val lastWeekRecords = getMenstrualCyclePhaseRecords(lastWeek)
        val lastMonthRecords = getMenstrualCyclePhaseRecords(lastMonth)

        insertRecords(records, manager)
        insertRecords(lastWeekRecords, manager)
        insertRecords(lastMonthRecords, manager)
    }

    private fun getMenstrualCyclePhaseRecords(startDate: Instant): List<MenstrualCyclePhaseRecord> {
        return (0L..3).map { dayOffSet ->
            getMenstrualCyclePhaseRecord(
                LocalDate.ofInstant(startDate.minus(ofDays(dayOffSet)), ZoneOffset.systemDefault())
            )
        }
    }

    private fun getMenstrualCyclePhaseRecord(date: LocalDate): MenstrualCyclePhaseRecord {
        return MenstrualCyclePhaseRecord.Builder(
                getMetaData(context),
                date,
                VALID_MENSTRUAL_CYCLE_PHASES.random(),
            )
            .setDayOfCycle(Random.nextInt(1, 180))
            .build()
    }
}
