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

package android.healthconnect.cts.device;

import static android.health.connect.datatypes.SymptomRecord.SYMPTOM_TYPE_COUGH;
import static android.health.connect.datatypes.SymptomRecord.SYMPTOM_TYPE_UNKNOWN;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.DeviceDataSource;
import android.health.connect.DeviceDataTypeSource;
import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(FLAG_DEVICE_DATA_PROVIDERS_API)
public class DeviceDataSourcesApiResponseTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void testDeviceDataSource_constructorAndGetters() {
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName("com.example").build();
        Device device =
                new Device.Builder()
                        .setManufacturer("Manufacturer")
                        .setModel("Model")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        DeviceDataTypeSource typeSource =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, true, true);
        Set<DeviceDataTypeSource> typeSources = Set.of(typeSource);

        DeviceDataSource dataSource = new DeviceDataSource(dataOrigin, device, typeSources);

        assertThat(dataSource.getDeviceDataOrigin()).isEqualTo(dataOrigin);
        assertThat(dataSource.getDevice()).isEqualTo(device);
        assertThat(dataSource.getDeviceDataTypeSources()).containsExactly(typeSource);
    }

    @Test
    public void testDeviceDataSource_equalsHashCodeToString() {
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName("com.example").build();
        Device device =
                new Device.Builder()
                        .setManufacturer("Manufacturer")
                        .setModel("Model")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        DeviceDataTypeSource typeSource =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, true, true);
        DeviceDataSource dataSource1 = new DeviceDataSource(dataOrigin, device, Set.of(typeSource));
        DeviceDataSource dataSource2 = new DeviceDataSource(dataOrigin, device, Set.of(typeSource));
        DeviceDataSource dataSource3 = new DeviceDataSource(dataOrigin, device, Set.of());

        assertThat(dataSource1).isEqualTo(dataSource2);
        assertThat(dataSource1.hashCode()).isEqualTo(dataSource2.hashCode());
        assertThat(dataSource1).isNotEqualTo(dataSource3);
        assertThat(dataSource1.toString()).contains("DeviceDataSource");
        assertThat(dataSource1.toString()).isNotNull();
    }

    @Test
    public void testDeviceDataTypeSource_ofDataType() {

        DeviceDataTypeSource source =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, true, false);

        assertThat(source.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(source.isAvailable()).isTrue();
        assertThat(source.isUserEnabled()).isFalse();
        assertThat(source.getSymptomType()).isEqualTo(SYMPTOM_TYPE_UNKNOWN);
    }

    @Test
    public void testDeviceDataTypeSource_ofSymptomType() {
        DeviceDataTypeSource source =
                DeviceDataTypeSource.ofSymptomType(SYMPTOM_TYPE_COUGH, false, true);

        assertThat(source.getDataType()).isEqualTo(SymptomRecord.class);
        assertThat(source.getSymptomType()).isEqualTo(SYMPTOM_TYPE_COUGH);
        assertThat(source.isAvailable()).isFalse();
        assertThat(source.isUserEnabled()).isTrue();
    }

    @Test
    public void testDeviceDataTypeSource_equalsHashCodeToString() {
        DeviceDataTypeSource source1 =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, true, false);
        DeviceDataTypeSource source2 =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, true, false);
        DeviceDataTypeSource source3 =
                DeviceDataTypeSource.ofDataType(StepsRecord.class, false, false);

        assertThat(source1).isEqualTo(source2);
        assertThat(source1.hashCode()).isEqualTo(source2.hashCode());
        assertThat(source1).isNotEqualTo(source3);
        assertThat(source1.toString()).contains("DeviceDataTypeSource");
        assertThat(source1.toString()).isNotNull();
    }

    @Test
    public void testGetDeviceDataSourcesResponse_constructorAndGetters() {

        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName("com.example").build();
        Device device = new Device.Builder().setManufacturer("M").setModel("M").build();
        DeviceDataSource dataSource = new DeviceDataSource(dataOrigin, device, Set.of());
        List<DeviceDataSource> dataSources = List.of(dataSource);

        GetDeviceDataSourcesResponse response = new GetDeviceDataSourcesResponse(dataSources);

        assertThat(response.getDeviceDataSources()).containsExactly(dataSource);
    }

    @Test
    public void testDeviceDataTypeAdvertisement_getSymptomType() {
        DeviceDataTypeAdvertisement advertisement =
                new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                        .setSymptomType(SYMPTOM_TYPE_COUGH)
                        .build();

        assertThat(advertisement.getSymptomType()).isEqualTo(SYMPTOM_TYPE_COUGH);
    }

    @Test
    public void testDeviceDataTypeAdvertisement_getSymptomType_unknown() {
        DeviceDataTypeAdvertisement advertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        assertThat(advertisement.getSymptomType()).isEqualTo(SYMPTOM_TYPE_UNKNOWN);
    }

    @Test
    public void testDeviceDataTypeAdvertisement_equalsHashCode() {
        DeviceDataTypeAdvertisement advertisement1 =
                new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                        .setSymptomType(SYMPTOM_TYPE_COUGH)
                        .build();
        DeviceDataTypeAdvertisement advertisement2 =
                new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                        .setSymptomType(SYMPTOM_TYPE_COUGH)
                        .build();
        DeviceDataTypeAdvertisement advertisement3 =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        assertThat(advertisement1).isEqualTo(advertisement2);
        assertThat(advertisement1.hashCode()).isEqualTo(advertisement2.hashCode());
        assertThat(advertisement1).isNotEqualTo(advertisement3);
        assertThat(advertisement1.toString()).isNotNull();
    }
}
