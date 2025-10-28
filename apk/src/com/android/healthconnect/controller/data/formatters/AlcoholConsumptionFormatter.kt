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
package com.android.healthconnect.controller.data.formatters

import android.content.Context
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
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_RUM
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SAKE
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SHOCHU
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SOJU
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_TEQUILA
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_VODKA
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WHISKEY
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.icu.text.MessageFormat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.data.formatters.shared.RecordDetailsFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/** Formatter for printing [AlcoholConsumptionRecord] data. */
@Singleton
class AlcoholConsumptionFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) :
    EntryFormatter<AlcoholConsumptionRecord>(context, timeFormatter, unitPreferences),
    RecordDetailsFormatter<AlcoholConsumptionRecord> {

    override suspend fun formatRecord(
        record: AlcoholConsumptionRecord,
        header: String,
        headerA11y: String,
    ): FormattedEntry {

        return FormattedEntry.SeriesDataEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record),
            titleA11y = formatA11yValue(record),
            dataType = record::class,
        )
    }

    override suspend fun formatValue(record: AlcoholConsumptionRecord): String {
        return formatType(record.beverageType)
    }

    private fun formatServingVolume(volume: Volume): String {
        return MessageFormat.format(
            context.getString(R.string.milliliter),
            mapOf("count" to formatVolumeValueMilliliters(volume)),
        )
    }

    private fun formatServingVolumeA11y(volume: Volume): String {
        return MessageFormat.format(
            context.getString(R.string.milliliter_long),
            mapOf("count" to formatVolumeValueMilliliters(volume)),
        )
    }

    private fun formatVolumeValueMilliliters(volume: Volume): Int {
        return round(volume.inLiters * 1000, 2)
    }

    private fun round(value: Double, scale: Int): Int {
        return value.toBigDecimal().setScale(scale, RoundingMode.UP).toInt()
    }

    private fun formatPercentageValue(percentage: Percentage): String {
        return MessageFormat.format(
            context.getString(R.string.percent),
            mapOf("value" to round(percentage.value, 2)),
        )
    }

    private fun formatPercentageValueA11y(percentage: Percentage): String {
        return MessageFormat.format(
            context.getString(R.string.percent_long),
            mapOf("value" to round(percentage.value, 2)),
        )
    }

    override suspend fun formatRecordDetails(
        record: AlcoholConsumptionRecord
    ): List<FormattedEntry> {

        val entries = mutableListOf<FormattedEntry>()

        if (record.servingVolume != null) {
            entries.add(
                FormattedEntry.ReverseSessionDetail(
                    uuid = record.metadata.id,
                    title = context.getString(R.string.serving_volume_title),
                    titleA11y = context.getString(R.string.serving_volume_title),
                    header = formatServingVolume(record.servingVolume!!),
                    headerA11y = formatServingVolumeA11y(record.servingVolume!!),
                )
            )
        }

        if (record.alcoholByVolume != null) {
            entries.add(
                FormattedEntry.ReverseSessionDetail(
                    uuid = record.metadata.id,
                    title = context.getString(R.string.alcohol_by_volume_title),
                    titleA11y = context.getString(R.string.alcohol_by_volume_title),
                    header = formatPercentageValue(record.alcoholByVolume!!),
                    headerA11y = formatPercentageValueA11y(record.alcoholByVolume!!),
                )
            )
        }

        if (record.notes != null) {
            entries.add(
                FormattedEntry.ReverseSessionDetail(
                    uuid = record.metadata.id,
                    title = context.getString(R.string.notes_title),
                    titleA11y = context.getString(R.string.notes_title),
                    header = record.notes.toString(),
                    headerA11y = record.notes.toString(),
                )
            )
        }

        if (entries.isNotEmpty()) {
            entries.add(
                0,
                FormattedEntry.FormattedSectionTitle(context.getString(R.string.details_title)),
            )
        }

        return entries
    }

    override suspend fun formatA11yValue(record: AlcoholConsumptionRecord): String {
        return formatValue(record)
    }

    private fun formatType(
        @AlcoholConsumptionRecord.AlcoholConsumptionBeverageType type: Int
    ): String {
        return when (type) {
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER ->
                context.getString(R.string.alcohol_consumption_beverage_type_beer)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE ->
                context.getString(R.string.alcohol_consumption_beverage_type_wine)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_VODKA ->
                context.getString(R.string.alcohol_consumption_beverage_type_vodka)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_GIN ->
                context.getString(R.string.alcohol_consumption_beverage_type_gin)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WHISKEY ->
                context.getString(R.string.alcohol_consumption_beverage_type_whiskey)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_RUM ->
                context.getString(R.string.alcohol_consumption_beverage_type_rum)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_TEQUILA ->
                context.getString(R.string.alcohol_consumption_beverage_type_tequila)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_LAGER ->
                context.getString(R.string.alcohol_consumption_beverage_type_lager)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CIDER ->
                context.getString(R.string.alcohol_consumption_beverage_type_cider)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SAKE ->
                context.getString(R.string.alcohol_consumption_beverage_type_sake)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SHOCHU ->
                context.getString(R.string.alcohol_consumption_beverage_type_shochu)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_SOJU ->
                context.getString(R.string.alcohol_consumption_beverage_type_soju)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_MEAD ->
                context.getString(R.string.alcohol_consumption_beverage_type_mead)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_ABSINTHE ->
                context.getString(R.string.alcohol_consumption_beverage_type_absinthe)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BRANDY ->
                context.getString(R.string.alcohol_consumption_beverage_type_brandy)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_COCKTAIL ->
                context.getString(R.string.alcohol_consumption_beverage_type_cocktail)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_CHUHAI ->
                context.getString(R.string.alcohol_consumption_beverage_type_chuhai)
            ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_HIGHBALL ->
                context.getString(R.string.alcohol_consumption_beverage_type_highball)
            else -> context.getString(R.string.alcohol_consumption_beverage_type_other)
        }
    }
}
