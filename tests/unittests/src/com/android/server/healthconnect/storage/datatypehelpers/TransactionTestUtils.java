/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper.PACKAGE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper.UNIQUE_COLUMN_INFO;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.BloodPressureRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTransactionRequest;


import java.util.List;

/** Util class provides shared functionality for db transaction testing. */
public final class TransactionTestUtils {
    private final TransactionManager mTransactionManager;
    private final Context mContext;

    public TransactionTestUtils(Context context, TransactionManager transactionManager) {
        mContext = context;
        mTransactionManager = transactionManager;
    }

    public void insertApp(String packageName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PACKAGE_COLUMN_NAME, packageName);
        mTransactionManager.insert(
                new UpsertTableRequest(
                        AppInfoHelper.TABLE_NAME, contentValues, UNIQUE_COLUMN_INFO));
        assertThat(AppInfoHelper.getInstance().getAppInfoId(packageName)).isEqualTo(1);
    }

    public List<String> insertRecords(RecordInternal<?>... records) {
        return insertRecords(List.of(records));
    }

    public List<String> insertRecords(List<RecordInternal<?>> records) {
        return mTransactionManager.insertAll(
                new UpsertTransactionRequest(
                        "package.name",
                        records,
                        mContext,
                        /* isInsertRequest= */ true,
                        /* skipPackageNameAndLogs= */ false));
    }

    public static RecordInternal<StepsRecord> createStepsRecord(
            long startTimeMillis, long endTimeMillis, int stepsCount) {
        return new StepsRecordInternal()
                .setCount(stepsCount)
                .setStartTime(startTimeMillis)
                .setEndTime(endTimeMillis);
    }

    public static RecordInternal<BloodPressureRecord> createBloodPressureRecord(
            long timeMillis, double systolic, double diastolic) {
        return new BloodPressureRecordInternal()
                .setSystolic(systolic)
                .setDiastolic(diastolic)
                .setTime(timeMillis);
    }
}
