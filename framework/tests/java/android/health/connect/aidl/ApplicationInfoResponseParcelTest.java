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

import android.health.connect.datatypes.AppInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class ApplicationInfoResponseParcelTest {

    @Test
    public void toMasked_masksPackageNames() {
        AppInfo appInfo1 = new AppInfo.Builder("com.example.app1", "App 1", null).build();
        AppInfo appInfo2 = new AppInfo.Builder("com.example.app2", "App 2", null).build();
        ApplicationInfoResponseParcel parcel =
                new ApplicationInfoResponseParcel(List.of(appInfo1, appInfo2));

        Function<String, String> masker = (packageName) -> packageName + ".masked";
        ApplicationInfoResponseParcel maskedParcel = parcel.toMasked(masker);

        assertThat(maskedParcel.getAppInfoList()).hasSize(2);

        // Masks package names
        assertThat(maskedParcel.getAppInfoList().get(0).getPackageName())
                .isEqualTo("com.example.app1.masked");
        assertThat(maskedParcel.getAppInfoList().get(1).getPackageName())
                .isEqualTo("com.example.app2.masked");

        // Does not change names
        assertThat(maskedParcel.getAppInfoList().get(0).getName()).isEqualTo("App 1");
        assertThat(maskedParcel.getAppInfoList().get(1).getName()).isEqualTo("App 2");

        // Does not change icons
        assertThat(maskedParcel.getAppInfoList().get(0).getIcon()).isNull();
        assertThat(maskedParcel.getAppInfoList().get(1).getIcon()).isNull();
    }
}
