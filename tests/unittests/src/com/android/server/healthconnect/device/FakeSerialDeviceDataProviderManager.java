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
package com.android.server.healthconnect.device;

import android.annotation.NonNull;
import android.content.Context;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;

/**
 * A {@link DeviceDataProviderManager} that overrides the serial number of the device to a fake
 * value and optionally ignores caller permission configuration.
 *
 * <p>This is designed to be used in testing, so that {@link android.os.Build#getSerial} doesn't
 * need to be called in a context where the permission is not present.
 *
 * <p>Optionally, {@code ignoresActionConfiguration} can be set in the ctor to true to bypass action
 * configurations checks when advertising.
 */
public class FakeSerialDeviceDataProviderManager extends DeviceDataProviderManager {
    /** The string that will be returned as the serial number. */
    public static final String TEST_SERIAL_NUMBER = "TEST_SERIAL_NUMBER";

    private final boolean mIgnoresActionConfiguration;

    public FakeSerialDeviceDataProviderManager(
            @NonNull Context context,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataSourceHelper deviceDataSourceHelper,
            @NonNull DeviceDataSourcesHelper deviceDataSourcesHelper,
            @NonNull DeviceDataProviderMetadataHelper deviceDataProviderMetadataHelper,
            @NonNull FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            @NonNull FitnessRecordReadHelper fitnessRecordReadHelper,
            @NonNull FitnessRecordDeleteHelper fitnessRecordDeleteHelper,
            @NonNull SyntheticPackageNameCreator syntheticPackageNameCreator,
            @NonNull PreferenceHelper preferenceHelper,
            @NonNull HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            @NonNull InternalHealthConnectMappings internalHealthConnectMappings,
            boolean ignoresActionConfiguration) {
        super(
                context,
                deviceInfoHelper,
                appInfoHelper,
                deviceDataSourceHelper,
                deviceDataSourcesHelper,
                deviceDataProviderMetadataHelper,
                fitnessRecordUpsertHelper,
                fitnessRecordReadHelper,
                fitnessRecordDeleteHelper,
                syntheticPackageNameCreator,
                preferenceHelper,
                healthDataCategoryPriorityHelper,
                internalHealthConnectMappings);
        mIgnoresActionConfiguration = ignoresActionConfiguration;
    }

    @Override
    String getSerial() {
        return TEST_SERIAL_NUMBER;
    }

    @Override
    protected void validateDdpConfiguration(String packageName) {
        if (mIgnoresActionConfiguration) return;

        super.validateDdpConfiguration(packageName);
    }
}
