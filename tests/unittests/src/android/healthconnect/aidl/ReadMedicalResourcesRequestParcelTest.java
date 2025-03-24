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
package android.healthconnect.aidl;

import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.MINIMUM_PAGE_SIZE;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.PAGE_TOKEN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.annotation.Nullable;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesPageRequest;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesRequestParcelTest {

    @Test
    public void testWriteInitialRequestToParcelThenRestore_propertiesAreIdentical() {
        ReadMedicalResourcesInitialRequest original =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .setPageSize(100)
                        .build();

        Parcel parcel = Parcel.obtain();
        original.toParcel().writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ReadMedicalResourcesRequestParcel restoredParcel =
                ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel);

        assertThat(restoredParcel.getMedicalResourceType())
                .isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(restoredParcel.getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
        assertThat(restoredParcel.getPageToken()).isNull();
        assertThat(restoredParcel.getPageSize()).isEqualTo(100);
        parcel.recycle();
    }

    @Test
    public void testWritePageRequestToParcelThenRestore_propertiesAreIdentical() {
        ReadMedicalResourcesPageRequest original =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100).build();

        Parcel parcel = Parcel.obtain();
        original.toParcel().writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ReadMedicalResourcesRequestParcel restoredParcel =
                ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel);

        assertThat(restoredParcel.getMedicalResourceType()).isEqualTo(0);
        assertThat(restoredParcel.getDataSourceIds()).isEmpty();
        assertThat(restoredParcel.getPageToken()).isEqualTo(PAGE_TOKEN);
        assertThat(restoredParcel.getPageSize()).isEqualTo(100);
    }

    @Test
    public void testRestoreInvalidPageRequestFromParcel_nullPageToken_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        // Write a bad parcel which has a null page token
        Parcel parcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ true,
                        /* medicalResourceType= */ 0,
                        /* dataSourceIds= */ Set.of(),
                        /* pageToken= */ null, // bad value being tested
                        /* pageSize= */ 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreInvalidMedicalResourceTypeFromParcel_expectException() {
        Parcel parcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ false,
                        /* medicalResourceType= */ -1, // bad value being tested
                        /* dataSourceIds= */ Set.of(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID),
                        /* pageToken= */ null,
                        /* pageSize= */ 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreInvalidDataSourceIdFromParcel_expectException() {
        Parcel parcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ false,
                        /* medicalResourceType= */ MEDICAL_RESOURCE_TYPE_VACCINES,
                        /* dataSourceIds= */ Set.of("1"), // bad value being tested, must be uuid
                        /* pageToken= */ null,
                        /* pageSize= */ 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreTooSmallPageSizeFromParcel_expectException() {
        Parcel parcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ false,
                        /* medicalResourceType= */ MEDICAL_RESOURCE_TYPE_VACCINES,
                        /* dataSourceIds= */ Set.of(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID),
                        /* pageToken= */ null,
                        /* pageSize= */ MINIMUM_PAGE_SIZE - 1); // bad value being tested

        assertThrows(
                IllegalArgumentException.class,
                () -> ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreTooLargePageSizeFromParcel_expectException() {
        Parcel parcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ false,
                        /* medicalResourceType= */ MEDICAL_RESOURCE_TYPE_VACCINES,
                        /* dataSourceIds= */ Set.of(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID),
                        /* pageToken= */ null,
                        /* pageSize= */ MAXIMUM_PAGE_SIZE + 1); // bad value being tested

        assertThrows(
                IllegalArgumentException.class,
                () -> ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testCreateUnvalidatedParcel_byDefault_success() {
        Parcel goodParcel =
                createUnvalidatedParcel(
                        /* isPageRequest= */ false,
                        /* medicalResourceType= */ MEDICAL_RESOURCE_TYPE_VACCINES,
                        /* dataSourceIds= */ Set.of(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID),
                        /* pageToken= */ null,
                        /* pageSize= */ 100);

        ReadMedicalResourcesRequestParcel restored =
                ReadMedicalResourcesRequestParcel.CREATOR.createFromParcel(goodParcel);
        assertThat(restored.getMedicalResourceType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(restored.getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
        assertThat(restored.getPageToken()).isNull();
        assertThat(restored.getPageSize()).isEqualTo(100);
    }

    /**
     * Create a parcel with values that cannot be created with the normal builder (because it
     * validates), but need to be tested as they could be received from apps.
     */
    private static Parcel createUnvalidatedParcel(
            boolean isPageRequest,
            int medicalResourceType,
            Set<String> dataSourceIds,
            @Nullable String pageToken,
            int pageSize) {
        Parcel parcel = Parcel.obtain();
        parcel.writeBoolean(isPageRequest);
        parcel.writeInt(medicalResourceType);
        parcel.writeStringList(new ArrayList<>(dataSourceIds));
        parcel.writeString(pageToken);
        parcel.writeInt(pageSize);
        parcel.setDataPosition(0);
        return parcel;
    }
}
