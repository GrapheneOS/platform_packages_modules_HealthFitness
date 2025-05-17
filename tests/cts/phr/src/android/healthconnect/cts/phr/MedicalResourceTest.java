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
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4B;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getFhirResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getFhirResourceAllergy;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResourceBuilder;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.MedicalResourceId;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalResource;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MedicalResourceTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testMedicalResourceBuilder_requiredFieldsOnly() {
        FhirResource fhirResource = getFhirResource();
        MedicalResource resource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                DATA_SOURCE_ID,
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();

        assertThat(resource.getType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(resource.getDataSourceId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(resource.getFhirVersion()).isEqualTo(FHIR_VERSION_R4);
        assertThat(resource.getFhirResource()).isEqualTo(fhirResource);
        assertThat(resource.getId())
                .isEqualTo(
                        new MedicalResourceId(
                                DATA_SOURCE_ID, fhirResource.getType(), fhirResource.getId()));
    }

    @Test
    public void testMedicalResourceBuilder_setAllFields() {
        FhirVersion differentFhirVersion = FHIR_VERSION_R4B;
        FhirResource differentFhirResource = getFhirResourceAllergy();
        MedicalResource resource =
                getMedicalResourceBuilder()
                        .setType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .setDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .setFhirVersion(differentFhirVersion)
                        .setFhirResource(differentFhirResource)
                        .build();

        assertThat(resource.getType()).isEqualTo(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        assertThat(resource.getDataSourceId()).isEqualTo(DIFFERENT_DATA_SOURCE_ID);
        assertThat(resource.getFhirVersion()).isEqualTo(differentFhirVersion);
        assertThat(resource.getFhirResource()).isEqualTo(differentFhirResource);
        assertThat(resource.getId())
                .isEqualTo(
                        new MedicalResourceId(
                                DIFFERENT_DATA_SOURCE_ID,
                                differentFhirResource.getType(),
                                differentFhirResource.getId()));
    }

    @Test
    public void testMedicalResourceBuilder_fromExistingBuilder() {
        MedicalResource.Builder original = getMedicalResourceBuilder();
        MedicalResource resource = new MedicalResource.Builder(original).build();

        assertThat(resource).isEqualTo(original.build());
    }

    @Test
    public void testMedicalResourceBuilder_fromExistingInstance() {
        MedicalResource original = getMedicalResource();
        MedicalResource resource = new MedicalResource.Builder(original).build();

        assertThat(resource).isEqualTo(original);
    }

    @Test
    public void testMedicalResourceBuilder_constructWithInvalidMedicalResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MedicalResource.Builder(
                                -1, DATA_SOURCE_ID, FHIR_VERSION_R4, getFhirResource()));
    }

    @Test
    public void testMedicalResourceBuilder_setInvalidMedicalResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MedicalResource.Builder(
                                        MEDICAL_RESOURCE_TYPE_VACCINES,
                                        DATA_SOURCE_ID,
                                        FHIR_VERSION_R4,
                                        getFhirResource())
                                .setType(-1));
    }

    @Test
    public void testMedicalResourceBuilder_constructWithInvalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                "1",
                                FHIR_VERSION_R4,
                                getFhirResource()));
    }

    @Test
    public void testMedicalResourceBuilder_setInvalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MedicalResource.Builder(
                                        MEDICAL_RESOURCE_TYPE_VACCINES,
                                        DATA_SOURCE_ID,
                                        FHIR_VERSION_R4,
                                        getFhirResource())
                                .setDataSourceId("1"));
    }

    @Test
    public void testMedicalResource_toString() {
        FhirResource fhirResource = getFhirResource();
        MedicalResource resource =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                DATA_SOURCE_ID,
                                FHIR_VERSION_R4,
                                fhirResource)
                        .build();
        String typeString = "type=1";
        String dataSourceIdString = "dataSourceId=" + DATA_SOURCE_ID;
        String fhirVersionString = "fhirVersion=" + FHIR_VERSION_R4;
        String fhirResourceString = "fhirResource=" + fhirResource;

        assertThat(resource.toString()).contains(typeString);
        assertThat(resource.toString()).contains(dataSourceIdString);
        assertThat(resource.toString()).contains(fhirVersionString);
        assertThat(resource.toString()).contains(fhirResourceString);
    }

    @Test
    public void testMedicalResource_equals() {
        MedicalResource resource1 = getMedicalResource();
        MedicalResource resource2 = getMedicalResource();

        assertThat(resource1.equals(resource2)).isTrue();
        assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
    }

    @Test
    public void testMedicalResource_equals_comparesAllValues() {
        MedicalResource resource = getMedicalResource();
        MedicalResource resourceDifferentType =
                new MedicalResource.Builder(resource)
                        .setType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        MedicalResource resourceDifferentDataSourceId =
                new MedicalResource.Builder(resource)
                        .setDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();
        MedicalResource resourceDifferentVersion =
                new MedicalResource.Builder(resource).setFhirVersion(FHIR_VERSION_R4B).build();
        MedicalResource resourceDifferentFhirResource =
                new MedicalResource.Builder(resource)
                        .setFhirResource(getFhirResourceAllergy())
                        .build();

        assertThat(resourceDifferentType.equals(resource)).isFalse();
        assertThat(resourceDifferentDataSourceId.equals(resource)).isFalse();
        assertThat(resourceDifferentVersion.equals(resource)).isFalse();
        assertThat(resourceDifferentFhirResource.equals(resource)).isFalse();
        assertThat(resourceDifferentType.hashCode()).isNotEqualTo(resource.hashCode());
        assertThat(resourceDifferentDataSourceId.hashCode()).isNotEqualTo(resource.hashCode());
        assertThat(resourceDifferentVersion.hashCode()).isNotEqualTo(resource.hashCode());
        assertThat(resourceDifferentFhirResource.hashCode()).isNotEqualTo(resource.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        MedicalResource original = getMedicalResource();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalResource restored = MedicalResource.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testRestoreInvalidMedicalResourceTypeFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResource original = getMedicalResource();
        setFieldValueUsingReflection(original, "mType", -1);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResource.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreInvalidDataSourceIdFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResource original = getMedicalResource();
        setFieldValueUsingReflection(original, "mDataSourceId", "1");

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResource.CREATOR.createFromParcel(parcel));
    }
}
