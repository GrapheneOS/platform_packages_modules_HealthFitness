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

package android.health.connect;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MatchmakingResponseTest {

    @Test
    public void builder_true_isMatchmakingPossibleIsTrue() {
        MatchmakingResponse response = new MatchmakingResponse.Builder(true).build();
        assertThat(response.isMatchmakingPossible()).isTrue();
    }

    @Test
    public void builder_false_isMatchmakingPossibleIsFalse() {
        MatchmakingResponse response = new MatchmakingResponse.Builder(false).build();
        assertThat(response.isMatchmakingPossible()).isFalse();
    }

    @Test
    public void builder_setMatchmakingPossible_true() {
        MatchmakingResponse response =
                new MatchmakingResponse.Builder(false).setMatchmakingPossible(true).build();
        assertThat(response.isMatchmakingPossible()).isTrue();
    }

    @Test
    public void builder_setMatchmakingPossible_false() {
        MatchmakingResponse response =
                new MatchmakingResponse.Builder(true).setMatchmakingPossible(false).build();
        assertThat(response.isMatchmakingPossible()).isFalse();
    }

    @Test
    public void parcelable_true() {
        MatchmakingResponse originalResponse = new MatchmakingResponse.Builder(true).build();

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        MatchmakingResponse parceledResponse = MatchmakingResponse.CREATOR.createFromParcel(parcel);
        assertThat(parceledResponse.isMatchmakingPossible()).isTrue();
        assertThat(parceledResponse).isEqualTo(originalResponse);
        assertThat(parceledResponse.hashCode()).isEqualTo(originalResponse.hashCode());

        parcel.recycle();
    }

    @Test
    public void parcelable_false() {
        MatchmakingResponse originalResponse = new MatchmakingResponse.Builder(false).build();

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        MatchmakingResponse parceledResponse = MatchmakingResponse.CREATOR.createFromParcel(parcel);
        assertThat(parceledResponse.isMatchmakingPossible()).isFalse();
        assertThat(parceledResponse).isEqualTo(originalResponse);
        assertThat(parceledResponse.hashCode()).isEqualTo(originalResponse.hashCode());

        parcel.recycle();
    }

    @Test
    public void testEqualsAndHashCode() {
        MatchmakingResponse response1 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response2 = new MatchmakingResponse.Builder(true).build();
        MatchmakingResponse response3 = new MatchmakingResponse.Builder(false).build();

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isNotEqualTo(response3.hashCode());
    }

    @Test
    public void describeContents_returnsZero() {
        MatchmakingResponse response = new MatchmakingResponse.Builder(true).build();
        assertThat(response.describeContents()).isEqualTo(0);
    }

    @Test
    public void creator_newArray_returnsCorrectSizeArray() {
        MatchmakingResponse[] responses = MatchmakingResponse.CREATOR.newArray(5);
        assertThat(responses.length).isEqualTo(5);
        assertThat(responses[0]).isNull();
    }
}
