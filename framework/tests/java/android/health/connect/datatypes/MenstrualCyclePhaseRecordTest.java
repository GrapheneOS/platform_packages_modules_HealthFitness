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

package android.health.connect.datatypes;

import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_LUTEAL;
import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_UNKNOWN;
import static android.health.connect.datatypes.RecordUtils.getDefaultZoneOffset;
import static android.healthconnect.testing.shared.DataFactory.generateMetadata;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB
})
public class MenstrualCyclePhaseRecordTest {
    @Rule public final SetFlagsRule mSetFlagRule = new SetFlagsRule();

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 11, 5);
    private static final ZoneOffset TEST_OFFSET = ZoneOffset.ofHours(4);
    private static final Metadata TEST_METADATA = generateMetadata();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setup() {
        assumeTrue(
                "Skipping tests because cycle phases is disabled",
                AconfigFlagHelper.isCyclePhasesEnabled());
        HealthConnectMappings.resetInstanceForTesting();
    }

    @Test
    public void builder_allFieldsSet() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .setStartZoneOffset(TEST_OFFSET)
                        .buildWithoutValidation();
        assertThat(record.getMetadata()).isEqualTo(TEST_METADATA);
        assertThat(record.getDate()).isEqualTo(TEST_DATE);
        assertThat(record.getStartTime())
                .isEqualTo(TEST_DATE.atStartOfDay().toInstant(TEST_OFFSET));
        assertThat(record.getEndTime())
                .isEqualTo(TEST_DATE.atTime(LocalTime.MAX).toInstant(TEST_OFFSET));
        assertThat(record.getStartZoneOffset()).isEqualTo(TEST_OFFSET);
        assertThat(record.getEndZoneOffset()).isEqualTo(TEST_OFFSET);
        assertThat(record.getPhase()).isEqualTo(PHASE_LUTEAL);
        assertThat(record.isDayOfCycleSet()).isTrue();
        assertThat(record.getDayOfCycle()).isEqualTo(2);
    }

    @Test
    public void builder_requiredFieldsSet() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        ZoneOffset defaultOffset =
                ZoneId.systemDefault().getRules().getOffset(TEST_DATE.atStartOfDay());

        assertThat(record.getMetadata()).isEqualTo(TEST_METADATA);
        assertThat(record.getDate()).isEqualTo(TEST_DATE);
        assertThat(record.getStartTime())
                .isEqualTo(TEST_DATE.atStartOfDay().toInstant(defaultOffset));
        assertThat(record.getEndTime())
                .isEqualTo(TEST_DATE.atTime(LocalTime.MAX).toInstant(defaultOffset));
        assertThat(record.getStartZoneOffset()).isEqualTo(defaultOffset);
        assertThat(record.getEndZoneOffset()).isEqualTo(defaultOffset);
        assertThat(record.getPhase()).isEqualTo(PHASE_LUTEAL);
        assertThat(record.isDayOfCycleSet()).isFalse();
        assertThrows(IllegalStateException.class, record::getDayOfCycle);
    }

    @Test
    public void builder_clearStartZoneOffset_isCleared() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setStartZoneOffset(TEST_OFFSET)
                        .clearStartZoneOffset()
                        .buildWithoutValidation();
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset());
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset());
        assertThat(record.getStartTime())
                .isEqualTo(TEST_DATE.atStartOfDay().toInstant(getDefaultZoneOffset()));
        assertThat(record.getEndTime())
                .isEqualTo(TEST_DATE.atTime(LocalTime.MAX).toInstant(getDefaultZoneOffset()));
    }

    @Test
    public void toRecordInternal_andBack_noChange() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .setStartZoneOffset(TEST_OFFSET)
                        .buildWithoutValidation();
        assertThat(record.toRecordInternal().toExternalRecord()).isEqualTo(record);
    }

    @Test
    public void equalsAndHashcode_allFieldsSame_isEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isEqualTo(record2);
        assertThat(record.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_metadataDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(
                                generateMetadata("a"), TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                generateMetadata("b"), TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_dateDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(
                                TEST_METADATA, TEST_DATE.plusDays(1), PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_startZoneDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setStartZoneOffset(ZoneOffset.ofHours(2))
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_phaseDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_FOLLICULAR)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_dayOfCycleDifferent_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(3)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_dayOfCycleSetOnOne_isNotEqual() {
        MenstrualCyclePhaseRecord record =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .setDayOfCycle(2)
                        .buildWithoutValidation();
        MenstrualCyclePhaseRecord record2 =
                new MenstrualCyclePhaseRecord.Builder(TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                        .buildWithoutValidation();
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
                                                TEST_METADATA, TEST_DATE, PHASE_UNKNOWN)
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
                                                TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
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
                                                TEST_METADATA, TEST_DATE, PHASE_LUTEAL)
                                        .setDayOfCycle(366)
                                        .build());
        assertThat(thrown).hasMessageThat().contains("dayOfCycle must not be more than");
    }
}
