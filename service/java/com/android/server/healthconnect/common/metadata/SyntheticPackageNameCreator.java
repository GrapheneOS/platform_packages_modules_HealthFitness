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

import static android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN;
import static android.health.connect.device.SyntheticPackageNameMatcher.DEVICE_TYPE_TO_DISPLAY_NAME;
import static android.health.connect.device.SyntheticPackageNameMatcher.UUID_HEX_LENGTH;
import static android.health.connect.device.SyntheticPackageNameMatcher.matchesCanonical;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Device.DeviceType;
import android.health.connect.device.SyntheticPackageNameMatcher;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Creates and validates Synthetic Package Names (SPNs) for Device Data Providers (DDP).
 *
 * <p>Health Connect requires a unique package name for every distinct data source. Since physical
 * devices (like watches or scales) do not have inherent Android package names, this utility
 * generates unique, stable, and valid identifiers that function as package names within Health
 * Connect.
 *
 * <p>See go/ddp-internals-package-names for the full design.
 *
 * <h2>Canonical vs. Masked SPNs</h2>
 *
 * To prevent cross-application tracking using stable device identifiers (a privacy requirement),
 * this utility generates two types of SPNs:
 *
 * <ul>
 *   <li><b>Canonical SPN:</b> The internal, stable identifier representing the physical device.
 *       This is stored in the Health Connect database.
 *   <li><b>Masked SPN:</b> An application-scoped identifier. This is what reading applications see
 *       when querying data (e.g., via DataOrigin). Each application receives a unique Masked SPN
 *       for the same underlying Canonical SPN.
 * </ul>
 *
 * <h2>Properties of SPNs</h2>
 *
 * <ul>
 *   <li><b>Deterministic and Reproducible:</b> For a specific user and device state, the same input
 *       will consistently produce the identical SPN. For canonical SPNs, this process relies on a
 *       per-user random salt.
 *       <p><b>Note:</b> This salt is regenerated during a factory reset. Consequently, SPNs
 *       generated after a factory reset will differ from those generated before it, even with the
 *       same inputs.
 *   <li><b>Stateful vs. Stateless Creation:</b> The creation differs based on the SPN type:
 *       <ul>
 *         <li><i>Canonical SPN</i> creation is <b>stateful</b>; creation depends on the salt stored
 *             persistently in a user's preferences.
 *         <li><i>Masked SPN</i> creation is <b>stateless</b>; creation relies solely on the inputs
 *             provided at the time of creation.
 *       </ul>
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
 * <p>Example Canonical SPN: {@code
 * com.android.healthconnect.phone.d73dd75dfbc2a4707a801f13199e0d984}
 *
 * <p>Example Masked SPN: {@code com.android.healthconnect.phone.j2e5fda2c648545328e401e627cd41ae9}
 *
 * <h3>DeviceTypeSegment</h3>
 *
 * This segment is derived from {@code @DeviceType}. A fixed, internal mapping ({@link
 * SyntheticPackageNameMatcher#DEVICE_TYPE_TO_DISPLAY_NAME}) is used to determine the displayed
 * name. Unknown types will fallback to using {@code DEVICE_TYPE_UNKNOWN}.
 *
 * <h3>UUID Segment and Prefixes</h3>
 *
 * This segment provides uniqueness and scoping. It is generated deterministically. A prefix
 * character ensures the segment starts with a letter and identifies the SPN type:
 *
 * <ul>
 *   <li>'d' ({@link #CANONICAL_UUID_SEGMENT_PREFIX}): Canonical SPN
 *   <li>'j' ({@link #MASKED_UUID_SEGMENT_PREFIX}): Masked SPN
 * </ul>
 *
 * <p>The implementation uses {@link UUID#nameUUIDFromBytes(byte[])}, which generates a **UUID
 * Version 3** (MD5 hash-based) identifier. Inputs are converted to bytes using UTF-8 encoding.
 * Hyphens are removed from the UUID to adhere to the Android package guidelines.
 *
 * <p>The seeds differ based on the type:
 *
 * <ul>
 *   <li><b>Canonical:</b> Seeded by a combination of {@code deviceId} and {@code salt}.
 *   <li><b>Masked:</b> Seeded by a combination of the Canonical SPN's UUID segment and the {@code
 *       callingPackageName}.
 * </ul>
 *
 * <h2>Uniqueness Guarantee and Limitations</h2>
 *
 * Uniqueness is dependent from the client providing a unique and stable {@code deviceId} for each
 * distinct physical device. If two different physical devices are registered with the identical
 * {@code deviceType} and {@code deviceId}, they will receive the same Canonical SPN, and their data
 * will be merged into a single source within Health Connect.
 *
 * @hide
 */
public class SyntheticPackageNameCreator {

    @VisibleForTesting
    public static final String SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY =
            "synthetic_package_name_salt";

    // Add a unique separator between, so that "foo" + "bar and "f" + "oobar" don't create
    // equal strings
    private static final char UNIQUE_SEPARATOR = '\u001F';

    // The standard Android reverse-DNS prefix used for all SPNs.
    private static final String PACKAGE_PREFIX = "com.android.healthconnect";

    // A fixed prefix is prepended to the UUID segment to ensure the segment starts with a letter,
    // complying with Android package name rules. The two prefixes differentiate the types.
    private static final char CANONICAL_UUID_SEGMENT_PREFIX = 'd';
    private static final char MASKED_UUID_SEGMENT_PREFIX = 'j';

    private static final @DeviceType int FALLBACK_DEVICE_TYPE = DEVICE_TYPE_UNKNOWN;

    private final PreferenceHelper mPreferenceHelper;

    public SyntheticPackageNameCreator(PreferenceHelper preferenceHelper) {
        mPreferenceHelper = preferenceHelper;
    }

    /**
     * Generates a Masked (application-scoped) Synthetic Package Name derived from a Canonical SPN
     * and the reading application's package name.
     *
     * <p>This eliminates the risk of cross-app tracking via canonical SPN UUIDs.
     *
     * @param canonicalSpn The internal SPN generated by {@link #createCanonical}.
     * @param callingPackageName The package name of the application requesting the data. If {@code
     *     null}, an empty string is used for scoping.
     * @return The generated Masked Synthetic Package Name.
     * @throws IllegalArgumentException if the provided {@code canonicalSpn} is not a valid
     *     Canonical SPN.
     */
    @NonNull
    public static String createMasked(
            @NonNull String canonicalSpn, @Nullable String callingPackageName)
            throws IllegalArgumentException {
        if (!matchesCanonical(canonicalSpn)) {
            throw new IllegalArgumentException(
                    ("Invalid SPN format: %s. Check SyntheticPackageNameCreator documentation "
                                    + "for reference.")
                            .formatted(canonicalSpn));
        }

        int prefixLength = canonicalSpn.length() - UUID_HEX_LENGTH;
        String canonicalDeviceId = canonicalSpn.substring(prefixLength);
        String maskedSeed = canonicalDeviceId + UNIQUE_SEPARATOR + callingPackageName;

        return canonicalSpn.substring(0, prefixLength - 1) // -1 to exclude the 'd' prefix
                + MASKED_UUID_SEGMENT_PREFIX
                + getNormalizedUuid(maskedSeed);
    }

    /**
     * Generates a Canonical (internal) Synthetic Package Name based on the device type, a unique
     * device ID, and a random, per-user persisted salt stored in preferences.
     *
     * <p>This identifier is stable and reproducible, serving as the primary key for the device
     * within the Health Connect database.
     *
     * @param deviceType The type of the device (e.g., {@code DEVICE_TYPE_WATCH}).
     * @param deviceId A unique, stable identifier for the physical device (e.g., serial number, MAC
     *     address). If {@code null}, an empty string is used as the seed.
     * @return The generated Canonical Synthetic Package Name.
     */
    @NonNull
    public String createCanonical(@DeviceType int deviceType, @Nullable String deviceId) {
        String deviceSegment =
                DEVICE_TYPE_TO_DISPLAY_NAME.getOrDefault(
                        deviceType, DEVICE_TYPE_TO_DISPLAY_NAME.get(FALLBACK_DEVICE_TYPE));
        String saltedDeviceId =
                (deviceId == null ? "" : deviceId) + UNIQUE_SEPARATOR + initializeOrGetSalt();
        String uuidSegment = CANONICAL_UUID_SEGMENT_PREFIX + getNormalizedUuid(saltedDeviceId);

        return PACKAGE_PREFIX + "." + deviceSegment + "." + uuidSegment;
    }

    /**
     * Initializes or retrieves the persisted salt for the current user. This ensures that stable
     * identifiers can be generated that change upon factory resets.
     */
    @NonNull
    public synchronized String initializeOrGetSalt() {
        String salt = mPreferenceHelper.getPreference(SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY);
        if (salt == null) {
            salt = UUID.randomUUID().toString();
            mPreferenceHelper.insertOrReplacePreference(
                    SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY, salt);
        }
        return salt;
    }

    /**
     * Generates a deterministic UUIDv3 from a seed string and normalizes it to a 32-character
     * lowercase hex string.
     *
     * @param seed The input string. If null, treated as an empty byte array.
     * @return The normalized UUID string.
     */
    @NonNull
    private static String getNormalizedUuid(@Nullable String seed) {
        byte[] seedBytes = seed == null ? new byte[0] : seed.getBytes(StandardCharsets.UTF_8);
        UUID seededUuid = UUID.nameUUIDFromBytes(seedBytes);
        return seededUuid.toString().replace("-", "");
    }
}
