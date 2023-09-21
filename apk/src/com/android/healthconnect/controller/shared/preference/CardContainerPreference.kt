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
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.permissions.connectedapps.ComparablePreference
import com.android.healthconnect.controller.utils.SystemTimeSource
import com.android.healthconnect.controller.utils.TimeSource

class CardContainerPreference constructor(
        context: Context,
        private val timeSource: TimeSource = SystemTimeSource
): Preference(context), ComparablePreference {

    init {
        layoutResource = R.layout.widget_card_preference
        isSelectable = false
    }

    private val mAggregationCardInfo: MutableList<AggregationCardInfo> = mutableListOf()

    fun setAggregationCardInfo(aggregationCardInfoList: List<AggregationCardInfo>) {

        if (aggregationCardInfoList.isEmpty()) {
            return
        }
        // We display a max of 2 cards, so we take the first two list items
        if (aggregationCardInfoList.size > 2) {
            this.mAggregationCardInfo.addAll(aggregationCardInfoList.subList(0, 2))

        } else {
            this.mAggregationCardInfo.addAll(aggregationCardInfoList)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val container = holder.itemView as ConstraintLayout
        container.removeAllViews()

        if (this.mAggregationCardInfo.isEmpty() || this.mAggregationCardInfo.size > 2) {
            return
        }

        if (mAggregationCardInfo.size == 1) {
            addSingleLargeCard(container, mAggregationCardInfo[0])

        } else {
            val (firstCard, secondCard) =
                addTwoSmallCards(container, mAggregationCardInfo[0],
                    mAggregationCardInfo[1])

            // Redraw cards if the text is too large
            val firstCardText = firstCard.findViewById<TextView>(R.id.card_title_number)
            val secondCardText = secondCard.findViewById<TextView>(R.id.card_title_number)

            // Check for the ellipsized text after the first card has been drawn
            firstCard.post {
                if (isTextEllipsized(firstCardText) || isTextEllipsized(secondCardText)) {
                    container.removeAllViews()
                    addTwoLargeCards(container, mAggregationCardInfo[0], mAggregationCardInfo[1])

                }
            }
        }
    }

    /**
     * Adds a single large [AggregationDataCard] to the provided container.
     * This should be called when there is only one available aggregate.
     */
    private fun addSingleLargeCard(container: ConstraintLayout, cardInfo: AggregationCardInfo) {
        val singleCard = AggregationDataCard(
                context,
                null,
                AggregationDataCard.CardTypeEnum.LARGE_CARD,
                cardInfo,
                timeSource)
        singleCard.id = View.generateViewId()
        val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT)
        singleCard.layoutParams = layoutParams
        container.addView(singleCard)
    }

    /**
     * Adds two small [AggregationDataCard]s to the provided container stacked horizontally.
     * This should be called when there are two available aggregates.
     */
    private fun addTwoSmallCards(
        container: ConstraintLayout,
        firstCardInfo: AggregationCardInfo,
        secondCardInfo: AggregationCardInfo): Pair<AggregationDataCard, AggregationDataCard> {

        // Construct the first card
        val firstCard = constructSmallCard(firstCardInfo, addMargin = true)

        // Construct the second card
        val secondCard = constructSmallCard(secondCardInfo, addMargin = false)

        container.addView(firstCard)
        container.addView(secondCard)

        applySmallCardConstraints(firstCard, secondCard, container)

        return Pair(firstCard, secondCard)
    }

    private fun applySmallCardConstraints(
        firstCard: AggregationDataCard,
        secondCard: AggregationDataCard,
        container: ConstraintLayout
    ) {
        // Add the constraints between the two cards in their ConstraintLayout container
        val constraintSet = ConstraintSet()
        constraintSet.clone(container)

        // Constraints for the first card
        constraintSet.connect(firstCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(firstCard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(firstCard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(firstCard.id, ConstraintSet.END, secondCard.id, ConstraintSet.START)

        // Constraints for the second card
        constraintSet.connect(secondCard.id, ConstraintSet.START, firstCard.id, ConstraintSet.END)
        constraintSet.connect(secondCard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(secondCard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(secondCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(container)
    }

    private fun constructSmallCard(
        cardInfo: AggregationCardInfo,
        addMargin: Boolean) : AggregationDataCard {

        val card = AggregationDataCard(
            context,
            null,
            AggregationDataCard.CardTypeEnum.SMALL_CARD,
            cardInfo,
            timeSource)
        card.id = View.generateViewId()
        val layoutParams = ConstraintLayout.LayoutParams(0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT)

        if (addMargin) {
            // Set a right margin of 16dp for the first (leftmost) card
            val marginInDp = 16
            val marginInPx = (marginInDp * context.resources.displayMetrics.density).toInt()
            layoutParams.setMargins(0,0, marginInPx, 0)
        }

        card.layoutParams = layoutParams

        return card
    }

    /**
     * Adds two large [AggregationDataCard]s to the provided container stacked vertically.
     * This should be called when there are two available aggregates and the text is
     * too large to fit into small cards.
     */
    private fun addTwoLargeCards(
        container: ConstraintLayout,
        firstCardInfo: AggregationCardInfo,
        secondCardInfo: AggregationCardInfo) {

        // Construct the first card
        val firstLongCard = constructLargeCard(firstCardInfo, addMargin = true)
        // Construct the second card
        val secondLongCard = constructLargeCard(secondCardInfo, addMargin = false)

        container.addView(firstLongCard)
        container.addView(secondLongCard)

        applyLargeCardConstraints(firstLongCard, secondLongCard, container)
    }

    private fun applyLargeCardConstraints(
        firstCard: AggregationDataCard,
        secondCard: AggregationDataCard,
        container: ConstraintLayout
    ) {
        // Add the constraints between the two cards in their ConstraintLayout container
        val constraintSet = ConstraintSet()
        constraintSet.clone(container)

        // Constraints for the first card
        constraintSet.connect(firstCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(firstCard.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(firstCard.id, ConstraintSet.BOTTOM, secondCard.id, ConstraintSet.TOP)
        constraintSet.connect(firstCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // Constraints for the first card
        constraintSet.connect(secondCard.id, ConstraintSet.TOP, firstCard.id, ConstraintSet.BOTTOM)
        constraintSet.connect(secondCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(secondCard.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(secondCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(container)
    }

    private fun constructLargeCard(
        cardInfo: AggregationCardInfo,
        addMargin: Boolean
    ): AggregationDataCard {

        val largeCard = AggregationDataCard(context, null,
            AggregationDataCard.CardTypeEnum.LARGE_CARD, cardInfo, timeSource)
        largeCard.id = View.generateViewId()

        val layoutParams = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)

        if (addMargin) {
            // Set a bottom margin of 16dp for the first (topmost) card
            val marginInDp = 16
            val marginInPx = (marginInDp * context.resources.displayMetrics.density).toInt()
            layoutParams.setMargins(0,0, 0, marginInPx)
        }

        largeCard.layoutParams = layoutParams
        return largeCard
    }

    /**
     * Returns true if the provided textView is ellipsized (...)
     */
    private fun isTextEllipsized(textView: TextView): Boolean {
        if (textView.layout != null) {
            val lines = textView.layout.lineCount
            if (lines > 0) {
                if (textView.layout.getEllipsisCount(lines - 1) > 0 ) {
                    return true
                }
            }
        }
        return false
    }

    override fun hasSameContents(preference: Preference): Boolean {
        return preference is CardContainerPreference &&
                preference.mAggregationCardInfo == this.mAggregationCardInfo
    }

    override fun isSameItem(preference: Preference): Boolean {
        return preference is CardContainerPreference &&
                this == preference    }
}