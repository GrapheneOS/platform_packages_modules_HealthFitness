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

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.MedicalResourceId;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_PERSONAL_HEALTH_RECORD,
})
public class MedicalResourceIdTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testMedicalResourceId_constructor() {
        MedicalResourceId medicalResourceId =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);

        assertThat(medicalResourceId.getDataSourceId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(medicalResourceId.getFhirResourceType())
                .isEqualTo(FHIR_RESOURCE_TYPE_IMMUNIZATION);
        assertThat(medicalResourceId.getFhirResourceId()).isEqualTo(FHIR_RESOURCE_ID_IMMUNIZATION);
    }

    @Test
    public void testMedicalResourceId_constructWithInvalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MedicalResourceId(
                                "1",
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION));
    }

    @Test
    public void testMedicalResourceId_constructWithInvalidFhirResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MedicalResourceId(DATA_SOURCE_ID, -1, FHIR_RESOURCE_ID_IMMUNIZATION));
    }

    @Test
    public void testMedicalResourceId_fromFhirReference_validReference() {
        MedicalResourceId medicalResourceId =
                MedicalResourceId.fromFhirReference(DATA_SOURCE_ID, "Immunization/034-AB16.0");

        assertThat(medicalResourceId.getDataSourceId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(medicalResourceId.getFhirResourceType())
                .isEqualTo(FHIR_RESOURCE_TYPE_IMMUNIZATION);
        assertThat(medicalResourceId.getFhirResourceId()).isEqualTo("034-AB16.0");
    }

    @Test
    public void testMedicalResourceId_fromFhirReference_invalidDataSourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.fromFhirReference("1", "Immunization/034-AB16.0"));
    }

    @Test
    public void testMedicalResourceId_fromFhirReference_unknownFhirResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.fromFhirReference(DATA_SOURCE_ID, "TestReport/034-AB16.0"));
    }

    @Test
    public void testMedicalResourceId_fromFhirReference_invalidFhirResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.fromFhirReference(DATA_SOURCE_ID, "Patient0/034-AB16.0"));
    }

    @Test
    public void testMedicalResourceId_fromFhirReference_invalidFhirResourceId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.fromFhirReference(DATA_SOURCE_ID, "Patient/034*AB16#0"));
    }

    @Test
    public void testMedicalResourceId_toString() {
        MedicalResourceId medicalResourceId =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);
        String dataSourceIdString = "dataSourceId=" + DATA_SOURCE_ID;
        String fhirResourceTypeString = "fhirResourceType=1";
        String fhirResourceIdString = "fhirResourceId=" + FHIR_RESOURCE_ID_IMMUNIZATION;

        assertThat(medicalResourceId.toString()).contains(dataSourceIdString);
        assertThat(medicalResourceId.toString()).contains(fhirResourceTypeString);
        assertThat(medicalResourceId.toString()).contains(fhirResourceIdString);
    }

    @Test
    public void testMedicalResourceId_equals() {
        MedicalResourceId medicalResourceId1 = getMedicalResourceId();
        MedicalResourceId medicalResourceId2 = getMedicalResourceId();

        assertThat(medicalResourceId1.equals(medicalResourceId2)).isTrue();
        assertThat(medicalResourceId1.hashCode()).isEqualTo(medicalResourceId2.hashCode());
    }

    @Test
    public void testMedicalResourceId_equals_comparesAllValues() {
        MedicalResourceId medicalResourceId =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);
        MedicalResourceId idWithDifferentDataSourceId =
                new MedicalResourceId(
                        DIFFERENT_DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);
        MedicalResourceId idWithDifferentFhirResourceType =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE,
                        FHIR_RESOURCE_ID_IMMUNIZATION);
        MedicalResourceId idWithDifferentFhirResourceId =
                new MedicalResourceId(
                        DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, FHIR_RESOURCE_ID_ALLERGY);

        assertThat(idWithDifferentDataSourceId.equals(medicalResourceId)).isFalse();
        assertThat(idWithDifferentFhirResourceType.equals(medicalResourceId)).isFalse();
        assertThat(idWithDifferentFhirResourceId.equals(medicalResourceId)).isFalse();
        assertThat(idWithDifferentDataSourceId.hashCode())
                .isNotEqualTo(medicalResourceId.hashCode());
        assertThat(idWithDifferentFhirResourceType.hashCode())
                .isNotEqualTo(medicalResourceId.hashCode());
        assertThat(idWithDifferentFhirResourceId.hashCode())
                .isNotEqualTo(medicalResourceId.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        MedicalResourceId original = getMedicalResourceId();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalResourceId restored = MedicalResourceId.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testRestoreInvalidMedicalResourceTypeFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResourceId original = getMedicalResourceId();
        setFieldValueUsingReflection(original, "mFhirResourceType", -1);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testRestoreInvalidDataSourceIdFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResourceId original = getMedicalResourceId();
        setFieldValueUsingReflection(original, "mDataSourceId", "1");

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceId.CREATOR.createFromParcel(parcel));
    }
}
