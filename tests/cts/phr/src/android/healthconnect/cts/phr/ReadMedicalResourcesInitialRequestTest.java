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

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.DataFactory.DEFAULT_PAGE_SIZE;
import static android.healthconnect.testing.shared.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.testing.shared.DataFactory.MINIMUM_PAGE_SIZE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesInitialRequestTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testRequestBuilder_requiredFieldsOnly() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        assertThat(request.getMedicalResourceType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(request.getDataSourceIds()).isEmpty();
        assertThat(request.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testRequestBuilder_setAllFields() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .setMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();

        assertThat(request.getMedicalResourceType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(request.getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
        assertThat(request.getPageSize()).isEqualTo(100);
    }

    @Test
    public void testRequestBuilder_clearDataSourceIds() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .clearDataSourceIds()
                        .build();

        assertThat(request.getDataSourceIds()).isEmpty();
    }

    @Test
    public void testRequestBuilder_invalidMedicalResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReadMedicalResourcesInitialRequest.Builder(-1));
    }

    @Test
    public void testRequestBuilder_addInvalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReadMedicalResourcesInitialRequest.Builder(
                                        MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addDataSourceId("foo"));
    }

    @Test
    public void testRequestBuilder_lowerThanMinPageSize_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReadMedicalResourcesInitialRequest.Builder(
                                        MEDICAL_RESOURCE_TYPE_VACCINES)
                                .setPageSize(MAXIMUM_PAGE_SIZE + 1));
    }

    @Test
    public void testRequestBuilder_exceedsMaxPageSize_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReadMedicalResourcesInitialRequest.Builder(
                                        MEDICAL_RESOURCE_TYPE_VACCINES)
                                .setPageSize(MINIMUM_PAGE_SIZE - 1));
    }

    @Test
    public void testRequestBuilder_fromExistingBuilder() {
        ReadMedicalResourcesInitialRequest.Builder original =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .setPageSize(100);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(original).build();

        assertThat(request).isEqualTo(original.build());
    }

    @Test
    public void testRequestBuilder_fromExistingInstance() {
        ReadMedicalResourcesInitialRequest original =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(original).build();

        assertThat(request).isEqualTo(original);
    }

    @Test
    public void testToString() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();
        String medicalResourceTypeString = "medicalResourceType=1";
        String dataSourceIdsString = "dataSourceIds={" + DATA_SOURCE_ID + "}";
        String pageSizeString = "pageSize=100";

        assertThat(request.toString()).contains(medicalResourceTypeString);
        assertThat(request.toString()).contains(dataSourceIdsString);
        assertThat(request.toString()).contains(pageSizeString);
    }

    @Test
    public void testEquals_sameRequests() {
        ReadMedicalResourcesInitialRequest request1 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();
        ReadMedicalResourcesInitialRequest request2 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();

        assertThat(request1.equals(request2)).isTrue();
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void testEquals_comparesAllValues() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .build();
        ReadMedicalResourcesInitialRequest requestDifferentType =
                new ReadMedicalResourcesInitialRequest.Builder(request)
                        .setMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesInitialRequest requestDifferentDataSource =
                new ReadMedicalResourcesInitialRequest.Builder(request)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();
        ReadMedicalResourcesInitialRequest requestDifferentPageSize =
                new ReadMedicalResourcesInitialRequest.Builder(request).setPageSize(100).build();

        assertThat(requestDifferentType.equals(request)).isFalse();
        assertThat(requestDifferentDataSource.equals(request)).isFalse();
        assertThat(requestDifferentPageSize.equals(request)).isFalse();
        assertThat(requestDifferentType.hashCode()).isNotEqualTo(request.hashCode());
        assertThat(requestDifferentDataSource.hashCode()).isNotEqualTo(request.hashCode());
        assertThat(requestDifferentPageSize.hashCode()).isNotEqualTo(request.hashCode());
    }
}
