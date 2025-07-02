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

import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class GetMatchingAppsResponseTest {

    private static final String TEST_PACKAGE_NAME = "com.test.package";

    @Test
    public void getMatchingApps_returnsCorrectly() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(matchingApps);
        assertThat(response.getMatchingApps()).isEqualTo(matchingApps);
    }

    @Test
    public void getMatchingApps_emptyMap_returnsEmptyMap() {
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(Map.of());
        assertThat(response.getMatchingApps()).isEmpty();
    }

    @Test
    public void hasMatchingApps_returnsCorrectly() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(matchingApps);
        assertThat(response.hasMatchingApps()).isTrue();
    }

    @Test
    public void hasMatchingApps_emptyMap_returnsFalse() {
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(Map.of());
        assertThat(response.hasMatchingApps()).isFalse();
    }

    @Test
    public void parcelable_writeToParcel_readFromParcel_objectsAreEqual() {
        Map<String, Set<String>> matchingApps =
                Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_SLEEP));
        GetMatchingAppsResponse originalResponse = new GetMatchingAppsResponse(matchingApps);

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        GetMatchingAppsResponse newResponse =
                GetMatchingAppsResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newResponse).isEqualTo(originalResponse);
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(matchingApps);
        assertThat(response.equals(response)).isTrue();
    }

    @Test
    public void equals_equalObjects_returnsTrue() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response1 = new GetMatchingAppsResponse(matchingApps1);
        Map<String, Set<String>> matchingApps2 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response2 = new GetMatchingAppsResponse(matchingApps2);
        assertThat(response1.equals(response2)).isTrue();
    }

    @Test
    public void equals_differentObjects_returnsFalse() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response1 = new GetMatchingAppsResponse(matchingApps1);
        GetMatchingAppsResponse response2 = new GetMatchingAppsResponse(Map.of());
        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response1 = new GetMatchingAppsResponse(matchingApps1);
        Map<String, Set<String>> matchingApps2 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response2 = new GetMatchingAppsResponse(matchingApps2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    public void toString_containsCorrectContent() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingAppsResponse response = new GetMatchingAppsResponse(matchingApps);
        String responseString = response.toString();
        assertThat(responseString)
                .contains("GetMatchingAppsResponse{hasMatchingApps=true,matchingApps=");
        assertThat(responseString).contains(TEST_PACKAGE_NAME);
        assertThat(responseString).contains(WRITE_STEPS);
    }
}
