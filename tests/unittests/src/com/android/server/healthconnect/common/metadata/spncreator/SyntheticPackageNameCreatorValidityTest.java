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
package com.android.server.healthconnect.common.metadata.spncreator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Device.DeviceType;
import android.health.connect.device.SyntheticPackageNameMatcher;
import android.platform.test.annotations.LargeTest;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SyntheticPackageNameCreatorValidityTest {
    // See {@link GetMedicalDataSourcesRequest}
    private static final String PACKAGE_NAME_REGEX =
            "^([A-Za-z][a-zA-Z0-9_]*\\.)+[A-Za-z][a-zA-Z0-9_]*$";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(PACKAGE_NAME_REGEX);

    private static final Charset[] ENCODINGS = {
        StandardCharsets.US_ASCII,
        StandardCharsets.ISO_8859_1,
        StandardCharsets.UTF_8,
        StandardCharsets.UTF_16,
        StandardCharsets.UTF_16LE,
        StandardCharsets.UTF_16BE
    };

    private @DeviceType int mDeviceType;
    private String mDeviceId;
    private String mCallingPackage;
    private SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static Collection<Object[]> getParams() {
        final List<Object[]> params = new ArrayList<>();
        final Random random = new Random(42);

        for (Integer type : Device.VALID_TYPES) {
            for (Charset encoding : ENCODINGS) {
                params.add(
                        new Object[] {
                            type,
                            generateRandomString(encoding, random),
                            generateRandomString(encoding, random)
                        });
            }
        }

        return params;
    }

    @Before
    public void setUp() throws Exception {
        Context applicationContext = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(applicationContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mSyntheticPackageNameCreator =
                new SyntheticPackageNameCreator(healthConnectInjector.getPreferenceHelper());
    }

    @Test
    public void withRandomDeviceId_createCanonical_isValidPackageName() {
        for (var params : getParams()) {
            initializeRun(params);
            String spn = mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);

            Matcher matcher = PACKAGE_PATTERN.matcher(spn);
            assertTrue(matcher.matches());
        }
    }

    @Test
    public void withRandomDeviceId_createCanonical_isCanonicalSpn() {
        for (var params : getParams()) {
            initializeRun(params);
            String spn = mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);

            assertTrue(SyntheticPackageNameMatcher.matchesCanonical(spn));
            assertFalse(SyntheticPackageNameMatcher.matchesMasked(spn));
        }
    }

    @Test
    public void withRandomDeviceId_createMasked_isValidPackageName() {
        for (var params : getParams()) {
            initializeRun(params);
            String canonicalSpn =
                    mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);
            String maskedSpn =
                    SyntheticPackageNameCreator.createMasked(canonicalSpn, mCallingPackage);

            Matcher matcher = PACKAGE_PATTERN.matcher(maskedSpn);
            assertTrue(matcher.matches());
        }
    }

    @Test
    public void withRandomSpnAndCallingPackage_createMasked_isMaskedSpn() {
        for (var params : getParams()) {
            initializeRun(params);
            String canonicalSpn =
                    mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);
            String maskedSpn =
                    SyntheticPackageNameCreator.createMasked(canonicalSpn, mCallingPackage);

            assertFalse(SyntheticPackageNameMatcher.matchesCanonical(maskedSpn));
            assertTrue(SyntheticPackageNameMatcher.matchesMasked(maskedSpn));
        }
    }

    private static String generateRandomString(Charset encoding, Random random) {
        byte[] array = new byte[32];
        random.nextBytes(array);
        return new String(array, encoding);
    }

    private void initializeRun(Object[] params) {
        mDeviceType = (int) params[0];
        mDeviceId = (String) params[1];
        mCallingPackage = (String) params[2];
    }
}
