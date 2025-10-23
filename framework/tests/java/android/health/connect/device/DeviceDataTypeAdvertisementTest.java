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

import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceDataTypeAdvertisementTest {

    @Test
    public void defaultStates_createsCorrectObject() {
        DeviceDataTypeAdvertisement state =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isTrue();
        assertThat(state.isUserEnabled()).isFalse();
        assertThat(state.isVisibleByDefaultInMatchmaking()).isTrue();
    }

    @Test
    public void allFieldsTrue_createsCorrectObject() {
        DeviceDataTypeAdvertisement state =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isTrue();
        assertThat(state.isUserEnabled()).isTrue();
        assertThat(state.isVisibleByDefaultInMatchmaking()).isTrue();
    }

    @Test
    public void allFieldsFalse_createsCorrectObject() {
        DeviceDataTypeAdvertisement state =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(false)
                        .setUserEnabled(false)
                        .setVisibleByDefaultInMatchmaking(false)
                        .build();

        assertThat(state.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(state.isAvailable()).isFalse();
        assertThat(state.isUserEnabled()).isFalse();
        assertThat(state.isVisibleByDefaultInMatchmaking()).isFalse();
    }

    @Test
    public void testParceling_defaultValues() {
        DeviceDataTypeAdvertisement state =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        Parcel parcel = Parcel.obtain();
        state.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataTypeAdvertisement newState =
                DeviceDataTypeAdvertisement.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newState.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(newState.isAvailable()).isTrue();
        assertThat(newState.isUserEnabled()).isFalse();
        assertThat(newState.isVisibleByDefaultInMatchmaking()).isTrue();
    }

    @Test
    public void testParceling_setValues() {
        DeviceDataTypeAdvertisement state =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();

        android.os.Parcel parcel = android.os.Parcel.obtain();
        state.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataTypeAdvertisement newState =
                DeviceDataTypeAdvertisement.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newState.isUserEnabled()).isTrue();
        assertThat(newState.isVisibleByDefaultInMatchmaking()).isTrue();
    }

    @Test
    public void testEqualsAndHashCode() {
        DeviceDataTypeAdvertisement base =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();
        DeviceDataTypeAdvertisement same =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();
        DeviceDataTypeAdvertisement diffAvailable =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(false)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();
        DeviceDataTypeAdvertisement diffUserEnabled =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(false)
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();
        DeviceDataTypeAdvertisement diffVisible =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .setVisibleByDefaultInMatchmaking(false)
                        .build();

        assertThat(base.equals(base)).isTrue();
        assertThat(base.equals(same)).isTrue();
        assertThat(same.equals(base)).isTrue();
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
        assertThat(base.equals(null)).isFalse();
        assertThat(base.equals(new Object())).isFalse();
        assertThat(base.equals(diffAvailable)).isFalse();
        assertThat(base.equals(diffUserEnabled)).isFalse();
        assertThat(base.equals(diffVisible)).isFalse();
    }
}
