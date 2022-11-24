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
import android.healthconnect.HealthConnectException
import android.healthconnect.HealthConnectManager
import android.healthconnect.datatypes.DataOrigin
import android.healthconnect.datatypes.Device
import android.healthconnect.datatypes.Metadata
import android.healthconnect.datatypes.Record
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.widget.Toast
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class GeneralUtils {

    companion object {
        fun getMetaData(context: Context): Metadata {
            val device: Device =
                Device.Builder().setManufacturer(MANUFACTURER).setModel(MODEL).setType(1).build()
            val dataOrigin = DataOrigin.Builder().setPackageName(context.packageName).build()
            return Metadata.Builder().setDevice(device).setDataOrigin(dataOrigin).build()
        }

        fun <T : Record> insertRecords(
            activity: Activity,
            context: Context,
            records: List<T>,
            manager: HealthConnectManager,
        ) {
            try {
                manager.insertRecords(records, Runnable::run) { response ->
                    activity.runOnUiThread {
                        Toast.makeText(
                                context,
                                "${response.records.size} records added!",
                                Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (ex: HealthConnectException) {
                activity.runOnUiThread {
                    Toast.makeText(context, "Failed to insert! $ex", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun <T : Any> getStaticFieldNamesAndValues(
            obj: KClass<T>,
        ): EnumFieldsWithValues {
            val fieldNameToValue: HashMap<String, Any> = HashMap()
            val fields: List<Field> =
                obj.java.declaredFields.filter { field -> Modifier.isStatic(field.modifiers) }
            for (field in fields) {
                fieldNameToValue[field.name] = field.get(obj)!!
            }
            return EnumFieldsWithValues(fieldNameToValue)
        }
    }
}
