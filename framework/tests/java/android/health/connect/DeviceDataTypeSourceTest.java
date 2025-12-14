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

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceDataTypeSourceTest {

    @Test
    public void testConstructor_symptomRecord() {
        DeviceDataTypeSource source =
                DeviceDataTypeSource.ofSymptomType(
                        SymptomRecord.SYMPTOM_TYPE_COUGH,
                        /* isAvailable= */ true,
                        /* isUserEnabled= */ true);

        assertThat(source.getDataType()).isEqualTo(SymptomRecord.class);
        assertThat(source.getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH);
    }

    @Test
    public void testConstructor_stepsRecord() {
        DeviceDataTypeSource source =
                DeviceDataTypeSource.ofDataType(
                        StepsRecord.class, /* isAvailable= */ true, /* isUserEnabled= */ true);

        assertThat(source.getDataType()).isEqualTo(StepsRecord.class);
        assertThat(source.getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_UNKNOWN);
    }

    @Test
    public void testConstructor_symptomRecord_unknownType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DeviceDataTypeSource.ofSymptomType(
                                SymptomRecord.SYMPTOM_TYPE_UNKNOWN,
                                /* isAvailable= */ true,
                                /* isUserEnabled= */ true));
    }

    @Test
    public void testParceling() {
        DeviceDataTypeSource source =
                DeviceDataTypeSource.ofSymptomType(
                        SymptomRecord.SYMPTOM_TYPE_COUGH,
                        /* isAvailable= */ true,
                        /* isUserEnabled= */ true);

        Parcel parcel = Parcel.obtain();
        source.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeviceDataTypeSource fromParcel = DeviceDataTypeSource.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(fromParcel).isEqualTo(source);
        assertThat(fromParcel.getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH);
    }
}
