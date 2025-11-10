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
package android.health.connect.device;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.Device;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceParcelTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testParceling_allFieldsSet() {
        Device originalDevice =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("My Test Phone")
                        .build();

        android.os.Parcel parcel = android.os.Parcel.obtain();
        new DeviceParcel(originalDevice).writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceParcel restoredDeviceParcel = DeviceParcel.CREATOR.createFromParcel(parcel);
        Device restoredDevice = restoredDeviceParcel.getDevice();
        parcel.recycle();

        assertThat(restoredDevice).isEqualTo(originalDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testParceling_nullFields() {
        Device originalDevice =
                new Device.Builder()
                        .setManufacturer(null)
                        .setModel(null)
                        .setType(Device.DEVICE_TYPE_UNKNOWN)
                        .setDisplayName(null)
                        .build();

        android.os.Parcel parcel = android.os.Parcel.obtain();
        new DeviceParcel(originalDevice).writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceParcel restoredDeviceParcel = DeviceParcel.CREATOR.createFromParcel(parcel);
        Device restoredDevice = restoredDeviceParcel.getDevice();
        parcel.recycle();

        assertThat(restoredDevice).isEqualTo(originalDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testParceling_displayNameDisabled() {
        Device originalDevice =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("My Test Phone")
                        .build();

        android.os.Parcel parcel = android.os.Parcel.obtain();
        new DeviceParcel(originalDevice).writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceParcel restoredDeviceParcel = DeviceParcel.CREATOR.createFromParcel(parcel);
        Device restoredDevice = restoredDeviceParcel.getDevice();
        parcel.recycle();

        Device expectedDevice =
                new Device.Builder()
                        .setManufacturer("BrandA")
                        .setModel("ModelX")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(null)
                        .build();
        assertThat(restoredDevice).isEqualTo(expectedDevice);
    }
}
