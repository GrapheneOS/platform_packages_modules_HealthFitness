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
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class GetMatchingAppsRequestTest {
    private static final String TEST_PACKAGE_NAME = "com.test.package";

    @Test
    public void builder_addRecordType_success() {
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();

        assertThat(request.getRecordTypes()).containsExactly(ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void builder_addMultipleRecordTypes_success() {
        Set<Class<? extends Record>> recordTypes =
                Set.of(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);

        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.getRecordTypes())
                .containsExactly(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testBuilder_setPackageName() {
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().setPackageName(TEST_PACKAGE_NAME).build();
        assertThat(request.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    public void builder_addRecordType_nullThrowsException() {
        GetMatchingAppsRequest.Builder builder = new GetMatchingAppsRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordType(null));
    }

    @Test
    public void builder_addRecordTypes_nullThrowsException() {
        GetMatchingAppsRequest.Builder builder = new GetMatchingAppsRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordTypes(null));
    }

    @Test
    public void getRecordTypes_correctSize() {
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
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

        GetMatchingAppsRequest originalRequest =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();
        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        GetMatchingAppsRequest parceledRequest =
                GetMatchingAppsRequest.CREATOR.createFromParcel(parcel);

        assertThat(parceledRequest.getRecordTypes()).isEqualTo(originalRequest.getRecordTypes());
        parcel.recycle();
    }

    @Test
    public void parcelable_withPackageName() {
        GetMatchingAppsRequest originalRequest =
                new GetMatchingAppsRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setPackageName(TEST_PACKAGE_NAME)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        GetMatchingAppsRequest newRequest = GetMatchingAppsRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.equals(new Object())).isFalse();
    }

    @Test
    public void equals_sameRecordTypes_returnsTrue() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isTrue();
    }

    @Test
    public void equals_differentRecordTypes_returnsFalse() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isFalse();
    }

    @Test
    public void equals_withPackageName() {
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        GetMatchingAppsRequest request3 =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName("com.another.package")
                        .addRecordType(StepsRecord.class)
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    public void hashCode_sameRecordTypes_returnsSameHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void hashCode_differentRecordTypes_returnsDifferentHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    public void hashCode_withPackageName() {
        GetMatchingAppsRequest request1 =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        GetMatchingAppsRequest request2 =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void toString_containsRecordTypes() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder().addRecordTypes(recordTypes).build();

        String toStringResult = request.toString();
        assertThat(toStringResult).contains("GetMatchingAppsRequest");
        assertThat(toStringResult).contains("recordTypes=" + recordTypes);
    }

    @Test
    public void toString_withPackageName() {
        GetMatchingAppsRequest request =
                new GetMatchingAppsRequest.Builder()
                        .setPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .addRecordType(SleepSessionRecord.class)
                        .build();
        String requestString = request.toString();
        assertThat(requestString).contains("packageName=" + TEST_PACKAGE_NAME);
        assertThat(requestString).contains("recordTypes=[");
        assertThat(requestString).contains(StepsRecord.class.toString());
        assertThat(requestString).contains(SleepSessionRecord.class.toString());
    }

    @Test
    public void describeContents_returnsZero() {
        GetMatchingAppsRequest request = new GetMatchingAppsRequest.Builder().build();

        assertThat(request.describeContents()).isEqualTo(0);
    }

    @Test
    public void creator_newArray_returnsCorrectSizeArray() {
        GetMatchingAppsRequest[] requests = GetMatchingAppsRequest.CREATOR.newArray(5);
        assertThat(requests.length).isEqualTo(5);
        assertThat(requests[0]).isNull();
    }
}
