/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.SystemTimeSource
import com.android.healthconnect.controller.utils.TimeSource
import java.time.Instant

/** A custom card to display the latest available data aggregations. */
class AggregationDataCard
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    cardType: CardTypeEnum,
    cardInfo: AggregationCardInfo,
    private val timeSource: TimeSource = SystemTimeSource,
) : LinearLayout(context, attrs) {
    private val dateFormatter = LocalDateTimeFormatter(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_aggregation_data_card, this, true)

        val cardIcon = findViewById<ImageView>(R.id.card_icon)
        val cardTitle = findViewById<TextView>(R.id.card_title_number)
        val cardDate = findViewById<TextView>(R.id.card_date)
        val titleAndDateContainer = findViewById<ConstraintLayout>(R.id.title_date_container)

        cardIcon.background =
            fromFitnessPermissionType(cardInfo.fitnessPermissionType).icon(context)
        cardTitle.text = cardInfo.aggregation.aggregation
        cardTitle.contentDescription = cardInfo.aggregation.aggregationA11y

        val totalStartDate = cardInfo.startDate
        val totalEndDate = cardInfo.endDate
        cardDate.text = formatDateText(totalStartDate, totalEndDate)

        val constraintSet = ConstraintSet()
        constraintSet.clone(titleAndDateContainer)

        // Rearrange the textViews based on the type of card
        if (cardType == CardTypeEnum.SMALL_CARD) {
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
            )
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.BOTTOM,
                R.id.card_date,
                ConstraintSet.TOP,
            )

            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
            )
            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.TOP,
                R.id.card_title_number,
                ConstraintSet.BOTTOM,
            )
        } else {
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
            )
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
            )
            constraintSet.connect(
                R.id.card_title_number,
                ConstraintSet.END,
                R.id.card_date,
                ConstraintSet.START,
            )

            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.START,
                R.id.card_title_number,
                ConstraintSet.END,
            )
            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
            )
            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
            )
            constraintSet.connect(
                R.id.card_date,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
            )

            constraintSet.createHorizontalChain(
                ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT,
                intArrayOf(R.id.card_title_number, R.id.card_date),
                null,
                ConstraintSet.CHAIN_SPREAD_INSIDE,
            )

            constraintSet.constrainedWidth(R.id.card_title_number, true)
            constraintSet.constrainedWidth(R.id.card_date, true)
        }

        constraintSet.applyTo(titleAndDateContainer)
    }

    private fun formatDateText(startDate: Instant, endDate: Instant?): String {
        return dateFormatter.formatDate(startDate, endDate, timeSource)
    }

    enum class CardTypeEnum {
        SMALL_CARD,
        LARGE_CARD,
    }
}
