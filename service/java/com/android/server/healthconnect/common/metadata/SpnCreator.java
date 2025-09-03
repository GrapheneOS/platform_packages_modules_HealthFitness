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

import static java.util.Map.entry;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Device.DeviceType;

import com.android.internal.annotations.VisibleForTesting;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Creates Synthetic Package Names (SPNs) for Device Data Providers (DDP).
 *
 * <p>Health Connect requires a unique package name for every distinct data source. Since physical
 * devices (like watches or scales) do not have inherent Android package names, this utility
 * generates unique, stable, and valid identifiers that function as package names within Health
 * Connect.
 *
 * <p>See go/ddp-internals-package-names for the full design.
 *
 * <h2>Properties of SPNs</h2>
 *
 * <ul>
 *   <li><b>Deterministic and Reproducible:</b> The same input {@code deviceType} and {@code
 *       deviceId} will always produce the identical SPN across different systems.
 *   <li><b>Stateless:</b> Generation does not rely on stored state.
 *   <li><b>Valid:</b> SPNs conform to Android package name syntax requirements, see <a
 *       href="https://developer.android.com/guide/topics/manifest/manifest-element.html#package">Android
 *       developer doc</a>
 * </ul>
 *
 * <h2>Structure</h2>
 *
 * The SPN format is: {@code com.android.healthconnect.ddp.<DeviceTypeSegment>.<Prefix
 * Character><UUID>}
 *
 * <p>Example: {@code com.android.healthconnect.ddp.phone.d73dd75dfbc2a4707a801f13199e0d984}
 *
 * <h3>DeviceTypeSegment</h3>
 *
 * This segment is derived from {@code @DeviceType}. A fixed, internal mapping ({@link
 * #DEVICE_TYPE_TO_DISPLAY_NAME}) is used to determine the displayed name. Unknown types will
 * fallback to using {@code DEVICE_TYPE_UNKNOWN}.
 *
 * <h3>UUID Segment</h3>
 *
 * This segment provides uniqueness. It is generated deterministically using the {@code deviceId} as
 * the seed.
 *
 * <p>The implementation uses {@link UUID#nameUUIDFromBytes(byte[])}, which generates a **UUID
 * Version 3** (MD5 hash-based) identifier. The input {@code deviceId} string is converted to bytes
 * using UTF-8 encoding. Hyphens are removed from the UUID to adhere to the Android package
 * guidelines.
 *
 * <h2>Uniqueness Guarantee and Limitations</h2>
 *
 * Uniqueness is dependent from the client providing a unique and stable {@code deviceId} for each
 * distinct physical device. If two different physical devices are registered with the identical
 * {@code deviceType} and {@code deviceId}, they will receive the same SPN, and their data will be
 * merged into a single source within Health Connect.
 *
 * @hide
 */
public class SpnCreator {

    private static final String PACKAGE_PREFIX = "com.android.healthconnect";

    // A fixed prefix is prepended to the UUID segment to ensure the segment starts with a letter,
    // complying with Android package name rules
    private static final char UUID_SEGMENT_PREFIX = 'd';

    private static final @DeviceType int FALLBACK_DEVICE_TYPE = DEVICE_TYPE_UNKNOWN;

    /**
     * A fixed, immutable mapping from {@code @DeviceType} to their corresponding string
     * representations used in SPNs. This mapping MUST NOT change to guarantee the stability and
     * reproducibility of SPNs across versions.
     */
    @VisibleForTesting
    public static final Map<Integer, String> DEVICE_TYPE_TO_DISPLAY_NAME =
            Map.ofEntries(
                    entry(DEVICE_TYPE_UNKNOWN, "unknown"),
                    entry(DEVICE_TYPE_WATCH, "watch"),
                    entry(DEVICE_TYPE_PHONE, "phone"),
                    entry(DEVICE_TYPE_SCALE, "scale"),
                    entry(DEVICE_TYPE_RING, "ring"),
                    entry(DEVICE_TYPE_HEAD_MOUNTED, "head_mounted"),
                    entry(DEVICE_TYPE_FITNESS_BAND, "fitness_band"),
                    entry(DEVICE_TYPE_CHEST_STRAP, "chest_strap"),
                    entry(DEVICE_TYPE_SMART_DISPLAY, "smart_display"),
                    entry(DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE, "consumer_medical_device"),
                    entry(DEVICE_TYPE_GLASSES, "glasses"),
                    entry(DEVICE_TYPE_HEARABLE, "hearable"),
                    entry(DEVICE_TYPE_FITNESS_MACHINE, "fitness_machine"),
                    entry(DEVICE_TYPE_FITNESS_EQUIPMENT, "fitness_equipment"),
                    entry(DEVICE_TYPE_PORTABLE_COMPUTER, "portable_computer"),
                    entry(DEVICE_TYPE_METER, "meter"));

    /**
     * Generates a Synthetic Package Name based on the device type and a unique device ID.
     *
     * @param deviceType The type of the device (e.g., {@code DEVICE_TYPE_WATCH}).
     * @param deviceId A unique, stable identifier for the physical device (e.g., serial number, MAC
     *     address).
     * @return The generated Synthetic Package Name.
     */
    @NonNull
    public static String create(@DeviceType int deviceType, @Nullable String deviceId) {
        String deviceSegment =
                DEVICE_TYPE_TO_DISPLAY_NAME.getOrDefault(
                        deviceType, DEVICE_TYPE_TO_DISPLAY_NAME.get(FALLBACK_DEVICE_TYPE));

        return PACKAGE_PREFIX + "." + deviceSegment + "." + getUuidSegment(deviceId);
    }

    @NonNull
    private static String getUuidSegment(@Nullable String seed) {
        byte[] seedBytes = seed == null ? new byte[0] : seed.getBytes(StandardCharsets.UTF_8);
        UUID seededUuid = UUID.nameUUIDFromBytes(seedBytes);
        String normalizedUuid = seededUuid.toString().replace("-", "");
        return UUID_SEGMENT_PREFIX + normalizedUuid;
    }
}
