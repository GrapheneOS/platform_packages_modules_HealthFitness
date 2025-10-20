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
package com.android.server.healthconnect.common.metadata;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import android.health.connect.internal.datatypes.AppInfoInternal;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.NoSuchElementException;

@RunWith(AndroidJUnit4.class)
public class SyntheticPackageNameResolverTest {
    private static final String TEST_CALLER_ONE = "foo";
    private static final String TEST_CALLER_TWO = "bar";

    private static final String TEST_APP_PACKAGE_NAME = "foobar";
    private static final String TEST_CANONICAL_SPN =
            "com.android.healthconnect.watch.de78f5438b48b39bcbdea61b73679449d";
    private static final String TEST_MASKED_ONE_SPN =
            SyntheticPackageNameCreator.createMasked(TEST_CANONICAL_SPN, TEST_CALLER_ONE);
    private static final String TEST_MASKED_TWO_SPN =
            SyntheticPackageNameCreator.createMasked(TEST_CANONICAL_SPN, TEST_CALLER_TWO);

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private AppInfoHelper mAppInfoHelper;
    private SyntheticPackageNameResolver mResolver;

    @Before
    public void setup() {
        when(mAppInfoHelper.getAppInfoMap())
                .thenReturn(
                        Map.of(
                                TEST_APP_PACKAGE_NAME,
                                        new AppInfoInternal(1, null, null, null, null, null),
                                TEST_CANONICAL_SPN,
                                        new AppInfoInternal(2, null, null, null, null, null)));
        mResolver = new SyntheticPackageNameResolver(mAppInfoHelper);
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withFlagDisabled_mask_returnsInput() {
        String result = mResolver.mask(TEST_CANONICAL_SPN, TEST_CALLER_ONE);

        assertThat(result).isEqualTo(TEST_CANONICAL_SPN);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withMaskedSpn_mask_throwsIllegalArgumentError() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mResolver.mask(TEST_MASKED_ONE_SPN, TEST_CALLER_ONE));
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withRegularApp_mask_returnsInput() {
        String result = mResolver.mask(TEST_APP_PACKAGE_NAME, TEST_CALLER_ONE);

        // Regular app packages are not canonical SPNs, so do not mask.
        assertThat(result).isEqualTo(TEST_APP_PACKAGE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withCanonicalSpn_mask_returnsMasked() {
        String result = mResolver.mask(TEST_CANONICAL_SPN, TEST_CALLER_ONE);

        assertThat(result).isEqualTo(TEST_MASKED_ONE_SPN);
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withFlagDisabled_unmask_returnsInput() {
        String result = mResolver.unmask(TEST_MASKED_ONE_SPN, TEST_CALLER_ONE);

        assertThat(result).isEqualTo(TEST_MASKED_ONE_SPN);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withRegularApp_unmask_returnsInput() {
        String result = mResolver.unmask(TEST_APP_PACKAGE_NAME, TEST_CALLER_ONE);

        assertThat(result).isEqualTo(TEST_APP_PACKAGE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withCanonicalSpn_unmask_returnsInput() {
        String result = mResolver.unmask(TEST_CANONICAL_SPN, TEST_CALLER_ONE);

        // Even though we throw an error for the masking equivalent of this edge case (masking a
        // masked SPN), if clients really happen to get a canonical SPN, the security requirement
        // is already broken, so the masking mechanic becomes obsolete.
        assertThat(result).isEqualTo(TEST_CANONICAL_SPN);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withMaskedSpn_unmask_returnsUnmasked() {
        String result = mResolver.unmask(TEST_MASKED_ONE_SPN, TEST_CALLER_ONE);

        assertThat(result).isEqualTo(TEST_CANONICAL_SPN);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withDifferentCallers_unmask_returnsUnmasked() {
        String resultOne = mResolver.unmask(TEST_MASKED_ONE_SPN, TEST_CALLER_ONE);
        String resultTwo = mResolver.unmask(TEST_MASKED_TWO_SPN, TEST_CALLER_TWO);
        assertThat(resultOne).isEqualTo(TEST_CANONICAL_SPN);
        assertThat(resultTwo).isEqualTo(TEST_CANONICAL_SPN);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withMismatchedCaller_unmask_throwsResolutionError() {
        // Trying to unmask a package masked for Caller One in the context of Caller Two should fail
        // resolution.
        NoSuchElementException exception =
                assertThrows(
                        NoSuchElementException.class,
                        () -> mResolver.unmask(TEST_MASKED_ONE_SPN, TEST_CALLER_TWO));

        assertThat(exception.getMessage())
                .contains("Could not resolve masked, synthetic package name");
        assertThat(exception.getMessage()).contains(TEST_MASKED_ONE_SPN);
        assertThat(exception.getMessage()).contains(" called by ");
        assertThat(exception.getMessage()).contains(TEST_CALLER_TWO);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void withUnknownCanonical_unmask_throwsResolutionError() {
        // Create a masked SPN for a canonical name that is NOT in the AppInfoHelper map.
        String unknownCanonical =
                "com.android.healthconnect.scale.d87d4eeb7dec7386490748d174c0e0a11";
        String unknownMasked =
                SyntheticPackageNameCreator.createMasked(unknownCanonical, TEST_CALLER_ONE);

        NoSuchElementException exception =
                assertThrows(
                        NoSuchElementException.class,
                        () -> mResolver.unmask(unknownMasked, TEST_CALLER_ONE));

        assertThat(exception.getMessage())
                .contains("Could not resolve masked, synthetic package name");
        assertThat(exception.getMessage()).contains(unknownMasked);
        assertThat(exception.getMessage()).contains(" called by ");
        assertThat(exception.getMessage()).contains(TEST_CALLER_ONE);
    }
}
