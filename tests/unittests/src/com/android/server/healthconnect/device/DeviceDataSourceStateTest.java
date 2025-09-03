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

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.StepsRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeviceDataSourceStateTest {

    @Test
    public void defaultStates_createsCorrectObject() {
        DeviceDataSourceState state = new DeviceDataSourceState.Builder(StepsRecord.class).build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isFalse();
        assertThat(state.isUserEnabled()).isFalse();
    }

    @Test
    public void allFieldsTrue_createsCorrectObject() {
        DeviceDataSourceState state =
                new DeviceDataSourceState.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isTrue();
        assertThat(state.isUserEnabled()).isTrue();
    }

    @Test
    public void allFieldsFalse_createsCorrectObject() {
        DeviceDataSourceState state =
                new DeviceDataSourceState.Builder(StepsRecord.class)
                        .setAvailable(false)
                        .setUserEnabled(false)
                        .build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isFalse();
        assertThat(state.isUserEnabled()).isFalse();
    }
}
