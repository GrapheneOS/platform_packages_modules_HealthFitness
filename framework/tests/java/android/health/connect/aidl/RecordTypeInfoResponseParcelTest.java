/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.StepsRecord;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class RecordTypeInfoResponseParcelTest {

    @Test
    public void toMasked_masksPackageNames() {
        DataOrigin origin = new DataOrigin.Builder().setPackageName("com.example.app").build();
        Map<Integer, List<DataOrigin>> recordTypeInfoResponses =
                Map.of(RECORD_TYPE_STEPS, List.of(origin));
        RecordTypeInfoResponseParcel parcel =
                new RecordTypeInfoResponseParcel(recordTypeInfoResponses);

        Function<String, String> masker = (packageName) -> packageName + ".masked";
        RecordTypeInfoResponseParcel maskedParcel = parcel.toMasked(masker);

        Map<Class<? extends android.health.connect.datatypes.Record>, RecordTypeInfoResponse>
                responses = maskedParcel.getRecordTypeInfoResponses();
        assertThat(responses).containsKey(StepsRecord.class);
        List<DataOrigin> maskedOrigins = responses.get(StepsRecord.class).getContributingPackages();
        assertThat(maskedOrigins).hasSize(1);
        assertThat(maskedOrigins.get(0).getPackageName()).isEqualTo("com.example.app.masked");
    }
}
