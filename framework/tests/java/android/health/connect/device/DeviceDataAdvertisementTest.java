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

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
import android.healthconnect.testing.shared.DataFactory;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
public class DeviceDataAdvertisementTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void constructAdvertisement_returnsCorrectValues() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);

        assertThat(advertisement.getDevice()).isEqualTo(device);
        assertThat(advertisement.getDeviceId()).isEqualTo(deviceId);
        assertThat(advertisement.getDeviceDataTypeAdvertisements())
                .isEqualTo(deviceDataTypeAdvertisement);
    }

    @Test
    public void constructAdvertisement_noDeviceDataTypeAdvertisement_returnsCorrectValues() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";

        ImmutableSet<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement = ImmutableSet.of();

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);

        assertThat(advertisement.getDevice()).isEqualTo(device);
        assertThat(advertisement.getDeviceId()).isEqualTo(deviceId);
        assertThat(advertisement.getDeviceDataTypeAdvertisements())
                .isEqualTo(deviceDataTypeAdvertisement);
    }

    @Test
    public void build_symptomRecordWithoutSymptomType_throwsException() {
        assertThrows(
                IllegalStateException.class,
                () -> new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class).build());
    }

    @Test
    public void testParceling_allFieldsSet() {

        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement originalAdvertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);

        android.os.Parcel parcel = android.os.Parcel.obtain();
        originalAdvertisement.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataAdvertisement restoredAdvertisement =
                DeviceDataAdvertisement.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restoredAdvertisement.getDevice()).isEqualTo(originalAdvertisement.getDevice());
        assertThat(restoredAdvertisement.getDeviceId())
                .isEqualTo(originalAdvertisement.getDeviceId());
        assertThat(restoredAdvertisement.getDeviceDataTypeAdvertisements())
                .isEqualTo(originalAdvertisement.getDeviceDataTypeAdvertisements());
    }

    @Test
    public void testParceling_withSymptomType() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                                .setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement originalAdvertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);

        android.os.Parcel parcel = android.os.Parcel.obtain();
        originalAdvertisement.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataAdvertisement restoredAdvertisement =
                DeviceDataAdvertisement.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restoredAdvertisement.getDevice()).isEqualTo(originalAdvertisement.getDevice());
        assertThat(restoredAdvertisement.getDeviceId())
                .isEqualTo(originalAdvertisement.getDeviceId());
        assertThat(restoredAdvertisement.getDeviceDataTypeAdvertisements())
                .isEqualTo(originalAdvertisement.getDeviceDataTypeAdvertisements());
    }

    @Test
    public void testParceling_emptyStateSet() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .setDisplayName("WatchA")
                        .build();
        String deviceId = "ABC123";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement = ImmutableSet.of();
        DeviceDataAdvertisement originalAdvertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);

        android.os.Parcel parcel = android.os.Parcel.obtain();
        originalAdvertisement.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataAdvertisement restoredAdvertisement =
                DeviceDataAdvertisement.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restoredAdvertisement.getDevice()).isEqualTo(originalAdvertisement.getDevice());
        assertThat(restoredAdvertisement.getDeviceId())
                .isEqualTo(originalAdvertisement.getDeviceId());
        assertThat(restoredAdvertisement.getDeviceDataTypeAdvertisements()).isEmpty();
    }

    @Test
    public void testEqualsAndHashCode() {
        Device device1 =
                new Device.Builder()
                        .setManufacturer("ManufacturerA")
                        .setModel("ModelA")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("DeviceA")
                        .build();
        Device device2 =
                new Device.Builder()
                        .setManufacturer("ManufacturerB")
                        .setModel("ModelB")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .setDisplayName("DeviceB")
                        .build();
        String deviceId1 = "Id1";
        String deviceId2 = "Id2";
        Set<DeviceDataTypeAdvertisement> advertisement1 =
                ImmutableSet.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        Set<DeviceDataTypeAdvertisement> advertisement2 =
                ImmutableSet.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(false)
                                .build());

        DeviceDataAdvertisement base =
                new DeviceDataAdvertisement(device1, deviceId1, advertisement1);
        DeviceDataAdvertisement same =
                new DeviceDataAdvertisement(device1, deviceId1, advertisement1);
        DeviceDataAdvertisement diffDevice =
                new DeviceDataAdvertisement(device2, deviceId1, advertisement1);
        DeviceDataAdvertisement diffId =
                new DeviceDataAdvertisement(device1, deviceId2, advertisement1);
        DeviceDataAdvertisement diffDataTypes =
                new DeviceDataAdvertisement(device1, deviceId1, advertisement2);
        DeviceDataAdvertisement emptyDataTypes =
                new DeviceDataAdvertisement(device1, deviceId1, ImmutableSet.of());

        assertThat(base.equals(base)).isTrue();
        assertThat(base.equals(same)).isTrue();
        assertThat(same.equals(base)).isTrue();
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
        assertThat(base.equals(null)).isFalse();
        assertThat(base.equals(new Object())).isFalse();
        assertThat(base.equals(diffDevice)).isFalse();
        assertThat(base.equals(diffId)).isFalse();
        assertThat(base.equals(diffDataTypes)).isFalse();
        assertThat(base.equals(emptyDataTypes)).isFalse();
    }

    @Test
    public void toUnmasked_withDeviceId_returnsNewUnmaskedInstance() {
        Device device = DataFactory.buildDevice();
        String maskedDeviceId = "Masked";
        String unmaskedDeviceId = "Unmasked";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());

        DeviceDataAdvertisement maskedAdvertisement =
                new DeviceDataAdvertisement(device, maskedDeviceId, deviceDataTypeAdvertisement);

        assertThat(maskedAdvertisement.getDeviceId()).isEqualTo(maskedDeviceId);

        Function<String, String> unmasker =
                packageName -> {
                    assertThat(packageName).isEqualTo(maskedDeviceId);
                    return unmaskedDeviceId;
                };

        DeviceDataAdvertisement unmaskedAdvertisement = maskedAdvertisement.toUnmasked(unmasker);

        // Assert that the original advertisement remains unchanged.
        assertThat(maskedAdvertisement.getDeviceId()).isEqualTo(maskedDeviceId);
        assertThat(unmaskedAdvertisement).isNotSameInstanceAs(maskedAdvertisement);

        assertThat(unmaskedAdvertisement.getDevice()).isEqualTo(maskedAdvertisement.getDevice());
        assertThat(unmaskedAdvertisement.getDeviceDataTypeAdvertisements())
                .isEqualTo(maskedAdvertisement.getDeviceDataTypeAdvertisements());
        assertThat(unmaskedAdvertisement.getDeviceId()).isEqualTo(unmaskedDeviceId);
    }
}
