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
package com.android.healthconnect.controller.tests.dataentries

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.DateNavigationPreference
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.TimeSource
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DateNavigationPreferenceTest {

    private lateinit var dateNavigationView: View
    private lateinit var previousDayButton: ImageButton
    private lateinit var nextDayButton: ImageButton
    private lateinit var selectedDateView: TextView

    private lateinit var preference: DateNavigationPreference
    private lateinit var context: Context
    private lateinit var viewHolder: PreferenceViewHolder
    private val dateChangedListener =
        Mockito.mock(DateNavigationPreference.OnDateChangedListener::class.java)
    private val timeSource: TimeSource =
        object : TimeSource {
            override fun currentTimeMillis(): Long {
                return NOW.toEpochMilli()
            }
        }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)

        dateNavigationView = View.inflate(context, R.layout.widget_date_navigation, null /* root */)
        selectedDateView = dateNavigationView.findViewById(R.id.selected_date)
        previousDayButton = dateNavigationView.findViewById(R.id.navigation_previous_day)
        nextDayButton = dateNavigationView.findViewById(R.id.navigation_next_day)
        viewHolder = PreferenceViewHolder.createInstanceForTests(dateNavigationView)
        preference =
            DateNavigationPreference(context = context, attrs = null, timeSource = timeSource)
    }

    @Test
    fun initDateNavigationPreference_titleSet() {
        preference.onBindViewHolder(viewHolder)

        assertThat(selectedDateView.visibility).isEqualTo(View.VISIBLE)
        assertThat(selectedDateView.text).isEqualTo("October 20, 2022")
    }

    @Test
    fun initDateNavigationPreference_nextNavigationDisabled() {
        preference.onBindViewHolder(viewHolder)

        assertThat(nextDayButton.visibility).isEqualTo(View.VISIBLE)
        assertThat(nextDayButton.isEnabled).isEqualTo(false)
    }

    @Test
    fun initDateNavigationPreference_prevNavigationEnabled() {
        preference.onBindViewHolder(viewHolder)

        assertThat(previousDayButton.visibility).isEqualTo(View.VISIBLE)
        assertThat(previousDayButton.isEnabled).isEqualTo(true)
    }

    @Test
    fun setDate_changesSelectedDateView() {
        preference.onBindViewHolder(viewHolder)

        preference.setDate(NOW.minus(Duration.ofDays(1)))

        assertThat(selectedDateView.text).isEqualTo("October 19, 2022")
    }

    @Test
    fun setDate_withValidFutureDates_nextButtonIsEnabled() {
        preference.onBindViewHolder(viewHolder)

        preference.setDate(NOW.minus(Duration.ofDays(1)))

        assertThat(nextDayButton.isEnabled).isEqualTo(true)
    }

    @Test
    fun onDateChanged_listenerIsCalled() {
        preference.onBindViewHolder(viewHolder)
        preference.setDateChangedListener(dateChangedListener)

        val newDate = NOW.minus(Duration.ofDays(1))
        preference.setDate(newDate)

        Mockito.verify(dateChangedListener).onDateChanged(newDate)
    }
}
