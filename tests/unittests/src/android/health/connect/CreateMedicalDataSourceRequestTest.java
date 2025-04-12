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

package android.health.connect;

import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_VERSION;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CreateMedicalDataSourceRequestTest {

    // This test tests the hidden getDataSize method. The rest of this class is tested in cts tests.
    @Test
    public void testCreateMedicalDataSourceRequest_getDataSize() {
        CreateMedicalDataSourceRequest original =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        CreateMedicalDataSourceRequest restored =
                CreateMedicalDataSourceRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        assertThat(restored.getDataSize()).isGreaterThan(0L);
        parcel.recycle();
    }
}
