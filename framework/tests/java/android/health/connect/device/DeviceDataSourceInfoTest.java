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

import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
public class DeviceDataSourceInfoTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void testDeviceDataSourceInfo_equalsAndHashCode() {
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("pkg1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("pkg2").build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo("pkg1", "id1", "", "", ImmutableSet.of());
        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo("pkg2", "id2", "", "", ImmutableSet.of());

        DeviceDataSourceInfo info1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo info1Copy =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo info2 =
                new DeviceDataSourceInfo(origin2, device2, false, List.of(providerInfo2));

        assertThat(info1).isEqualTo(info1Copy);
        assertThat(info1.hashCode()).isEqualTo(info1Copy.hashCode());
        assertThat(info1).isNotEqualTo(info2);
    }

    @Test
    public void testDeviceDataProviderInfo_equalsAndHashCode() {
        DeviceDataTypeAdvertisement ad1 =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();
        DeviceDataTypeAdvertisement ad2 =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(false)
                        .build();

        DeviceDataProviderInfo info1 =
                new DeviceDataProviderInfo(
                        "pkg1", "id1", "onboard", "manage", ImmutableSet.of(ad1));
        DeviceDataProviderInfo info1Copy =
                new DeviceDataProviderInfo(
                        "pkg1", "id1", "onboard", "manage", ImmutableSet.of(ad1));
        DeviceDataProviderInfo info2 =
                new DeviceDataProviderInfo("pkg2", "id2", "", "", ImmutableSet.of(ad2));

        assertThat(info1).isEqualTo(info1Copy);
        assertThat(info1.hashCode()).isEqualTo(info1Copy.hashCode());
        assertThat(info1).isNotEqualTo(info2);
    }

    @Test
    public void testDeviceDataSourceInfo_parcelable() {
        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName("pkg1").build();
        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo("pkg1", "id1", "onboard", "manage", ImmutableSet.of());
        DeviceDataSourceInfo original =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataSourceInfo restored = DeviceDataSourceInfo.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restored).isEqualTo(original);
    }

    @Test
    public void testDeviceDataProviderInfo_parcelable() {
        DeviceDataTypeAdvertisement ad =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();
        DeviceDataProviderInfo original =
                new DeviceDataProviderInfo("pkg1", "id1", "onboard", "manage", ImmutableSet.of(ad));

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataProviderInfo restored = DeviceDataProviderInfo.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(restored).isEqualTo(original);
    }

    @Test
    public void testImmutability() {
        DeviceDataTypeAdvertisement ad =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();
        Set<DeviceDataTypeAdvertisement> advertisements = new HashSet<>();
        advertisements.add(ad);

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo("pkg", "id", "", "", advertisements);

        // Modify original list
        advertisements.clear();

        assertThat(providerInfo.getDeviceDataTypeAdvertisements()).hasSize(1);
        assertThat(providerInfo.getDeviceDataTypeAdvertisements()).contains(ad);

        List<DeviceDataProviderInfo> mutableProviderList = new java.util.ArrayList<>();
        mutableProviderList.add(providerInfo);

        DeviceDataSourceInfo sourceInfo =
                new DeviceDataSourceInfo(
                        new DataOrigin.Builder().setPackageName("pkg").build(),
                        new Device.Builder().build(),
                        true,
                        mutableProviderList);

        // Modify original list
        mutableProviderList.clear();

        assertThat(sourceInfo.getDeviceDataProviderInfos()).hasSize(1);
        assertThat(sourceInfo.getDeviceDataProviderInfos()).contains(providerInfo);
    }
}
