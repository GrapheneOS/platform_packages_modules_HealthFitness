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

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.AggregateRecordsResponse;
import android.health.connect.AggregateResult;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class AggregateDataResponseParcelTest {

    @Test
    public void toMasked_packageNamesAreMasked() {
        String internalPackageName = "com.example.package";
        String maskedPackageName = "com.example.package.masked";
        int aggregationTypeId = AggregationTypeIdMapper.getInstance().getIdFor(STEPS_COUNT_TOTAL);

        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName(internalPackageName).build();
        AggregateResult<Long> aggregateResult =
                new AggregateResult<>(100L, null, Set.of(dataOrigin));
        AggregateRecordsResponse<Long> response =
                new AggregateRecordsResponse<>(Map.of(aggregationTypeId, aggregateResult));
        AggregateDataResponseParcel parcel = new AggregateDataResponseParcel(List.of(response));

        Function<String, String> masker = (name) -> name + ".masked";
        AggregateDataResponseParcel maskedParcel = parcel.toMasked(masker);

        AggregateRecordsResponse<Long> maskedResponse =
                (AggregateRecordsResponse<Long>) maskedParcel.getAggregateDataResponse();

        assertThat(maskedResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(maskedResponse.getDataOrigins(STEPS_COUNT_TOTAL)).hasSize(1);
        assertThat(
                        maskedResponse
                                .getDataOrigins(STEPS_COUNT_TOTAL)
                                .iterator()
                                .next()
                                .getPackageName())
                .isEqualTo(maskedPackageName);
    }
}
