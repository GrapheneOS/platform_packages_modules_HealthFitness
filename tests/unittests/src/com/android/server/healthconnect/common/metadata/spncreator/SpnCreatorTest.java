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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.health.connect.datatypes.Device.DeviceType;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SpnCreator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SpnCreatorTest {
    private static final @DeviceType int TEST_DEVICE_TYPE_ONE = DEVICE_TYPE_PHONE;
    private static final @DeviceType int TEST_DEVICE_TYPE_TWO = DEVICE_TYPE_WATCH;
    private static final @DeviceType int TEST_DEVICE_TYPE_THREE = DEVICE_TYPE_SCALE;
    private static final String TEST_DEVICE_ID_ONE = "foo";
    private static final String TEST_DEVICE_ID_TWO = "bar";
    private static final String TEST_DEVICE_ID_THREE = "world";

    @Test
    public void withNullIdsAndSameType_create_returnsSameSpn() {
        String id = null;

        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullIdsAndDifferentTypes_create_returnsDifferentSpns() {
        String id = null;

        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_TWO, id);

        assertNotEquals(first, second);
    }

    @Test
    public void withEmptyIdsAndSameType_create_returnsSameSpn() {
        String id = "";

        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, id);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_ONE, id);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withNullAndEmptyIdsAndSameType_create_returnsSameSpn() {
        String firstId = null;
        String secondId = "";

        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, firstId);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_ONE, secondId);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdAndSameType_create_returnsSameSpn() {
        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withSameIdsAndDifferentTypes_create_returnsDifferentSpns() {
        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_ONE);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndSameTypes_create_returnsDifferentSpns() {
        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withDifferentIdsAndDifferentTypes_create_returnsDifferentSpns() {
        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidTypeAndSameId_create_returnsSameSpn() {
        int invalidType = -1;

        String first = SpnCreator.create(invalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(invalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndSameId_create_returnsSameSpn() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first = SpnCreator.create(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(secondInvalidType, TEST_DEVICE_ID_ONE);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void withInvalidTypesAndDifferentIds_create_returnsDifferentSpns() {
        int firstInvalidType = -1;
        int secondInvalidType = -2;

        String first = SpnCreator.create(firstInvalidType, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(secondInvalidType, TEST_DEVICE_ID_TWO);

        assertNotEquals(first, second);
    }

    @Test
    public void withInvalidType_create_returnsFallbackTypeInSpn() {
        String actual = SpnCreator.create(-1, TEST_DEVICE_ID_ONE);

        assertTrue(actual.startsWith("com.android.healthconnect.unknown."));
    }

    @Test
    public void withDifferentOrder_create_ignoresOrdering() {
        String first = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);
        String second = SpnCreator.create(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String third = SpnCreator.create(TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);

        String newFirst = SpnCreator.create(TEST_DEVICE_TYPE_TWO, TEST_DEVICE_ID_TWO);
        String newSecond = SpnCreator.create(TEST_DEVICE_TYPE_THREE, TEST_DEVICE_ID_THREE);
        String newThird = SpnCreator.create(TEST_DEVICE_TYPE_ONE, TEST_DEVICE_ID_ONE);

        assertEquals(first, newThird);
        assertEquals(second, newFirst);
        assertEquals(third, newSecond);
    }

    // When this test fails, the values from Device.VALID_TYPES and
    // SpnCreator.DEVICE_TYPE_TO_DISPLAY_NAME have diverged. Sync them to fix this test.
    @Test
    public void withMapping_deviceTypeToDisplayName_isComplete() {
        Set<Integer> spnDeviceTypes = SpnCreator.DEVICE_TYPE_TO_DISPLAY_NAME.keySet();

        assertEquals(VALID_TYPES, spnDeviceTypes);
    }
}
