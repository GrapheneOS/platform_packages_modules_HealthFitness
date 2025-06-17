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

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.Record;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CanConnectMatchingAppsRequestTest {

    @Test
    public void builder_addRecordType_success() {
        CanConnectMatchingAppsRequest request =
                new CanConnectMatchingAppsRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();

        assertThat(request.getRecordTypes()).containsExactly(ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void builder_addMultipleRecordTypes_success() {
        Set<Class<? extends Record>> recordTypes =
                Set.of(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);

        CanConnectMatchingAppsRequest request =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.getRecordTypes())
                .containsExactly(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);
    }

    @Test
    public void builder_addRecordType_nullThrowsException() {
        CanConnectMatchingAppsRequest.Builder builder = new CanConnectMatchingAppsRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordType(null));
    }

    @Test
    public void builder_addRecordTypes_nullThrowsException() {
        CanConnectMatchingAppsRequest.Builder builder = new CanConnectMatchingAppsRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordTypes(null));
    }

    @Test
    public void getRecordTypes_correctSize() {
        CanConnectMatchingAppsRequest request =
                new CanConnectMatchingAppsRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();

        Set<Class<? extends Record>> recordTypes = request.getRecordTypes();
        assertThat(recordTypes.size()).isEqualTo(1);
        assertThat(recordTypes).contains(ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void parcelable_writeToParcelAndCreateFromParcel_success() {
        Set<Class<? extends Record>> recordTypes =
                Set.of(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);

        CanConnectMatchingAppsRequest originalRequest =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        CanConnectMatchingAppsRequest parceledRequest =
                CanConnectMatchingAppsRequest.CREATOR.createFromParcel(parcel);

        assertThat(parceledRequest.getRecordTypes()).isEqualTo(originalRequest.getRecordTypes());
        parcel.recycle();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.equals(new Object())).isFalse();
    }

    @Test
    public void equals_sameRecordTypes_returnsTrue() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request1 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request2 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isTrue();
    }

    @Test
    public void equals_differentRecordTypes_returnsFalse() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request1 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        CanConnectMatchingAppsRequest request2 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isFalse();
    }

    @Test
    public void hashCode_sameRecordTypes_returnsSameHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request1 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request2 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void hashCode_differentRecordTypes_returnsDifferentHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request1 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        CanConnectMatchingAppsRequest request2 =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    public void toString_containsRecordTypes() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        CanConnectMatchingAppsRequest request =
                new CanConnectMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        String toStringResult = request.toString();
        assertThat(toStringResult).contains("CanConnectMatchingAppsRequest");
        assertThat(toStringResult).contains("recordTypes=" + recordTypes);
    }

    @Test
    public void describeContents_returnsZero() {
        CanConnectMatchingAppsRequest request = new CanConnectMatchingAppsRequest.Builder().build();

        assertThat(request.describeContents()).isEqualTo(0);
    }

    @Test
    public void creator_newArray_returnsCorrectSizeArray() {
        CanConnectMatchingAppsRequest[] requests =
                CanConnectMatchingAppsRequest.CREATOR.newArray(5);
        assertThat(requests.length).isEqualTo(5);
        assertThat(requests[0]).isNull();
    }
}
