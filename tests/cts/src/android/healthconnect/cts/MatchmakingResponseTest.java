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

import android.health.connect.MatchmakingResponse;
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

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_MATCHMAKING})
public class MatchmakingResponseTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void testBuilder_setMatchmakingPossible_true() {
        MatchmakingResponse response =
                new MatchmakingResponse.Builder(/* isMatchmakingPossible= */ true).build();
        assertThat(response.isMatchmakingPossible()).isTrue();
    }

    @Test
    public void testBuilder_setMatchmakingPossible_false() {
        MatchmakingResponse response =
                new MatchmakingResponse.Builder(
                                /* isMatchmakingPossible= */
                                /* isMatchmakingPossible= */ false)
                        .build();
        assertThat(response.isMatchmakingPossible()).isFalse();
    }

    @Test
    public void testParcelable_true() {
        MatchmakingResponse originalResponse =
                new MatchmakingResponse.Builder(/* isMatchmakingPossible= */ true).build();
        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingResponse newResponse = MatchmakingResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newResponse).isEqualTo(originalResponse);
    }

    @Test
    public void testParcelable_false() {
        MatchmakingResponse originalResponse =
                new MatchmakingResponse.Builder(/* isMatchmakingPossible= */ false).build();
        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingResponse newResponse = MatchmakingResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newResponse).isEqualTo(originalResponse);
    }

    @Test
    public void testEquals() {
        MatchmakingResponse response1 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response2 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response3 = new MatchmakingResponse.Builder(false).build();

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
    }

    @Test
    public void testHashCode() {
        MatchmakingResponse response1 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response2 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response3 = new MatchmakingResponse.Builder(false).build();

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.hashCode()).isNotEqualTo(response3.hashCode());
    }
}
