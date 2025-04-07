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
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.MedicalResourceTypeInfo;
import android.health.connect.datatypes.MedicalDataSource;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class MedicalResourceTypeInfoTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_EmptyContributingDataSources() {
        MedicalResourceTypeInfo info =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, Set.of());

        assertThat(info.getMedicalResourceType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(info.getContributingDataSources()).isEmpty();
    }

    @Test
    public void testConstructor_withContributingDataSources() {
        Set<MedicalDataSource> dataSources = Set.of(getMedicalDataSourceRequiredFieldsOnly());
        MedicalResourceTypeInfo info =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);

        assertThat(info.getMedicalResourceType()).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
        assertThat(info.getContributingDataSources()).isEqualTo(dataSources);
    }

    @Test
    public void testConstructor_invalidMedicalResourceType_throws() {
        assertThrows(
                IllegalArgumentException.class, () -> new MedicalResourceTypeInfo(1000, Set.of()));
    }

    @Test
    public void testEquals() {
        Set<MedicalDataSource> dataSources = Set.of(getMedicalDataSourceRequiredFieldsOnly());
        MedicalResourceTypeInfo info1 =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);
        MedicalResourceTypeInfo info2 =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);

        assertThat(info1.equals(info2)).isTrue();
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }

    @Test
    public void testEquals_comparesAllValues() {
        Set<MedicalDataSource> dataSources = Set.of(getMedicalDataSourceRequiredFieldsOnly());
        MedicalResourceTypeInfo info =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);
        MedicalResourceTypeInfo infoDifferentMedicalResourceType =
                new MedicalResourceTypeInfo(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, dataSources);
        MedicalResourceTypeInfo infoDifferentDataSources =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, Set.of());

        assertThat(infoDifferentMedicalResourceType.equals(info)).isFalse();
        assertThat(infoDifferentDataSources.equals(info)).isFalse();
        assertThat(infoDifferentMedicalResourceType.hashCode()).isNotEqualTo(info.hashCode());
        assertThat(infoDifferentDataSources.hashCode()).isNotEqualTo(info.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        Set<MedicalDataSource> dataSources = Set.of(getMedicalDataSourceRequiredFieldsOnly());
        MedicalResourceTypeInfo original =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalResourceTypeInfo restored = MedicalResourceTypeInfo.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testRestoreInvalidMedicalResourceTypeFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        Set<MedicalDataSource> dataSources = Set.of(getMedicalDataSourceRequiredFieldsOnly());
        MedicalResourceTypeInfo original =
                new MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, dataSources);
        setFieldValueUsingReflection(original, "mMedicalResourceType", -1);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceTypeInfo.CREATOR.createFromParcel(parcel));
    }
}
