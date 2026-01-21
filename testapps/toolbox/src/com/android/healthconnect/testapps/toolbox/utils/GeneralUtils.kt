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
import android.content.Intent
import android.health.connect.AggregateRecordsRequest
import android.health.connect.AggregateRecordsResponse
import android.health.connect.HealthConnectManager
import android.health.connect.InsertRecordsResponse
import android.health.connect.MatchmakingRequest
import android.health.connect.MatchmakingResponse
import android.health.connect.ReadRecordsRequest
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.TimeRangeFilter
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE
import android.health.connect.datatypes.Device.DEVICE_TYPE_SCALE
import android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.testapps.toolbox.R
import java.io.Serializable
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlinx.coroutines.suspendCancellableCoroutine

class GeneralUtils {

    data class DeviceDataSource(
        val device: Device,
        val deviceId: String,
        val advertisedDataType: Class<out Record> = StepsRecord::class.java,
    )

    companion object {
        val DEVICE_DATA_SOURCES =
            listOf(
                DeviceDataSource(
                    Device.Builder()
                        .setManufacturer("FitTastic")
                        .setModel("FitWatch 2000")
                        .setType(DEVICE_TYPE_WATCH)
                        .setDisplayName("FitWatch")
                        .build(),
                    "TestDeviceId",
                ),
                DeviceDataSource(
                    Device.Builder()
                        .setManufacturer("WeighLess")
                        .setModel("ScalePro 5")
                        .setType(DEVICE_TYPE_SCALE)
                        .setDisplayName("ScalePro")
                        .build(),
                    "ScaleDeviceId",
                ),
                DeviceDataSource(
                    Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel 9")
                        .setType(DEVICE_TYPE_PHONE)
                        .setDisplayName("Pixel 9")
                        .build(),
                    "PhoneDeviceId",
                ),
            )

        fun getMetaData(context: Context, recordUuid: String, udi: String? = null): Metadata {
            val device: Device =
                Device.Builder()
                    .setManufacturer(MANUFACTURER)
                    .setModel(MODEL)
                    .setType(DEVICE_TYPE_WATCH)
                    .setUdi(udi)
                    .build()
            val dataOrigin = DataOrigin.Builder().setPackageName(context.packageName).build()
            return Metadata.Builder()
                .setDevice(device)
                .setDataOrigin(dataOrigin)
                .setId(recordUuid)
                .build()
        }

        fun getMetaData(context: Context, udi: String? = null): Metadata {
            val device: Device =
                Device.Builder()
                    .setManufacturer(MANUFACTURER)
                    .setModel(MODEL)
                    .setType(DEVICE_TYPE_WATCH)
                    .setUdi(udi)
                    .build()
            val dataOrigin = DataOrigin.Builder().setPackageName(context.packageName).build()
            return Metadata.Builder().setDevice(device).setDataOrigin(dataOrigin).build()
        }

        suspend fun <T : Record> insertRecords(
            records: List<T>,
            manager: HealthConnectManager,
        ): List<Record> {

            val insertedRecords =
                try {
                    suspendCancellableCoroutine<InsertRecordsResponse> { continuation ->
                            manager.insertRecords(
                                records,
                                Runnable::run,
                                continuation.asOutcomeReceiver(),
                            )
                        }
                        .records
                } catch (ex: Exception) {
                    throw ex
                }
            return insertedRecords
        }

        suspend fun <T : Record> insertDeviceRecords(
            records: List<T>,
            deviceId: String,
            manager: HealthConnectManager,
        ): List<Record> {
            val insertedRecords =
                try {
                    suspendCancellableCoroutine<InsertRecordsResponse> { continuation ->
                            manager.insertDeviceRecords(
                                deviceId,
                                records,
                                Runnable::run,
                                continuation.asOutcomeReceiver(),
                            )
                        }
                        .records
                } catch (ex: Exception) {
                    throw ex
                }
            return insertedRecords
        }

        suspend fun <T : Record> updateRecords(records: List<T>, manager: HealthConnectManager) {
            try {
                suspendCancellableCoroutine<Void> { continuation ->
                    manager.updateRecords(records, Runnable::run, continuation.asOutcomeReceiver())
                }
            } catch (ex: Exception) {
                throw ex
            }
        }

        fun <T : Any> getStaticFieldNamesAndValues(obj: KClass<T>): EnumFieldsWithValues {
            return obj.java.declaredFields
                .filter { field ->
                    Modifier.isStatic(field.modifiers) && field.type == Int::class.java
                }
                .associate { it.name to it.get(obj)!! }
                .let { EnumFieldsWithValues(it) }
        }

        suspend fun readRecords(
            recordType: Class<out Record>,
            timeFilterRange: TimeRangeFilter,
            numberOfRecordsPerBatch: Long,
            manager: HealthConnectManager,
            ascending: Boolean = true,
        ): List<Record> {
            val filter =
                ReadRecordsRequestUsingFilters.Builder(recordType)
                    .setTimeRangeFilter(timeFilterRange)
                    .setPageSize(numberOfRecordsPerBatch.toInt())
                    .setAscending(ascending)
                    .build()
            val records =
                suspendCancellableCoroutine<ReadRecordsResponse<*>> { continuation ->
                        manager.readRecords(filter, Runnable::run, continuation.asOutcomeReceiver())
                    }
                    .records
            Log.d("READ_RECORDS", "Read ${records.size} records")
            return records
        }

        suspend fun <T : Record> readRecords(
            manager: HealthConnectManager,
            request: ReadRecordsRequest<T>,
        ): List<T> {
            val records =
                suspendCancellableCoroutine<ReadRecordsResponse<T>> { continuation ->
                        manager.readRecords(
                            request,
                            Runnable::run,
                            continuation.asOutcomeReceiver(),
                        )
                    }
                    .records
            Log.d("READ_RECORDS", "Read ${records.size} records")
            return records
        }

        suspend fun <T> aggregate(
            manager: HealthConnectManager,
            timeRangeFilter: TimeInstantRangeFilter,
            metrics: Set<AggregationType<T>>,
        ): AggregateRecordsResponse<T> {
            val request = AggregateRecordsRequest.Builder<T>(timeRangeFilter)

            for (metric in metrics) {
                request.addAggregationType(metric)
            }

            return suspendCancellableCoroutine { continuation ->
                manager.aggregate(request.build(), Runnable::run, continuation.asOutcomeReceiver())
            }
        }

        suspend fun deleteRecords(
            manager: HealthConnectManager,
            recordType: Class<out Record>,
            timeRangeFilter: TimeInstantRangeFilter,
        ) {
            suspendCancellableCoroutine<Void> { continuation ->
                manager.deleteRecords(
                    recordType,
                    timeRangeFilter,
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        }

        suspend fun isMatchmakingPossible(manager: HealthConnectManager): MatchmakingResponse {
            return suspendCancellableCoroutine { continuation ->
                manager.isMatchmakingPossible(
                    MatchmakingRequest.Builder().build(),
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        }

        inline fun <reified T> Context.requireSystemService(): T =
            requireNotNull(getSystemService(T::class.java))

        fun Intent.requireStringExtra(name: String): String = requireNotNull(getStringExtra(name))

        fun Intent.requireByteArrayExtra(name: String): ByteArray =
            requireNotNull(getByteArrayExtra(name))

        inline fun <reified T : Serializable> Intent.requireSerializable(name: String): T =
            requireNotNull(getSerializableExtra(name, T::class.java))

        fun Context.showMessageDialog(text: String) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_label)
                .setNegativeButton(android.R.string.cancel, null)
                .setMessage(text)
                .show()
        }
    }
}
