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
package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.LOCAL_DATE_TIME_END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.LOCAL_DATE_TIME_START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Pair;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RunWith(TestParameterInjector.class)
public class MergeDataHelperTest {
    @TestParameter private boolean mUseLocalTime;

    private static final long APP_ID_1 = 0xDEADBEEFL;
    private static final long APP_ID_2 = 0x12345L;
    private static final long APP_ID_3 = 0xE1E1E1E1L;
    private static final String VALUE_COLUMN_NAME = "value";

    @Test
    public void testReadCursor_emptyTable_returnsZero() {
        MatrixCursor cursor = new MatrixCursor(Row.GENERAL_COLUMNS);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);

        long startTime = 0L;
        long endTime = 1000L;
        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(0.0);
    }

    @Test
    public void testReadCursor_zeroLengthIntervalLong_returnsZero() {
        long value = 26;
        long startTime = 0L;
        long endTime = 1000L;
        long dataTime = 500L;
        Cursor cursor =
                new Row<>(value, dataTime, dataTime, APP_ID_1, dataTime).toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(0.0);
    }

    @Test
    public void testReadCursor_oneIntervalLong_returnsItAsDouble() {
        long value = 26;
        long startTime = 0L;
        long endTime = 1000L;
        long dataStartTime = 500L;
        long dataEndTime = 600L;
        Cursor cursor =
                new Row<>(value, dataStartTime, dataEndTime, APP_ID_1, dataEndTime)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal())
                .isEqualTo((double) value);
    }

    @Test
    public void testReadCursor_unknownAppId_ignoredForTotal() {
        long value = 26;
        long startTime = 0L;
        long endTime = 1000L;
        long dataStartTime = 500L;
        long dataEndTime = 600L;
        Cursor cursor =
                new Row<>(value, dataStartTime, dataEndTime, APP_ID_1, dataEndTime)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_2), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(0.0);
    }

    @Test
    public void testReadCursor_oneIntervalDouble_returnsIt() {
        double value = 123.4;
        long startTime = 0L;
        long endTime = 1000L;
        long dataStartTime = 500L;
        long dataEndTime = 600L;
        Cursor cursor =
                new Row<>(value, dataStartTime, dataEndTime, APP_ID_1, dataEndTime)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Double.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(value);
    }

    @Test
    public void testReadCursor_matchedIntervals_highestPriority() {
        List<Long> priorities = List.of(APP_ID_1, APP_ID_2);
        long startTime = 0L;
        long endTime = 1000L;
        long t1 = 100;
        long t2 = 200;
        long lastModifiedTime = 50_000;
        double value1 = 12.3;
        Row<Double> row1 = new Row<>(value1, t1, t2, APP_ID_1, lastModifiedTime);
        double value2 = 67.8;
        Row<Double> row2 = new Row<>(value2, t1, t2, APP_ID_2, lastModifiedTime);
        Cursor cursor = Row.rowsToCursor(List.of(row1, row2), mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(priorities, VALUE_COLUMN_NAME, Double.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(value1);
    }

    @Test
    public void testReadCursor_matchedIntervalsPriorityReversed_highestPriority() {
        List<Long> priorities = List.of(APP_ID_2, APP_ID_1);
        long startTime = 0L;
        long endTime = 1000L;
        long t1 = 100;
        long t2 = 200;
        long lastModifiedTime = 50_000;
        double value1 = 12.3;
        Row<Double> row1 = new Row<>(value1, t1, t2, APP_ID_1, lastModifiedTime);
        double value2 = 67.8;
        Row<Double> row2 = new Row<>(value2, t1, t2, APP_ID_2, lastModifiedTime);
        Cursor cursor = Row.rowsToCursor(List.of(row1, row2), mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(priorities, VALUE_COLUMN_NAME, Double.class, mUseLocalTime);

        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal()).isEqualTo(value2);
    }

    @Test
    public void testReadCursor_overlappingIntervals_correctValuesFromJavadoc() {
        List<Long> priorities = List.of(APP_ID_1, APP_ID_2, APP_ID_3);
        long startTime = 0L;
        long endTime = 1000L;
        long t1 = 100;
        long t2 = 200;
        long t3 = 300;
        long t4 = 400;
        long lastModifiedTime = 50_000;
        double value1 = 12.3;
        Row<Double> row1 = new Row<>(value1, t1, t2, APP_ID_1, lastModifiedTime);
        double value2 = 67.8;
        Row<Double> row2 = new Row<>(value2, t1, t3, APP_ID_2, lastModifiedTime);
        double value3 = 67.8;
        Row<Double> row3 = new Row<>(value2, t2, t4, APP_ID_3, lastModifiedTime);
        Cursor cursor = Row.rowsToCursor(List.of(row1, row2, row3), mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(priorities, VALUE_COLUMN_NAME, Double.class, mUseLocalTime);

        // From javadoc:
        // App1 : T1-T2 -> value1, App2 : T2-T3 -> value2*(T3-T2)/(T3-T1), App3 : T3-T4 ->
        //     * value3*(T4-T3)/(T4-T2)
        double expectedValue =
                value1
                        + value2 * (t3 - t2) / ((double) (t3 - t1))
                        + value3 * (t4 - t3) / ((double) (t4 - t2));
        assertThat(helper.readCursor(cursor, startTime, endTime).getTotal())
                .isEqualTo(expectedValue);
    }

    @Test
    public void test_unsupportedType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MergeDataHelper(
                                List.of(APP_ID_1),
                                VALUE_COLUMN_NAME,
                                Integer.class,
                                mUseLocalTime));
    }

    @Test
    public void testGetEmptyIntervals_emptyData_wholeThingEmptyInterval() {
        MatrixCursor cursor = new MatrixCursor(Row.GENERAL_COLUMNS);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, 0, 1000);

        Instant startTime = Instant.ofEpochMilli(0);
        Instant endTime = Instant.ofEpochMilli(1000);
        assertThat(mergeResult.getEmptyIntervals(startTime, endTime))
                .containsExactly(new Pair<>(startTime, endTime));
    }

    @Test
    public void testGetEmptyIntervals_emptyDataPointInTime_noEmptyInterval() {
        MatrixCursor cursor = new MatrixCursor(Row.GENERAL_COLUMNS);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, 0, 1000);

        Instant time = Instant.ofEpochMilli(0);
        assertThat(mergeResult.getEmptyIntervals(time, time)).isEmpty();
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalMatches_noEmptyInterval() {
        long startMillis = 0;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, startMillis, endMillis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .isEmpty();
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalWiderThanTarget_noEmptyInterval() {
        long startMillis = 0;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, startMillis, endMillis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis + 10),
                                Instant.ofEpochMilli(endMillis - 10)))
                .isEmpty();
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalAtStart_oneEmptyInterval() {
        long startMillis = 0;
        long midMillis = 500;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, startMillis, midMillis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(midMillis), Instant.ofEpochMilli(endMillis)));
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalAtEnd_oneEmptyInterval() {
        long startMillis = 0;
        long midMillis = 500;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, midMillis, endMillis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(startMillis),
                                Instant.ofEpochMilli(midMillis)));
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalInMiddle_twoEmptyIntervals() {
        long startMillis = 0;
        long mid1Millis = 500;
        long mid2Millis = 600;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, mid1Millis, mid2Millis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(startMillis),
                                Instant.ofEpochMilli(mid1Millis)),
                        new Pair<>(
                                Instant.ofEpochMilli(mid2Millis), Instant.ofEpochMilli(endMillis)));
    }

    @Test
    public void testGetEmptyIntervals_singleIntervalInMiddleUnnownAppId_ignored() {
        long startMillis = 0;
        long mid1Millis = 500;
        long mid2Millis = 600;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                new Row<>(1L, mid1Millis, mid2Millis, APP_ID_1, lastModifiedMillis)
                        .toCursor(mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_2), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(startMillis),
                                Instant.ofEpochMilli(endMillis)));
    }

    @Test
    public void testGetEmptyIntervals_twoAdjacentIntervalsInMiddle_twoEmptyIntervals() {
        long startMillis = 0;
        long interval1Start = 500;
        long interval1End = 600;
        long interval2Start = interval1End;
        long interval2End = 700;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                Row.rowsToCursor(
                        List.of(
                                new Row<>(
                                        1L,
                                        interval1Start,
                                        interval1End,
                                        APP_ID_1,
                                        lastModifiedMillis),
                                new Row<>(
                                        1L,
                                        interval2Start,
                                        interval2End,
                                        APP_ID_1,
                                        lastModifiedMillis)),
                        mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult result = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        result.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(startMillis),
                                Instant.ofEpochMilli(interval1Start)),
                        new Pair<>(
                                Instant.ofEpochMilli(interval2End),
                                Instant.ofEpochMilli(endMillis)));
    }

    @Test
    public void testGetEmptyIntervals_twoSeparateIntervalsInMiddle_threeEmptyIntervals() {
        long startMillis = 0;
        long interval1Start = 500;
        long interval1End = 600;
        long interval2Start = 700;
        long interval2End = 800;
        long endMillis = 1_000;
        long lastModifiedMillis = 2_000;
        Cursor cursor =
                Row.rowsToCursor(
                        List.of(
                                new Row<>(
                                        1L,
                                        interval1Start,
                                        interval1End,
                                        APP_ID_1,
                                        lastModifiedMillis),
                                new Row<>(
                                        1L,
                                        interval2Start,
                                        interval2End,
                                        APP_ID_1,
                                        lastModifiedMillis)),
                        mUseLocalTime);
        MergeDataHelper helper =
                new MergeDataHelper(
                        List.of(APP_ID_1), VALUE_COLUMN_NAME, Long.class, mUseLocalTime);
        MergeDataHelper.MergeResult mergeResult = helper.readCursor(cursor, startMillis, endMillis);

        assertThat(
                        mergeResult.getEmptyIntervals(
                                Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis)))
                .containsExactly(
                        new Pair<>(
                                Instant.ofEpochMilli(startMillis),
                                Instant.ofEpochMilli(interval1Start)),
                        new Pair<>(
                                Instant.ofEpochMilli(interval1End),
                                Instant.ofEpochMilli(interval2Start)),
                        new Pair<>(
                                Instant.ofEpochMilli(interval2End),
                                Instant.ofEpochMilli(endMillis)));
    }

    private record Row<T>(
            T value, long startTime, long endTime, long appId, long lastModifiedTime) {
        public static final String[] GENERAL_COLUMNS =
                new String[] {
                    VALUE_COLUMN_NAME,
                    START_TIME_COLUMN_NAME,
                    END_TIME_COLUMN_NAME,
                    APP_INFO_ID_COLUMN_NAME,
                    LAST_MODIFIED_TIME_COLUMN_NAME,
                };
        public static final String[] LOCAL_TIME_COLUMNS =
                new String[] {
                    VALUE_COLUMN_NAME,
                    LOCAL_DATE_TIME_START_TIME_COLUMN_NAME,
                    LOCAL_DATE_TIME_END_TIME_COLUMN_NAME,
                    APP_INFO_ID_COLUMN_NAME,
                    LAST_MODIFIED_TIME_COLUMN_NAME,
                };

        Object[] toRow() {
            return new Object[] {value, startTime, endTime, appId, lastModifiedTime};
        }

        Cursor toCursor(boolean useLocalTime) {
            MatrixCursor cursor;
            if (useLocalTime) {
                cursor = new MatrixCursor(LOCAL_TIME_COLUMNS);
            } else {
                cursor = new MatrixCursor(GENERAL_COLUMNS);
            }
            cursor.addRow(toRow());
            return cursor;
        }

        static <T> Cursor rowsToCursor(Collection<Row<T>> rows, boolean useLocalTime) {
            MatrixCursor cursor;
            if (useLocalTime) {
                cursor = new MatrixCursor(LOCAL_TIME_COLUMNS);
            } else {
                cursor = new MatrixCursor(GENERAL_COLUMNS);
            }
            for (Row<T> row : rows) {
                cursor.addRow(row.toRow());
            }
            return cursor;
        }
    }
}
