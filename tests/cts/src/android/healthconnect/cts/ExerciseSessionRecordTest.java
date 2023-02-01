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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Length;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ExerciseSessionRecordTest {
    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e4);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e5);

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                ExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testExerciseSession_buildSession_buildCorrectObject() {
        ExerciseSessionRecord record = buildSessionMinimal();
        assertThat(record.getStartTime()).isEqualTo(START_TIME);
        assertThat(record.getEndTime()).isEqualTo(END_TIME);
        assertThat(record.hasRoute()).isFalse();
        assertThat(record.getRoute()).isNull();
        assertThat(record.getNotes()).isNull();
        assertThat(record.getTitle()).isNull();
        assertThat(record.getSegments()).isEmpty();
        assertThat(record.getLaps()).isEmpty();
    }

    @Test
    public void testExerciseSession_buildEqualSessions_equalsReturnsTrue() {
        Metadata metadata = TestUtils.generateMetadata();
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        ExerciseSessionRecord record2 =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        assertThat(record).isEqualTo(record2);
    }

    @Test
    public void testExerciseSession_buildSessionWithAllFields_buildCorrectObject() {
        ExerciseRoute route = TestUtils.buildExerciseRoute();
        CharSequence notes = "rain";
        CharSequence title = "Morning training";
        List<ExerciseSegment> segmentList =
                List.of(
                        new ExerciseSegment.Builder(
                                        START_TIME,
                                        END_TIME,
                                        ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                                .setRepetitionsCount(10)
                                .build());

        List<ExerciseLap> lapsList =
                List.of(
                        new ExerciseLap.Builder(START_TIME, END_TIME)
                                .setLength(Length.fromMeters(10))
                                .build());
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                TestUtils.generateMetadata(),
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setRoute(route)
                        .setEndZoneOffset(ZoneOffset.MAX)
                        .setStartZoneOffset(ZoneOffset.MIN)
                        .setNotes(notes)
                        .setTitle(title)
                        .setSegments(segmentList)
                        .setLaps(lapsList)
                        .build();

        assertThat(record.hasRoute()).isTrue();
        assertThat(record.getRoute()).isEqualTo(route);
        assertThat(record.getEndZoneOffset()).isEqualTo(ZoneOffset.MAX);
        assertThat(record.getStartZoneOffset()).isEqualTo(ZoneOffset.MIN);
        assertThat(record.getExerciseType())
                .isEqualTo(ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN);
        assertThat(record.getNotes()).isEqualTo(notes);
        assertThat(record.getSegments()).isEqualTo(segmentList);
        assertThat(record.getLaps()).isEqualTo(lapsList);
        assertThat(record.getTitle()).isEqualTo(title);
    }

    @Test
    public void testExerciseSessionBuilds_zoneOffsets_offsetsAreDefault() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        ExerciseRoute route = TestUtils.buildExerciseRoute();
        CharSequence notes = "rain";
        CharSequence title = "Morning training";
        ExerciseSessionRecord.Builder builder =
                new ExerciseSessionRecord.Builder(
                                TestUtils.generateMetadata(),
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setRoute(route)
                        .setEndZoneOffset(ZoneOffset.MAX)
                        .setStartZoneOffset(ZoneOffset.MIN)
                        .setNotes(notes)
                        .setTitle(title);

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    @Test
    public void testInsertRecord_apiReturnsRequestedRecords() throws InterruptedException {
        List<Record> records = Arrays.asList(buildSession(), buildSession());
        List<Record> insertedRecords = TestUtils.insertRecords(records);

        records.sort(Comparator.comparing(item -> item.getMetadata().getId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getId()));
        assertThat(records).isEqualTo(insertedRecords);
    }

    @Test
    public void testReadById_insertAndReadById_recordsAreEqual() throws InterruptedException {
        List<Record> records = List.of(buildSession(), buildSessionMinimal());
        TestUtils.insertRecords(records);

        ReadRecordsRequestUsingIds.Builder<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class);
        request.addId(records.get(0).getMetadata().getId());
        request.addId(records.get(1).getMetadata().getId());

        assertRecordsAreEqual(records, TestUtils.readRecords(request.build()));
    }

    @Test
    public void testReadById_insertAndReadByIdOne_recordsAreEqual() throws InterruptedException {
        List<Record> records = List.of(buildSession());
        TestUtils.insertRecords(records);

        ReadRecordsRequestUsingIds.Builder<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class);
        request.addId(records.get(0).getMetadata().getId());

        ExerciseSessionRecord readRecord = TestUtils.readRecords(request.build()).get(0);
        ExerciseSessionRecord insertedRecord = (ExerciseSessionRecord) records.get(0);
        assertThat(readRecord.hasRoute()).isEqualTo(insertedRecord.hasRoute());
        assertThat(readRecord.getMetadata()).isEqualTo(insertedRecord.getMetadata());
        assertThat(readRecord.getRoute()).isEqualTo(insertedRecord.getRoute());
    }

    @Test
    public void testReadByClientId_insertAndReadByClientId_recordsAreEqual()
            throws InterruptedException {
        List<Record> records = List.of(buildSession(), buildSessionMinimal());
        TestUtils.insertRecords(records);

        ReadRecordsRequestUsingIds.Builder<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class);
        request.addClientRecordId(records.get(0).getMetadata().getClientRecordId());
        request.addClientRecordId(records.get(1).getMetadata().getClientRecordId());

        assertRecordsAreEqual(records, TestUtils.readRecords(request.build()));
    }

    @Test
    public void testReadByClientId_insertAndReadByDefaultFilter_filteredAll()
            throws InterruptedException {
        List<Record> records = List.of(buildSession(), buildSessionMinimal());
        TestUtils.insertRecords(records);

        List<ExerciseSessionRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertRecordsAreEqual(records, readRecords);
    }

    @Test
    public void testReadByClientId_insertAndReadByTimeFilter_filteredCorrectly()
            throws InterruptedException {
        List<Record> records = List.of(buildSession(), buildSessionMinimal());
        TestUtils.insertRecords(records);

        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(START_TIME.minusMillis(10))
                        .setEndTime(END_TIME.plusMillis(10))
                        .build();

        ExerciseSessionRecord outOfRangeRecord =
                buildSession(END_TIME.plusMillis(100), END_TIME.plusMillis(200));
        TestUtils.insertRecords(List.of(outOfRangeRecord));

        List<ExerciseSessionRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertRecordsAreEqual(records, readRecords);
    }

    @Test
    public void testDeleteRecords_insertAndDeleteById_recordsNotFoundAnymore()
            throws InterruptedException {
        List<Record> records = List.of(buildSession(), buildSessionMinimal());
        List<Record> insertedRecords = TestUtils.insertRecords(records);

        TestUtils.assertRecordFound(
                records.get(0).getMetadata().getId(), ExerciseSessionRecord.class);
        TestUtils.assertRecordFound(
                records.get(1).getMetadata().getId(), ExerciseSessionRecord.class);

        TestUtils.deleteRecords(insertedRecords);

        TestUtils.assertRecordNotFound(
                records.get(0).getMetadata().getId(), ExerciseSessionRecord.class);
        TestUtils.assertRecordNotFound(
                records.get(1).getMetadata().getId(), ExerciseSessionRecord.class);
    }

    private void assertRecordsAreEqual(List<Record> records, List<ExerciseSessionRecord> result) {
        ArrayList<ExerciseSessionRecord> recordsExercises = new ArrayList<>();
        for (Record record : records) {
            recordsExercises.add((ExerciseSessionRecord) record);
        }
        recordsExercises.sort(Comparator.comparing(item -> item.getMetadata().getId()));
        result.sort(Comparator.comparing(item -> item.getMetadata().getId()));

        assertThat(result).isEqualTo(recordsExercises);
    }

    private static ExerciseSessionRecord buildSession() {
        return buildSession(TestUtils.buildExerciseRoute(), "Morning training", "rain");
    }

    private static ExerciseSessionRecord buildSession(
            ExerciseRoute route, String title, String notes) {
        return new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        START_TIME,
                        END_TIME,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(route)
                .setEndZoneOffset(ZoneOffset.MAX)
                .setStartZoneOffset(ZoneOffset.MIN)
                .setNotes(notes)
                .setTitle(title)
                .build();
    }

    private static ExerciseSessionRecord buildSession(Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(TestUtils.buildExerciseRoute())
                .setEndZoneOffset(ZoneOffset.MAX)
                .setStartZoneOffset(ZoneOffset.MIN)
                .setNotes("notes")
                .setTitle("title")
                .build();
    }

    private static ExerciseSessionRecord buildSessionMinimal() {
        return new ExerciseSessionRecord.Builder(
                        new Metadata.Builder()
                                .setDataOrigin(
                                        new DataOrigin.Builder()
                                                .setPackageName("android.healthconnect.cts")
                                                .build())
                                .setId("ExerciseSession" + Math.random())
                                .setClientRecordId("ExerciseSessionClient" + Math.random())
                                .build(),
                        START_TIME,
                        END_TIME,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .build();
    }

    private static Metadata generateMetadata() {
        return new Metadata.Builder()
                .setDevice(buildDevice())
                .setId("ExerciseSession" + Math.random())
                .setClientRecordId("ExerciseSessionClient" + Math.random())
                .setDataOrigin(
                        new DataOrigin.Builder()
                                .setPackageName("android.healthconnect.cts")
                                .build())
                .build();
    }

    private static Device buildDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setType(2)
                .build();
    }
}
