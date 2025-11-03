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
import static org.mockito.Mockito.when;

import android.health.connect.datatypes.Device.DeviceType;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

    private static final String MASKING_SALT = "hello, world";
    private @DeviceType int mDeviceType;

    private String mDeviceId;

    private String mExpectedCanonicalSpn;

    private String mCallingPackage;

    private String mExpectedMaskedSpn;

    private SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    @Mock PreferenceHelper mPreferenceHelper;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static Collection<Object[]> getParams() {
        return Arrays.asList(
                new Object[][] {
                    {
                        DEVICE_TYPE_UNKNOWN,
                        "Lorem",
                        "com.android.healthconnect.unknown.d8e655407e6e53b1599ebd3dce5eaffe8",
                        "Sed",
                        "com.android.healthconnect.unknown.j93788532bc583c57bfcdaf196d2a2737",
                    },
                    {
                        DEVICE_TYPE_WATCH,
                        "ipsum",
                        "com.android.healthconnect.watch.dae97b731dde83745b62b9111deee3456",
                        "ut",
                        "com.android.healthconnect.watch.jc45bd741a7123764b514e2c27df9fe41",
                    },
                    {
                        DEVICE_TYPE_PHONE,
                        "dolor",
                        "com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594",
                        "perspiciatis",
                        "com.android.healthconnect.phone.jd5bdd37e1a8d3667a05d0abebfc4a89e",
                    },
                    {
                        DEVICE_TYPE_SCALE,
                        "sit",
                        "com.android.healthconnect.scale.da92e471d92de356aa26b27a6fa8fccbe",
                        "unde",
                        "com.android.healthconnect.scale.j00e7797d540a3800867a1e33d194a303",
                    },
                    {
                        DEVICE_TYPE_RING,
                        "amet",
                        "com.android.healthconnect.ring.d1f88e36504ef3be5bae98df79f051292",
                        "omnis",
                        "com.android.healthconnect.ring.jf8a16fcc22f53d81ad63f162937d5550",
                    },
                    {
                        DEVICE_TYPE_HEAD_MOUNTED,
                        "consectetur",
                        "com.android.healthconnect.head_mounted.da56dc1beb9aa3d0390810b069718a221",
                        "iste",
                        "com.android.healthconnect.head_mounted.jc82409a2505a375abdd0ab3e21dfc8e5",
                    },
                    {
                        DEVICE_TYPE_FITNESS_BAND,
                        "adipiscing",
                        "com.android.healthconnect.fitness_band.decdd2c2752fb3cc980905c30a34710a6",
                        "natus",
                        "com.android.healthconnect.fitness_band.jad1c8aeb718931ada6cceed18c13c69b",
                    },
                    {
                        DEVICE_TYPE_CHEST_STRAP,
                        "elit",
                        "com.android.healthconnect.chest_strap.db382b5c536ab3a8b9bc730c24fb5aa91",
                        "error",
                        "com.android.healthconnect.chest_strap.j431aa8563dee313f80ac3df60f99c936",
                    },
                    {
                        DEVICE_TYPE_SMART_DISPLAY,
                        "sed",
                        "com.android.healthconnect.smart_display.dfdfeaa691eae39899ccffc44d0f107ea",
                        "vit",
                        "com.android.healthconnect.smart_display.j313d02356658300e99b0708b238f18f0",
                    },
                    {
                        DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE,
                        "do",
                        "com.android.healthconnect.consumer_medical_device"
                                + ".d1351dab026c9346581e09e0d89d06575",
                        "voluptatem",
                        "com.android.healthconnect."
                                + "consumer_medical_device.j29e8513581e334cc81aeb36e19516279",
                    },
                    {
                        DEVICE_TYPE_GLASSES,
                        "eiusmod",
                        "com.android.healthconnect.glasses.d6289604fb7a53e94be1ed2ff39a6d7ff",
                        "accusantium",
                        "com.android.healthconnect.glasses.j92b15f75c13a35eeb61084f568c7107d",
                    },
                    {
                        DEVICE_TYPE_HEARABLE,
                        "tempor",
                        "com.android.healthconnect.hearable.deae4d82fff843e0c9d04c763a4cd1d88",
                        "doloremque",
                        "com.android.healthconnect.hearable.jc98027fc84803f92ae1c4aa8b951b0c8",
                    },
                    {
                        DEVICE_TYPE_FITNESS_MACHINE,
                        "incididunt",
                        "com.android.healthconnect."
                                + "fitness_machine.d7092885843503461a4f3d943b94514d7",
                        "laudantium",
                        "com.android.healthconnect."
                                + "fitness_machine.j89d6e0b3a3b334cc85a29f758709406e",
                    },
                    {
                        DEVICE_TYPE_FITNESS_EQUIPMENT,
                        "ut",
                        "com.android.healthconnect."
                                + "fitness_equipment.ddd9d69cb88a538ce9b6419620472e80b",
                        "totam",
                        "com.android.healthconnect."
                                + "fitness_equipment.j78344c0db9cf345ca48df4c1b34d6111",
                    },
                    {
                        DEVICE_TYPE_PORTABLE_COMPUTER,
                        "labore",
                        "com.android.healthconnect."
                                + "portable_computer.db7c36ef46a37328790f8b04bd50d8dfb",
                        "rem",
                        "com.android.healthconnect."
                                + "portable_computer.j2cdf4c8a1cd0374f88f118b5d550f72b",
                    },
                    {
                        DEVICE_TYPE_METER,
                        "et",
                        "com.android.healthconnect.meter.d69a5ff98056135bcbe661f7ce21bfa5c",
                        "aperiam",
                        "com.android.healthconnect.meter.j94a54af32c6d32a79061a48b04d4342c",
                    },
                });
    }

    @Before
    public void setUp() throws Exception {
        when(mPreferenceHelper.getPreference(
                        SyntheticPackageNameCreator.SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY))
                .thenReturn(MASKING_SALT);

        mSyntheticPackageNameCreator = new SyntheticPackageNameCreator(mPreferenceHelper);
    }

    @Test
    public void withTypeAndId_createCanonical_staysConsistent() {
        for (var params : getParams()) {
            initializeRun(params);
            String actualSpn = mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId);

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
        long expectedChecksum = 3783683381L;

        StringBuilder ingestion = new StringBuilder();

        for (var params : getParams()) {
            initializeRun(params);
            ingestion.append(mSyntheticPackageNameCreator.createCanonical(mDeviceType, mDeviceId));
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
