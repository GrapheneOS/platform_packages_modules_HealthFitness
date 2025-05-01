/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.phr;

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.PAGE_TOKEN;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getFhirResourceAllergy;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResourceBuilder;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.datatypes.MedicalResource;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesResponseTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testReadMedicalResourcesResponse_constructor_emptyList() {
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(List.of(), null, 0);

        assertThat(response.getMedicalResources()).isEqualTo(List.of());
        assertThat(response.getNextPageToken()).isNull();
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    public void testReadMedicalResourcesResponse_constructor_singleton() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(medicalResources, null, 0);

        assertThat(response.getMedicalResources()).isEqualTo(medicalResources);
        assertThat(response.getNextPageToken()).isNull();
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    public void testReadMedicalResourcesResponse_constructor_multipleEntries() {
        List<MedicalResource> medicalResources =
                List.of(
                        getMedicalResource(),
                        getMedicalResourceBuilder()
                                .setType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .setDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                                .setFhirResource(getFhirResourceAllergy())
                                .build());
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(medicalResources, null, 0);

        assertThat(response.getMedicalResources()).isEqualTo(medicalResources);
        assertThat(response.getNextPageToken()).isNull();
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    public void testReadMedicalResourcesResponse_constructor_withPageToken() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 11);

        assertThat(response.getMedicalResources()).isEqualTo(medicalResources);
        assertThat(response.getNextPageToken()).isEqualTo(PAGE_TOKEN);
        assertThat(response.getRemainingCount()).isEqualTo(11);
    }

    @Test
    public void testReadMedicalResourcesResponse_equals() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response1 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 10);
        ReadMedicalResourcesResponse response2 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 10);

        assertThat(response1.equals(response2)).isTrue();
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    public void testReadMedicalResourcesResponseEquals_comparesTotalSize() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response1 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 10);
        ReadMedicalResourcesResponse response2 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 11);

        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    public void testReadMedicalResourcesResponseEquals_comparesPageToken() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response1 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 10);
        ReadMedicalResourcesResponse response2 =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN + "1", 10);

        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    public void testReadMedicalResourcesResponseEquals_comparesResources() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 11);
        ReadMedicalResourcesResponse responseDifferentList =
                new ReadMedicalResourcesResponse(
                        List.of(
                                getMedicalResourceBuilder()
                                        .setType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                        .setDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                                        .setFhirResource(getFhirResourceAllergy())
                                        .build()),
                        PAGE_TOKEN,
                        11);

        assertThat(responseDifferentList.equals(response)).isFalse();
    }

    @Test
    public void testToString() {
        MedicalResource medicalResource = getMedicalResource();
        ReadMedicalResourcesResponse response =
                new ReadMedicalResourcesResponse(List.of(medicalResource), PAGE_TOKEN, 1);
        String medicalResourceTypeString = "medicalResources=[" + medicalResource + "]";
        String nextPageTokenString = "nextPageToken=" + PAGE_TOKEN;

        assertThat(response.toString()).contains(medicalResourceTypeString);
        assertThat(response.toString()).contains(nextPageTokenString);
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        List<MedicalResource> medicalResources =
                List.of(
                        getMedicalResource(),
                        getMedicalResourceBuilder()
                                .setType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .setDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                                .setFhirResource(getFhirResourceAllergy())
                                .build());
        ReadMedicalResourcesResponse original =
                new ReadMedicalResourcesResponse(medicalResources, PAGE_TOKEN, 11);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ReadMedicalResourcesResponse restored =
                ReadMedicalResourcesResponse.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }
}
