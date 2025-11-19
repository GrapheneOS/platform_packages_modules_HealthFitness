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

package com.android.healthfitness.flags;

import static com.android.healthfitness.flags.AconfigFlagHelper.getDbVersionToDbFlagMap;
import static com.android.healthfitness.flags.AconfigFlagHelper.isAlcoholConsumptionEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isCloudBackupRestoreEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isCyclePhasesEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isDeviceDataProvidersEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isSymptomsEnabled;
import static com.android.healthfitness.flags.DatabaseVersions.LAST_ROLLED_OUT_DB_VERSION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.function.BooleanSupplier;

@RunWith(AndroidJUnit4.class)
public class AconfigFlagHelperTest {
    @ClassRule public static final SetFlagsRule.ClassRule mClassRule = new SetFlagsRule.ClassRule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void getDbVersionToDbFlagMap_expectNoDbVersionSmallerThanBaseline() {
        // The baseline is the DB version when go/hc-aconfig-and-db is first introduced, which is
        // LAST_ROLLED_OUT_DB_VERSION.
        int baseline = LAST_ROLLED_OUT_DB_VERSION;

        for (int version : getDbVersionToDbFlagMap().keySet()) {
            assertThat(version).isGreaterThan(baseline);
        }
    }

    @Test
    public void testToEnsureLastRolledOutDbVersionIsSetCorrectly() {
        // This test is to prevent the case where the instructions in
        // go/hc-mainline-dev/trunk_stable/add-db-changes aren't followed correctly.
        // Specifically, it prevents the case in which a DB version is set to
        // LAST_ROLLED_OUT_DB_VERSION without being guarded with an aconfig flag while there are
        // DB versions being rolled out.
        // For example, if:
        // - LAST_ROLLED_OUT_DB_VERSION is currently 14
        // - DB_VERSION_TO_DB_FLAG_MAP contains a single entry of 15 => false
        // Now, if a version X = 16 is added to DatabaseVersions.java, and X is assigned to
        // LAST_ROLLED_OUT_DB_VERSION, then this test would fail.
        for (Map.Entry<Integer, BooleanSupplier> entry : getDbVersionToDbFlagMap().entrySet()) {
            int dbVersion = entry.getKey();
            boolean flagValue = entry.getValue().getAsBoolean();
            if (!flagValue) { // flagValue being `false` means the feature hasn't been rolled out
                // If a feature hasn't been rolled out, then its DB version must be greater than
                // the last rolled out DB version.
                assertTrue(
                        String.format(
                                "DB version %d hasn't been rolled out yet, it's likely a mistake to"
                                        + " set DatabaseVersions#LAST_ROLLED_OUT_DB_VERSION to a "
                                        + "number"
                                        + " greater than %d. Make sure you follow the "
                                        + "instructions in"
                                        + " go/hc-mainline-dev/trunk_stable/add-db-changes.",
                                dbVersion, dbVersion),
                        dbVersion > LAST_ROLLED_OUT_DB_VERSION);
            }
        }
    }

    @Test
    @EnableFlags({Flags.FLAG_CLOUD_BACKUP_AND_RESTORE})
    public void cloudBackupAndRestore_featureFlagTrue_expectTrue() {
        assertThat(isCloudBackupRestoreEnabled()).isTrue();
    }

    @Test
    @DisableFlags({Flags.FLAG_CLOUD_BACKUP_AND_RESTORE})
    public void cloudBackupAndRestore_featureFlagFalse_expectFalse() {
        assertThat(isCloudBackupRestoreEnabled()).isFalse();
    }

    @Test
    @DisableFlags({Flags.FLAG_DEVELOPMENT_DATABASE_RW, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void isDeviceDataProvidersEnabled_flagOff_expectFalse() {
        assertThat(isDeviceDataProvidersEnabled()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE_RW, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void isDeviceDataProvidersEnabled_flagOn_expectTrue() {
        assertThat(isDeviceDataProvidersEnabled()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_SYMPTOMS)
    @EnableFlags({Flags.FLAG_SYMPTOMS_DB, Flags.FLAG_SMOKING_DB})
    public void symptoms_featureFlagFalseAndDbTrue_expectFalse() {
        assertThat(isSymptomsEnabled()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SMOKING_DB})
    @DisableFlags({Flags.FLAG_SYMPTOMS_DB})
    public void symptoms_featureFlagTrueAndDbFalse_expectFalse() {
        assertThat(isSymptomsEnabled()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB, Flags.FLAG_SMOKING_DB})
    public void symptoms_featureFlagTrueAndDbTrue_expectTrue() {
        assertThat(isSymptomsEnabled()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_ALCOHOL_CONSUMPTION)
    @EnableFlags({Flags.FLAG_ALCOHOL_CONSUMPTION_DB, Flags.FLAG_SYMPTOMS_DB, Flags.FLAG_SMOKING_DB})
    public void alcohol_consumption_featureFlagFalseAndDbTrue_expectFalse() {
        assertThat(isAlcoholConsumptionEnabled()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_ALCOHOL_CONSUMPTION, Flags.FLAG_SYMPTOMS_DB, Flags.FLAG_SMOKING_DB})
    @DisableFlags({Flags.FLAG_ALCOHOL_CONSUMPTION_DB})
    public void alcohol_consumption_featureFlagTrueAndDbFalse_expectFalse() {
        assertThat(isAlcoholConsumptionEnabled()).isFalse();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ALCOHOL_CONSUMPTION,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_SMOKING_DB
    })
    public void alcohol_consumption_featureFlagTrueAndDbTrue_expectTrue() {
        assertThat(isAlcoholConsumptionEnabled()).isTrue();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    public void isCyclePhaseEnabled_bothFlagsTrue_expectTrue() {
        assertThat(isCyclePhasesEnabled()).isTrue();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    @DisableFlags(Flags.FLAG_CYCLE_PHASES_FLAG)
    public void isCyclePhaseEnabled_featureFlagFalseDbFlagTrue_expectFalse() {
        assertThat(isCyclePhasesEnabled()).isFalse();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    @DisableFlags(Flags.FLAG_CYCLE_PHASES_DB)
    public void isCyclePhaseEnabled_featureFlagTrueDbFlagFalse_expectFalse() {
        assertThat(isCyclePhasesEnabled()).isFalse();
    }
}
