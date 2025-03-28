/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_DOUBLE;

import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.LOCAL_DATE_TIME_END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.LOCAL_DATE_TIME_START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;

import android.database.Cursor;
import android.util.Pair;

import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TimeUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * A helper class to merge records from multiple apps with overlapping time interval based on app
 * priority for the record type.
 *
 * @hide
 */
public final class MergeDataHelper {
    /** Class to hold cursor entry for the Tree buffer window */
    private record RecordData(
            Instant startTime,
            Instant endTime,
            long lastModifiedTime,
            double value,
            int priority) {}

    private static final List<Class<?>> ALLOWED_COLUMN_TYPES = List.of(Long.class, Double.class);
    private static final Comparator<RecordData> PRIORITY_COMPARATOR =
            Comparator.comparing(RecordData::priority).thenComparing(RecordData::lastModifiedTime);
    private static final Comparator<RecordData> RECORD_DATA_COMPARATOR =
            Comparator.comparing(RecordData::startTime)
                    .thenComparing(PRIORITY_COMPARATOR.reversed());
    private final List<Long> mReversedPriorityList;
    private Instant mStartTime;
    private Instant mEndTime;
    private final String mColumnNameToMerge;
    private final Class<?> mValueColumnType;

    private final boolean mUseLocalTime;

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    public MergeDataHelper(
            List<Long> priorityList,
            String columnNameToMerge,
            Class<?> valueColumnType,
            boolean useLocalTime) {
        Objects.requireNonNull(priorityList);
        Objects.requireNonNull(columnNameToMerge);
        Objects.requireNonNull(valueColumnType);
        if (!ALLOWED_COLUMN_TYPES.contains(valueColumnType)) {
            throw new IllegalArgumentException("Unsupported column type" + valueColumnType);
        }
        // In priority list, the first element has the highest priority. To make it easier to
        // understand and code, reverse the list and use index as data points' priorities
        mReversedPriorityList = new ArrayList<>(priorityList);
        Collections.reverse(mReversedPriorityList);
        mColumnNameToMerge = columnNameToMerge;
        mValueColumnType = valueColumnType;
        mUseLocalTime = useLocalTime;
    }

    /**
     * Returns the aggregate sum for the records by iterating the cursor to form a buffer window by
     * eliminating overlapping records between the interval based on App priority
     *
     * <p>Example: App1 > App2 > App3 Before:App1 : T1-T2 -> value1, App2 : T1-T3 -> value2 , App3 :
     * T2-T4 -> value3
     *
     * <p>After merging overlapping data between T1-T4 below values will be taken from each app:
     *
     * <p>App1 : T1-T2 -> value1, App2 : T2-T3 -> value2*(T3-T2)/(T3-T1), App3 : T3-T4 ->
     * value3*(T4-T3)/(T4-T2)
     */
    public MergeResult readCursor(Cursor cursor, long startTime, long endTime) {
        TreeSet<RecordData> bufferWindow = new TreeSet<>(RECORD_DATA_COMPARATOR);
        mStartTime = Instant.ofEpochMilli(startTime);
        mEndTime = Instant.ofEpochMilli(endTime);
        List<RecordData> recordDataList = new ArrayList<>();
        cursor.moveToPosition(-1);
        while (true) {
            if (!bufferWindow.isEmpty()) {
                recordDataList.add(bufferWindow.pollFirst());
            }
            // Fill window with any raw data that overlaps with the first element of the
            // bufferWindow, in other words until window.first.end < window.last.start.
            while ((bufferWindow.size() < 2
                            || bufferWindow
                                    .last()
                                    .startTime()
                                    .isBefore(bufferWindow.first().endTime()))
                    && cursor.moveToNext()) {
                if (cursorOutOfRange(cursor)) {
                    continue;
                }
                RecordData recordData = getRecordData(cursor);
                if (recordData != null) {
                    bufferWindow.add(recordData);
                }
            }

            // End of the cursor and there is no data to process so exit
            if (bufferWindow.isEmpty()) {
                break;
            }
            // Trim window so window.first.end <= window.second.start.
            bufferWindow = eliminateEarliestRecordOverlaps(bufferWindow);
        }
        return new MergeResult(recordDataList);
    }

    private boolean cursorOutOfRange(Cursor cursor) {
        long cursorStartTime = StorageUtils.getCursorLong(cursor, startTimeColumnName());
        long cursorEndTime = StorageUtils.getCursorLong(cursor, endTimeColumnName());
        return (cursorStartTime < mStartTime.toEpochMilli()
                        && cursorEndTime <= mStartTime.toEpochMilli())
                || (cursorStartTime > mEndTime.toEpochMilli()
                        && cursorEndTime > mEndTime.toEpochMilli());
    }

    private String startTimeColumnName() {
        return mUseLocalTime ? LOCAL_DATE_TIME_START_TIME_COLUMN_NAME : START_TIME_COLUMN_NAME;
    }

    private String endTimeColumnName() {
        return mUseLocalTime ? LOCAL_DATE_TIME_END_TIME_COLUMN_NAME : END_TIME_COLUMN_NAME;
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private TreeSet<RecordData> eliminateEarliestRecordOverlaps(TreeSet<RecordData> bufferWindow) {
        RecordData firstBufferData = bufferWindow.pollFirst();
        if (firstBufferData == null) {
            return null;
        }
        TreeSet<RecordData> newBuffer = new TreeSet<>(RECORD_DATA_COMPARATOR);
        Iterator<RecordData> bufferIterator = bufferWindow.iterator();
        // Iterate until a higher priority data trims firstBufferData or bufferIterator ends.
        while (bufferIterator.hasNext()) {
            RecordData bufferData = bufferIterator.next();
            // BufferData has lower priority.
            if (PRIORITY_COMPARATOR.compare(firstBufferData, bufferData) > 0) {
                if (bufferData.endTime().isAfter(firstBufferData.endTime())) {
                    RecordData trimmed =
                            trimRecordData(
                                    bufferData,
                                    TimeUtils.latest(
                                            firstBufferData.endTime(), bufferData.startTime()),
                                    bufferData.endTime());
                    if (trimmed != null) {
                        newBuffer.add(trimmed);
                    }
                }
            } else { // BufferData has higher priority.
                // The comparator guarantees that firstBufferData is never fully trimmed by
                // bufferData.
                newBuffer.add(bufferData);
                if (firstBufferData.endTime().isAfter(bufferData.endTime())) {
                    RecordData trimmed =
                            trimRecordData(
                                    firstBufferData,
                                    bufferData.endTime(),
                                    firstBufferData.endTime());
                    if (trimmed != null) {
                        newBuffer.add(trimmed);
                    }
                }
                firstBufferData =
                        trimRecordData(
                                firstBufferData,
                                firstBufferData.startTime(),
                                TimeUtils.earliest(
                                        firstBufferData.endTime(), bufferData.startTime()));
                break;
            }
        }
        if (firstBufferData != null) {
            newBuffer.add(firstBufferData);
        }

        // Put all remaining points into the new buffer window for the next iteration.
        addAll(newBuffer, bufferIterator);
        return newBuffer;
    }

    /**
     * Adds all elements in {@code iterator} to {@code collection}. The iterator will be left
     * exhausted: its {@code hasNext()} method will return {@code false}.
     */
    @SuppressWarnings("ExtendsObject")
    private static <T extends Object> void addAll(
            Collection<T> addTo, Iterator<? extends T> iterator) {
        Objects.requireNonNull(addTo);
        Objects.requireNonNull(iterator);
        while (iterator.hasNext()) {
            addTo.add(iterator.next());
        }
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private RecordData getRecordData(Cursor cursor) {
        if (cursor != null) {
            double factor = 1;

            Instant startTime =
                    Instant.ofEpochMilli(StorageUtils.getCursorLong(cursor, startTimeColumnName()));
            Instant endTime =
                    Instant.ofEpochMilli(StorageUtils.getCursorLong(cursor, endTimeColumnName()));
            Instant currentStartTime = TimeUtils.latest(startTime, mStartTime);
            Instant currentEndTime = TimeUtils.earliest(endTime, mEndTime);
            double aggregateData = getDataToAggregate(cursor);
            if (currentStartTime.equals(mStartTime) || currentEndTime.equals(mEndTime)) {
                // If either startTime or endTime of current cursor was outside the range of
                // current group, then calculate factor of value for the time range that is within
                // the group.
                factor =
                        (double) TimeUtils.getDurationInMillis(currentStartTime, currentEndTime)
                                / TimeUtils.getDurationInMillis(startTime, endTime);
                aggregateData *= factor;
            }

            if (!currentEndTime.isAfter(currentStartTime)) {
                return null;
            }
            long appId = StorageUtils.getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
            int priority = mReversedPriorityList.indexOf(appId);
            if (priority == -1) {
                return null;
            }
            return new RecordData(
                    currentStartTime,
                    currentEndTime,
                    StorageUtils.getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME),
                    aggregateData,
                    priority);
        }
        return null;
    }

    /**
     * Trims an input record if needed for the overlapping time interval and returns a trimmed
     * non-overlapping record having updated interval between startTime and endTime. It also updates
     * the data column value based on a multiplying factor calculated for the duration of
     * non-overlapping time interval. This data will be added to form a new buffer window.
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private RecordData trimRecordData(RecordData data, Instant startTime, Instant endTime) {
        if (startTime.isAfter(data.endTime())) {
            // throw new IllegalArgumentException("startTime must be before data.endTime to trim.");
            return null;
        }
        if (!endTime.isAfter(startTime)) {
            // throw new IllegalArgumentException("startTime must be before endTime to trim.");
            return null;
        }
        if (endTime.isBefore(data.startTime())) {
            // throw new IllegalArgumentException("endTime must be after data.startTime to trim.");
            return null;
        }
        startTime = startTime.isBefore(data.startTime()) ? data.startTime() : startTime;
        endTime = endTime.isAfter(data.endTime()) ? data.endTime() : endTime;
        double factor =
                (double) TimeUtils.getDurationInMillis(startTime, endTime)
                        / getDurationInMillis(data);

        if (!endTime.isAfter(startTime)) {
            return null;
        }

        return new RecordData(
                startTime,
                endTime,
                data.lastModifiedTime(),
                data.value() * factor,
                data.priority());
    }

    private double getDataToAggregate(Cursor cursor) {
        if (mValueColumnType == Double.class) {
            return StorageUtils.getCursorDouble(cursor, mColumnNameToMerge);
        } else if (mValueColumnType == Long.class) {
            return StorageUtils.getCursorLong(cursor, mColumnNameToMerge);
        }
        return DEFAULT_DOUBLE;
    }

    private int getRecentUpdated(RecordData data1, RecordData data2) {
        // data1 and data2 are from the same app, or they are both absent from priority list
        return data1.lastModifiedTime() > data2.lastModifiedTime() ? 1 : -1;
    }

    private static long getDurationInMillis(RecordData data) {
        return TimeUtils.getDurationInMillis(data.startTime(), data.endTime());
    }

    public static class MergeResult {
        private final List<RecordData> mRecordDataList;

        private MergeResult(List<RecordData> recordDataList) {
            mRecordDataList = recordDataList;
        }

        /** Returns sum of the values from the intervals window */
        public double getTotal() {
            double sum = 0;
            for (RecordData item : mRecordDataList) {
                sum += item.value();
            }
            return sum;
        }

        /**
         * Returns list of empty intervals where there are gaps without any record data in the final
         * merge used to calculate aggregate
         */
        public List<Pair<Instant, Instant>> getEmptyIntervals(Instant startTime, Instant endTime) {
            List<Pair<Instant, Instant>> emptyIntervals = new ArrayList<>();
            if (mRecordDataList.size() == 0) {
                if (!startTime.equals(endTime)) {
                    emptyIntervals.add(new Pair<>(startTime, endTime));
                }
                return emptyIntervals;
            }

            if (startTime.isBefore(mRecordDataList.get(0).startTime())) {
                emptyIntervals.add(new Pair<>(startTime, mRecordDataList.get(0).startTime()));
            }

            for (int i = 0; i < mRecordDataList.size() - 1; i++) {
                Instant currentEnd = mRecordDataList.get(i).endTime();
                Instant nextStart = mRecordDataList.get(i + 1).startTime();
                if (nextStart.isAfter(currentEnd)) {
                    emptyIntervals.add(new Pair<>(currentEnd, nextStart));
                }
            }

            if (endTime.isAfter(mRecordDataList.get(mRecordDataList.size() - 1).endTime())) {
                emptyIntervals.add(
                        new Pair<>(
                                mRecordDataList.get(mRecordDataList.size() - 1).endTime(),
                                endTime));
            }

            return emptyIntervals;
        }
    }
}
