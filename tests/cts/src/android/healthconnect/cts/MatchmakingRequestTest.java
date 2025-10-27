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

import static com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.MatchmakingRequest;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.os.Parcel;
import android.platform.test.annotations.AppModeFull;
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
}
