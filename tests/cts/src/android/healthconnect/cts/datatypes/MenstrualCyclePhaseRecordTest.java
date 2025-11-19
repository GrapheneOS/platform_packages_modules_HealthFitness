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

package android.healthconnect.cts.datatypes;

import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_LUTEAL;
import static android.healthconnect.testing.shared.DataFactory.getEmptyMetadata;
import static android.healthconnect.testing.shared.DataFactory.getMetadataForClientId;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.MenstrualCyclePhaseRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB
})
public class MenstrualCyclePhaseRecordTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 11, 5);
    private static final ZoneOffset TEST_OFFSET = ZoneOffset.ofHours(4);
    private final ZoneOffset mDefaultZone =
            ZoneOffset.systemDefault().getRules().getOffset(Instant.now());

    @Before
    public void setUp() throws Exception {
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.verifyDeleteRecords(
                MenstrualCyclePhaseRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertAndReadRecord_allFields() throws Exception {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                getMetadataForClientId("test_client_id"),
                                TEST_DATE,
                                MenstrualCyclePhaseRecord.PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .setStartZoneOffset(TEST_OFFSET)
                        .build();
        TestUtils.insertRecord(record);

        List<MenstrualCyclePhaseRecord> readRecords = readMenstrualCyclePhaseRecords();
        assertThat(readRecords.size()).isEqualTo(1);
        MenstrualCyclePhaseRecord readRecord = readRecords.get(0);
        assertThat(readRecord.getMetadata().getClientRecordId()).isEqualTo("test_client_id");
        assertThat(readRecord.isDayOfCycleSet()).isTrue();
        assertThat(readRecord.getDayOfCycle()).isEqualTo(2);
        assertThat(readRecord.getPhase()).isEqualTo(MenstrualCyclePhaseRecord.PHASE_LUTEAL);
        assertThat(readRecord.getDate()).isEqualTo(TEST_DATE);
        assertThat(readRecord.getStartTime())
                .isEqualTo(TEST_DATE.atStartOfDay().atZone(TEST_OFFSET).toInstant());
        assertThat(readRecord.getStartZoneOffset()).isEqualTo(TEST_OFFSET);
        assertThat(readRecord.getEndTime())
                .isEqualTo(LocalTime.MAX.atDate(TEST_DATE).atZone(TEST_OFFSET).toInstant());
        assertThat(readRecord.getEndZoneOffset()).isEqualTo(TEST_OFFSET);
    }

    @Test
    public void testInsertAndReadRecord_requiredFields() throws Exception {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                getEmptyMetadata(),
                                TEST_DATE,
                                MenstrualCyclePhaseRecord.PHASE_FOLLICULAR)
                        .build();
        TestUtils.insertRecord(record);

        List<MenstrualCyclePhaseRecord> readRecords = readMenstrualCyclePhaseRecords();
        assertThat(readRecords.size()).isEqualTo(1);
        MenstrualCyclePhaseRecord readRecord = readRecords.get(0);
        assertThat(readRecord.getMetadata().getClientRecordId()).isNull();
        assertThat(readRecord.isDayOfCycleSet()).isFalse();
        assertThat(readRecord.getDate()).isEqualTo(TEST_DATE);
        assertThrows(IllegalStateException.class, readRecord::getDayOfCycle);
        assertThat(readRecord.getPhase()).isEqualTo(MenstrualCyclePhaseRecord.PHASE_FOLLICULAR);
    }

    @Test
    public void builder_clearStartZoneOffset_isCleared() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .setStartZoneOffset(TEST_OFFSET)
                        .clearStartZoneOffset()
                        .build();
        assertThat(record.getStartZoneOffset()).isEqualTo(mDefaultZone);
        assertThat(record.getEndZoneOffset()).isEqualTo(mDefaultZone);
    }

    @Test
    public void equalsAndHashCode_allFieldsSame_isEqual() {
        String clientId = "client-id";
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                getMetadataForClientId(clientId), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(5)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                getMetadataForClientId(clientId), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(5)
                        .build();
        assertThat(record).isEqualTo(record2);
        assertThat(record.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_metadataDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                getMetadataForClientId("client-id-1"), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(5)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                getMetadataForClientId("client-id-2"), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(5)
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_dateDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                getEmptyMetadata(), TEST_DATE.plusDays(1), PHASE_LUTEAL)
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_startZoneDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                getEmptyMetadata(), TEST_DATE, PHASE_FOLLICULAR)
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                getEmptyMetadata(), TEST_DATE, PHASE_FOLLICULAR)
                        .setStartZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_phaseDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                getEmptyMetadata(), TEST_DATE, PHASE_FOLLICULAR)
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_dayOfCycleDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(3)
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashCode_dayOfCycleSetOnOne_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .build();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                        .build();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void build_unknownPhase_throwsException() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new MenstrualCyclePhaseRecord.Builder(
                                                getEmptyMetadata(), TEST_DATE, /* phase= */ 0)
                                        .build());
        assertThat(thrown).hasMessageThat().contains("Unknown Intdef value");
    }

    @Test
    public void build_dayOfCycleBelowLowerBound_throwsException() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new MenstrualCyclePhaseRecord.Builder(
                                                getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                                        .setDayOfCycle(0)
                                        .build());
        assertThat(thrown).hasMessageThat().contains("dayOfCycle must not be less than");
    }

    @Test
    public void build_dayOfCycleAboveUpperBound_throwsException() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new MenstrualCyclePhaseRecord.Builder(
                                                getEmptyMetadata(), TEST_DATE, PHASE_LUTEAL)
                                        .setDayOfCycle(366)
                                        .build());
        assertThat(thrown).hasMessageThat().contains("dayOfCycle must not be more than");
    }

    private List<MenstrualCyclePhaseRecord> readMenstrualCyclePhaseRecords() throws Exception {
        return TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(MenstrualCyclePhaseRecord.class)
                        .build());
    }
}
