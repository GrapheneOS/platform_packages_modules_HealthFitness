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

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.datatypes.DataOrigin;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class GetPriorityResponseParcelTest {

    @Test
    public void toMasked_masksPackageNames() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("com.example.app1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("com.example.app2").build();
        FetchDataOriginsPriorityOrderResponse response =
                new FetchDataOriginsPriorityOrderResponse(List.of(origin1, origin2));
        GetPriorityResponseParcel parcel = new GetPriorityResponseParcel(response);

        Function<String, String> masker = (packageName) -> packageName + ".masked";
        GetPriorityResponseParcel maskedParcel = parcel.toMasked(masker);

        List<DataOrigin> maskedOrigins =
                maskedParcel.getPriorityResponse().getDataOriginsPriorityOrder();
        assertThat(maskedOrigins).hasSize(2);
        assertThat(maskedOrigins.get(0).getPackageName()).isEqualTo("com.example.app1.masked");
        assertThat(maskedOrigins.get(1).getPackageName()).isEqualTo("com.example.app2.masked");
    }
}
