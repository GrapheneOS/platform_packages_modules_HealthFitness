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

package java.com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.datatypehelpers.ActiveCaloriesBurnedRecordHelper.ACTIVE_CALORIES_BURNED_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ActiveCaloriesBurnedRecordHelper.ENERGY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.BasalMetabolicRateRecordHelper.BASAL_METABOLIC_RATE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.BasalMetabolicRateRecordHelper.BASAL_METABOLIC_RATE_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.InstantRecordHelper.TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.START_TIME_COLUMN_NAME;

import android.annotation.NonNull;
import android.database.Cursor;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.DeriveBasalCaloriesBurnedHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MergeDataHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Helper class for deriving TotalCaloriesBurned aggregate from {@link
 * android.health.connect.datatypes.BasalMetabolicRateRecord} and {@link
 * android.health.connect.datatypes.ActiveCaloriesBurnedRecord}
 *
 * @hide
 */
public final class DeriveTotalCaloriesBurnedHelper {
    private final long mStartTime;
    private final long mEndTime;
    private final List<Long> mPriority;
    private Cursor mActiveCaloriesBurnedCursor;
    private Cursor mBasalCaloriesBurnedCursor;
    private MergeDataHelper mMergeDataHelper;
    private DeriveBasalCaloriesBurnedHelper mBasalCaloriesBurnedHelper;

    public DeriveTotalCaloriesBurnedHelper(
            long startTime, long endTime, @NonNull List<Long> priorityList) {
        Objects.requireNonNull(priorityList);
        mStartTime = startTime;
        mEndTime = endTime;
        mPriority = priorityList;
        inititalizeCursors();
    }

    private void inititalizeCursors() {
        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        mActiveCaloriesBurnedCursor =
                transactionManager.read(
                        new ReadTableRequest(ACTIVE_CALORIES_BURNED_RECORD_TABLE_NAME)
                                .setColumnNames(Arrays.asList(ENERGY_COLUMN_NAME))
                                .setWhereClause(
                                        new WhereClauses()
                                                .addWhereBetweenTimeClause(
                                                        START_TIME_COLUMN_NAME,
                                                        mStartTime,
                                                        mEndTime)));
        mBasalCaloriesBurnedCursor =
                transactionManager.read(
                        new ReadTableRequest(BASAL_METABOLIC_RATE_RECORD_TABLE_NAME)
                                .setWhereClause(
                                        new WhereClauses()
                                                .addWhereBetweenTimeClause(
                                                        TIME_COLUMN_NAME, mStartTime, mEndTime)));
        mMergeDataHelper =
                new MergeDataHelper(
                        mActiveCaloriesBurnedCursor, mPriority, ENERGY_COLUMN_NAME, Double.class);
        mBasalCaloriesBurnedHelper =
                new DeriveBasalCaloriesBurnedHelper(
                        mBasalCaloriesBurnedCursor, BASAL_METABOLIC_RATE_COLUMN_NAME);
    }

    /** Close the cursors created */
    public void closeCursors() {
        if (mActiveCaloriesBurnedCursor != null) {
            mActiveCaloriesBurnedCursor.close();
        }
        if (mBasalCaloriesBurnedCursor != null) {
            mBasalCaloriesBurnedCursor.close();
        }
    }

    /**
     * Calculates and returns total derived calories for the empty interval time gaps where there is
     * no entry in {@link android.health.connect.datatypes.TotalCaloriesBurnedRecord}
     */
    public double getDerivedCalories(List<Pair<Instant, Instant>> emptyIntervalList) {
        double totalDerivedCalories = 0.0;
        for (int i = 0; i < emptyIntervalList.size(); i++) {
            long intervalStartTime = emptyIntervalList.get(i).first.toEpochMilli();
            long intervalEndTime = emptyIntervalList.get(i).second.toEpochMilli();
            totalDerivedCalories +=
                    mMergeDataHelper.readCursor(intervalStartTime, intervalEndTime)
                            + mBasalCaloriesBurnedHelper.getBasalCaloriesBurned(
                                    intervalStartTime, intervalEndTime);
        }
        return totalDerivedCalories;
    }
}
