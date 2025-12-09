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

package android.health.connect;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class DeviceDataSourceTest {
    private static final DataOrigin DATA_ORIGIN =
            new DataOrigin.Builder().setPackageName("com.example.package").build();
    private static final Device DEVICE =
            new Device.Builder().setManufacturer("Manufacturer").setModel("Model").build();
    private static final DeviceDataTypeSource DEVICE_DATA_TYPE_SOURCE =
            new DeviceDataTypeSource(StepsRecord.class, true, true);

    @Test
    public void testConstructorAndGetters() {
        DeviceDataSource deviceDataSource =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));

        assertThat(deviceDataSource.getDeviceDataOrigin()).isEqualTo(DATA_ORIGIN);
        assertThat(deviceDataSource.getDevice()).isEqualTo(DEVICE);
        assertThat(deviceDataSource.getDeviceDataTypeSources())
                .containsExactly(DEVICE_DATA_TYPE_SOURCE);
    }

    @Test
    public void testEqualsAndHashCode() {
        DeviceDataSource base =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));
        DeviceDataSource same =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));
        DeviceDataSource diffOrigin =
                new DeviceDataSource(
                        new DataOrigin.Builder().setPackageName("com.other").build(),
                        DEVICE,
                        Set.of(DEVICE_DATA_TYPE_SOURCE));
        DeviceDataSource diffDevice =
                new DeviceDataSource(
                        DATA_ORIGIN,
                        new Device.Builder().setManufacturer("Other").build(),
                        Set.of(DEVICE_DATA_TYPE_SOURCE));
        DeviceDataSource diffSources =
                new DeviceDataSource(
                        DATA_ORIGIN,
                        DEVICE,
                        Set.of(new DeviceDataTypeSource(StepsRecord.class, false, false)));

        assertThat(base.equals(base)).isTrue();
        assertThat(base.equals(same)).isTrue();
        assertThat(same.equals(base)).isTrue();
        assertThat(base.hashCode()).isEqualTo(same.hashCode());

        assertThat(base.equals(null)).isFalse();
        assertThat(base.equals(new Object())).isFalse();
        assertThat(base.equals(diffOrigin)).isFalse();
        assertThat(base.equals(diffDevice)).isFalse();
        assertThat(base.equals(diffSources)).isFalse();
    }

    @Test
    public void testToMasked() {
        DeviceDataSource deviceDataSource =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));
        Function<String, String> masker = s -> s + "_masked";

        DeviceDataSource masked = deviceDataSource.toMasked(masker);

        assertThat(masked.getDeviceDataOrigin().getPackageName())
                .isEqualTo(DATA_ORIGIN.getPackageName() + "_masked");
        assertThat(masked.getDevice()).isEqualTo(DEVICE);
        assertThat(masked.getDeviceDataTypeSources()).containsExactly(DEVICE_DATA_TYPE_SOURCE);
    }

    @Test
    public void testParceling() {
        DeviceDataSource deviceDataSource =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));

        Parcel parcel = Parcel.obtain();
        deviceDataSource.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataSource fromParcel = DeviceDataSource.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(fromParcel).isEqualTo(deviceDataSource);
    }

    @Test
    public void testToString() {
        DeviceDataSource deviceDataSource =
                new DeviceDataSource(DATA_ORIGIN, DEVICE, Set.of(DEVICE_DATA_TYPE_SOURCE));
        String string = deviceDataSource.toString();

        assertThat(string).contains("DeviceDataSource");
        assertThat(string).contains("mDeviceDataOrigin=" + DATA_ORIGIN);
        assertThat(string).contains("mDevice=" + DEVICE);
        assertThat(string).contains("mDataTypes=" + Set.of(DEVICE_DATA_TYPE_SOURCE));
    }
}
