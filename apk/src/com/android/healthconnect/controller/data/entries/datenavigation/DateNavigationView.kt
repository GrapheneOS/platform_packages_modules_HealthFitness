/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries.datenavigation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_DAY
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_MONTH
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod.PERIOD_WEEK
import com.android.healthconnect.controller.utils.SystemTimeSource
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.getPeriodStartDate
import com.android.healthconnect.controller.utils.logging.DataEntriesElement
import com.android.healthconnect.controller.utils.logging.EntriesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthconnect.controller.utils.toLocalDate
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Allows the user to navigate in time to see their past data. */
class DateNavigationView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    private val timeSource: TimeSource = SystemTimeSource,
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val logger: HealthConnectLogger

    private lateinit var previousDayButton: ImageButton
    private lateinit var nextDayButton: ImageButton
    private lateinit var datePickerSpinner: Spinner
    private lateinit var disabledSpinner: TextView
    private var selectedDate = Instant.ofEpochMilli(timeSource.currentTimeMillis())
    private var period: DateNavigationPeriod = PERIOD_DAY
    private var onDateChangedListener: OnDateChangedListener? = null
    private var nextDayEnabled = true
    private var maxDate: Instant? = timeSource.currentTimeMillis().toInstant()

    init {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()

        val view = inflate(context, R.layout.widget_date_navigation_with_spinner, this)
        bindDateTextView(view)
        bindNextDayButton(view)
        bindPreviousDayButton(view)
        updateDisplayedDates()
    }

    fun setDateChangedListener(mDateChangedListener: OnDateChangedListener?) {
        this.onDateChangedListener = mDateChangedListener
    }

    fun setDate(date: Instant) {
        selectedDate = date
        updateDisplayedDates()
    }

    fun setPeriod(period: DateNavigationPeriod) {
        this.period = period
        updateDisplayedDates()
    }

    fun setMaxDate(instant: Instant?) {
        maxDate = instant
        updateDisplayedDates()
    }

    fun getDate(): Instant {
        return selectedDate
    }

    fun getPeriod(): DateNavigationPeriod {
        return period
    }

    fun disableDateNavigationView(isEnabled: Boolean, text: String) {
        setSpinnerText(text)
        disableButtons(isEnabled)
        toggleSpinnerVisibility(isEnabled)
    }

    fun getDateNavigationText(): String? {
        return (datePickerSpinner.adapter as DatePickerSpinnerAdapter).getText()
    }

    private fun setSpinnerText(text: String) {
        // text from the adapter can be null on rotation
        if (getDateNavigationText() == null) {
            disabledSpinner.text = text
        } else {
            disabledSpinner.text = (datePickerSpinner.adapter as DatePickerSpinnerAdapter).getText()
        }
    }

    private fun toggleSpinnerVisibility(isEnabled: Boolean) {
        if (!isEnabled) {
            datePickerSpinner.visibility = GONE
            disabledSpinner.visibility = VISIBLE
        } else {
            datePickerSpinner.visibility = VISIBLE
            disabledSpinner.visibility = GONE
        }
    }

    private fun disableButtons(isEnabled: Boolean) {
        updateButtonAppearance(nextDayButton, isEnabled && nextDayEnabled)
        updateButtonAppearance(previousDayButton, isEnabled)
    }

    private fun bindNextDayButton(view: View) {
        nextDayButton = view.findViewById(R.id.navigation_next_day) as ImageButton
        logger.logImpression(DataEntriesElement.NEXT_DAY_BUTTON)
        nextDayButton.setOnClickListener {
            logger.logInteraction(DataEntriesElement.NEXT_DAY_BUTTON)
            selectedDate =
                selectedDate.atZone(ZoneId.systemDefault()).plus(toPeriod(period)).toInstant()
            updateDisplayedDates()
        }
    }

    private fun bindPreviousDayButton(view: View) {
        previousDayButton = view.findViewById(R.id.navigation_previous_day) as ImageButton
        logger.logImpression(DataEntriesElement.PREVIOUS_DAY_BUTTON)
        previousDayButton.setOnClickListener {
            logger.logInteraction(DataEntriesElement.PREVIOUS_DAY_BUTTON)
            selectedDate =
                selectedDate.atZone(ZoneId.systemDefault()).minus(toPeriod(period)).toInstant()
            updateDisplayedDates()
        }
    }

    private fun bindDateTextView(view: View) {
        datePickerSpinner = view.findViewById(R.id.date_picker_spinner) as Spinner
        disabledSpinner = view.findViewById(R.id.disabled_spinner)
        val adapter =
            DatePickerSpinnerAdapter(
                view.context,
                getPeriodStartDate(selectedDate, period),
                period,
                timeSource,
            )
        adapter.setDropDownViewResource(R.layout.date_navigation_spinner_item)
        datePickerSpinner.adapter = adapter

        datePickerSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    unused: View?,
                    position: Int,
                    id: Long,
                ) {
                    val period: DateNavigationPeriod =
                        when (position) {
                            0 -> {
                                logger.logInteraction(EntriesElement.DATE_VIEW_SPINNER_DAY)
                                PERIOD_DAY
                            }
                            1 -> {
                                logger.logInteraction(EntriesElement.DATE_VIEW_SPINNER_WEEK)
                                PERIOD_WEEK
                            }
                            2 -> {
                                logger.logInteraction(EntriesElement.DATE_VIEW_SPINNER_MONTH)
                                PERIOD_MONTH
                            }
                            else -> throw IllegalStateException("Not supported time period.")
                        }
                    setPeriod(period)
                    updateDisplayedDates()
                }
            }

        datePickerSpinner.accessibilityDelegate =
            object : AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfoCompat.ACTION_CLICK,
                            context.getString(R.string.selected_date_view_action_description),
                        )
                    )
                }
            }
        when (period) {
            PERIOD_DAY -> logger.logImpression(EntriesElement.DATE_VIEW_SPINNER_DAY)
            PERIOD_WEEK -> logger.logImpression(EntriesElement.DATE_VIEW_SPINNER_WEEK)
            PERIOD_MONTH -> logger.logImpression(EntriesElement.DATE_VIEW_SPINNER_MONTH)
        }
    }

    private fun updateDisplayedDates() {
        onDateChangedListener?.onDateChanged(getPeriodStartDate(selectedDate, period), period)
        val today =
            LocalDate.ofInstant(
                    Instant.ofEpochMilli(timeSource.currentTimeMillis()),
                    timeSource.deviceZoneOffset(),
                )
                .atStartOfDay(timeSource.deviceZoneOffset())
                .toInstant()

        // This can happen if e.g. today is Monday, user navigates back to Sunday, sets the period
        // from Day to Week (underlying selected day is still Sunday), navigates to the next week
        // (underlying selected day is next Sunday), sets the period back to Day => displayed day
        // would be next Sunday. Instead, display today.
        if (today.isBefore(selectedDate) && maxDate != null) {
            selectedDate = today
        }

        val displayedEndDate =
            getPeriodStartDate(selectedDate, period)
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .plus(toPeriod(period))
                .toInstant()
        updateButtonAppearance(nextDayButton, maxDate == null || !displayedEndDate.isAfter(today))
        nextDayEnabled = nextDayButton.isEnabled
        (datePickerSpinner.adapter as DatePickerSpinnerAdapter).setStartTimeAndPeriod(
            getPeriodStartDate(selectedDate, period),
            period,
        )
        // Needed to trigger the text update
        datePickerSpinner.adapter.getView(
            datePickerSpinner.selectedItemPosition,
            null,
            datePickerSpinner,
        )

        datePickerSpinner.contentDescription =
            (datePickerSpinner.adapter as DatePickerSpinnerAdapter).getText()
        nextDayButton.contentDescription =
            when (period) {
                PERIOD_DAY -> resources.getString(R.string.a11y_next_day)
                PERIOD_WEEK -> resources.getString(R.string.a11y_next_week)
                PERIOD_MONTH -> resources.getString(R.string.a11y_next_month)
            }
        previousDayButton.contentDescription =
            when (period) {
                PERIOD_DAY -> resources.getString(R.string.a11y_previous_day)
                PERIOD_WEEK -> resources.getString(R.string.a11y_previous_week)
                PERIOD_MONTH -> resources.getString(R.string.a11y_previous_month)
            }
    }

    private fun updateButtonAppearance(button: ImageButton, enable: Boolean) {
        button.isEnabled = enable
        button.isClickable = enable
    }

    interface OnDateChangedListener {
        fun onDateChanged(displayedStartDate: Instant, period: DateNavigationPeriod)
    }
}
