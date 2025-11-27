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

package android.healthconnect.cts.datatypes;

import static android.healthconnect.testing.shared.DataFactory.buildExerciseRoute;
import static android.healthconnect.testing.shared.DataFactory.buildExerciseSession;
import static android.healthconnect.testing.shared.DataFactory.buildLocationTimePoint;
import static android.healthconnect.testing.shared.DataFactory.generateMetadata;
import static android.healthconnect.testing.shared.DataFactory.sessionEndTime;
import static android.healthconnect.testing.shared.DataFactory.sessionStartTime;

import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseRoute.Location;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DataFactory;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ExerciseSessionRecordTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private final Instant mNow = DataFactory.now();

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void testExerciseSession_buildSession_buildCorrectObject() {
        ExerciseSessionRecord record = buildSessionMinimal();
        assertThat(record.getStartTime()).isEqualTo(sessionStartTime(mNow));
        assertThat(record.getEndTime()).isEqualTo(sessionEndTime(mNow));
        assertThat(record.hasRoute()).isFalse();
        assertThat(record.getRoute()).isNull();
        assertThat(record.getNotes()).isNull();
        assertThat(record.getTitle()).isNull();
        assertThat(record.getSegments()).isEmpty();
        assertThat(record.getLaps()).isEmpty();
        if (Flags.exerciseSegmentImprovements()) {
            assertThat(record.hasRateOfPerceivedExertion()).isEqualTo(false);
        }
    }

    @Test
    @RequiresFlagsEnabled({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testExerciseSessionWithRpe_buildSession_buildCorrectObject() {
        ExerciseSessionRecord record = buildSessionWithRpe();
        assertThat(record.getStartTime()).isEqualTo(sessionStartTime(mNow));
        assertThat(record.getEndTime()).isEqualTo(sessionEndTime(mNow));
        assertThat(record.hasRoute()).isFalse();
        assertThat(record.getRoute()).isNull();
        assertThat(record.getNotes()).isNull();
        assertThat(record.getTitle()).isNull();
        assertThat(record.getSegments()).isEmpty();
        assertThat(record.getLaps()).isEmpty();
        assertThat(record.hasRateOfPerceivedExertion()).isEqualTo(true);
        assertThat(record.getRateOfPerceivedExertion()).isEqualTo(4.5f);
    }

    @Test
    public void testBuildSession_noException() {
        for (int i = 0; i < 200; i++) {
            buildExerciseSession();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @RequiresFlagsEnabled({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testBuildSessionWithRpeTooHigh_throwsException() {
        Metadata metadata = generateMetadata();
        new ExerciseSessionRecord.Builder(
                        metadata,
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                .setRateOfPerceivedExertion(11f)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    @RequiresFlagsEnabled({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testBuildSessionWithRpeNegative_throwsException() {
        Metadata metadata = generateMetadata();
        new ExerciseSessionRecord.Builder(
                        metadata,
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                .setRateOfPerceivedExertion(-1.5f)
                .build();
    }

    @Test
    public void testExerciseSession_buildEqualSessions_equalsReturnsTrue() {
        Metadata metadata = generateMetadata();
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                sessionStartTime(mNow),
                                sessionEndTime(mNow),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        ExerciseSessionRecord record2 =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                sessionStartTime(mNow),
                                sessionEndTime(mNow),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        assertThat(record).isEqualTo(record2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testExerciseSession_buildSessionWithAllFields_buildCorrectObject() {
        ExerciseRoute route = buildExerciseRoute();
        String notes = "rain";
        String title = "Morning training";
        List<ExerciseSegment> segmentList =
                List.of(
                        new ExerciseSegment.Builder(
                                        sessionStartTime(mNow),
                                        sessionEndTime(mNow),
                                        ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)
                                .setRepetitionsCount(10)
                                .setRateOfPerceivedExertion(6.0f)
                                .setSetIndex(1)
                                .setWeight(Mass.fromGrams(2000))
                                .build());

        List<ExerciseLap> lapsList =
                List.of(
                        new ExerciseLap.Builder(sessionStartTime(mNow), sessionEndTime(mNow))
                                .setLength(Length.fromMeters(10))
                                .build());
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                generateMetadata(),
                                sessionStartTime(mNow),
                                sessionEndTime(mNow),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setRoute(route)
                        .setEndZoneOffset(ZoneOffset.MAX)
                        .setStartZoneOffset(ZoneOffset.MIN)
                        .setNotes(notes)
                        .setTitle(title)
                        .setSegments(segmentList)
                        .setLaps(lapsList)
                        .setRateOfPerceivedExertion(5.0f)
                        .build();

        assertThat(record.hasRoute()).isTrue();
        assertThat(record.getRoute()).isEqualTo(route);
        assertThat(record.getEndZoneOffset()).isEqualTo(ZoneOffset.MAX);
        assertThat(record.getStartZoneOffset()).isEqualTo(ZoneOffset.MIN);
        assertThat(record.getExerciseType())
                .isEqualTo(ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN);
        assertThat(record.getNotes().toString()).isEqualTo(notes);
        assertThat(record.getSegments()).isEqualTo(segmentList);
        assertThat(record.getLaps()).isEqualTo(lapsList);
        assertThat(record.getTitle().toString()).isEqualTo(title);
        assertThat(record.hasRateOfPerceivedExertion()).isEqualTo(true);
        assertThat(record.getRateOfPerceivedExertion()).isEqualTo(5.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSessionBuilds_routeTimestampAfterSessionEnd_throwsException() {
        new ExerciseSessionRecord.Builder(
                        new Metadata.Builder().build(),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(
                        new ExerciseRoute(
                                List.of(
                                        new Location.Builder(
                                                        sessionEndTime(mNow).plusSeconds(1),
                                                        10.0,
                                                        10.0)
                                                .build())))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSessionBuilds_routeTimestampBeforeSessionStart_throwsException() {
        new ExerciseSessionRecord.Builder(
                        new Metadata.Builder().build(),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRoute(
                        new ExerciseRoute(
                                List.of(
                                        new Location.Builder(
                                                        sessionStartTime(mNow).minusSeconds(1),
                                                        10.0,
                                                        10.0)
                                                .build())))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSessionBuilds_sessionTypeDoesntMatchSegment_throwsException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE);
    }

    @Test
    public void testExerciseSessionBuilds_sessionTypeSwimming_noException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE);
    }

    @Test
    public void testExerciseSessionBuilds_segmentsTypeExercises_noException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE);
    }

    @Test
    public void testExerciseSessionBuilds_segmentTypeRest_noException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST);
    }

    @Test
    public void testExerciseSessionBuilds_universalSegment_noException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSessionBuilds_negativeSessionType_throwsException() {
        buildRecordWithOneSegment(-1, ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSessionBuilds_negativeSegmentType_throwsException() {
        buildRecordWithOneSegment(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY, -2);
    }

    @Test
    public void testExerciseSessionBuilds_unknownSessionType_noException() {
        buildRecordWithOneSegment(1000, ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST);
    }

    @Test
    public void testExerciseSessionBuilds_unknownSegmentType_noException() {
        buildRecordWithOneSegment(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY, 1000);
    }

    @Test
    public void testExerciseSessionBuilds_zoneOffsets_offsetsAreDefault() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        ExerciseRoute route = buildExerciseRoute();
        CharSequence notes = "rain";
        CharSequence title = "Morning training";
        ExerciseSessionRecord.Builder builder =
                new ExerciseSessionRecord.Builder(
                                generateMetadata(),
                                sessionStartTime(mNow),
                                sessionEndTime(mNow),
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
    @RequiresFlagsEnabled({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testRead_insertAndReadByIdWithRpe_recordsAreEqual() throws InterruptedException {
        List<Record> records =
                TestUtils.insertRecords(List.of(buildExerciseSession(), buildSessionWithRpe()));

        ReadRecordsRequestUsingIds.Builder<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class);
        request.addId(records.get(0).getMetadata().getId());
        request.addId(records.get(1).getMetadata().getId());

        assertRecordsAreEqual(records, TestUtils.readRecords(request.build()));
    }

    @Test
    public void testRead_pagination_filteredCorrectly() throws InterruptedException {
        var startTime = Instant.ofEpochSecond(123456789);
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            records.add(
                    buildSession(
                            startTime.plus(Duration.ofHours(i)),
                            startTime.plus(Duration.ofHours(i + 1))));
        }
        List<Record> insertedRecords = TestUtils.insertRecords(records);

        var nextPageToken = -1L;
        for (int i = 0; i < 10; i++) {
            var response =
                    TestUtils.getReadRecordsResponse(
                            new ReadRecordsRequestUsingFilters.Builder<>(
                                            ExerciseSessionRecord.class)
                                    .setPageSize(100)
                                    .setPageToken(nextPageToken)
                                    .build());
            nextPageToken = response.getNextPageToken();
            assertRecordsAreEqual(
                    insertedRecords.subList(i * 100, (i + 1) * 100), response.getRecords());
        }
    }

    @Test
    public void insertRecords_withDuplicatedClientRecordId_readNoDuplicates() throws Exception {
        int distinctRecordCount = 10;
        List<ExerciseSessionRecord> records = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < distinctRecordCount; i++) {
            ExerciseSessionRecord record =
                    buildSession(
                            /* startTime= */ now.minusSeconds(i + 1),
                            /* endTime= */ now.minusSeconds(i),
                            /* clientRecordId= */ "client_id_" + i);

            records.add(record);
            records.add(record); // Add each record twice
        }

        List<Record> insertedRecords = TestUtils.insertRecords(records);
        assertThat(insertedRecords.size()).isEqualTo(records.size());

        List<Record> distinctRecords = TestUtils.distinctByUuid(insertedRecords);
        assertThat(distinctRecords.size()).isEqualTo(distinctRecordCount);

        readAndAssertEquals(distinctRecords);
    }

    private ExerciseSessionRecord buildRecordWithOneSegment(int sessionType, int segmentType) {
        return new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        sessionType)
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                sessionStartTime(mNow),
                                                sessionEndTime(mNow),
                                                segmentType)
                                        .build()))
                .build();
    }

    private void assertRecordsAreEqual(List<Record> records, List<ExerciseSessionRecord> result) {
        ArrayList<ExerciseSessionRecord> recordsExercises = new ArrayList<>();
        for (Record record : records) {
            recordsExercises.add((ExerciseSessionRecord) record);
        }
        assertThat(result.size()).isEqualTo(recordsExercises.size());
        assertThat(result).containsExactlyElementsIn(recordsExercises);
    }

    private void readAndAssertEquals(List<Record> records) throws InterruptedException {
        List<ExerciseSessionRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertRecordsAreEqual(records, readRecords);
    }

    private static ExerciseSessionRecord buildSession(Instant startTime, Instant endTime) {
        return buildSession(
                startTime, endTime, /* clientRecordId= */ "ExerciseSessionClient" + Math.random());
    }

    private static ExerciseSessionRecord buildSession(
            Instant startTime, Instant endTime, String clientRecordId) {
        return buildSession(startTime, endTime, clientRecordId, buildLocationTimePoint(startTime));
    }

    private static ExerciseSessionRecord buildSession(
            Instant startTime, Instant endTime, double lat, double lng) {
        return buildSession(/* id= */ null, startTime, endTime, lat, lng);
    }

    private static ExerciseSessionRecord buildSession(
            String id, Instant startTime, Instant endTime, double lat, double lng) {
        return buildSession(
                id,
                startTime,
                endTime,
                /* clientRecordId= */ null,
                /* clientRecordVersion= */ 0,
                new Location.Builder(startTime, lat, lng).build());
    }

    private static ExerciseSessionRecord buildSession(
            Instant startTime, Instant endTime, String clientRecordId, double lat, double lng) {
        return buildSession(
                startTime,
                endTime,
                clientRecordId,
                /* clientRecordVersion= */ 0L,
                new Location.Builder(startTime, lat, lng).build());
    }

    private static ExerciseSessionRecord buildSession(
            Instant startTime, Instant endTime, String clientRecordId, Location location) {
        return buildSession(
                startTime, endTime, clientRecordId, /* clientRecordVersion= */ 0L, location);
    }

    private static ExerciseSessionRecord buildSession(
            Instant startTime,
            Instant endTime,
            String clientRecordId,
            long clientRecordVersion,
            Location location) {
        return buildSession(
                /* id= */ null, startTime, endTime, clientRecordId, clientRecordVersion, location);
    }

    private static ExerciseSessionRecord buildSession(
            String id,
            Instant startTime,
            Instant endTime,
            String clientRecordId,
            long clientRecordVersion,
            Location location) {
        return buildSession(
                id,
                startTime,
                ZoneOffset.MIN,
                endTime,
                ZoneOffset.MAX,
                clientRecordId,
                clientRecordVersion,
                location);
    }

    private static ExerciseSessionRecord buildSession(
            String id,
            Instant startTime,
            ZoneOffset startZoneOffset,
            Instant endTime,
            ZoneOffset endZoneOffset,
            String clientRecordId,
            long clientRecordVersion,
            Location location) {
        return new ExerciseSessionRecord.Builder(
                        buildMetadata(id, clientRecordId, clientRecordVersion),
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setEndZoneOffset(endZoneOffset)
                .setStartZoneOffset(startZoneOffset)
                .setRoute(new ExerciseRoute(List.of(location)))
                .setNotes("notes")
                .setTitle("title")
                .build();
    }

    private ExerciseSessionRecord buildSessionMinimal() {
        return new ExerciseSessionRecord.Builder(
                        buildMetadata("ExerciseSessionClient" + Math.random()),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .build();
    }

    private ExerciseSessionRecord buildSessionWithRpe() {
        return new ExerciseSessionRecord.Builder(
                        buildMetadata("ExerciseSessionClient" + Math.random()),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                .setRateOfPerceivedExertion(4.5f)
                .build();
    }

    private static Metadata buildMetadata(String clientRecordId) {
        return buildMetadata(clientRecordId, /* clientRecordVersion= */ 0L);
    }

    private static Metadata buildMetadata(String clientRecordId, long clientRecordVersion) {
        return buildMetadata(UUID.randomUUID().toString(), clientRecordId, clientRecordVersion);
    }

    private static Metadata buildMetadata(
            String id, String clientRecordId, long clientRecordVersion) {
        return new Metadata.Builder()
                .setDataOrigin(
                        new DataOrigin.Builder()
                                .setPackageName("android.healthconnect.cts")
                                .build())
                .setId(id != null ? id : "")
                .setClientRecordId(clientRecordId)
                .setClientRecordVersion(clientRecordVersion)
                .setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
                .build();
    }
}
