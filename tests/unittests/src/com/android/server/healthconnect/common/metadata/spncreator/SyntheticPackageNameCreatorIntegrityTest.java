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

import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;

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
public class SyntheticPackageNameCreatorIntegrityTest {

    private @DeviceType int mDeviceType;

    private String mDeviceId;

    private String mExpectedCanonicalSpn;

    private String mCallingPackage;

    private String mExpectedMaskedSpn;

    private static Collection<Object[]> getParams() {
        return Arrays.asList(
                new Object[][] {
                    {
                        DEVICE_TYPE_UNKNOWN,
                        "Lorem",
                        "com.android.healthconnect.unknown.ddb6ff2ffe2df3b8cbc0d9542bdce27dc",
                        "Sed",
                        "com.android.healthconnect.unknown.j9d045768e8223bef94a8bf9f73ed3d02",
                    },
                    {
                        DEVICE_TYPE_WATCH,
                        "ipsum",
                        "com.android.healthconnect.watch.de78f5438b48b39bcbdea61b73679449d",
                        "ut",
                        "com.android.healthconnect.watch.j8da3f17e1ed33b5eaf98448eb09541ca",
                    },
                    {
                        DEVICE_TYPE_PHONE,
                        "dolor",
                        "com.android.healthconnect.phone.da98931d104a73b8fb0450547d97e7ca5",
                        "perspiciatis",
                        "com.android.healthconnect.phone.j3ff65c1ded2e30f4aba4dc3b8d6bf17a",
                    },
                    {
                        DEVICE_TYPE_SCALE,
                        "sit",
                        "com.android.healthconnect.scale.d87d4eeb7dec7386490748d174c0e0a11",
                        "unde",
                        "com.android.healthconnect.scale.jad09fb44f0c4342486a640b98f7981dc",
                    },
                    {
                        DEVICE_TYPE_RING,
                        "amet",
                        "com.android.healthconnect.ring.d7f9a983a540e30939a69382161bdd265",
                        "omnis",
                        "com.android.healthconnect.ring.j1936c3c879ab3f5e86c376f33dcd2d24",
                    },
                    {
                        DEVICE_TYPE_HEAD_MOUNTED,
                        "consectetur",
                        "com.android.healthconnect.head_mounted.d4c480b2170d036b2af6f98af80902ce0",
                        "iste",
                        "com.android.healthconnect.head_mounted.j1856a3758e7335a1aac56d4f900b28b9",
                    },
                    {
                        DEVICE_TYPE_FITNESS_BAND,
                        "adipiscing",
                        "com.android.healthconnect.fitness_band.dd540f9a8003e31e089342a40200192ea",
                        "natus",
                        "com.android.healthconnect.fitness_band.j51ed1beec757320e87b41dd72ec8d367",
                    },
                    {
                        DEVICE_TYPE_CHEST_STRAP,
                        "elit",
                        "com.android.healthconnect.chest_strap.d4b77a01c91783f8f8c13fea642393955",
                        "error",
                        "com.android.healthconnect.chest_strap.j7fbecab5948a3e2fb17b36fd206fb11a",
                    },
                    {
                        DEVICE_TYPE_SMART_DISPLAY,
                        "sed",
                        "com.android.healthconnect.smart_display.d177544aa797a36f3a2f8caa5e80e7f24",
                        "vit",
                        "com.android.healthconnect.smart_display.j5b49ab289bee319aac83dcfde8cc7c1c",
                    },
                    {
                        DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE,
                        "do",
                        "com.android.healthconnect."
                                + "consumer_medical_device.dd4579b2688d635239f402f6b4b43bcbf",
                        "voluptatem",
                        "com.android.healthconnect."
                                + "consumer_medical_device.j57213303a1363f93be76968d33482ff3",
                    },
                    {
                        DEVICE_TYPE_GLASSES,
                        "eiusmod",
                        "com.android.healthconnect.glasses.d389ce233dac234259f17bdc8b78ba20f",
                        "accusantium",
                        "com.android.healthconnect.glasses.jac0137ffcfda3be68eb39f5f83e68838",
                    },
                    {
                        DEVICE_TYPE_HEARABLE,
                        "tempor",
                        "com.android.healthconnect.hearable.d4087a2d652673c108c9219d058745bc2",
                        "doloremque",
                        "com.android.healthconnect.hearable.j3cb1b16da0fb379cbfc877df5947136b",
                    },
                    {
                        DEVICE_TYPE_FITNESS_MACHINE,
                        "incididunt",
                        "com.android.healthconnect."
                                + "fitness_machine.d5c09603d80523880a2952fdb118c0b60",
                        "laudantium",
                        "com.android.healthconnect."
                                + "fitness_machine.j1f6a7602f022352ba3a84a74c0cbcf86",
                    },
                    {
                        DEVICE_TYPE_FITNESS_EQUIPMENT,
                        "ut",
                        "com.android.healthconnect."
                                + "fitness_equipment.db1a5d251fa4f35988b947ffc42b9cbed",
                        "totam",
                        "com.android.healthconnect."
                                + "fitness_equipment.j66b45553777a36dbb625703cab92d367",
                    },
                    {
                        DEVICE_TYPE_PORTABLE_COMPUTER,
                        "labore",
                        "com.android.healthconnect."
                                + "portable_computer.d1337d38747e639ed836548ae6cda7cc2",
                        "rem",
                        "com.android.healthconnect."
                                + "portable_computer.j146cfae9b1673de4bd5e50b42e76fda9",
                    },
                    {
                        DEVICE_TYPE_METER,
                        "et",
                        "com.android.healthconnect.meter.d4de1b7a4dc5334a88c25ffb7cdb580ee",
                        "aperiam",
                        "com.android.healthconnect.meter.j7cebbc056d1d3d07b5e3f2af9d738bc3",
                    },
                });
    }

    @Test
    public void withTypeAndId_createCanonical_staysConsistent() {
        for (var params : getParams()) {
            initializeRun(params);
            String actualSpn = SyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);

            assertEquals(mExpectedCanonicalSpn, actualSpn);
        }
    }

    @Test
    public void withCanonicalAndCallingPackage_createMasked_staysConsistent() {
        for (var params : getParams()) {
            initializeRun(params);
            String actualSpn =
                    SyntheticPackageNameCreator.createMasked(
                            mExpectedCanonicalSpn, mCallingPackage);

            assertEquals(mExpectedMaskedSpn, actualSpn);
        }
    }

    @Test
    public void withSpns_create_matchesChecksum() {
        long expectedChecksum = 2222836158L;

        StringBuilder ingestion = new StringBuilder();

        for (var params : getParams()) {
            initializeRun(params);
            ingestion.append(SyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId));
            ingestion.append('\u001F');
            ingestion.append(
                    SyntheticPackageNameCreator.createMasked(
                            mExpectedCanonicalSpn, mCallingPackage));
            ingestion.append('\u001F');
        }

        CRC32 crc = new CRC32();
        crc.update(ingestion.toString().getBytes());
        long actualChecksum = crc.getValue();

        assertEquals(expectedChecksum, actualChecksum);
    }

    private void initializeRun(Object[] params) {
        mDeviceType = (int) params[0];
        mDeviceId = (String) params[1];
        mExpectedCanonicalSpn = (String) params[2];
        mCallingPackage = (String) params[3];
        mExpectedMaskedSpn = (String) params[4];
    }
}
