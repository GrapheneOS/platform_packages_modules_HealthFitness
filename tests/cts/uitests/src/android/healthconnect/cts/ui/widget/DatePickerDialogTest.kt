/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package android.healthconnect.cts.ui.widget

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.DialogInterface
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.annotation.UiThreadTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class DatePickerDialogTest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    @UiThreadTest
    fun testConstructor() {
        val datePickerDialog = DatePickerDialog(context)
        assertThat(datePickerDialog.datePicker).isNotNull()
    }

    @Test
    @UiThreadTest
    fun testUpdateDate_shouldUpdateTheListenerPassedInto() {
        val mockCallBack: OnDateSetListener = mock(OnDateSetListener::class.java)
        val datePickerDialog = DatePickerDialog(context)
        datePickerDialog.setOnDateSetListener(mockCallBack)

        datePickerDialog.updateDate(/* year= */ 2019, /* month= */ 1, /* dayOfMonth= */ 1)
        datePickerDialog.onClick(datePickerDialog, DialogInterface.BUTTON_POSITIVE)

        verify(mockCallBack, atLeast(1)).onDateSet(any(), eq(2019), eq(1), eq(1))
    }
}
