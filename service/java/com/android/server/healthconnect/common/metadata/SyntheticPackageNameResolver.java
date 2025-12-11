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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.HealthConnectManager;
import android.health.connect.device.SyntheticPackageNameMatcher;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.device.DeviceDataProviderManager;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A wrapper class for masking and unmasking Synthetic Package Names (SPNs) that are used for
 * identifying data from physical devices. This includes the current device id returned by {@link
 * HealthConnectManager#getCurrentDeviceId()}.
 *
 * <p>This class handles the translation between canonical package names (used internally by the
 * system) and masked package names (exposed to external callers). The masking is caller-specific,
 * ensuring that different external callers receive different masked identifiers for the same
 * device, thereby protecting physical device identity.
 *
 * <p>See go/ddp-internals-package-names for the full design and {@link SyntheticPackageNameCreator}
 * for further details.
 *
 * @hide
 */
public class SyntheticPackageNameResolver {
    private final AppInfoHelper mAppInfoHelper;
    @Nullable private final DeviceDataProviderManager mDeviceDataProviderManager;

    public SyntheticPackageNameResolver(
            @NonNull AppInfoHelper appInfoHelper,
            @Nullable DeviceDataProviderManager deviceDataProviderManager) {
        mAppInfoHelper = appInfoHelper;
        mDeviceDataProviderManager = deviceDataProviderManager;
    }

    /**
     * Masks a SPN from an internal caller's context to its external representation.
     *
     * <p>If the package name is a canonical SPN, it is converted into a masked SPN specific to the
     * caller. Otherwise, the original package name is returned.
     *
     * @param packageName The package name (potentially canonical) to mask.
     * @param callingPackageName The package name of the caller requesting the operation.
     * @return The masked SPN if required by the calling context, otherwise the original.
     */
    @NonNull
    public String mask(@NonNull String packageName, @NonNull String callingPackageName)
            throws IllegalArgumentException {
        if (!requiresMasking(packageName)) {
            return packageName;
        }

        return SyntheticPackageNameCreator.createMasked(packageName, callingPackageName);
    }

    /**
     * Unmasks a SPN from an external caller's context to its internal representation.
     *
     * <p>If the package name is a masked SPN, it is converted back into its canonical SPN. In the
     * case that the masked SPN can not be resolved to a canonical one, the original masked name is
     * returned. For any other package names, the original name is returned as well.
     *
     * @param packageName The package name (potentially masked) to unmask.
     * @param callingPackageName The package name of the caller requesting the operation.
     * @return The canonical SPN if required by the calling context, otherwise the original.
     */
    @NonNull
    public String unmask(@NonNull String packageName, @NonNull String callingPackageName)
            throws NoSuchElementException {
        if (!requiresUnmasking(packageName)) {
            return packageName;
        }

        Optional<String> canonicalName =
                getAllPackageNames().stream()
                        .filter(SyntheticPackageNameMatcher::matchesCanonical)
                        .filter(
                                canonicalSpn ->
                                        Objects.equals(
                                                SyntheticPackageNameCreator.createMasked(
                                                        canonicalSpn, callingPackageName),
                                                packageName))
                        .findFirst();

        // The given SPN might be the current device id - try to match the masked name with the
        // runtime id which is not in persisted storage but in cache
        if (canonicalName.isEmpty()
                && mDeviceDataProviderManager != null
                && Objects.equals(
                        SyntheticPackageNameCreator.createMasked(
                                mDeviceDataProviderManager.getCurrentDeviceId(),
                                callingPackageName),
                        packageName)) {
            canonicalName = Optional.of(mDeviceDataProviderManager.getStableCurrentDeviceId());
        }

        return canonicalName.orElse(packageName);
    }

    private List<String> getAllPackageNames() {
        return mAppInfoHelper.getAppInfoMap().keySet().stream().toList();
    }

    private static boolean requiresUnmasking(@NonNull String packageName) {
        return AconfigFlagHelper.isDeviceDataProvidersEnabled()
                && SyntheticPackageNameMatcher.matchesMasked(packageName);
    }

    private static boolean requiresMasking(@NonNull String packageName) {
        return AconfigFlagHelper.isDeviceDataProvidersEnabled()
                && SyntheticPackageNameMatcher.matchesCanonical(packageName);
    }
}
