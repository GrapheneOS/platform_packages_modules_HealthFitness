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

import static com.android.server.healthconnect.storage.datatypehelpers.InstantRecordHelper.TIME_COLUMN_NAME;

import android.annotation.NonNull;
import android.database.Cursor;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.util.Pair;

import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Helper class to Derive BasalCaloriesTotal aggregate
 *
 * @hide
 */
public final class DeriveBasalCaloriesBurnedHelper {
    public static final double WATT_TO_CAL_PER_HR = 860;
    public static final int HOURS_PER_DAY = 24;
    private final Cursor mCursor;
    private final String mColumnName;

    public DeriveBasalCaloriesBurnedHelper(@NonNull Cursor cursor, @NonNull String columnName) {
        Objects.requireNonNull(cursor);
        Objects.requireNonNull(columnName);
        mCursor = cursor;
        mColumnName = columnName;
    }

    /**
     * Calculates and returns aggregate of total basal calories burned from table {@link
     * BasalMetabolicRateRecord} for the interval.
     */
    @NonNull
    public double getBasalCaloriesBurned(long intervalStartTime, long intervalEndTime) {
        Instant currentGroupStartTime = Instant.ofEpochMilli(intervalStartTime);
        Instant currentGroupEndTime = Instant.ofEpochMilli(intervalEndTime);
        double currentGroupTotal = 0;
        boolean endOfCurrentGroup = false;
        // calculate aggregate for current interval between start and end time by iterating
        // cursor until current time is inside current group interval

        Instant currentStart = currentGroupStartTime;
        while (!endOfCurrentGroup && mCursor.moveToNext()) {
            Instant currentItemTime =
                    Instant.ofEpochMilli(StorageUtils.getCursorLong(mCursor, TIME_COLUMN_NAME));
            if (currentItemTime.isBefore(currentGroupStartTime)) {
                continue;
            }

            if (currentStart != null && currentStart.isBefore(currentItemTime)) {
                currentItemTime = currentStart;
                currentStart = null;
            }

            Instant nextItemTime;
            double rateOfEnergyBurntInWatts = StorageUtils.getCursorDouble(mCursor, mColumnName);
            if (mCursor.moveToNext()) {
                nextItemTime =
                        Instant.ofEpochMilli(StorageUtils.getCursorLong(mCursor, TIME_COLUMN_NAME));
                if (nextItemTime.isAfter(currentGroupEndTime)) {
                    endOfCurrentGroup = true;
                    nextItemTime = currentGroupEndTime;
                }
                mCursor.moveToPrevious();
            } else {
                endOfCurrentGroup = true;
                nextItemTime = currentGroupEndTime;
            }
            currentGroupTotal +=
                    getCurrentIntervalEnergy(
                            rateOfEnergyBurntInWatts, currentItemTime, nextItemTime);
        }
        return currentGroupTotal;
    }

    /**
     * Calculates and returns an array of aggregate of total basal calories burned from table {@link
     * BasalMetabolicRateRecord} for group of intervals.
     */
    public double[] getBasalCaloriesBurned(@NonNull List<Pair<Long, Long>> groupIntervalList) {
        double[] basalCaloriesBurned = new double[groupIntervalList.size()];
        for (int group = 0; group < groupIntervalList.size(); group++) {
            basalCaloriesBurned[group] =
                    getBasalCaloriesBurned(
                            groupIntervalList.get(group).first,
                            groupIntervalList.get(group).second);
        }
        return basalCaloriesBurned;
    }

    private double getCurrentIntervalEnergy(
            double rateOfEnergyBurntInWatts, Instant currentItemTime, Instant nextItemTime) {
        return getCalPerDay(rateOfEnergyBurntInWatts)
                * Duration.between(currentItemTime, nextItemTime).toMillis()
                / Duration.ofDays(1).toMillis();
    }

    private double getCalPerDay(double rateOfEnergyBurntInWatt) {
        return rateOfEnergyBurntInWatt * HOURS_PER_DAY * WATT_TO_CAL_PER_HR;
    }
}
