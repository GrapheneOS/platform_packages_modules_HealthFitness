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
import static android.health.connect.datatypes.Device.VALID_TYPES;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.health.connect.datatypes.Device.DeviceType;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SpnCreator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SpnCreatorTest {
    private static final @DeviceType int TEST_DEVICE_TYPE_ONE = DEVICE_TYPE_PHONE;
    private static final @DeviceType int TEST_DEVICE_TYPE_TWO = DEVICE_TYPE_WATCH;
    private static final @DeviceType int TEST_DEVICE_TYPE_THREE = DEVICE_TYPE_SCALE;
    private static final String TEST_DEVICE_ID_ONE = "foo";
    private static final String TEST_DEVICE_ID_TWO = "bar";
    private static final String TEST_DEVICE_ID_THREE = "world";

    private static final String TEST_CALLER_ONE = "foo";
    private static final String TEST_CALLER_TWO = "bar";
    private static final String TEST_CALLER_THREE = "healthconnect";

    private static final String TEST_CANONICAL_SPN_ONE =
            SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
    private static final String TEST_CANONICAL_SPN_TWO =
            SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
    private static final String TEST_CANONICAL_SPN_THREE =
            SpnCreator.createCanonical(TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);

    @Test
    public void withNullIdsAndSameType_createCanonical_returnsSameSpn() {
        String id = null;

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String id = null;

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, id);

        assertNotEquals(first, second);
    }

    @Test
    public void withEmptyIdsAndSameType_createCanonical_returnsSameSpn() {
        String id = "";

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withEmptyIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String id = "";

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, id);

        assertNotEquals(first, second);
    }

    @Test
    public void withNullAndEmptyIdsAndSameType_createCanonical_returnsSameSpn() {
        String firstId = null;
        String secondId = "";

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, firstId);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, secondId);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdAndSameType_createCanonical_returnsSameSpn() {
        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_ONE);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndSameTypes_createCanonical_returnsDifferentSpns() {
        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidTypeAndSameId_createCanonical_returnsSameSpn() {
        int invalidType = -1;

        String first = SpnCreator.createCanonical(invalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(invalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndSameId_createCanonical_returnsSameSpn() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first = SpnCreator.createCanonical(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(secondInvalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndDifferentIds_createCanonical_returnsDifferentSpns() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first = SpnCreator.createCanonical(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(secondInvalidType, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidType_createCanonical_returnsFallbackTypeInSpn() {
        String actual = SpnCreator.createCanonical(-1, TEST_DEVICE_ID_ONE);

        assertTrue(actual.startsWith("com.android.healthconnect.unknown."));
    }

    @Test
    public void withDifferentOrder_createCanonical_ignoresOrdering() {
        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String third = SpnCreator.createCanonical(TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);

        String newFirst = SpnCreator.createCanonical(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String newSecond = SpnCreator.createCanonical(TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);
        String newThird = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertEquals(first, newThird);
        assertEquals(second, newFirst);
        assertEquals(third, newSecond);
    }

    @Test
    public void withNullCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String caller = null;

        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, caller);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, caller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullCallerAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String caller = null;

        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, caller);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, caller);

        assertNotEquals(first, second);
    }

    @Test
    public void withEmptyCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String caller = "";

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, caller);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, caller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withEmptyCallerAndDifferentTypes_createCanonical_returnsDifferentSpns() {
        String caller = "";

        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, caller);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, caller);

        assertNotEquals(first, second);
    }

    @Test
    public void withNullAndEmptyCallersAndSameCanonical_createMasked_returnsSameSpn() {
        String firstCaller = null;
        String secondCaller = "";

        String first = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, firstCaller);
        String second = SpnCreator.createCanonical(TEST_DEVICE_TYPE_ONE, secondCaller);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameCallerAndSameCanonical_createMasked_returnsSameSpn() {
        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameCallersAndDifferentCanonicals_createMasked_returnsDifferentSpns() {
        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, TEST_CALLER_ONE);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentCallersAndSameCanonical_createMasked_returnsDifferentSpns() {
        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentCallersAndDifferentCanonicals_createMasked_returnsDifferentSpns() {
        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, TEST_CALLER_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentOrder_createMasked_ignoresOrdering() {
        String first = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);
        String second = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, TEST_CALLER_TWO);
        String third = SpnCreator.createMasked(TEST_CANONICAL_SPN_THREE, TEST_CALLER_THREE);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);

        String newFirst = SpnCreator.createMasked(TEST_CANONICAL_SPN_TWO, TEST_CALLER_TWO);
        String newSecond = SpnCreator.createMasked(TEST_CANONICAL_SPN_THREE, TEST_CALLER_THREE);
        String newThird = SpnCreator.createMasked(TEST_CANONICAL_SPN_ONE, TEST_CALLER_ONE);

        assertEquals(first, newThird);
        assertEquals(second, newFirst);
        assertEquals(third, newSecond);
    }

    @Test
    public void withInvalidCanonical_createMasked_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SpnCreator.createMasked("not.a.spn", "fakeCaller"));
    }

    @Test
    public void withEmptyString_isSpn_returnsFalse() {
        assertFalse(SpnCreator.isSpn(""));
    }

    @Test
    public void withShorterUuid_isSpn_returnsFalse() {
        assertFalse(
                SpnCreator.isSpn(
                        TEST_CANONICAL_SPN_ONE.substring(0, TEST_CANONICAL_SPN_ONE.length() - 1)));
    }

    @Test
    public void withLongerUuid_isSpn_returnsFalse() {
        assertFalse(SpnCreator.isSpn(TEST_CANONICAL_SPN_ONE + "a"));
    }

    @Test
    public void withMissingPartsInPackagePrefix_isSpn_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SpnCreator.isSpn(canonicalSpn));

        assertFalse(SpnCreator.isSpn("com.android.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SpnCreator.isSpn("com.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(SpnCreator.isSpn("com.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(
                SpnCreator.isSpn(
                        "android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(SpnCreator.isSpn("android.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(SpnCreator.isSpn("healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
        assertFalse(SpnCreator.isSpn("unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMissingDeviceType_isSpn_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SpnCreator.isSpn(canonicalSpn));

        assertFalse(
                SpnCreator.isSpn("com.android.healthconnect.ddb6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMissingUuidPrefix_isSpn_returnsFalse() {
        String canonicalSpn = "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc";
        assertTrue(SpnCreator.isSpn(canonicalSpn));

        assertFalse(
                SpnCreator.isSpn(
                        "com.android.healthconnect.unknown.db6ff2ffe2df3b8cbc0d9542bdce27dc"));
    }

    @Test
    public void withMutation_isSpn_returnsFalse() {
        String validSpn = TEST_CANONICAL_SPN_ONE;
        assertTrue(SpnCreator.isSpn(validSpn));

        for (int i = 0; i < validSpn.length(); i++) {
            char[] charArray = validSpn.toCharArray();
            charArray[i] = 'z';
            String mutatedSpn = Arrays.toString(charArray);
            assertFalse(SpnCreator.isSpn(mutatedSpn));
        }
    }

    // When this test fails, the values from Device.VALID_TYPES and
    // SpnCreator.DEVICE_TYPE_TO_DISPLAY_NAME have diverged. Sync them to fix this test.
    @Test
    public void withMapping_deviceTypeToDisplayName_isComplete() {
        Set<Integer> spnDeviceTypes = SpnCreator.DEVICE_TYPE_TO_DISPLAY_NAME.keySet();

        assertEquals(VALID_TYPES, spnDeviceTypes);
    }
}
