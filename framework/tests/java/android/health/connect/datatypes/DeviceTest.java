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

package android.health.connect.datatypes;

import static com.google.common.truth.Truth.assertThat;

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.testing.EqualsTester;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testEqualsHashcode_displayNameEnabled() {
        new EqualsTester()
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setDisplayName("My Test Phone")
                                .build(),
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setDisplayName("My Test Phone")
                                .build())
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandB")
                                .setModel("ModelY")
                                .setType(Device.DEVICE_TYPE_WATCH)
                                .setDisplayName("My Test Watch")
                                .build())
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setDisplayName("Your Test Phone")
                                .build())
                .testEquals();
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testEqualsHashcode_displayNameDisabled() {
        new EqualsTester()
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .build(),
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .build())
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandB")
                                .setModel("ModelY")
                                .setType(Device.DEVICE_TYPE_WATCH)
                                .build())
                .testEquals();
    }

    @Test
    public void testBuilder() {
        Device device =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        assertThat(device.getManufacturer()).isEqualTo("BrandA");
        assertThat(device.getModel()).isEqualTo("ModelX");
        assertThat(device.getType()).isEqualTo(Device.DEVICE_TYPE_PHONE);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testBuilderWithDisplayName_displayNameEnabled() {
        Device device =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("My Test Phone")
                        .build();
        assertThat(device.getDisplayName()).isEqualTo("My Test Phone");
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testBuilderWithDisplayName_displayNameDisabled() {
        Device device =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("My Test Phone")
                        .build();
        assertThat(device.getDisplayName()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_UDI)
    public void testEqualsHashcode_udiEnabled() {
        new EqualsTester()
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setUdi("My Test Udi")
                                .build(),
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setUdi("My Test Udi")
                                .build())
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandB")
                                .setModel("ModelY")
                                .setType(Device.DEVICE_TYPE_WATCH)
                                .setUdi("My Test Udi 2")
                                .build())
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .build())
                .testEquals();
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_UDI)
    public void testEqualsHashcode_udiDisabled() {
        new EqualsTester()
                .addEqualityGroup(
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setUdi("My Test Udi")
                                .build(),
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .setUdi("My Test Udi 2")
                                .build(),
                        new Device.Builder()
                                .setManufacturer("BrandA")
                                .setModel("ModelX")
                                .setType(Device.DEVICE_TYPE_PHONE)
                                .build())
                .testEquals();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_UDI)
    public void testBuilderWithUdi_udiEnabled() {
        Device device =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setUdi("My Test Udi")
                        .build();
        assertThat(device.getUdi()).isEqualTo("My Test Udi");
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_UDI)
    public void testBuilderWithUdi_udiDisabled() {
        Device device =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setUdi("My Test Udi")
                        .build();
        assertThat(device.getUdi()).isNull();
    }
}
