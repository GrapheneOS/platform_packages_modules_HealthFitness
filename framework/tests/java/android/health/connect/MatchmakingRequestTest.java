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

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class MatchmakingRequestTest {
    private static final String TEST_PACKAGE_NAME = "com.test.package";
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void builder_addRecordType_success() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();

        assertThat(request.getRecordTypes()).containsExactly(ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void builder_addMultipleRecordTypes_success() {
        Set<Class<? extends Record>> recordTypes =
                Set.of(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);

        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.getRecordTypes())
                .containsExactly(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);
    }

    @Test
    public void testBuilder_setCallingPackageName() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().setCallingPackageName(TEST_PACKAGE_NAME).build();
        assertThat(request.getCallingPackageName()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    public void builder_addRecordType_nullThrowsException() {
        MatchmakingRequest.Builder builder = new MatchmakingRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordType(null));
    }

    @Test
    public void builder_addRecordTypes_nullThrowsException() {
        MatchmakingRequest.Builder builder = new MatchmakingRequest.Builder();
        assertThrows(NullPointerException.class, () -> builder.addRecordTypes(null));
    }

    @Test
    public void getRecordTypes_correctSize() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();

        Set<Class<? extends Record>> recordTypes = request.getRecordTypes();
        assertThat(recordTypes.size()).isEqualTo(1);
        assertThat(recordTypes).contains(ActiveCaloriesBurnedRecord.class);
    }

    @Test
    public void builder_setIncludeDataOrigins_success() {
        Set<DataOrigin> dataOrigins = new HashSet<>();
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package1").build());
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package2").build());
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package3").build());

        MatchmakingRequest request =
                new MatchmakingRequest.Builder().setIncludedDataSources(dataOrigins).build();

        assertThat(request.getIncludedDataSources()).containsExactlyElementsIn(dataOrigins);
        assertThat(request.getExcludedDataSources()).isEmpty();
    }

    @Test
    public void builder_setExcludeDataOrigins_success() {
        Set<DataOrigin> dataOrigins = new HashSet<>();
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package1").build());
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package2").build());
        dataOrigins.add(new DataOrigin.Builder().setPackageName("package3").build());

        MatchmakingRequest request =
                new MatchmakingRequest.Builder().setExcludedDataSources(dataOrigins).build();

        assertThat(request.getExcludedDataSources()).containsExactlyElementsIn(dataOrigins);
        assertThat(request.getIncludedDataSources()).isEmpty();
    }

    @Test
    public void builder_setIncludeDataOrigins_whenExcludesSet_throws() {
        Set<DataOrigin> includeDataOrigins = new HashSet<>();
        includeDataOrigins.add(new DataOrigin.Builder().setPackageName("package1").build());
        includeDataOrigins.add(new DataOrigin.Builder().setPackageName("package2").build());
        includeDataOrigins.add(new DataOrigin.Builder().setPackageName("package3").build());
        Set<DataOrigin> excludeDataOrigins =
                Set.of(new DataOrigin.Builder().setPackageName("exclude.package").build());

        MatchmakingRequest.Builder requestBuilder =
                new MatchmakingRequest.Builder().setExcludedDataSources(excludeDataOrigins);

        assertThrows(
                IllegalStateException.class,
                () -> requestBuilder.setIncludedDataSources(includeDataOrigins));
    }

    @Test
    public void builder_setExcludeDataOrigins_whenIncludesSet_throws() {
        Set<DataOrigin> excludeDataOrigins = new HashSet<>();
        excludeDataOrigins.add(new DataOrigin.Builder().setPackageName("package1").build());
        excludeDataOrigins.add(new DataOrigin.Builder().setPackageName("package2").build());
        excludeDataOrigins.add(new DataOrigin.Builder().setPackageName("package3").build());
        Set<DataOrigin> includeDataOrigins =
                Set.of(new DataOrigin.Builder().setPackageName("exclude.package").build());

        MatchmakingRequest.Builder requestBuilder =
                new MatchmakingRequest.Builder().setIncludedDataSources(includeDataOrigins);

        assertThrows(
                IllegalStateException.class,
                () -> requestBuilder.setExcludedDataSources(excludeDataOrigins));
    }

    @Test
    public void parcelable_writeToParcelAndCreateFromParcel_success() {
        Set<Class<? extends Record>> recordTypes =
                Set.of(ActiveCaloriesBurnedRecord.class, BasalMetabolicRateRecord.class);

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();
        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        MatchmakingRequest parceledRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);

        assertThat(parceledRequest.getRecordTypes()).isEqualTo(originalRequest.getRecordTypes());
        parcel.recycle();
    }

    @Test
    public void parcelable_withCallingPackageName() {
        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingRequest newRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void parcelable_withIncludeDataOrigins() {
        DataOrigin include = new DataOrigin.Builder().setPackageName("include.pkg").build();

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setIncludedDataSources(Set.of(include))
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingRequest newRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
        assertThat(newRequest.getIncludedDataSources()).containsExactly(include);
        assertThat(newRequest.getExcludedDataSources()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void parcelable_withExcludeDataOrigins() {
        DataOrigin exclude = new DataOrigin.Builder().setPackageName("exclude.pkg").build();

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setExcludedDataSources(Set.of(exclude))
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        MatchmakingRequest newRequest = MatchmakingRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
        assertThat(newRequest.getExcludedDataSources()).containsExactly(exclude);
        assertThat(newRequest.getIncludedDataSources()).isEmpty();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();

        assertThat(request.equals(new Object())).isFalse();
    }

    @Test
    public void equals_sameRecordTypes_returnsTrue() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isTrue();
    }

    @Test
    public void equals_differentRecordTypes_returnsFalse() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.equals(request2)).isFalse();
    }

    @Test
    public void equals_withCallingPackageName() {
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName("com.another.package")
                        .addRecordType(StepsRecord.class)
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void equals_withIncludeDataOrigins() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("pkg1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("pkg2").build();

        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder()
                        .setIncludedDataSources(Set.of(origin2)) // Different included
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void equals_withExcludeDataOrigins() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("pkg1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("pkg2").build();

        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder()
                        .setExcludedDataSources(Set.of(origin2)) // Different excluded
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }

    @Test
    public void hashCode_sameRecordTypes_returnsSameHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void hashCode_differentRecordTypes_returnsDifferentHashCode() {
        Set<Class<? extends Record>> recordTypes1 = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes1).build();

        Set<Class<? extends Record>> recordTypes2 = Set.of(BasalMetabolicRateRecord.class);
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes2).build();

        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    public void hashCode_withCallingPackageName() {
        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void hashCode_withIncludeDataSources() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("pkg1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("pkg2").build();

        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(origin2)).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void hashCode_withExcludeDataSources() {
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName("pkg1").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName("pkg2").build();

        MatchmakingRequest request1 =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request2 =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin1)).build();
        MatchmakingRequest request3 =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(origin2)).build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }

    @Test
    public void toString_containsRecordTypes() {
        Set<Class<? extends Record>> recordTypes = Set.of(ActiveCaloriesBurnedRecord.class);
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().addRecordTypes(recordTypes).build();

        String toStringResult = request.toString();
        assertThat(toStringResult).contains("MatchmakingRequest");
        assertThat(toStringResult).contains("recordTypes=" + recordTypes);
    }

    @Test
    public void toString_withCallingPackageName() {
        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .addRecordType(StepsRecord.class)
                        .addRecordType(SleepSessionRecord.class)
                        .build();
        String requestString = request.toString();
        assertThat(requestString).contains("callingPackageName=" + TEST_PACKAGE_NAME);
        assertThat(requestString).contains("recordTypes=[");
        assertThat(requestString).contains(StepsRecord.class.toString());
        assertThat(requestString).contains(SleepSessionRecord.class.toString());
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void toString_containsIncludeDataOrigins() {
        DataOrigin include = new DataOrigin.Builder().setPackageName("included.pkg").build();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().setIncludedDataSources(Set.of(include)).build();

        assertThat(request.toString()).contains("includedDataSources=[");
        assertThat(request.toString()).contains("included.pkg");
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void toString_containsExcludeDataOrigins() {
        DataOrigin exclude = new DataOrigin.Builder().setPackageName("excluded.pkg").build();
        MatchmakingRequest request =
                new MatchmakingRequest.Builder().setExcludedDataSources(Set.of(exclude)).build();

        assertThat(request.toString()).contains("excludedDataSources=[");
        assertThat(request.toString()).contains("excluded.pkg");
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void toUnmasked_withIncludeDataOrigins_returnsNewUnmaskedInstance() {
        final String maskedPackageName = "masked.package.name";
        final String unmaskedPackageName = "unmasked.package.name";
        DataOrigin include = new DataOrigin.Builder().setPackageName(maskedPackageName).build();

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .setIncludedDataSources(Set.of(include))
                        .build();

        Function<String, String> unmasker =
                packageName -> {
                    assertThat(packageName).isEqualTo(maskedPackageName);
                    return unmaskedPackageName;
                };

        MatchmakingRequest unmaskedRequest = originalRequest.toUnmasked(unmasker);

        assertThat(unmaskedRequest).isNotSameInstanceAs(originalRequest);
        assertThat(unmaskedRequest.getRecordTypes()).isEqualTo(originalRequest.getRecordTypes());
        // Calling package name should not be masked
        assertThat(unmaskedRequest.getCallingPackageName())
                .isEqualTo(originalRequest.getCallingPackageName());
        DataOrigin unmaskedInclude =
                new DataOrigin.Builder().setPackageName(unmaskedPackageName).build();
        assertThat(unmaskedRequest.getIncludedDataSources()).containsExactly(unmaskedInclude);
        assertThat(unmaskedRequest.getExcludedDataSources()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void toUnmasked_withExcludeDataOrigins_returnsNewUnmaskedInstance() {
        final String maskedPackageName = "masked.package.name";
        final String unmaskedPackageName = "unmasked.package.name";
        DataOrigin exclude = new DataOrigin.Builder().setPackageName(maskedPackageName).build();

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .setExcludedDataSources(Set.of(exclude))
                        .build();

        Function<String, String> unmasker =
                packageName -> {
                    assertThat(packageName).isEqualTo(maskedPackageName);
                    return unmaskedPackageName;
                };

        MatchmakingRequest unmaskedRequest = originalRequest.toUnmasked(unmasker);

        assertThat(unmaskedRequest).isNotSameInstanceAs(originalRequest);
        assertThat(unmaskedRequest.getRecordTypes()).isEqualTo(originalRequest.getRecordTypes());
        // Calling package name should not be masked
        assertThat(unmaskedRequest.getCallingPackageName())
                .isEqualTo(originalRequest.getCallingPackageName());
        DataOrigin unmaskedInclude =
                new DataOrigin.Builder().setPackageName(unmaskedPackageName).build();
        assertThat(unmaskedRequest.getExcludedDataSources()).containsExactly(unmaskedInclude);
        assertThat(unmaskedRequest.getIncludedDataSources()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void toUnmasked_withoutIncludeOrExclude_returnsEquivalentRequest() {
        final String maskedPackageName = "masked.package.name";
        final String unmaskedPackageName = "unmasked.package.name";

        MatchmakingRequest originalRequest =
                new MatchmakingRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .setCallingPackageName(TEST_PACKAGE_NAME)
                        .build();

        Function<String, String> unmasker =
                packageName -> {
                    assertThat(packageName).isEqualTo(maskedPackageName);
                    return unmaskedPackageName;
                };

        MatchmakingRequest unmaskedRequest = originalRequest.toUnmasked(unmasker);

        assertThat(unmaskedRequest).isEqualTo(originalRequest);
    }

    @Test
    public void describeContents_returnsZero() {
        MatchmakingRequest request = new MatchmakingRequest.Builder().build();

        assertThat(request.describeContents()).isEqualTo(0);
    }

    @Test
    public void creator_newArray_returnsCorrectSizeArray() {
        MatchmakingRequest[] requests = MatchmakingRequest.CREATOR.newArray(5);
        assertThat(requests.length).isEqualTo(5);
        assertThat(requests[0]).isNull();
    }
}
