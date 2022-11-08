/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.dataentries

import android.app.DatePickerDialog
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.DatePickerFactory
import com.android.healthconnect.controller.utils.SystemTimeSource
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.getInstant
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthconnect.controller.utils.toLocaleDate
import java.time.Duration.ofDays
import java.time.Instant

/**
 * This DateNavigationPreference allows the user to navigate in time to see their past data.
 */
class DateNavigationPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    private val timeSource: TimeSource = SystemTimeSource
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        isSelectable = false
        layoutResource = R.layout.widget_date_navigation
    }

    private lateinit var previousDayButton: ImageButton
    private lateinit var nextDayButton: ImageButton
    private lateinit var selectedDateView: TextView

    private var selectedDate: Instant = timeSource.currentTimeMillis().toInstant()
    private var dateChangedListener: OnDateChangedListener? = null
    private val onDateChangedListener =
        DatePickerDialog.OnDateSetListener { _, year, month, day ->
            selectedDate = getInstant(year, month, day)
            updateSelectedDate()
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindDateTextView(holder)
        bindPreviousDayButton(holder)
        bindNextDayButton(holder)

        updateSelectedDate()
    }

    fun setDateChangedListener(mDateChangedListener: OnDateChangedListener?) {
        this.dateChangedListener = mDateChangedListener
    }

    fun setDate(date: Instant) {
        selectedDate = date
        updateSelectedDate()
    }

    private fun bindNextDayButton(holder: PreferenceViewHolder) {
        nextDayButton = holder.findViewById(R.id.navigation_next_day) as ImageButton
        nextDayButton.setOnClickListener {
            selectedDate = selectedDate.plus(ofDays(1))
            updateSelectedDate()
        }
    }

    private fun bindPreviousDayButton(holder: PreferenceViewHolder) {
        previousDayButton = holder.findViewById(R.id.navigation_previous_day) as ImageButton
        previousDayButton.setOnClickListener {
            selectedDate = selectedDate.minus(ofDays(1))
            updateSelectedDate()
        }
    }

    private fun bindDateTextView(holder: PreferenceViewHolder) {
        selectedDateView = holder.findViewById(R.id.selected_date) as TextView
        selectedDateView.setOnClickListener {
            val today = timeSource.currentTimeMillis().toInstant()
            val datePickerDialog = DatePickerFactory.create(
                context, selectedDate, today
            )
            datePickerDialog.setOnDateSetListener(onDateChangedListener)
            datePickerDialog.show()
        }
    }

    private fun updateSelectedDate() {
        dateChangedListener?.onDateChanged(selectedDate)
        selectedDateView.text =
            DateFormat.getLongDateFormat(context).format(selectedDate.toEpochMilli())
        val today = timeSource.currentTimeMillis().toInstant().toLocaleDate().atStartOfDay()
        val curDate = selectedDate.toLocaleDate().atStartOfDay()
        nextDayButton.isEnabled = curDate.isBefore(today)
    }

    interface OnDateChangedListener {
        fun onDateChanged(selectedDate: Instant)
    }

}