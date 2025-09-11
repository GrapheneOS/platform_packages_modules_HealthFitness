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

import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

@RunWith(JUnit4.class)
public class DeviceDataSourceAdvertisementTest {

    @Test
    public void constructAdvertisement_returnsCorrectValues() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        String displayName = "TestDisplayName";
        String deviceId = "TestDeviceId";
        Set<DeviceDataSourceState> deviceDataSourceState =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());

        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, displayName, deviceId, deviceDataSourceState);

        assertThat(advertisement.getDevice()).isEqualTo(device);
        assertThat(advertisement.getDisplayName()).isEqualTo(displayName);
        assertThat(advertisement.getDeviceId()).isEqualTo(deviceId);
        assertThat(advertisement.getDeviceDataSourceState()).isEqualTo(deviceDataSourceState);
    }

    @Test
    public void constructAdvertisement_noDeviceDataSourceState_returnsCorrectValues() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        String displayName = "TestDisplayName";
        String deviceId = "TestDeviceId";

        ImmutableSet<DeviceDataSourceState> deviceDataSourceState = ImmutableSet.of();

        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, displayName, deviceId, deviceDataSourceState);

        assertThat(advertisement.getDevice()).isEqualTo(device);
        assertThat(advertisement.getDisplayName()).isEqualTo(displayName);
        assertThat(advertisement.getDeviceId()).isEqualTo(deviceId);
        assertThat(advertisement.getDeviceDataSourceState()).isEqualTo(deviceDataSourceState);
    }
}
