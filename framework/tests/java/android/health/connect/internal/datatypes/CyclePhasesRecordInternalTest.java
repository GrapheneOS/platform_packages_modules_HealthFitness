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

package android.health.connect.internal.datatypes;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_LUTEAL;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

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

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB
})
public class CyclePhasesRecordInternalTest {
    @Rule public final SetFlagsRule mSetFlagRule = new SetFlagsRule();

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
    public void testPhase_setterAndGetter() {
        CyclePhasesRecordInternal record = new CyclePhasesRecordInternal();
        assertThat(record.getPhase()).isEqualTo(PHASE_UNKNOWN);
        record.setPhase(PHASE_FOLLICULAR);
        assertThat(record.getPhase()).isEqualTo(PHASE_FOLLICULAR);
        record.setPhase(PHASE_LUTEAL);
        assertThat(record.getPhase()).isEqualTo(PHASE_LUTEAL);
    }

    @Test
    public void testDayOfCycle_setterAndGetter() {
        CyclePhasesRecordInternal record = new CyclePhasesRecordInternal();
        assertThat(record.getDayOfCycle()).isEqualTo(DEFAULT_INT);
        record.setDayOfCycle(12);
        assertThat(record.getDayOfCycle()).isEqualTo(12);
    }

    @Test
    public void toExternalRecord_andBack_noChange() {
        CyclePhasesRecordInternal record =
                new CyclePhasesRecordInternal().setPhase(PHASE_FOLLICULAR).setDayOfCycle(3);
        Instant time = Instant.ofEpochMilli(123456);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(3);
        Instant startOfDay =
                time.atOffset(startZoneOffset).truncatedTo(ChronoUnit.DAYS).toInstant();

        ZoneOffset endZoneOffset = ZoneOffset.ofHours(4);
        Instant endOfDay = time.atOffset(endZoneOffset).with(LocalTime.MAX).toInstant();

        record.setStartTime(startOfDay.toEpochMilli())
                .setEndTime(endOfDay.toEpochMilli())
                .setStartZoneOffset(startZoneOffset.getTotalSeconds())
                .setEndZoneOffset(endZoneOffset.getTotalSeconds())
                .setPackageName("test.package");
        CyclePhasesRecordInternal recordAfterRoundTrip =
                (CyclePhasesRecordInternal) record.toExternalRecord().toRecordInternal();
        assertThat(recordAfterRoundTrip.getPackageName()).isEqualTo(record.getPackageName());
        assertThat(recordAfterRoundTrip.getStartTimeInMillis())
                .isEqualTo(record.getStartTimeInMillis());
        assertThat(recordAfterRoundTrip.getEndTimeInMillis())
                .isEqualTo(record.getEndTimeInMillis());
        assertThat(recordAfterRoundTrip.getStartZoneOffsetInSeconds())
                .isEqualTo(record.getStartZoneOffsetInSeconds());
        assertThat(recordAfterRoundTrip.getEndZoneOffsetInSeconds())
                .isEqualTo(record.getEndZoneOffsetInSeconds());
        assertThat(recordAfterRoundTrip.getPhase()).isEqualTo(record.getPhase());
        assertThat(recordAfterRoundTrip.getDayOfCycle()).isEqualTo(record.getDayOfCycle());
    }
}
