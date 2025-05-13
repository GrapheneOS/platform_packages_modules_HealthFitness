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
import static android.healthconnect.testing.cts.TestUtils.setFieldValueUsingReflection;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.DeleteMedicalResourcesRequest;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class DeleteMedicalResourcesRequestTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testRequestBuilder_noMedicalResourceTypesAndDataSources_throws() {
        DeleteMedicalResourcesRequest.Builder request = new DeleteMedicalResourcesRequest.Builder();

        assertThrows(IllegalArgumentException.class, request::build);
    }

    @Test
    public void testRequestBuilder_invalidMedicalResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteMedicalResourcesRequest.Builder().addMedicalResourceType(-1));
    }

    @Test
    public void testRequestBuilder_invalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteMedicalResourcesRequest.Builder().addDataSourceId("1"));
    }

    @Test
    public void testRequestBuilder_oneDatasource_ok() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        assertThat(request.getDataSourceIds()).containsExactly(DATA_SOURCE_ID);
    }

    @Test
    public void testRequestBuilder_multipleDatasource_ok() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        assertThat(request.getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
    }

    @Test
    public void testRequestBuilder_oneResourceType_ok() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        assertThat(request.getMedicalResourceTypes())
                .containsExactly(MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    public void testRequestBuilder_multipleResourceTypes_ok() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        assertThat(request.getMedicalResourceTypes())
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    public void testRequestBuilder_multipleResourceTypesAndDataSources_ok() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        assertThat(request.getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);

        assertThat(request.getMedicalResourceTypes())
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    public void testRequestBuilder_fromExistingBuilder() {
        DeleteMedicalResourcesRequest.Builder original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder(original).build();

        assertThat(request).isEqualTo(original.build());
    }

    @Test
    public void testRequestBuilder_fromExistingBuilder_changeIndependently() {
        DeleteMedicalResourcesRequest.Builder original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID);
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        original.addDataSourceId(DIFFERENT_DATA_SOURCE_ID);

        assertThat(original.build().getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
        assertThat(copy.build().getDataSourceIds()).containsExactly(DATA_SOURCE_ID);
    }

    @Test
    public void testRequestBuilder_fromExistingBuilderResourceType() {
        DeleteMedicalResourcesRequest.Builder original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder(original).build();

        assertThat(request).isEqualTo(original.build());
    }

    @Test
    public void testRequestBuilder_fromExistingBuilderClearDataSources() {
        DeleteMedicalResourcesRequest.Builder original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES);
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        original.addDataSourceId(DIFFERENT_DATA_SOURCE_ID);

        copy.clearDataSourceIds();

        assertThat(original.build().getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
        assertThat(copy.build().getDataSourceIds()).isEmpty();
    }

    @Test
    public void testRequestBuilder_fromExistingBuilderClearMedicalResourceTypes() {
        DeleteMedicalResourcesRequest.Builder original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES);
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        original.addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);

        copy.clearMedicalResourceTypes();

        assertThat(original.build().getMedicalResourceTypes())
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        assertThat(copy.build().getMedicalResourceTypes()).isEmpty();
    }

    @Test
    public void testRequestBuilder_fromExistingInstance() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder(original).build();

        assertThat(request).isEqualTo(original);
    }

    @Test
    public void testRequestBuilder_fromExistingInstance_changeIndependently() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        copy.addDataSourceId(DIFFERENT_DATA_SOURCE_ID);

        assertThat(original.getDataSourceIds()).containsExactly(DATA_SOURCE_ID);
        assertThat(copy.build().getDataSourceIds())
                .containsExactly(DATA_SOURCE_ID, DIFFERENT_DATA_SOURCE_ID);
    }

    @Test
    public void testRequestBuilder_fromExistingInstanceResourceType() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder(original).build();

        assertThat(request).isEqualTo(original);
    }

    @Test
    public void testRequestBuilder_fromExistingInstanceClearDataSources() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        copy.clearDataSourceIds();

        assertThat(original.getDataSourceIds()).containsExactly(DATA_SOURCE_ID);
        assertThat(copy.build().getDataSourceIds()).isEmpty();
    }

    @Test
    public void testRequestBuilder_fromExistingInstanceClearMedicalResourceTypes() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        DeleteMedicalResourcesRequest.Builder copy =
                new DeleteMedicalResourcesRequest.Builder(original);
        copy.clearMedicalResourceTypes();

        assertThat(original.getMedicalResourceTypes())
                .containsExactly(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(copy.build().getMedicalResourceTypes()).isEmpty();
    }

    @Test
    public void testRequest_equalsSameIdOnly() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        DeleteMedicalResourcesRequest same =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        assertThat(request.equals(same)).isTrue();
        assertThat(request.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    public void testRequest_equalsSameResourceOnly() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        DeleteMedicalResourcesRequest same =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        assertThat(request.equals(same)).isTrue();
        assertThat(request.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    public void testRequest_equalsSameResourceAndId() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .build();
        DeleteMedicalResourcesRequest same =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .build();

        assertThat(request.equals(same)).isTrue();
        assertThat(request.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    public void testRequest_equalsDifferentOrderSame() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .build();
        DeleteMedicalResourcesRequest same =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        assertThat(request.equals(same)).isTrue();
        assertThat(request.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    public void testRequest_equalsDifferent() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        DeleteMedicalResourcesRequest different =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        assertThat(request.equals(different)).isFalse();
    }

    @Test
    public void testRequest_equalsDifferByResource() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        DeleteMedicalResourcesRequest different =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        assertThat(request.equals(different)).isFalse();
    }

    @Test
    public void testRequest_equalsDifferById() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        DeleteMedicalResourcesRequest different =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        assertThat(request.equals(different)).isFalse();
    }

    @Test
    public void testToString_idOnly() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        String dataSourceIdsString = "dataSourceIds=[" + DATA_SOURCE_ID + "]";
        String resourceTypesString = "medicalResourceTypes=[]";

        assertThat(request.toString()).contains(dataSourceIdsString);
        assertThat(request.toString()).contains(resourceTypesString);
    }

    @Test
    public void testToString_resourceAndId() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        String dataSourceIdsString = "dataSourceIds=[" + DATA_SOURCE_ID + "]";
        String resourceTypesString = "medicalResourceTypes=[1]";

        assertThat(request.toString()).contains(dataSourceIdsString);
        assertThat(request.toString()).contains(resourceTypesString);
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeleteMedicalResourcesRequest restored =
                DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcelThenRestore_multiple_objectsAreIdentical() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeleteMedicalResourcesRequest restored =
                DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcelThenRestore_justIds_objectsAreIdentical() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeleteMedicalResourcesRequest restored =
                DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcelThenRestore_justResources_objectsAreIdentical() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        DeleteMedicalResourcesRequest restored =
                DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testReadFromEmptyParcel_illegalArgument() {
        Parcel parcel = Parcel.obtain();
        parcel.writeStringList(Collections.emptyList());
        parcel.writeIntArray(new int[0]);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel));
        parcel.recycle();
    }

    @Test
    public void testCreateFromParcel_null_throwsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> DeleteMedicalResourcesRequest.CREATOR.createFromParcel(null));
    }

    @Test
    public void testDescribeContents_noFlags() {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        assertThat(original.describeContents()).isEqualTo(0);
    }

    @Test
    public void testRestoreInvalidMedicalResourceTypesFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        setFieldValueUsingReflection(original, "mMedicalResourceTypes", Set.of(-1));

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreInvalidDataSourceIdsFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {

        DeleteMedicalResourcesRequest original =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        setFieldValueUsingReflection(original, "mDataSourceIds", Set.of("1"));

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> DeleteMedicalResourcesRequest.CREATOR.createFromParcel(parcel));
    }
}
