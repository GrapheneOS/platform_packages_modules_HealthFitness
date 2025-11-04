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
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;

/**
 * A {@link DeviceDataProviderManager} that overrides the serial number of the device to a fake
 * value.
 *
 * <p>This is designed to be used in testing, so that {@link android.os.Build#getSerial} doesn't
 * need to be called in a context where the permission is not present.
 */
public class FakeSerialDeviceDataProviderManager extends DeviceDataProviderManager {
    /** The string that will be returned as the serial number. */
    public static final String TEST_SERIAL_NUMBER = "TEST_SERIAL_NUMBER";

    public FakeSerialDeviceDataProviderManager(
            @NonNull Context context,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataSourcesHelper deviceDataSourcesHelper,
            @NonNull DeviceDataProviderMetadataHelper deviceDataProviderMetadataHelper,
            @NonNull FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            @NonNull FitnessRecordReadHelper fitnessRecordReadHelper,
            @NonNull SyntheticPackageNameCreator syntheticPackageNameCreator) {
        super(
                context,
                deviceInfoHelper,
                appInfoHelper,
                deviceDataSourcesHelper,
                deviceDataProviderMetadataHelper,
                fitnessRecordUpsertHelper,
                fitnessRecordReadHelper,
                syntheticPackageNameCreator);
    }

    @Override
    String getSerial() {
        return TEST_SERIAL_NUMBER;
    }
}
