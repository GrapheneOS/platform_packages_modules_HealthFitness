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

import static android.health.connect.datatypes.FhirVersion.parseFhirVersion;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.R4B_VERSION_STRING;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.R4_VERSION_STRING;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.UNSUPPORTED_VERSION_STRING;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.FhirVersion;
import android.os.Parcel;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FhirVersionTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testFhirVersion_parseFhirVersion_validSingleDigits() {
        FhirVersion fhirVersion = parseFhirVersion("4.0.1");

        assertThat(fhirVersion.getMajor()).isEqualTo(4);
        assertThat(fhirVersion.getMinor()).isEqualTo(0);
        assertThat(fhirVersion.getPatch()).isEqualTo(1);
    }

    @Test
    public void testFhirVersion_parseFhirVersion_validMultipleDigits() {
        FhirVersion fhirVersion = parseFhirVersion("100.11.1002");

        assertThat(fhirVersion.getMajor()).isEqualTo(100);
        assertThat(fhirVersion.getMinor()).isEqualTo(11);
        assertThat(fhirVersion.getPatch()).isEqualTo(1002);
    }

    @Test
    public void testFhirVersion_parseFhirVersion_invalidEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> parseFhirVersion(""));
    }

    @Test
    public void testFhirVersion_parseFhirVersion_invalidNonDigit() {
        assertThrows(IllegalArgumentException.class, () -> parseFhirVersion("4.0.1x"));
    }

    @Test
    public void testFhirVersion_parseFhirVersion_invalidLessNumbers() {
        assertThrows(IllegalArgumentException.class, () -> parseFhirVersion("4.0"));
    }

    @Test
    public void testFhirVersion_parseFhirVersion_invalidMoreNumbers() {
        assertThrows(IllegalArgumentException.class, () -> parseFhirVersion("4.0.1.2"));
    }

    @Test
    public void testFhirVersion_parseFhirVersion_invalidNegativeNumbers() {
        assertThrows(IllegalArgumentException.class, () -> parseFhirVersion("-4.0.1"));
    }

    @Test
    public void testFhirVersion_toString() {
        FhirVersion fhirVersion = parseFhirVersion(R4_VERSION_STRING);

        assertThat(fhirVersion.toString()).isEqualTo(R4_VERSION_STRING);
    }

    @Test
    public void testFhirVersion_equals() {
        FhirVersion fhirVersion1 = parseFhirVersion(R4_VERSION_STRING);
        FhirVersion fhirVersion2 = parseFhirVersion(R4_VERSION_STRING);

        assertThat(fhirVersion1.equals(fhirVersion2)).isTrue();
        assertThat(fhirVersion1.hashCode()).isEqualTo(fhirVersion2.hashCode());
    }

    @Test
    public void testFhirVersion_equals_comparesAllValues() {
        FhirVersion fhirVersion = parseFhirVersion("4.0.1");
        FhirVersion fhirVersionDifferentMajor = parseFhirVersion("5.0.1");
        FhirVersion fhirVersionDifferentMinor = parseFhirVersion("4.1.1");
        FhirVersion fhirVersionDifferentPatch = parseFhirVersion("4.0.2");

        assertThat(fhirVersionDifferentMajor.equals(fhirVersion)).isFalse();
        assertThat(fhirVersionDifferentMinor.equals(fhirVersion)).isFalse();
        assertThat(fhirVersionDifferentPatch.equals(fhirVersion)).isFalse();
        assertThat(fhirVersionDifferentMajor.hashCode()).isNotEqualTo(fhirVersion.hashCode());
        assertThat(fhirVersionDifferentMinor.hashCode()).isNotEqualTo(fhirVersion.hashCode());
        assertThat(fhirVersionDifferentPatch.hashCode()).isNotEqualTo(fhirVersion.hashCode());
    }

    @Test
    public void testWriteToParcelThenRestore_objectsAreIdentical() {
        FhirVersion original = FHIR_VERSION_R4;

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        FhirVersion restored = FhirVersion.CREATOR.createFromParcel(parcel);

        assertThat(restored).isEqualTo(original);
        parcel.recycle();
    }

    @Test
    public void testWriteToParcel_negativeVersionNumbers_throws() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(-1);
        parcel.writeInt(0);
        parcel.writeInt(0);
        parcel.setDataPosition(0);

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FhirVersion.CREATOR.createFromParcel(parcel));

        assertThat(thrown).hasMessageThat().contains("Version numbers can not be negative");
        parcel.recycle();
    }

    @Test
    public void testFhirVersion_isSupportedFhirVersion_supportsR4() {
        FhirVersion fhirVersion = parseFhirVersion(R4_VERSION_STRING);

        assertThat(fhirVersion.isSupportedFhirVersion()).isTrue();
    }

    @Test
    public void testFhirVersion_isSupportedFhirVersion_supportsR4B() {
        FhirVersion fhirVersion = parseFhirVersion(R4B_VERSION_STRING);

        assertThat(fhirVersion.isSupportedFhirVersion()).isTrue();
    }

    @Test
    public void testFhirVersion_isSupportedFhirVersion_differentVersionNotSupported() {
        FhirVersion fhirVersion = parseFhirVersion(UNSUPPORTED_VERSION_STRING);

        assertThat(fhirVersion.isSupportedFhirVersion()).isFalse();
    }
}
