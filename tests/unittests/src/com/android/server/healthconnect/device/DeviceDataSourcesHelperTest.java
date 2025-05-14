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

package com.android.server.healthconnect.device;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;

import static com.android.server.healthconnect.device.DeviceDataSourcesHelper.DISPLAY_NAME_MAX_LENGTH;

import static com.google.common.truth.Truth.assertThat;

import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceDataSourcesHelperTest {

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void getCurrentDevice() {
        DeviceDataSource deviceDataSource =
                new FakeSerialDeviceDataSourcesHelper()
                        .getCurrentDevice(
                                InstrumentationRegistry.getInstrumentation().getContext());
        assertThat(deviceDataSource.getDeviceId())
                .isEqualTo(FakeSerialDeviceDataSourcesHelper.TEST_SERIAL_NUMBER);
        assertThat(deviceDataSource.getDeviceInfo().getDeviceType()).isEqualTo(DEVICE_TYPE_PHONE);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void isValidDisplayName_rejectsNullValues() {
        assertThat(DeviceDataSourcesHelper.isValidDisplayName(null)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void isValidDisplayName_rejectsEmptyStrings() {
        assertThat(DeviceDataSourcesHelper.isValidDisplayName("")).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void isValidDisplayName_rejectsStringsThatAreEmptyAfterTrimming() {
        assertThat(DeviceDataSourcesHelper.isValidDisplayName("   ")).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void sanitize_trimsWhiteSpace() {
        assertThat(DeviceDataSourcesHelper.sanitize("  Some Device  ")).isEqualTo("Some Device");
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void sanitize_truncatesToMaxLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DISPLAY_NAME_MAX_LENGTH * 2; i++) {
            sb.append("x");
        }
        assertThat(DeviceDataSourcesHelper.sanitize(sb.toString()))
                .hasLength(DISPLAY_NAME_MAX_LENGTH);
    }
}
