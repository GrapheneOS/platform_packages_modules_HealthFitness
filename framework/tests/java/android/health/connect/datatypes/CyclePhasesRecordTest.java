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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_LUTEAL;
import static android.health.connect.datatypes.RecordUtils.getDefaultZoneOffset;
import static android.healthconnect.testing.shared.DataFactory.generateMetadata;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

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

import java.time.Instant;
import java.time.ZoneOffset;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB
})
public class CyclePhasesRecordTest {
    @Rule public final SetFlagsRule mSetFlagRule = new SetFlagsRule();

    private static final ZoneOffset TEST_OFFSET = ZoneOffset.ofHours(3);
    private static final Metadata TEST_METADATA = generateMetadata();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final Instant TIME_MILLIS = Instant.ofEpochMilli(123456);

    @Before
    public void setup() {
        assumeTrue(
                "Skipping tests because cycle phases is disabled",
                AconfigFlagHelper.isCyclePhasesEnabled());
    }

    @Test
    public void builder_allFieldsSet() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(TEST_OFFSET)
                        .setDayOfCycle(2)
                        .buildWithoutValidation();
        assertThat(record.getMetadata()).isEqualTo(TEST_METADATA);
        assertThat(record.getTime()).isEqualTo(TIME_MILLIS);
        assertThat(record.getZoneOffset()).isEqualTo(TEST_OFFSET);
        assertThat(record.getPhase()).isEqualTo(PHASE_LUTEAL);
        assertThat(record.getDayOfCycle()).isEqualTo(2);
    }

    @Test
    public void builder_requiredFieldsSet() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record.getMetadata()).isEqualTo(TEST_METADATA);
        assertThat(record.getTime()).isEqualTo(TIME_MILLIS);
        assertThat(record.getZoneOffset()).isEqualTo(getDefaultZoneOffset());
        assertThat(record.getPhase()).isEqualTo(PHASE_LUTEAL);
        assertThat(record.getDayOfCycle()).isEqualTo(DEFAULT_INT);
    }

    @Test
    public void toRecordInternal_andBack_noChange() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(TEST_OFFSET)
                        .setDayOfCycle(2)
                        .buildWithoutValidation();
        assertThat(record.toRecordInternal().toExternalRecord()).isEqualTo(record);
    }

    @Test
    public void equalsAndHashcode_allFieldsSame_isEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isEqualTo(record2);
        assertThat(record.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_metadataDifferent_isNotEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(generateMetadata("a"), TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(generateMetadata("b"), TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_timeDifferent_isNotEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(
                                TEST_METADATA, TIME_MILLIS.plusMillis(1), PHASE_LUTEAL)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_zoneDifferent_isNotEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(ZoneOffset.ofHours(1))
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(ZoneOffset.ofHours(2))
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_phaseDifferent_isNotEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_FOLLICULAR)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equalsAndHashcode_dayOfCycleDifferent_isNotEqual() {
        CyclePhasesRecord record =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(TEST_OFFSET)
                        .setDayOfCycle(2)
                        .buildWithoutValidation();
        CyclePhasesRecord record2 =
                new CyclePhasesRecord.Builder(TEST_METADATA, TIME_MILLIS, PHASE_LUTEAL)
                        .setZoneOffset(TEST_OFFSET)
                        .setDayOfCycle(3)
                        .buildWithoutValidation();
        assertThat(record).isNotEqualTo(record2);
        assertThat(record.hashCode()).isNotEqualTo(record2.hashCode());
    }
}
