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

import static android.health.connect.datatypes.Device.DEVICE_TYPE_CHEST_STRAP;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_BAND;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_EQUIPMENT;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_MACHINE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_GLASSES;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_HEAD_MOUNTED;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_HEARABLE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_METER;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PORTABLE_COMPUTER;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_RING;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_SCALE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_SMART_DISPLAY;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;

import static org.junit.Assert.assertEquals;

import android.health.connect.datatypes.Device.DeviceType;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SpnCreator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.zip.CRC32;

/**
 * The following tests are to ensure that existing SPNs don't change in the future. Once records are
 * inserted, we MUST ensure that devices keep writing to the same source. In one of these tests fail
 * against the assertions, DO NOT change the expected values.
 */
@RunWith(AndroidJUnit4.class)
public class SpnCreatorIntegrityTest {

    private @DeviceType int mDeviceType;

    private String mDeviceId;

    private String mExpectedSpn;

    private long mExpectedChecksum;

    private static Collection<Object[]> getParams() {
        return Arrays.asList(
                new Object[][] {
                    {
                        DEVICE_TYPE_UNKNOWN,
                        "Lorem",
                        "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc",
                        1832637465L
                    },
                    {
                        DEVICE_TYPE_WATCH,
                        "ipsum",
                        "com.android.healthconnect.watch.de78f5438b48b39bcbdea61b73679449d",
                        1218186228L
                    },
                    {
                        DEVICE_TYPE_PHONE,
                        "dolor",
                        "com.android.healthconnect.phone.da98931d104a73b8fb0450547d97e7ca5",
                        1776413148L
                    },
                    {
                        DEVICE_TYPE_SCALE,
                        "sit",
                        "com.android.healthconnect.scale.d87d4eeb7dec7386490748d174c0e0a11",
                        3504894597L
                    },
                    {
                        DEVICE_TYPE_RING,
                        "amet",
                        "com.android.healthconnect.ring.d7f9a983a540e30939a69382161bdd265",
                        3498739830L
                    },
                    {
                        DEVICE_TYPE_HEAD_MOUNTED,
                        "consectetur",
                        "com.android.healthconnect.head_mounted.d4c480b2170d036b2af6f98af80902ce0",
                        754393699L
                    },
                    {
                        DEVICE_TYPE_FITNESS_BAND,
                        "adipiscing",
                        "com.android.healthconnect.fitness_band.dd540f9a8003e31e089342a40200192ea",
                        1666983534L
                    },
                    {
                        DEVICE_TYPE_CHEST_STRAP,
                        "elit",
                        "com.android.healthconnect.chest_strap.d4b77a01c91783f8f8c13fea642393955",
                        3749846843L
                    },
                    {
                        DEVICE_TYPE_SMART_DISPLAY,
                        "sed",
                        "com.android.healthconnect.smart_display.d177544aa797a36f3a2f8caa5e80e7f24",
                        3566592729L
                    },
                    {
                        DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE,
                        "do",
                        "com.android.healthconnect."
                                + "consumer_medical_device.dd4579b2688d635239f402f6b4b43bcbf",
                        3775193950L
                    },
                    {
                        DEVICE_TYPE_GLASSES,
                        "eiusmod",
                        "com.android.healthconnect.glasses.d389ce233dac234259f17bdc8b78ba20f",
                        398380681L
                    },
                    {
                        DEVICE_TYPE_HEARABLE,
                        "tempor",
                        "com.android.healthconnect.hearable.d4087a2d652673c108c9219d058745bc2",
                        95356798L
                    },
                    {
                        DEVICE_TYPE_FITNESS_MACHINE,
                        "incididunt",
                        "com.android.healthconnect."
                                + "fitness_machine.d5c09603d80523880a2952fdb118c0b60",
                        2233592519L
                    },
                    {
                        DEVICE_TYPE_FITNESS_EQUIPMENT,
                        "ut",
                        "com.android.healthconnect."
                                + "fitness_equipment.db1a5d251fa4f35988b947ffc42b9cbed",
                        861365113L
                    },
                    {
                        DEVICE_TYPE_PORTABLE_COMPUTER,
                        "labore",
                        "com.android.healthconnect."
                                + "portable_computer.d1337d38747e639ed836548ae6cda7cc2",
                        721476133L
                    },
                    {
                        DEVICE_TYPE_METER,
                        "et",
                        "com.android.healthconnect.meter.d4de1b7a4dc5334a88c25ffb7cdb580ee",
                        307756507L
                    },
                });
    }

    @Test
    public void withTypeAndId_create_staysConsistent() {
        for (var params : getParams()) {
            initializeRun(params);
            String actualSpn = SpnCreator.create(mDeviceType, mDeviceId);

            assertEquals(mExpectedSpn, actualSpn);
        }
    }

    @Test
    public void withTypeAndId_create_matchesChecksums() {
        for (var params : getParams()) {
            initializeRun(params);
            String actualSpn = SpnCreator.create(mDeviceType, mDeviceId);

            CRC32 crc = new CRC32();
            crc.update(actualSpn.getBytes());
            long actualChecksum = crc.getValue();

            assertEquals(mExpectedChecksum, actualChecksum);
        }
    }

    private void initializeRun(Object[] params) {
        mDeviceType = (int) params[0];
        mDeviceId = (String) params[1];
        mExpectedSpn = (String) params[2];
        mExpectedChecksum = (long) params[3];
    }
}
