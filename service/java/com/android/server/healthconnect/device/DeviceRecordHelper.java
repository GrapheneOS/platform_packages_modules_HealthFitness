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

import android.health.connect.internal.datatypes.RecordInternal;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;

import java.util.List;
import java.util.Set;

/**
 * Manages insertions of device level data.
 *
 * @hide
 */
public class DeviceRecordHelper {
    public static final String DEVICE_DATA_PROVIDER_PACKAGE = "android";
    private final FitnessRecordUpsertHelper mUpsertHelper;

    public DeviceRecordHelper(FitnessRecordUpsertHelper upsertHelper) {
        mUpsertHelper = upsertHelper;
    }

    /**
     * Inserts the provided records as device records (i.e. attributed to the 'android' package).
     */
    public void insertRecords(
            DeviceDataSource deviceDataSource, List<? extends RecordInternal<?>> records) {
        if (!Flags.stepTrackingEnabled()) {
            return;
        }
        addDeviceMetadataToRecords(deviceDataSource, records);

        // Treat all extra permissions as granted to pass any per-record checks.
        Set<String> grantedExtraWritePermissions = mUpsertHelper.getAllExtraWritePermissions();
        mUpsertHelper.insertRecords(
                DEVICE_DATA_PROVIDER_PACKAGE,
                records,
                grantedExtraWritePermissions,
                /* shouldGenerateAccessLogs= */ false);
    }

    private void addDeviceMetadataToRecords(
            DeviceDataSource deviceDataSource, List<? extends RecordInternal<?>> records) {
        // Note: for now (during development) we will just write the device metadata normally.
        // TODO(b/405980323) check if this DDP exists. If not, create it.
        // TODO(b/405980291) add ID_COLUMN_NAME and DISPLAY_NAME_COLUMN_NAME to DeviceInfoHelper
        for (RecordInternal<?> record : records) {
            record.setManufacturer(deviceDataSource.getManufacturer());
            record.setModel(deviceDataSource.getModel());
            record.setDeviceType(deviceDataSource.getDeviceType());
        }
    }
}
