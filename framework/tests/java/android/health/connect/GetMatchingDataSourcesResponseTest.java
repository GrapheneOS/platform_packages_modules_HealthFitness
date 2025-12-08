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

import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class GetMatchingDataSourcesResponseTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private static final String DEVICE_PACKAGE_NAME = "device.package.name";

    @Test
    public void getMatchingApps_returnsCorrectly() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingDataSourcesResponse response = new GetMatchingDataSourcesResponse(matchingApps);
        assertThat(response.getMatchingApps()).isEqualTo(matchingApps);
    }

    @Test
    public void getMatchingApps_emptyMap_returnsEmptyMap() {
        GetMatchingDataSourcesResponse response = new GetMatchingDataSourcesResponse(Map.of());
        assertThat(response.getMatchingApps()).isEmpty();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void getMatchingDevices_whenFlagOn_returnsCorrectly() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        assertThat(response.getMatchingDevices()).isEqualTo(matchingDevices);
    }

    @Test
    @DisableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API})
    public void getMatchingDevices_whenFlagOff_returnsEmptyMap() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        assertThat(response.getMatchingDevices()).isEmpty();
    }

    @Test
    public void getMatchingDevices_whenEmpty_returnsEmptyMap() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices = Map.of();
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        assertThat(response.getMatchingDevices()).isEmpty();
    }

    @Test
    public void hasMatchingApps_returnsCorrectly() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingDataSourcesResponse response = new GetMatchingDataSourcesResponse(matchingApps);
        assertThat(response.hasMatchingApps()).isTrue();
    }

    @Test
    public void hasMatchingApps_emptyMap_returnsFalse() {
        GetMatchingDataSourcesResponse response = new GetMatchingDataSourcesResponse(Map.of());
        assertThat(response.hasMatchingApps()).isFalse();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void hasMatchingDevices_returnsCorrectly() {
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(Map.of(), matchingDevices);
        assertThat(response.hasMatchingDevices()).isTrue();
    }

    @Test
    public void hasMatchingDevices_emptyMap_returnsFalse() {
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(Map.of(), Map.of());
        assertThat(response.hasMatchingDevices()).isFalse();
    }

    @Test
    public void hasMatchingDataSources_whenSomeMatchingApps_andNoMatchingDevices_returnsTrue() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(matchingApps, Map.of());
        assertThat(response.hasMatchingDataSources()).isTrue();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void hasMatchingDataSources_whenNoMatchingApps_andSomeMatchingDevices_returnsTrue() {
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(Map.of(), matchingDevices);
        assertThat(response.hasMatchingDataSources()).isTrue();
    }

    @Test
    public void hasMatchingDataSources_whenSomeMatchingApps_andSomeMatchingDevices_returnsTrue() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        assertThat(response.hasMatchingDataSources()).isTrue();
    }

    @Test
    public void hasMatchingDataSources_whenNoMatchingApps_andNoMatchingDevices_returnsFalse() {
        GetMatchingDataSourcesResponse response =
                new GetMatchingDataSourcesResponse(Map.of(), Map.of());
        assertThat(response.hasMatchingDataSources()).isFalse();
    }

    @Test
    public void parcelable_writeToParcel_readFromParcel_objectsAreEqual() {
        Map<String, Set<String>> matchingApps =
                Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_SLEEP));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));

        GetMatchingDataSourcesResponse originalResponse;

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            originalResponse = new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        } else {
            originalResponse = new GetMatchingDataSourcesResponse(matchingApps);
        }

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        GetMatchingDataSourcesResponse newResponse =
                GetMatchingDataSourcesResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newResponse).isEqualTo(originalResponse);
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));
        GetMatchingDataSourcesResponse response;

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response = new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        } else {
            response = new GetMatchingDataSourcesResponse(matchingApps);
        }
        assertThat(response.equals(response)).isTrue();
    }

    @Test
    public void equals_equalObjects_returnsTrue() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices1 =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));

        Map<String, Set<String>> matchingApps2 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices2 =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));

        GetMatchingDataSourcesResponse response1;
        GetMatchingDataSourcesResponse response2;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1, matchingDevices1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2, matchingDevices2);
        } else {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2);
        }
        assertThat(response1.equals(response2)).isTrue();
    }

    @Test
    public void equals_differentObjects_returnsFalse() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingApps2 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_DISTANCE));
        Map<String, Set<String>> matchingDevices1 =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));
        Map<String, Set<String>> matchingDevices2 =
                Map.of("another.package.name", Set.of(WRITE_DISTANCE, WRITE_STEPS));

        GetMatchingDataSourcesResponse response1;
        GetMatchingDataSourcesResponse response2;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1, matchingDevices1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2, matchingDevices2);
        } else {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2);
        }
        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        Map<String, Set<String>> matchingApps1 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices1 =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));

        Map<String, Set<String>> matchingApps2 = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices2 =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE, WRITE_STEPS));

        GetMatchingDataSourcesResponse response1;
        GetMatchingDataSourcesResponse response2;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1, matchingDevices1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2, matchingDevices2);
        } else {
            response1 = new GetMatchingDataSourcesResponse(matchingApps1);
            response2 = new GetMatchingDataSourcesResponse(matchingApps2);
        }
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    public void toString_containsCorrectContent() {
        Map<String, Set<String>> matchingApps = Map.of(TEST_PACKAGE_NAME, Set.of(WRITE_STEPS));
        Map<String, Set<String>> matchingDevices =
                Map.of(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response = new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        } else {
            response = new GetMatchingDataSourcesResponse(matchingApps);
        }
        String responseString = response.toString();
        assertThat(responseString)
                .contains("GetMatchingDataSourcesResponse{hasMatchingApps=true,matchingApps=");
        assertThat(responseString).contains(TEST_PACKAGE_NAME);
        assertThat(responseString).contains(WRITE_STEPS);
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            assertThat(responseString).contains("hasMatchingDevices=true,matchingDevices=");
            assertThat(responseString).contains(DEVICE_PACKAGE_NAME);
            assertThat(responseString).contains(WRITE_DISTANCE);
        }
    }

    @Test
    public void toMasked_packageNamesAreMasked() {
        // Verifies that the package names in the map are correctly masked.
        Map<String, Set<String>> matchingApps =
                Map.of(
                        "com.test.package1", Set.of(WRITE_STEPS),
                        "com.test.package2", Set.of(WRITE_SLEEP));
        Map<String, Set<String>> matchingDevices =
                Map.of(
                        "com.device.package1", Set.of(WRITE_STEPS),
                        "com.device.package2", Set.of(WRITE_DISTANCE));
        GetMatchingDataSourcesResponse response;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response = new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        } else {
            response = new GetMatchingDataSourcesResponse(matchingApps);
        }

        // Define a simple masker that appends a suffix.
        Function<String, String> masker = (packageName) -> packageName + ".masked";
        GetMatchingDataSourcesResponse maskedResponse = response.toMasked(masker);

        // Create the expected map after masking.
        Map<String, Set<String>> expectedAppsMap =
                Map.of(
                        "com.test.package1.masked", Set.of(WRITE_STEPS),
                        "com.test.package2.masked", Set.of(WRITE_SLEEP));
        Map<String, Set<String>> expectedDevicesMap =
                Map.of(
                        "com.device.package1.masked", Set.of(WRITE_STEPS),
                        "com.device.package2.masked", Set.of(WRITE_DISTANCE));

        // Assert that the new response contains the masked package names and original permissions.
        assertThat(maskedResponse.getMatchingApps()).isEqualTo(expectedAppsMap);
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            assertThat(maskedResponse.getMatchingDevices()).isEqualTo(expectedDevicesMap);
        }
    }

    @Test
    public void toMasked_emptyMap_returnsEmptyResponse() {
        // Verifies that masking an empty response results in another empty response.
        GetMatchingDataSourcesResponse response;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response = new GetMatchingDataSourcesResponse(Map.of(), Map.of());
        } else {
            response = new GetMatchingDataSourcesResponse(Map.of());
        }

        // Define a masker (it won't be called).
        Function<String, String> masker = (packageName) -> packageName + ".masked";
        GetMatchingDataSourcesResponse maskedResponse = response.toMasked(masker);

        // Assert that the masked response is also empty.
        assertThat(maskedResponse.getMatchingApps()).isEmpty();
        assertThat(maskedResponse.hasMatchingApps()).isFalse();
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            assertThat(maskedResponse.getMatchingDevices()).isEmpty();
            assertThat(maskedResponse.hasMatchingDevices()).isFalse();
        }
    }

    @Test
    public void toMasked_maskerReturnsSameKey_overwritesPreviousEntry() {
        // Verifies behavior when the masker function produces key collisions.
        Map<String, Set<String>> matchingApps =
                Map.of(
                        "com.test.package1", Set.of(WRITE_STEPS),
                        "com.test.package2", Set.of(WRITE_SLEEP));
        Map<String, Set<String>> matchingDevices =
                Map.of(
                        "com.device.package1", Set.of(WRITE_DISTANCE),
                        "com.device.package2", Set.of(WRITE_SLEEP));
        GetMatchingDataSourcesResponse response;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            response = new GetMatchingDataSourcesResponse(matchingApps, matchingDevices);
        } else {
            response = new GetMatchingDataSourcesResponse(matchingApps);
        }

        // Define a masker that always returns the same string.
        Function<String, String> masker = (packageName) -> "com.masked.package";
        GetMatchingDataSourcesResponse maskedResponse = response.toMasked(masker);

        // Because Map iteration order is not guaranteed, the final value could be either set.
        // However, the map size must be 1.
        assertThat(maskedResponse.getMatchingApps()).hasSize(1);
        assertThat(maskedResponse.getMatchingApps().keySet()).containsExactly("com.masked.package");
        // Check that the value is one of the original permission sets.
        Set<String> permissions = maskedResponse.getMatchingApps().get("com.masked.package");
        assertThat(permissions).isAnyOf(Set.of(WRITE_STEPS), Set.of(WRITE_SLEEP));

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            assertThat(maskedResponse.getMatchingDevices()).hasSize(1);
            assertThat(maskedResponse.getMatchingDevices().keySet())
                    .containsExactly("com.masked.package");
            Set<String> devicePermissions =
                    maskedResponse.getMatchingDevices().get("com.masked.package");
            assertThat(devicePermissions).isAnyOf(Set.of(WRITE_DISTANCE), Set.of(WRITE_SLEEP));
        }
    }
}
