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
package android.health.connect.aidl;

import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createVaccineMedicalResource;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.MedicalResource;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(Flags.FLAG_PERSONAL_HEALTH_RECORD)
public class MedicalResourceListParcelTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testMedicalResourceListParcel_successfulCreateAndGet() {
        List<MedicalResource> resources =
                List.of(
                        createAllergyMedicalResource(DATA_SOURCE_ID),
                        createVaccineMedicalResource(DATA_SOURCE_ID));

        MedicalResourceListParcel medicalResourceListParcel =
                new MedicalResourceListParcel(resources);

        assertThat(medicalResourceListParcel.getMedicalResources())
                .containsExactlyElementsIn(resources);
    }

    @Test
    public void testMedicalResourceListParcel_nullRequests_throws() {
        assertThrows(NullPointerException.class, () -> new MedicalResourceListParcel(null));
    }

    @Test
    public void testMedicalResourceListParcelWriteToParcelThenRestore_propertiesAreIdentical() {
        List<MedicalResource> resources =
                List.of(
                        createAllergyMedicalResource(DATA_SOURCE_ID),
                        createVaccineMedicalResource(DATA_SOURCE_ID));
        MedicalResourceListParcel original = new MedicalResourceListParcel(resources);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MedicalResourceListParcel restoredParcel =
                MedicalResourceListParcel.CREATOR.createFromParcel(parcel);

        assertThat(restoredParcel.getMedicalResources()).isEqualTo(resources);
        parcel.recycle();
    }
}
