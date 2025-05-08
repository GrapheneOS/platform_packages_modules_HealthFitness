/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.aggregation;

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;
import static android.health.connect.datatypes.ActivityIntensityRecord.INTENSITY_MINUTES_TOTAL;
import static android.health.connect.datatypes.ActivityIntensityRecord.MODERATE_DURATION_TOTAL;
import static android.health.connect.datatypes.ActivityIntensityRecord.VIGOROUS_DURATION_TOTAL;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.insertRecord;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_HEALTH_CONNECT_MAPPINGS;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Duration.ofMinutes;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.testing.shared.recordfactory.ActivityIntensityRecordFactory;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.annotation.Nullable;

import com.android.compatibility.common.util.ApiTest;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@RequiresFlagsEnabled({
    FLAG_ACTIVITY_INTENSITY,
    FLAG_ACTIVITY_INTENSITY_DB,
    FLAG_HEALTH_CONNECT_MAPPINGS
})
@ApiTest(apis = {"android.health.connect.datatypes.ActivityIntensityRecord#MINUTES_TOTAL"})
public class ActivityIntensityMinutesAggregationTest
        extends BaseDurationAggregationTest<ActivityIntensityRecord, Long> {

    private final ActivityIntensityRecordFactory mRecordFactory =
            new ActivityIntensityRecordFactory();

    public ActivityIntensityMinutesAggregationTest() {
        super(INTENSITY_MINUTES_TOTAL, HealthDataCategory.ACTIVITY);
    }

    @Override
    ActivityIntensityRecord createRecord(
            Instant startTime,
            Instant endTime,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset) {
        return mRecordFactory.newRecord(
                startTime,
                endTime,
                ACTIVITY_INTENSITY_TYPE_MODERATE,
                startZoneOffset,
                endZoneOffset);
    }

    @Override
    Long getExpectedValueFromDuration(Duration duration) {
        return duration.toMinutes();
    }

    @Test
    public void moderateRecord_multiplierIsOne_roundsToMinutes() throws Exception {
        ActivityIntensityRecord record =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(17).plusSeconds(32).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);

        insertRecords(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(getRecordDuration(record).toMinutes());
    }

    @Test
    public void vigorousRecord_multiplierIsTwo_roundsToMinutes() throws Exception {
        ActivityIntensityRecord record =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(13).plusSeconds(32).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);

        insertRecords(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(getRecordDuration(record).multipliedBy(2).toMinutes());
    }

    @Test
    public void multipleRecords_returnsTotalWeightedDuration_inMinutes() throws Exception {
        var records =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.toInstant(),
                                YESTERDAY_11AM.plusMinutes(7).plusSeconds(20).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(10).plusSeconds(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(13).plusSeconds(40).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).plusSeconds(11).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(25).plusSeconds(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(25).plusSeconds(35).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS));

        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(
                        getRecordDuration(records.get(1))
                                .plus(getRecordDuration(records.get(3)))
                                .multipliedBy(2)
                                .plus(getRecordDuration(records.get(0)))
                                .plus(getRecordDuration(records.get(2)))
                                .toMinutes());
    }

    @Test
    public void multiApp_masksOutLowerPriorityVigorousRecord() throws Exception {
        ActivityIntensityRecord higherPriorityModerateRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        ActivityIntensityRecord lowerPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusMinutes(5).toInstant(),
                        YESTERDAY_11AM.plusHours(1).plusMinutes(17).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        insertRecord(higherPriorityModerateRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityVigorousRecord);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(
                        ofMinutes(5)
                                .plusMinutes(17)
                                .multipliedBy(2)
                                .plus(getRecordDuration(higherPriorityModerateRecord))
                                .toMinutes());
        assertThat(response.getDataOrigins(INTENSITY_MINUTES_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    @Test
    public void multiApp_masksOutLowerPriorityModerateRecord() throws Exception {
        ActivityIntensityRecord higherPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        ActivityIntensityRecord lowerPriorityModerateRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusMinutes(5).toInstant(),
                        YESTERDAY_11AM.plusHours(1).plusMinutes(17).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        insertRecord(higherPriorityVigorousRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityModerateRecord);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(
                        getRecordDuration(higherPriorityVigorousRecord)
                                .multipliedBy(2)
                                .plusMinutes(5)
                                .plusMinutes(17)
                                .toMinutes());
        assertThat(response.getDataOrigins(INTENSITY_MINUTES_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    @Test
    public void overlappingRecords_equalsToModeratePlusDoubleVigorous() throws Exception {
        var higherPriorityRecords =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE));

        var lowerPriorityRecords =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(9).toInstant(),
                                YESTERDAY_11AM.plusMinutes(16).plusSeconds(45).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(17).toInstant(),
                                YESTERDAY_11AM.plusMinutes(32).plusSeconds(52).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(41).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).plusSeconds(49).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS));

        insertRecords(higherPriorityRecords);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityRecords);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> durationsResponse =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(MODERATE_DURATION_TOTAL)
                                .addAggregationType(VIGOROUS_DURATION_TOTAL)
                                .build());
        AggregateRecordsResponse<Long> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(INTENSITY_MINUTES_TOTAL)
                                .build());

        assertThat(response.get(INTENSITY_MINUTES_TOTAL)).isNotNull();
        assertThat(durationsResponse.get(MODERATE_DURATION_TOTAL)).isNotNull();
        assertThat(durationsResponse.get(VIGOROUS_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(INTENSITY_MINUTES_TOTAL))
                .isEqualTo(
                        durationsResponse
                                .get(VIGOROUS_DURATION_TOTAL)
                                .multipliedBy(2)
                                .plus(durationsResponse.get(MODERATE_DURATION_TOTAL))
                                .toMinutes());
    }
}
