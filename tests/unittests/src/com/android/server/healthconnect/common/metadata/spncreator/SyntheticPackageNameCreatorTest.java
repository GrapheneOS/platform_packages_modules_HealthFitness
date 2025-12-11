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

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_SCALE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.health.connect.datatypes.Device.DeviceType;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SyntheticPackageNameCreatorTest {
    private static final @DeviceType int TEST_DEVICE_TYPE_ONE = DEVICE_TYPE_PHONE;
    private static final @DeviceType int TEST_DEVICE_TYPE_TWO = DEVICE_TYPE_WATCH;
    private static final @DeviceType int TEST_DEVICE_TYPE_THREE = DEVICE_TYPE_SCALE;
    private static final String TEST_DEVICE_ID_ONE = "foo";
    private static final String TEST_DEVICE_ID_TWO = "bar";
    private static final String TEST_DEVICE_ID_THREE = "world";

    private static final String TEST_CALLER_ONE = "foo";
    private static final String TEST_CALLER_TWO = "bar";
    private static final String TEST_CALLER_THREE = "healthconnect";
    private static final String PREFERENCE_KEY =
            SyntheticPackageNameCreator.SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY;

    private PreferenceHelper mPreferenceHelper;
    private SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    private String mTestCanonicalSpnOne;
    private String mTestCanonicalSpnTwo;
    private String mTestCanonicalSpnThree;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(
                                ApplicationProvider.getApplicationContext())
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mPreferenceHelper = healthConnectInjector.getPreferenceHelper();
        mSyntheticPackageNameCreator = new SyntheticPackageNameCreator(mPreferenceHelper);

        mTestCanonicalSpnOne =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        mTestCanonicalSpnTwo =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);

        mTestCanonicalSpnThree =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);

        mPreferenceHelper.removeKey(PREFERENCE_KEY);
    }

    @Test
    public void withNullIdsAndSameType_createCanonical_returnsSameSpn() {
        String id = null;

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String id = null;

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_TWO, id);

        assertNotEquals(first, second);
    }

    @Test
    public void withEmptyIdsAndSameType_createCanonical_returnsSameSpn() {
        String id = "";

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withEmptyIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String id = "";

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_TWO, id);

        assertNotEquals(first, second);
    }

    @Test
    public void withNullAndEmptyIdsAndSameType_createCanonical_returnsSameSpn() {
        String firstId = null;
        String secondId = "";

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, firstId);
        String second =
                mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, secondId);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdAndSameType_createCanonical_returnsSameSpn() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_ONE);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndSameTypes_createCanonical_returnsDifferentSpns() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidTypeAndSameId_createCanonical_returnsSameSpn() {
        int invalidType = -1;

        String first =
                mSyntheticPackageNameCreator.createCanonical(invalidType, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(invalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndSameId_createCanonical_returnsSameSpn() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first =
                mSyntheticPackageNameCreator.createCanonical(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(secondInvalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndDifferentIds_createCanonical_returnsDifferentSpns() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first =
                mSyntheticPackageNameCreator.createCanonical(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(secondInvalidType, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidType_createCanonical_returnsFallbackTypeInSpn() {
        String actual = mSyntheticPackageNameCreator.createCanonical(-1, TEST_DEVICE_ID_ONE);

        assertTrue(actual.startsWith("com.android.healthconnect.unknown."));
    }

    @Test
    public void withDifferentOrder_createCanonical_ignoresOrdering() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String third =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);

        String newFirst =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String newSecond =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);
        String newThird =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertEquals(first, newThird);
        assertEquals(second, newFirst);
        assertEquals(third, newSecond);
    }

    @Test
    public void withNullCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String caller = null;

        String first = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, caller);
        String second = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, caller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullCallerAndDifferentTypes_createMaskedreturnsDifferentSpns() {
        String caller = null;

        String first = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, caller);
        String second = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, caller);

        assertNotEquals(first, second);
    }

    @Test
    public void withEmptyCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String caller = "";

        String first = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, caller);
        String second = mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, caller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withEmptyCallerAndDifferentTypes_createMasked_returnsDifferentSpns() {
        String caller = "";

        String first = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, caller);
        String second = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, caller);

        assertNotEquals(first, second);
    }

    @Test
    public void withNullAndEmptyCallersAndSameCanonical_createMasked_returnsSameSpn() {
        String firstCaller = null;
        String secondCaller = "";

        String first =
                mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, firstCaller);
        String second =
                mSyntheticPackageNameCreator.createCanonical(TEST_DEVICE_TYPE_ONE, secondCaller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String first =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);
        String second =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameCallersAndDifferentCanonicals_createMasked_returnsDifferentSpns() {
        String first =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);
        String second =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, TEST_CALLER_ONE);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentCallersAndSameCanonical_createMasked_returnsDifferentSpns() {
        String first =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);
        String second =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentCallersAndDifferentCanonicals_createMasked_returnsDifferentSpns() {
        String first =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);
        String second =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, TEST_CALLER_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentOrder_createMasked_ignoresOrdering() {
        String first =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);
        String second =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, TEST_CALLER_TWO);
        String third =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnThree, TEST_CALLER_THREE);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);

        String newFirst =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnTwo, TEST_CALLER_TWO);
        String newSecond =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnThree, TEST_CALLER_THREE);
        String newThird =
                SyntheticPackageNameCreator.createMasked(mTestCanonicalSpnOne, TEST_CALLER_ONE);

        assertEquals(first, newThird);
        assertEquals(second, newFirst);
        assertEquals(third, newSecond);
    }

    @Test
    public void withInvalidCanonical_createMasked_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SyntheticPackageNameCreator.createMasked("not.a.spn", "fakeCaller"));
    }

    @Test
    public void withEmptyPreference_initializeOrGetSalt_addsPreference() {
        assertNull(mPreferenceHelper.getPreference(PREFERENCE_KEY));

        String salt = mSyntheticPackageNameCreator.initializeOrGetSalt();

        assertEquals(salt, mPreferenceHelper.getPreference(PREFERENCE_KEY));
    }

    @Test
    public void withMultipleCalls_initializeOrGetSalt_returnsSameSalt() {
        String firstSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();
        String secondSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();

        assertEquals(firstSalt, secondSalt);
    }

    @Test
    public void withMultipleCallsAndSaltReset_initializeOrGetSalt_generatesDifferentSalts() {
        String firstSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();

        resetSaltPreference();

        String secondSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();

        assertNotEquals(firstSalt, secondSalt);
    }

    @Test
    public void withMultipleCallsAndSaltReset_initializeOrGetSalt_addsPreferenceTwice() {
        assertNull(mPreferenceHelper.getPreference(PREFERENCE_KEY));

        String firstSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();
        assertEquals(firstSalt, mPreferenceHelper.getPreference(PREFERENCE_KEY));

        resetSaltPreference();

        String secondSalt = mSyntheticPackageNameCreator.initializeOrGetSalt();
        assertEquals(secondSalt, mPreferenceHelper.getPreference(PREFERENCE_KEY));
    }

    @Test
    public void withMultipleCallsAndSaltReset_createCanonical_returnsDifferentSpns() {
        String first =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        resetSaltPreference();

        String second =
                mSyntheticPackageNameCreator.createCanonical(
                        TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertNotEquals(first, second);
    }

    private void resetSaltPreference() {
        mPreferenceHelper.removeKey(PREFERENCE_KEY);
        assertNull(mPreferenceHelper.getPreference(PREFERENCE_KEY));
    }
}
