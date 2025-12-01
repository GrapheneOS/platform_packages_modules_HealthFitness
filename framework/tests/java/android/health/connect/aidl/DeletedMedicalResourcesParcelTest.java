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
package android.health.connect.aidl;

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.MedicalResourceId;
import android.health.connect.changelog.ChangeLogsResponse.DeletedMedicalResource;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_PHR_CHANGE_LOGS,
    FLAG_DEVELOPMENT_DATABASE_RW,
})
public class DeletedMedicalResourcesParcelTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final Instant DELETION_TIME_1 = Instant.ofEpochMilli(123456);
    private static final Instant DELETION_TIME_2 = Instant.ofEpochMilli(654321);
    private static final MedicalResourceId ID_1 =
            new MedicalResourceId(DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "id1");
    private static final MedicalResourceId ID_2 =
            new MedicalResourceId(DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "id2");

    @Test
    public void testParcelAndUnparcel_nonEmptyList() {
        List<DeletedMedicalResource> originalList =
                List.of(
                        new DeletedMedicalResource(ID_1, DELETION_TIME_1),
                        new DeletedMedicalResource(ID_2, DELETION_TIME_2));

        DeletedMedicalResourcesParcel originalParcel =
                new DeletedMedicalResourcesParcel(originalList);

        Parcel parcel = Parcel.obtain();
        originalParcel.writeToParcel(parcel, 0);
        parcel.setDataPosition(0); // Rewind parcel for reading

        DeletedMedicalResourcesParcel recreatedParcel =
                DeletedMedicalResourcesParcel.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(recreatedParcel).isNotNull();
        List<DeletedMedicalResource> recreatedList = recreatedParcel.getDeletedMedicalResources();
        assertThat(recreatedList).isNotNull();
        assertThat(recreatedList).isEqualTo(originalList);
    }

    @Test
    public void testParcelAndUnparcel_emptyList() {
        List<DeletedMedicalResource> originalList = Collections.emptyList();
        DeletedMedicalResourcesParcel originalParcel =
                new DeletedMedicalResourcesParcel(originalList);

        Parcel parcel = Parcel.obtain();
        originalParcel.writeToParcel(parcel, 0);
        parcel.setDataPosition(0); // Rewind parcel for reading

        DeletedMedicalResourcesParcel recreatedParcel =
                DeletedMedicalResourcesParcel.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(recreatedParcel).isNotNull();
        List<DeletedMedicalResource> recreatedList = recreatedParcel.getDeletedMedicalResources();
        assertThat(recreatedList).isNotNull();
        assertThat(recreatedList).isEmpty();
    }

    @Test
    public void getDeletedMedicalResources_returnsCorrectList() {
        List<DeletedMedicalResource> expectedList =
                List.of(
                        new DeletedMedicalResource(ID_1, DELETION_TIME_1),
                        new DeletedMedicalResource(ID_2, DELETION_TIME_2));
        DeletedMedicalResourcesParcel parcel = new DeletedMedicalResourcesParcel(expectedList);

        List<DeletedMedicalResource> actualList = parcel.getDeletedMedicalResources();

        assertThat(actualList).isSameInstanceAs(expectedList);
        assertThat(actualList).isEqualTo(expectedList);
    }
}
