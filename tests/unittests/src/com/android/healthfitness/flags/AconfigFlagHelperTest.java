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

import static com.android.healthfitness.flags.AconfigFlagHelper.DB_VERSION_TO_DB_FLAG_MAP;
import static com.android.healthfitness.flags.AconfigFlagHelper.getDbVersion;
import static com.android.healthfitness.flags.AconfigFlagHelper.isCloudBackupRestoreEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isEcosystemMetricsEnabled;
import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;
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
    @DisableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    public void infraToGuardDbChangesDisabled() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        // putting a very high DB version mapping to true to the map
        DB_VERSION_TO_DB_FLAG_MAP.put(1000_000, () -> true);

        // since FLAG_INFRA_TO_GUARD_DB_CHANGES is disabled, that very high version shouldn't be
        // taken into account.
        assertThat(getDbVersion()).isEqualTo(LAST_ROLLED_OUT_DB_VERSION);
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void infraToGuardDbChangesEnabled() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        assertThat(getDbVersion()).isEqualTo(LAST_ROLLED_OUT_DB_VERSION);
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    public void readDbVersionToDbFlagMap_expectNoDbVersionSmallerThanBaseline() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        // The baseline is the DB version when go/hc-aconfig-and-db is first introduced, which is
        // LAST_ROLLED_OUT_DB_VERSION.
        int baseline = LAST_ROLLED_OUT_DB_VERSION;

        // Initialize the map, it won't be empty after this method is called.
        getDbVersion();

        for (int version : DB_VERSION_TO_DB_FLAG_MAP.keySet()) {
            assertThat(version).isGreaterThan(baseline);
        }
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    public void testGetDbVersion_true_true_true() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        // initialize DB_VERSION_TO_DB_FLAG_MAP, so it won't be empty when getDbVersion() is called,
        // so the entries created in this test will be used.
        DB_VERSION_TO_DB_FLAG_MAP.put(1, () -> true);
        DB_VERSION_TO_DB_FLAG_MAP.put(2, () -> true);
        DB_VERSION_TO_DB_FLAG_MAP.put(3, () -> true);

        assertThat(getDbVersion()).isEqualTo(3);
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    public void testGetDbVersion_true_false_true() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        // initialize DB_VERSION_TO_DB_FLAG_MAP, so it won't be empty when getDbVersion() is called,
        // so the entries created in this test will be used.
        DB_VERSION_TO_DB_FLAG_MAP.put(1, () -> true);
        DB_VERSION_TO_DB_FLAG_MAP.put(2, () -> false);
        DB_VERSION_TO_DB_FLAG_MAP.put(3, () -> true);

        assertThat(getDbVersion()).isEqualTo(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
    public void testGetDbVersion_true_false_false() {
        // clear the map to setup a hypothetical test case
        DB_VERSION_TO_DB_FLAG_MAP.clear();
        // initialize DB_VERSION_TO_DB_FLAG_MAP, so it won't be empty when getDbVersion() is called,
        // so the entries created in this test will be used.
        DB_VERSION_TO_DB_FLAG_MAP.put(1, () -> true);
        DB_VERSION_TO_DB_FLAG_MAP.put(2, () -> false);
        DB_VERSION_TO_DB_FLAG_MAP.put(3, () -> false);

        assertThat(getDbVersion()).isEqualTo(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES})
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
        for (Map.Entry<Integer, BooleanSupplier> entry : DB_VERSION_TO_DB_FLAG_MAP.entrySet()) {
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
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void phr_featureFlagTrueAndDbFlagTrue_expectTrue() {
        assertThat(isPersonalHealthRecordEnabled()).isTrue();
    }

    @Test
    @DisableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void phr_featureFlagFalseAndDbFlagFalse_expectFalse() {
        assertThat(isPersonalHealthRecordEnabled()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void phr_featureFlagFalseAndDbTrue_expectFalse() {
        assertThat(isPersonalHealthRecordEnabled()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void phr_featureFlagTrueAndDbFalse_expectFalse() {
        assertThat(isPersonalHealthRecordEnabled()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES)
    @DisableFlags(Flags.FLAG_ECOSYSTEM_METRICS)
    public void isEcosystemMetricsEnabled_featureFlagOff_expectFalse() {
        assertThat(isEcosystemMetricsEnabled()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ECOSYSTEM_METRICS)
    @DisableFlags(Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES)
    public void isEcosystemMetricsEnabled_dbFlagOff_expectFalse() {
        assertThat(isEcosystemMetricsEnabled()).isFalse();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ECOSYSTEM_METRICS,
        Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void isEcosystemMetricsEnabled_bothFlagsOn_expectTrue() {
        assertThat(isEcosystemMetricsEnabled()).isTrue();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CLOUD_BACKUP_AND_RESTORE,
        Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
        Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES
    })
    public void cloudBackupAndRestore_featureFlagTrueAndDbFlagTrue_expectTrue() {
        assertThat(isCloudBackupRestoreEnabled()).isTrue();
    }

    @Test
    @DisableFlags({Flags.FLAG_CLOUD_BACKUP_AND_RESTORE, Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB})
    public void cloudBackupAndRestore_featureFlagFalseAndDbFlagFalse_expectFalse() {
        assertThat(isCloudBackupRestoreEnabled()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE)
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB)
    public void cloudBackupAndRestore_featureFlagFalseAndDbTrue_expectFalse() {
        assertThat(isCloudBackupRestoreEnabled()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE)
    @DisableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB)
    public void cloudBackupAndRestore_featureFlagTrueAndDbFalse_expectFalse() {
        assertThat(isCloudBackupRestoreEnabled()).isFalse();
    }
}
