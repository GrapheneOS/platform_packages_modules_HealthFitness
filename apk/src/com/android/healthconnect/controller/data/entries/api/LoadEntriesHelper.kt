/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries.api

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.ReadMedicalResourcesInitialRequest
import android.health.connect.ReadMedicalResourcesResponse
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.MedicalResource
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.Record
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.entries.datenavigation.toPeriod
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.toMedicalResourceType
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.SystemTimeSource
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.toLocalDate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Helper methods for loading normal data entries ([LoadDataEntriesUseCase], menstruation entries
 * ([LoadMenstruationDataUseCase]) and aggregations ([LoadDataAggregationsUseCase]).).
 */
@Singleton
class LoadEntriesHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val healthDataEntryFormatter: HealthDataEntryFormatter,
    private val menstruationPeriodFormatter: MenstruationPeriodFormatter,
    private val healthConnectManager: HealthConnectManager,
    private val dataSourceReader: MedicalDataSourceReader,
    private val timeSource: TimeSource = SystemTimeSource,
) {
    private val dateFormatter = LocalDateTimeFormatter(context)

    companion object {
        private const val TAG = "LoadDataUseCaseHelper"
    }

    /**
     * Returns a list of records from a data type sorted in descending order of their start time.
     */
    suspend fun readDataType(
        data: Class<out Record>,
        timeFilterRange: TimeInstantRangeFilter,
        packageName: String?,
        ascending: Boolean = false,
        pageSize: Int = 1000,
    ): List<Record> {
        val filter =
            buildReadRecordsRequestUsingFilters(
                data,
                timeFilterRange,
                packageName,
                ascending,
                pageSize,
            )
        val records =
            suspendCancellableCoroutine<ReadRecordsResponse<*>> { continuation ->
                    healthConnectManager.readRecords(
                        filter,
                        Runnable::run,
                        continuation.asOutcomeReceiver(),
                    )
                }
                .records
                .sortedByDescending { record -> getStartTime(record) }
        return records
    }

    /** Returns a list of records from an input sorted in descending order of their start time. */
    suspend fun readRecords(input: LoadDataEntriesInput): List<Record> {
        val timeFilterRange =
            getTimeFilter(input.displayedStartTime, input.period, endTimeExclusive = true)
        val dataTypes = HealthPermissionToDatatypeMapper.getDataTypes(input.permissionType)

        return dataTypes
            .map { dataType -> readDataType(dataType, timeFilterRange, input.packageName) }
            .flatten()
            .sortedByDescending { record ->
                when (record) {
                    is InstantRecord -> record.time
                    is IntervalRecord -> record.startTime
                    else -> Instant.EPOCH
                }
            }
    }

    /** Returns a list containing the most recent record from the specified input. */
    suspend fun readLastRecord(input: LoadDataEntriesInput): List<Record> {
        val timeFilterRange =
            getTimeFilter(input.displayedStartTime, input.period, endTimeExclusive = true)
        val dataTypes = HealthPermissionToDatatypeMapper.getDataTypes(input.permissionType)

        return dataTypes
            .map { dataType ->
                readDataType(
                    dataType,
                    timeFilterRange,
                    input.packageName,
                    ascending = false,
                    pageSize = 1,
                )
            }
            .flatten()
    }

    /** Returns a list of records from a MedicalPermissionType. */
    suspend fun readMedicalRecords(input: LoadMedicalEntriesInput): List<MedicalResource> {
        val medicalResourceType: Int
        try {
            medicalResourceType = toMedicalResourceType(input.medicalPermissionType)
        } catch (ex: IllegalArgumentException) {
            Log.i(TAG, "Failed to convert permission type to medical resource type.")
            return emptyList()
        }
        val filter =
            input.packageName?.let {
                buildMedicalResourceRequest(
                    medicalResourceType,
                    dataSourceReader.fromPackageName(it),
                )
            } ?: buildMedicalResourceRequest(medicalResourceType)
        val medicalResources =
            suspendCancellableCoroutine<ReadMedicalResourcesResponse> { continuation ->
                    healthConnectManager.readMedicalResources(
                        filter,
                        Runnable::run,
                        continuation.asOutcomeReceiver(),
                    )
                }
                .medicalResources
        // TODO(b/362672526): Sort by descending time.
        return medicalResources
    }

    /**
     * If more than one day's data is displayed, inserts a section header for each day: 'Today',
     * 'Yesterday', then date format, and group Menstruation Period and Flow entries together under
     * the same header.
     */
    suspend fun maybeAddDateSectionHeadersForMenstruation(
        startTime: Instant,
        entries: List<Record>,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean,
    ): List<FormattedEntry> {
        if (entries.isEmpty()) {
            return listOf()
        }
        if (period == DateNavigationPeriod.PERIOD_DAY) {
            return entries
                .map { record ->
                    if (record is MenstruationPeriodRecord) {
                        menstruationPeriodFormatter.format(
                            startTime,
                            record,
                            period,
                            showDataOrigin,
                        )
                    } else {
                        getFormatterRecord(record, showDataOrigin)
                    }
                }
                .filterNotNull()
        }

        val entriesWithSectionHeaders: MutableList<FormattedEntry> = mutableListOf()
        var lastHeaderDate = Instant.EPOCH

        entries.forEach {
            val possibleNextHeaderDate = getStartTime(it)
            if (!areOnSameDay(lastHeaderDate, possibleNextHeaderDate)) {
                lastHeaderDate = possibleNextHeaderDate
                val sectionTitle = getSectionTitle(lastHeaderDate)
                entriesWithSectionHeaders.add(FormattedEntry.EntryDateSectionHeader(sectionTitle))
            }
            if (it is MenstruationPeriodRecord) {
                menstruationPeriodFormatter.format(startTime, it, period, showDataOrigin).let {
                    formattedRecord ->
                    entriesWithSectionHeaders.add(formattedRecord)
                }
            } else if (it is MenstruationFlowRecord) {
                getFormatterRecord(it, showDataOrigin)?.let { formattedRecord ->
                    entriesWithSectionHeaders.add(formattedRecord)
                }
            }
        }
        return entriesWithSectionHeaders.toList()
    }

    /**
     * If more than one day's data is displayed, inserts a section header for each day: 'Today',
     * 'Yesterday', then date format.
     */
    suspend fun maybeAddDateSectionHeaders(
        entries: List<Record>,
        period: DateNavigationPeriod,
        showDataOrigin: Boolean,
    ): List<FormattedEntry> {
        if (entries.isEmpty()) {
            return listOf()
        }
        if (period == DateNavigationPeriod.PERIOD_DAY) {
            return entries.mapNotNull { record -> getFormatterRecord(record, showDataOrigin) }
        }

        val entriesWithSectionHeaders: MutableList<FormattedEntry> = mutableListOf()
        var lastHeaderDate = Instant.EPOCH

        entries.forEach {
            val possibleNextHeaderDate = getStartTime(it)
            if (!areOnSameDay(lastHeaderDate, possibleNextHeaderDate)) {
                lastHeaderDate = possibleNextHeaderDate
                val sectionTitle = getSectionTitle(lastHeaderDate)
                entriesWithSectionHeaders.add(FormattedEntry.EntryDateSectionHeader(sectionTitle))
            }
            getFormatterRecord(it, showDataOrigin)?.let { formattedRecord ->
                entriesWithSectionHeaders.add(formattedRecord)
            }
        }
        return entriesWithSectionHeaders.toList()
    }

    private fun getSectionTitle(date: Instant): String {
        val today =
            Instant.ofEpochMilli(timeSource.currentTimeMillis())
                .toLocalDate()
                .atStartOfDay(timeSource.deviceZoneOffset())
                .toInstant()
        val yesterday =
            today
                .toLocalDate()
                .minus(Period.ofDays(1))
                .atStartOfDay(timeSource.deviceZoneOffset())
                .toInstant()

        return if (areOnSameDay(date, today)) {
            context.getString(R.string.today_header)
        } else if (areOnSameDay(date, yesterday)) {
            context.getString(R.string.yesterday_header)
        } else {
            dateFormatter.formatLongDate(date)
        }
    }

    private fun areOnSameDay(instant1: Instant, instant2: Instant): Boolean {
        val localDate1 = instant1.atZone(timeSource.deviceZoneOffset()).toLocalDate()
        val localDate2 = instant2.atZone(timeSource.deviceZoneOffset()).toLocalDate()
        return localDate1 == localDate2
    }

    fun getStartTime(record: Record): Instant {
        return when (record) {
            is InstantRecord -> {
                record.time
            }
            is IntervalRecord -> {
                record.startTime
            }
            else -> {
                throw IllegalArgumentException("unsupported record type!")
            }
        }
    }

    private suspend fun getFormatterRecord(
        record: Record,
        showDataOrigin: Boolean,
    ): FormattedEntry? {
        return try {
            healthDataEntryFormatter.format(record, showDataOrigin)
        } catch (ex: Exception) {
            Log.i(TAG, "Failed to format record!")
            null
        }
    }

    fun getTimeFilter(
        startTime: Instant,
        period: DateNavigationPeriod,
        endTimeExclusive: Boolean,
    ): TimeInstantRangeFilter {

        val start =
            startTime
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        var end = start.atZone(ZoneId.systemDefault()).plus(toPeriod(period)).toInstant()
        if (endTimeExclusive) {
            end = end.minus(Duration.ofMillis(1))
        }

        return TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build()
    }

    fun getTimeFilter(startTime: Instant, endTime: Instant): TimeInstantRangeFilter {
        return TimeInstantRangeFilter.Builder().setStartTime(startTime).setEndTime(endTime).build()
    }

    @VisibleForTesting
    fun buildReadRecordsRequestUsingFilters(
        data: Class<out Record>,
        timeFilterRange: TimeInstantRangeFilter,
        packageName: String?,
        ascending: Boolean = true,
        pageSize: Int = 1000,
    ): ReadRecordsRequestUsingFilters<out Record> {
        val filter =
            ReadRecordsRequestUsingFilters.Builder(data)
                .setAscending(ascending)
                .setPageSize(pageSize)
                .setTimeRangeFilter(timeFilterRange)
        if (packageName != null) {
            filter.addDataOrigins(DataOrigin.Builder().setPackageName(packageName).build()).build()
        }
        return filter.build()
    }

    @VisibleForTesting
    fun buildMedicalResourceRequest(
        medicalResourceType: Int,
        dataSources: List<MedicalDataSource>,
    ): ReadMedicalResourcesInitialRequest {
        val filter = ReadMedicalResourcesInitialRequest.Builder(medicalResourceType)
        dataSources.map { it.id }.forEach { filter.addDataSourceId(it) }
        return filter.build()
    }

    @VisibleForTesting
    fun buildMedicalResourceRequest(medicalResourceType: Int): ReadMedicalResourcesInitialRequest {
        val filter = ReadMedicalResourcesInitialRequest.Builder(medicalResourceType)
        return filter.build()
    }
}
