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

import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_LAST_DATA_UPDATE_TIME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_LAST_DATA_UPDATE_TIME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalDataSourceBuilderWithOptionalFields;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalDataSourceWithOptionalFields;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.MedicalDataSource;
import android.net.Uri;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MedicalDataSourceTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testMedicalDataSourceBuilder_constructor() {
        MedicalDataSource dataSource =
                new MedicalDataSource.Builder(
                                DATA_SOURCE_ID,
                                DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();

        assertThat(dataSource.getId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(dataSource.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(dataSource.getFhirBaseUri()).isEqualTo(DATA_SOURCE_FHIR_BASE_URI);
        assertThat(dataSource.getDisplayName()).isEqualTo(DATA_SOURCE_DISPLAY_NAME);
        assertThat(dataSource.getFhirVersion()).isEqualTo(DATA_SOURCE_FHIR_VERSION);
        assertThat(dataSource.getLastDataUpdateTime()).isNull();
    }

    @Test
    public void testMedicalDataSourceBuilder_setAllFields() {
        MedicalDataSource dataSource =
                new MedicalDataSource.Builder("", "", Uri.EMPTY, "", DATA_SOURCE_FHIR_VERSION)
                        .setId(DATA_SOURCE_ID)
                        .setPackageName(DATA_SOURCE_PACKAGE_NAME)
                        .setFhirBaseUri(DATA_SOURCE_FHIR_BASE_URI)
                        .setDisplayName(DATA_SOURCE_DISPLAY_NAME)
                        .setFhirVersion(DIFFERENT_DATA_SOURCE_FHIR_VERSION)
                        .setLastDataUpdateTime(DATA_SOURCE_LAST_DATA_UPDATE_TIME)
                        .build();

        assertThat(dataSource.getId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(dataSource.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(dataSource.getFhirBaseUri()).isEqualTo(DATA_SOURCE_FHIR_BASE_URI);
        assertThat(dataSource.getDisplayName()).isEqualTo(DATA_SOURCE_DISPLAY_NAME);
        assertThat(dataSource.getFhirVersion()).isEqualTo(DIFFERENT_DATA_SOURCE_FHIR_VERSION);
        assertThat(dataSource.getLastDataUpdateTime()).isEqualTo(DATA_SOURCE_LAST_DATA_UPDATE_TIME);
    }

    @Test
    public void testMedicalDataSourceBuilder_fromExistingBuilder() {
        MedicalDataSource.Builder original = getMedicalDataSourceBuilderWithOptionalFields();

        MedicalDataSource dataSource = new MedicalDataSource.Builder(original).build();

        assertThat(dataSource).isEqualTo(original.build());
    }

    @Test
    public void testMedicalDataSourceBuilder_fromExistingInstance() {
        MedicalDataSource original = getMedicalDataSourceWithOptionalFields();

        MedicalDataSource dataSource = new MedicalDataSource.Builder(original).build();

        assertThat(dataSource).isEqualTo(original);
    }

    @Test
    public void testMedicalDataSource_toString() {
        MedicalDataSource dataSource =
                new MedicalDataSource.Builder(
                                DATA_SOURCE_ID,
                                DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .setLastDataUpdateTime(DATA_SOURCE_LAST_DATA_UPDATE_TIME)
                        .build();
        String idString = "id=" + DATA_SOURCE_ID;
        String packageNameString = "packageName=" + DATA_SOURCE_PACKAGE_NAME;
        String fhirBaseUriString = "fhirBaseUri=" + DATA_SOURCE_FHIR_BASE_URI;
        String displayNameString = "displayName=" + DATA_SOURCE_DISPLAY_NAME;
        String fhirVersionString = "fhirVersion=" + DATA_SOURCE_FHIR_VERSION;
        String lastDataUpdateTimeString = "lastDataUpdateTime=" + DATA_SOURCE_LAST_DATA_UPDATE_TIME;

        assertThat(dataSource.toString()).contains(idString);
        assertThat(dataSource.toString()).contains(packageNameString);
        assertThat(dataSource.toString()).contains(fhirBaseUriString);
        assertThat(dataSource.toString()).contains(displayNameString);
        assertThat(dataSource.toString()).contains(fhirVersionString);
        assertThat(dataSource.toString()).contains(lastDataUpdateTimeString);
    }

    @Test
    public void testMedicalDataSource_toStringWithOptionalParamsNull() {
        MedicalDataSource dataSource =
                new MedicalDataSource.Builder(
                                DATA_SOURCE_ID,
                                DATA_SOURCE_PACKAGE_NAME,
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .setLastDataUpdateTime(null)
                        .build();
        String expectedPropertiesString =
                String.format(
                        "id=%s,packageName=%s,fhirBaseUri=%s,displayName=%s,fhirVersion=%s,"
                                + "lastDataUpdateTime=%s",
                        DATA_SOURCE_ID,
                        DATA_SOURCE_PACKAGE_NAME,
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        null);

        assertThat(dataSource.toString())
                .isEqualTo(String.format("MedicalDataSource{%s}", expectedPropertiesString));
    }

    @Test
    public void testMedicalDataSource_equals() {
        MedicalDataSource dataSource1 = getMedicalDataSourceRequiredFieldsOnly();
        MedicalDataSource dataSource2 = getMedicalDataSourceRequiredFieldsOnly();

        assertThat(dataSource1.equals(dataSource2)).isTrue();
        assertThat(dataSource1.hashCode()).isEqualTo(dataSource2.hashCode());
    }

    @Test
    public void testMedicalDataSource_withOptionalParams_equals() {
        MedicalDataSource dataSource1 = getMedicalDataSourceWithOptionalFields();
        MedicalDataSource dataSource2 = getMedicalDataSourceWithOptionalFields();

        assertThat(dataSource1.equals(dataSource2)).isTrue();
        assertThat(dataSource1.hashCode()).isEqualTo(dataSource2.hashCode());
    }

    @Test
    public void testMedicalDataSource_equals_comparesAllValues() {
        MedicalDataSource dataSource = getMedicalDataSourceWithOptionalFields();
        MedicalDataSource dataSourceDifferentBaseUri =
                new MedicalDataSource.Builder(dataSource)
                        .setFhirBaseUri(DIFFERENT_DATA_SOURCE_BASE_URI)
                        .build();
        MedicalDataSource dataSourceDifferentDisplayName =
                new MedicalDataSource.Builder(dataSource)
                        .setDisplayName(DIFFERENT_DATA_SOURCE_DISPLAY_NAME)
                        .build();
        MedicalDataSource dataSourceDifferentFhirVersion =
                new MedicalDataSource.Builder(dataSource)
                        .setFhirVersion(DIFFERENT_DATA_SOURCE_FHIR_VERSION)
                        .build();
        MedicalDataSource dataSourceDifferentId =
                new MedicalDataSource.Builder(dataSource).setId(DIFFERENT_DATA_SOURCE_ID).build();
        MedicalDataSource dataSourceDifferentPackageName =
                new MedicalDataSource.Builder(dataSource)
                        .setPackageName(DIFFERENT_DATA_SOURCE_PACKAGE_NAME)
                        .build();
        MedicalDataSource dataSourceDifferentLastDataUpdateTime =
                new MedicalDataSource.Builder(dataSource)
                        .setLastDataUpdateTime(DIFFERENT_DATA_SOURCE_LAST_DATA_UPDATE_TIME)
                        .build();

        assertThat(dataSourceDifferentBaseUri.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentDisplayName.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentFhirVersion.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentId.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentPackageName.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentLastDataUpdateTime.equals(dataSource)).isFalse();
        assertThat(dataSourceDifferentBaseUri.hashCode()).isNotEqualTo(dataSource.hashCode());
        assertThat(dataSourceDifferentDisplayName.hashCode()).isNotEqualTo(dataSource.hashCode());
        assertThat(dataSourceDifferentFhirVersion.hashCode()).isNotEqualTo(dataSource.hashCode());
        assertThat(dataSourceDifferentId.hashCode()).isNotEqualTo(dataSource.hashCode());
        assertThat(dataSourceDifferentPackageName.hashCode()).isNotEqualTo(dataSource.hashCode());
        assertThat(dataSourceDifferentLastDataUpdateTime.hashCode())
                .isNotEqualTo(dataSource.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        MedicalDataSource original = getMedicalDataSourceRequiredFieldsOnly();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalDataSource restored = MedicalDataSource.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcelThenRestore_withOptionalValues_objectsAreIdentical() {
        MedicalDataSource original = getMedicalDataSourceWithOptionalFields();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalDataSource restored = MedicalDataSource.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }
}
