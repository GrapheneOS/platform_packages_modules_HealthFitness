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

import android.health.connect.HealthDataCategory;
import android.health.connect.UpdateDataOriginPriorityOrderRequest;
import android.health.connect.datatypes.DataOrigin;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class UpdatePriorityRequestParcelTest {

    @Test
    public void toUnmasked_packageNamesAreUnmasked() {
        String maskedPackageName = "com.example.package1.masked";
        String unmaskedPackageName = "com.example.package1";

        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(maskedPackageName).build();
        UpdateDataOriginPriorityOrderRequest request =
                new UpdateDataOriginPriorityOrderRequest(
                        List.of(maskedOrigin), HealthDataCategory.ACTIVITY);
        UpdatePriorityRequestParcel parcel = new UpdatePriorityRequestParcel(request);

        Function<String, String> unmasker = (name) -> unmaskedPackageName;

        UpdatePriorityRequestParcel unmaskedParcel = parcel.toUnmasked(unmasker);

        assertThat(unmaskedParcel.getPackagePriorityOrder()).containsExactly(unmaskedPackageName);
        assertThat(unmaskedParcel.getDataCategory()).isEqualTo(HealthDataCategory.ACTIVITY);
    }
}
