/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.phr;

import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.GetMedicalDataSourcesRequest;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Sets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class GetMedicalDataSourcesRequestTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testBuilder_constructor() {
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        assertThat(request.getPackageNames()).isEmpty();
    }

    @Test
    public void testBuilder_addValidPackageNames() {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();

        assertThat(request.getPackageNames()).containsExactly("com.foo", "com.bar");
    }

    @Test
    public void testBuilder_fromExistingBuilder() {
        GetMedicalDataSourcesRequest.Builder original =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar");
        GetMedicalDataSourcesRequest copy =
                new GetMedicalDataSourcesRequest.Builder(original).build();

        assertThat(copy).isEqualTo(original.build());
    }

    @Test
    public void testBuilder_fromExistingBuilder_changeIndependently() {
        GetMedicalDataSourcesRequest.Builder original =
                new GetMedicalDataSourcesRequest.Builder().addPackageName("com.foo");
        GetMedicalDataSourcesRequest.Builder copy =
                new GetMedicalDataSourcesRequest.Builder(original);
        original.addPackageName("com.bar");

        assertThat(original.build().getPackageNames()).containsExactly("com.foo", "com.bar");
        assertThat(copy.build().getPackageNames()).containsExactly("com.foo");
    }

    @Test
    public void testBuilder_fromExistingBuilderClearPackageNames() {
        GetMedicalDataSourcesRequest.Builder original =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar");
        GetMedicalDataSourcesRequest.Builder copy =
                new GetMedicalDataSourcesRequest.Builder(original);
        original.clearPackageNames();

        assertThat(original.build().getPackageNames()).isEmpty();
        assertThat(copy.build().getPackageNames()).containsExactly("com.foo", "com.bar");
    }

    @Test
    public void testBuilder_fromExistingInstance() {
        GetMedicalDataSourcesRequest original =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();
        GetMedicalDataSourcesRequest copy =
                new GetMedicalDataSourcesRequest.Builder(original).build();

        assertThat(copy).isEqualTo(original);
    }

    @Test
    public void testBuilder_fromExistingInstance_changeIndependently() {
        GetMedicalDataSourcesRequest original =
                new GetMedicalDataSourcesRequest.Builder().addPackageName("com.foo").build();
        GetMedicalDataSourcesRequest.Builder copy =
                new GetMedicalDataSourcesRequest.Builder(original);
        copy.addPackageName("com.bar");

        assertThat(original.getPackageNames()).containsExactly("com.foo");
        assertThat(copy.build().getPackageNames()).containsExactly("com.foo", "com.bar");
    }

    @Test
    public void testBuilder_fromExistingInstanceClearPackageNames() {
        GetMedicalDataSourcesRequest original =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();
        GetMedicalDataSourcesRequest.Builder copy =
                new GetMedicalDataSourcesRequest.Builder(original);
        copy.clearPackageNames();

        assertThat(original.getPackageNames()).containsExactly("com.foo", "com.bar");
        assertThat(copy.build().getPackageNames()).isEmpty();
    }

    @Test
    public void testBuilder_addInvalidPackageNames_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetMedicalDataSourcesRequest.Builder().addPackageName("foo"));
    }

    @Test
    public void testRequest_equalsAndHashcode() {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();
        GetMedicalDataSourcesRequest requestSame =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.bar")
                        .addPackageName("com.foo")
                        .build();
        GetMedicalDataSourcesRequest requestDifferent =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.bar")
                        .addPackageName("com.foo")
                        .addPackageName("com.baz")
                        .build();

        assertThat(request).isEqualTo(requestSame);
        assertThat(request.hashCode()).isEqualTo(requestSame.hashCode());
        assertThat(request).isNotEqualTo(requestDifferent);
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        GetMedicalDataSourcesRequest original =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        GetMedicalDataSourcesRequest restored =
                GetMedicalDataSourcesRequest.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testRequest_toString() {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName("com.foo")
                        .addPackageName("com.bar")
                        .build();
        String expectedPropertiesStringOrder1 = "packageNames={com.foo, com.bar}";
        String expectedPropertiesStringOrder2 = "packageNames={com.bar, com.foo}";

        String formatString = "GetMedicalDataSourcesRequest{%s}";
        assertThat(request.toString())
                .isAnyOf(
                        String.format(formatString, expectedPropertiesStringOrder1),
                        String.format(formatString, expectedPropertiesStringOrder2));
    }

    @Test
    public void testRestoreInvalidPackageNameFromParcel_expectException()
            throws NoSuchFieldException, IllegalAccessException {
        GetMedicalDataSourcesRequest original = new GetMedicalDataSourcesRequest.Builder().build();
        Set<String> validPackageNames =
                Set.of("com.foo", "com.FOO", "com.foo_bar", "com.foo_bar.baz1");
        Set<String> invalidPackageNames =
                Set.of(
                        ".",
                        ".foo",
                        "foo.",
                        "com",
                        "_com.bar",
                        "com.1bar",
                        "1com.bar",
                        "com..bar",
                        "com.-bar",
                        "delete * from table");
        Set<String> packageNames = Sets.union(validPackageNames, invalidPackageNames);
        setFieldValueUsingReflection(original, "mPackageNames", packageNames);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> GetMedicalDataSourcesRequest.CREATOR.createFromParcel(parcel));

        for (String validPackageName : validPackageNames) {
            assertThat(exception.getMessage()).doesNotContain(validPackageName);
        }
        for (String invalidPackageName : invalidPackageNames) {
            assertThat(exception.getMessage()).contains(invalidPackageName);
        }
    }
}
