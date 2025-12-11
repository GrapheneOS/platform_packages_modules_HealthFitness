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
package android.health.connect.device;

import static android.health.connect.datatypes.Device.VALID_TYPES;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SyntheticPackageNameMatcherTest {
    private final String mCanonicalSpn =
            "com.android.healthconnect.watch.dae97b731dde83745b62b9111deee3456";

    private final String mMaskedSpn =
            "com.android.healthconnect.watch.jc45bd741a7123764b514e2c27df9fe41";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Test
    public void replaceAllCanonical_singleCanonicalInSpn_getsRedacted() {
        String message = "Error with " + mCanonicalSpn;

        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualMessage).contains("com.android.healthconnect.watch");
    }

    @Test
    public void replaceAllCanonical_multipleSpacedCanonicalInSpn_getsRedacted() {
        String message = "Error with " + mCanonicalSpn + " " + mCanonicalSpn;

        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualMessage)
                .contains("com.android.healthconnect.watch com.android.healthconnect.watch");
    }

    @Test
    public void replaceAllCanonical_multipleCanonicalInSpn_getsRedacted() {
        String message = "Error with " + mCanonicalSpn + mCanonicalSpn;

        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualMessage)
                .contains("com.android.healthconnect.watchcom.android.healthconnect.watch");
    }

    @Test
    public void replaceAllCanonical_multipleDifferentSpn_canonicalInGetsRedacted() {
        String message = "Error with " + mCanonicalSpn + mMaskedSpn;

        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualMessage).contains(mMaskedSpn);
        assertThat(actualMessage).contains("com.android.healthconnect.watch" + mMaskedSpn);
    }

    @Test
    public void replaceAllCanonical_In_maskedSpn_staysTheSame() {
        String message = "Error with " + mMaskedSpn;
        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).isEqualTo(message);
    }

    @Test
    public void replaceAllCanonical_In_normalPackage_staysTheSame() {
        String packageName = "com.example.app";
        String message = "Error with " + packageName;
        String actualMessage = SyntheticPackageNameMatcher.replaceAllCanonicalIn(message);

        assertThat(actualMessage).isEqualTo(message);
    }

    @Test
    public void withEmptyString_matches_returnsFalse() {
        assertFalse(SyntheticPackageNameMatcher.matches(""));
    }

    @Test
    public void withSpaceBefore_matches_returnsFalse() {
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        " com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594"));
    }

    @Test
    public void withSpaceAfter_matches_returnsFalse() {
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594 "));
    }

    @Test
    public void withOtherCharacters_matches_returnsFalse() {
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "hello com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594"
                                + " !"));
    }

    @Test
    public void withShorterUuid_matches_returnsFalse() {
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        mCanonicalSpn.substring(0, mCanonicalSpn.length() - 1)));
    }

    @Test
    public void withLongerUuid_matches_returnsFalse() {
        assertFalse(SyntheticPackageNameMatcher.matches(mCanonicalSpn + "a"));
    }

    @Test
    public void withMissingPartsInPackagePrefix_matches_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SyntheticPackageNameMatcher.matches(canonicalSpn));

        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.android.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "android.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SyntheticPackageNameMatcher.matches("unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMissingDeviceType_matches_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SyntheticPackageNameMatcher.matches(canonicalSpn));

        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.android.healthconnect.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMissingUuidPrefix_matches_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SyntheticPackageNameMatcher.matches(canonicalSpn));

        assertFalse(
                SyntheticPackageNameMatcher.matches(
                        "com.android.healthconnect.unknown.db6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMutation_matches_returnsFalse() {
        String validSpn = mCanonicalSpn;
        assertTrue(SyntheticPackageNameMatcher.matches(validSpn));

        for (int i = 0; i < validSpn.length(); i++) {
            char[] charArray = validSpn.toCharArray();
            charArray[i] = 'z';
            String mutatedSpn = Arrays.toString(charArray);
            assertFalse(SyntheticPackageNameMatcher.matches(mutatedSpn));
        }
    }

    // When this test fails, the values from Device.VALID_TYPES and
    // SyntheticPackageNameCreator.DEVICE_TYPE_TO_DISPLAY_NAME have diverged. Sync them to fix this
    // test.
    @Test
    public void withMapping_deviceTypeToDisplayName_isComplete() {
        Set<Integer> spnDeviceTypes =
                SyntheticPackageNameMatcher.DEVICE_TYPE_TO_DISPLAY_NAME.keySet();

        assertEquals(VALID_TYPES, spnDeviceTypes);
    }
}
