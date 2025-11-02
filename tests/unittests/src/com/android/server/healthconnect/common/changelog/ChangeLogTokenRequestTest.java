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
package com.android.server.healthconnect.common.changelog;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.annotation.SuppressLint;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ChangeLogTokenRequestTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String TEST_PACKAGE_1 = "com.example.test1";
    private static final String TEST_PACKAGE_2 = "com.example.test2";

    @Test
    @RequiresFlagsDisabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void build_noRecordTypes_flagDisabled_throws() {
        ChangeLogTokenRequest.Builder builder = new ChangeLogTokenRequest.Builder();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void build_noTypes_flagEnabled_throws() {
        ChangeLogTokenRequest.Builder builder = new ChangeLogTokenRequest.Builder();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void addBothTypes_recordFirst_flagEnabled_throws_beforeBuild() {
        var builder = new ChangeLogTokenRequest.Builder().addRecordType(StepsRecord.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        builder.addMedicalResourceType(
                                MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES));
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void addBothTypes_medicalResourceFirst_flagEnabled_throws_beforeBuild() {
        var builder =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThrows(
                IllegalArgumentException.class, () -> builder.addRecordType(StepsRecord.class));
    }

    @Test
    public void build_invalidRecordType_throws() {
        // Record.class itself is not a valid concrete record type
        ChangeLogTokenRequest.Builder builder =
                new ChangeLogTokenRequest.Builder().addRecordType(Record.class);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void build_invalidMedicalResourceType_throws() {
        @SuppressLint("WrongConstant") // Testing invalid type
        ChangeLogTokenRequest.Builder builder =
                new ChangeLogTokenRequest.Builder().addMedicalResourceType(-1);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void build_withRecordTypes_success() {
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        DataOrigin dataOrigin2 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_2).build();

        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addDataOriginFilter(dataOrigin1)
                        .addDataOriginFilter(dataOrigin2)
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .build();

        assertThat(request.getDataOriginFilters()).containsExactly(dataOrigin1, dataOrigin2);
        assertThat(request.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1, TEST_PACKAGE_2);
        assertThat(request.getRecordTypes())
                .containsExactly(StepsRecord.class, HeartRateRecord.class);
        assertThat(request.getRecordTypeIds())
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_HEART_RATE);
        assertThat(request.getMedicalResourceTypes()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void build_withMedicalResourceTypes_success() {
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        DataOrigin dataOrigin2 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_2).build();

        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addDataOriginFilter(dataOrigin1)
                        .addDataOriginFilter(dataOrigin2)
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES)
                        .build();

        assertThat(request.getDataOriginFilters()).containsExactly(dataOrigin1, dataOrigin2);
        assertThat(request.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1, TEST_PACKAGE_2);
        assertThat(request.getRecordTypes()).isEmpty();
        assertThat(request.getRecordTypeIds()).isEmpty();
        assertThat(request.getMedicalResourceTypes())
                .containsExactly(
                        MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                        MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES);
    }

    @Test
    public void parcelAndUnparcel_recordTypes_noFilters_equals() {
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ChangeLogTokenRequest unparceledRequest =
                ChangeLogTokenRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledRequest).isEqualTo(originalRequest);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void parcelAndUnparcel_medicalResourceTypes_noFilters_equals() {
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ChangeLogTokenRequest unparceledRequest =
                ChangeLogTokenRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledRequest).isEqualTo(originalRequest);
    }

    @Test
    public void parcelAndUnparcel_recordTypes_equals() {
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .addDataOriginFilter(dataOrigin1)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ChangeLogTokenRequest unparceledRequest =
                ChangeLogTokenRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledRequest).isEqualTo(originalRequest);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void parcelAndUnparcel_medicalResourceTypes_equals() {
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES)
                        .addDataOriginFilter(dataOrigin)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ChangeLogTokenRequest unparceledRequest =
                ChangeLogTokenRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledRequest).isEqualTo(originalRequest);
    }

    @Test
    public void getters_returnCorrectValues_recordTypes() {
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        DataOrigin dataOrigin2 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_2).build();

        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addDataOriginFilter(dataOrigin1)
                        .addDataOriginFilter(dataOrigin2)
                        .build();

        assertThat(request.getDataOriginFilters()).containsExactly(dataOrigin1, dataOrigin2);
        assertThat(request.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1, TEST_PACKAGE_2);
        assertThat(request.getRecordTypes()).containsExactly(StepsRecord.class);
        assertThat(request.getRecordTypeIds()).containsExactly(RECORD_TYPE_STEPS);
        assertThat(request.getMedicalResourceTypes()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void getters_returnCorrectValues_medicalResourceTypes() {
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        DataOrigin dataOrigin2 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_2).build();

        ChangeLogTokenRequest request =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataOriginFilter(dataOrigin1)
                        .addDataOriginFilter(dataOrigin2)
                        .build();

        assertThat(request.getDataOriginFilters()).containsExactly(dataOrigin1, dataOrigin2);
        assertThat(request.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1, TEST_PACKAGE_2);
        assertThat(request.getRecordTypes()).isEmpty();
        assertThat(request.getRecordTypeIds()).isEmpty();
        assertThat(request.getMedicalResourceTypes())
                .containsExactly(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    public void toUnmasked_withRecordTypes_unmasksPackageNamesCorrectly() {
        // Verifies that toUnmasked correctly applies the transformation to package names
        // when the request is built with record types.
        DataOrigin dataOrigin1 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        DataOrigin dataOrigin2 = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_2).build();
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addDataOriginFilter(dataOrigin1)
                        .addDataOriginFilter(dataOrigin2)
                        .build();

        ChangeLogTokenRequest unmaskedRequest =
                originalRequest.toUnmasked(packageName -> packageName + "_unmasked");

        // Assert that the new request has the unmasked package names.
        assertThat(unmaskedRequest.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1 + "_unmasked", TEST_PACKAGE_2 + "_unmasked");
        // Assert that other fields are unchanged.
        assertThat(unmaskedRequest.getRecordTypeIds())
                .isEqualTo(originalRequest.getRecordTypeIds());
        assertThat(unmaskedRequest.getMedicalResourceTypes())
                .isEqualTo(originalRequest.getMedicalResourceTypes());
        // Assert that the original request is not modified.
        assertThat(originalRequest.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1, TEST_PACKAGE_2);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
    })
    public void toUnmasked_withMedicalResourceTypes_unmasksPackageNamesCorrectly() {
        // Verifies that toUnmasked correctly applies the transformation to package names
        // when the request is built with medical resource types.
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(TEST_PACKAGE_1).build();
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataOriginFilter(dataOrigin)
                        .build();

        ChangeLogTokenRequest unmaskedRequest =
                originalRequest.toUnmasked(packageName -> packageName + "_unmasked");

        // Assert that the new request has the unmasked package names.
        assertThat(unmaskedRequest.getPackageNamesToFilter())
                .containsExactly(TEST_PACKAGE_1 + "_unmasked");
        // Assert that other fields are unchanged.
        assertThat(unmaskedRequest.getRecordTypeIds())
                .isEqualTo(originalRequest.getRecordTypeIds());
        assertThat(unmaskedRequest.getMedicalResourceTypes())
                .isEqualTo(originalRequest.getMedicalResourceTypes());
        // Assert that the original request is not modified.
        assertThat(originalRequest.getPackageNamesToFilter()).containsExactly(TEST_PACKAGE_1);
    }

    @Test
    public void toUnmasked_noPackageNames_returnsEquivalentRequest() {
        // Verifies that toUnmasked returns an equivalent request when there are no package names
        // to transform.
        ChangeLogTokenRequest originalRequest =
                new ChangeLogTokenRequest.Builder().addRecordType(StepsRecord.class).build();

        ChangeLogTokenRequest unmaskedRequest =
                originalRequest.toUnmasked(packageName -> packageName + "_unmasked");

        // Assert that the new request is equal to the original, as there are no package names.
        assertThat(unmaskedRequest).isEqualTo(originalRequest);
        assertThat(unmaskedRequest.getPackageNamesToFilter()).isEmpty();
    }
}
