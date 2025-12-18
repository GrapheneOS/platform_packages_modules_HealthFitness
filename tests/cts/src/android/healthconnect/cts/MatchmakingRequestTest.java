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
package android.healthconnect.cts;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.MatchmakingRequest;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.os.Parcel;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_MATCHMAKING})
public class MatchmakingRequestTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void testBuilder_noRecordType_buildsWithEmptyRecordType() {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();
        assertThat(request.getRecordTypes()).isNotNull();
        assertThat(request.getRecordTypes()).isEmpty();
    }

    @Test
    public void testBuilder_emptyRecordType_buildsWithEmptyRecordType() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(Set.of()).build();
        assertThat(request.getRecordTypes()).isEmpty();
    }

    @Test
    public void testBuilder_addRecordType() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        assertThat(request.getRecordTypes()).containsExactly(StepsRecord.class);
    }

    @Test
    public void testBuilder_addRecordTypes_containsOneRecords() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(Set.of(StepsRecord.class)).build();
        assertThat(request.getRecordTypes()).containsExactly(StepsRecord.class);
    }

    @Test
    public void testBuilder_addRecordTypes_containsMultipleRecords() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordTypes(Set.of(StepsRecord.class, HeartRateRecord.class))
                        .build();
        assertThat(request.getRecordTypes())
                .containsExactly(HeartRateRecord.class, StepsRecord.class);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_noIncludedDataSources_buildsWithEmptyIncludedDataSources() {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();

        assertThat(request.getIncludedDataSources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_emptyIncludedDataSources_resetsIncludedDataSources() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName("package.name")
                                                .build()))
                        .setIncludedDataSources(Set.of())
                        .build();

        assertThat(request.getIncludedDataSources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_addIncludedDataSources_buildsWithIncludedDataSources() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("package.name").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("package.name2").build();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setIncludedDataSources(Set.of(origin1, origin2))
                        .build();

        assertThat(request.getIncludedDataSources()).containsExactly(origin1, origin2);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_noExcludedDataSources_buildsWithEmptyExcludedDataSources() {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();

        assertThat(request.getExcludedDataSources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_emptyExcludedDataSources_resetsExcludedDataSources() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setExcludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName("package.name")
                                                .build()))
                        .setExcludedDataSources(Set.of())
                        .build();

        assertThat(request.getExcludedDataSources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_addExcludedDataSources_buildsWithExcludedDataSources() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("package.name").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("package.name2").build();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setExcludedDataSources(Set.of(origin1, origin2))
                        .build();

        assertThat(request.getExcludedDataSources()).containsExactly(origin1, origin2);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_addIncludedDataSources_whenExcludedAlreadyExists_throws() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("package.name").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("package.name2").build();
        MatchmakingRequest.Builder requestBuilder =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin1));

        assertThrows(
                IllegalStateException.class,
                () -> requestBuilder.setIncludedDataSources(Set.of(origin2)));
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testBuilder_addExcludedDataSources_whenIncludedAlreadyExists_throws() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("package.name").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("package.name2").build();
        MatchmakingRequest.Builder requestBuilder =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin1));

        assertThrows(
                IllegalStateException.class,
                () -> requestBuilder.setExcludedDataSources(Set.of(origin2)));
    }

    @Test
    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testParcelable() {
        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingRequest newRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testParcelable_withDataSources() {
        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setExcludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName("package.name")
                                                .build()))
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingRequest newRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testEquals() {
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isEqualTo(request3);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testEquals_withDataSources() {
        String packageName1 = "package.name";
        String packageName2 = "package.name.2";
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName1)
                                                .build()))
                        .build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName1)
                                                .build()))
                        .build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName2)
                                                .build()))
                        .build();
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testHashCode() {
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder().addRecordType(StepsRecord.class).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isEqualTo(request3.hashCode());
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
        FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testHashCode_withDataSources() {
        String packageName1 = "package.name";
        String packageName2 = "package.name.2";
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName1)
                                                .build()))
                        .build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName1)
                                                .build()))
                        .build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(
                                Set.of(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName2)
                                                .build()))
                        .build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }
}
