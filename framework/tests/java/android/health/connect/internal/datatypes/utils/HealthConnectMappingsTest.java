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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;
import static android.health.connect.internal.datatypes.utils.DataTypeDescriptors.getAllDataTypeDescriptors;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.health.connect.HealthPermissionCategory;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.RecordInternal;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class HealthConnectMappingsTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void getAllRecordTypeIdentifiers() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        Set<Integer> recordTypeIds = healthConnectMappings.getAllRecordTypeIdentifiers();

        assertThat(recordTypeIds).doesNotContain(RECORD_TYPE_UNKNOWN);
        assertThat(recordTypeIds).containsNoDuplicates();
        assertThat(recordTypeIds).hasSize(getAllDataTypeDescriptors().size());
    }

    @Test
    public void getHealthReadPermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            assertThat(
                            healthConnectMappings.getHealthReadPermission(
                                    descriptor.getPermissionCategory()))
                    .isEqualTo(descriptor.getReadPermission());
        }
    }

    @Test
    public void getHealthWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            assertWithMessage(descriptor.getRecordClass().getSimpleName())
                    .that(
                            healthConnectMappings.getHealthWritePermission(
                                    descriptor.getPermissionCategory()))
                    .isEqualTo(descriptor.getWritePermission());
        }
    }

    @Test
    public void isWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            assertWithMessage(descriptor.getWritePermission())
                    .that(healthConnectMappings.isWritePermission(descriptor.getWritePermission()))
                    .isTrue();
            assertWithMessage(descriptor.getReadPermission())
                    .that(healthConnectMappings.isWritePermission(descriptor.getReadPermission()))
                    .isFalse();
        }
    }

    @Test
    public void getHealthDataCategoryForWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            String writePermission = descriptor.getWritePermission();
            String readPermission = descriptor.getReadPermission();

            assertWithMessage(writePermission)
                    .that(
                            healthConnectMappings.getHealthDataCategoryForWritePermission(
                                    writePermission))
                    .isEqualTo(descriptor.getDataCategory());

            assertWithMessage(readPermission)
                    .that(
                            healthConnectMappings.getHealthDataCategoryForWritePermission(
                                    readPermission))
                    .isEqualTo(DEFAULT_INT);
        }

        assertThat(healthConnectMappings.getHealthDataCategoryForWritePermission(null))
                .isEqualTo(DEFAULT_INT);
        assertThat(healthConnectMappings.getHealthDataCategoryForWritePermission("foo.bar"))
                .isEqualTo(DEFAULT_INT);
    }

    @DisableFlags({Flags.FLAG_ACTIVITY_INTENSITY, Flags.FLAG_SMOKING})
    @Test
    public void getHealthDataCategoryForWritePermission_equalsToLegacy() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            String writePermission = descriptor.getWritePermission();
            assertWithMessage(writePermission)
                    .that(
                            healthConnectMappings.getHealthDataCategoryForWritePermission(
                                    writePermission))
                    .isEqualTo(
                            HealthPermissions.getHealthDataCategoryForWritePermission(
                                    writePermission));
        }
    }

    @Test
    public void getWriteHealthPermissionsFor() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            String[] permissions =
                    healthConnectMappings.getWriteHealthPermissionsFor(
                            descriptor.getDataCategory());

            assertThat(permissions).isNotEmpty();
            assertThat(permissions).asList().containsNoDuplicates();
            for (String permission : permissions) {
                assertThat(healthConnectMappings.isWritePermission(permission)).isTrue();
                assertThat(
                                healthConnectMappings.getHealthDataCategoryForWritePermission(
                                        permission))
                        .isEqualTo(descriptor.getDataCategory());
            }
        }

        assertThat(
                        healthConnectMappings.getWriteHealthPermissionsFor(
                                HealthPermissionCategory.UNKNOWN))
                .isEmpty();
        assertThat(healthConnectMappings.getWriteHealthPermissionsFor(100)).isEmpty();
    }

    @Test
    public void getRecordIdToExternalRecordClassMap() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        Map<Integer, Class<? extends Record>> map =
                healthConnectMappings.getRecordIdToExternalRecordClassMap();

        assertThat(map).hasSize(getAllDataTypeDescriptors().size());
        assertThat(map.keySet()).isEqualTo(healthConnectMappings.getAllRecordTypeIdentifiers());
        assertThat(map.values()).containsNoDuplicates();
    }

    @Test
    public void getRecordIdToInternalRecordClassMap() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        Map<Integer, Class<? extends RecordInternal<?>>> map =
                healthConnectMappings.getRecordIdToInternalRecordClassMap();

        assertThat(map).hasSize(getAllDataTypeDescriptors().size());
        assertThat(map.keySet()).isEqualTo(healthConnectMappings.getAllRecordTypeIdentifiers());
        assertThat(map.values()).containsNoDuplicates();
    }

    @Test
    public void getRecordType() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (var descriptor : getAllDataTypeDescriptors()) {
            assertWithMessage(descriptor.getRecordClass().getSimpleName())
                    .that(healthConnectMappings.getRecordType(descriptor.getRecordClass()))
                    .isEqualTo(descriptor.getRecordTypeIdentifier());
        }
    }

    @Test
    public void hasRecordType() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (var descriptor : getAllDataTypeDescriptors()) {
            assertWithMessage(descriptor.getRecordClass().getSimpleName())
                    .that(healthConnectMappings.hasRecordType(descriptor.getRecordClass()))
                    .isTrue();
        }
    }

    @Test
    public void getRecordCategoryForRecordType() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (var descriptor : getAllDataTypeDescriptors()) {
            int dataCategory =
                    healthConnectMappings.getRecordCategoryForRecordType(
                            descriptor.getRecordTypeIdentifier());

            assertThat(dataCategory).isEqualTo(descriptor.getDataCategory());
        }
    }

    @DisableFlags({Flags.FLAG_ACTIVITY_INTENSITY, Flags.FLAG_SMOKING})
    @Test
    public void getRecordCategoryForRecordType_equalsToLegacy() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (var descriptor : getAllDataTypeDescriptors()) {
            int dataCategory =
                    healthConnectMappings.getRecordCategoryForRecordType(
                            descriptor.getRecordTypeIdentifier());

            assertThat(dataCategory)
                    .isEqualTo(
                            RecordTypeRecordCategoryMapper.getRecordCategoryForRecordType(
                                    descriptor.getRecordTypeIdentifier()));
        }
    }

    @Test
    public void getAllHealthDataCategories() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        Set<Integer> categories = healthConnectMappings.getAllHealthDataCategories();

        for (var descriptor : getAllDataTypeDescriptors()) {
            assertThat(categories).contains(descriptor.getDataCategory());
        }
        assertThat(categories)
                .containsExactlyElementsIn(
                        getAllDataTypeDescriptors().stream()
                                .map(DataTypeDescriptor::getDataCategory)
                                .collect(Collectors.toSet()));
    }

    @EnableFlags({Flags.FLAG_ACTIVITY_INTENSITY, Flags.FLAG_ACTIVITY_INTENSITY_DB})
    @Test
    public void activityIntensityFlagsEnabled_containsActivityIntensity() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .contains(RECORD_TYPE_ACTIVITY_INTENSITY);
    }

    @EnableFlags(Flags.FLAG_ACTIVITY_INTENSITY_DB)
    @DisableFlags(Flags.FLAG_ACTIVITY_INTENSITY)
    @Test
    public void activityIntensityFlagDisabled_doesNotContainsActivityIntensity() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_ACTIVITY_INTENSITY);
    }

    @EnableFlags(Flags.FLAG_ACTIVITY_INTENSITY)
    @DisableFlags(Flags.FLAG_ACTIVITY_INTENSITY_DB)
    @Test
    public void activityIntensityDbFlagDisabled_doesNotContainsActivityIntensity() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_ACTIVITY_INTENSITY);
    }

    @EnableFlags({
        Flags.FLAG_SMOKING,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
        Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB,
        Flags.FLAG_PHR_CHANGE_LOGS_DB
    })
    @Test
    public void nicotineIntakeFlagEnabled_containsNicotineIntake() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .contains(RECORD_TYPE_NICOTINE_INTAKE);
    }

    @EnableFlags(Flags.FLAG_SMOKING_DB)
    @DisableFlags(Flags.FLAG_SMOKING)
    @Test
    public void nicotineIntakeFlagDisabled_doesNotContainsNicotineIntake() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_NICOTINE_INTAKE);
    }

    @EnableFlags(Flags.FLAG_SMOKING)
    @DisableFlags(Flags.FLAG_SMOKING_DB)
    @Test
    public void nicotineIntakeDbFlagDisabled_doesNotContainsNicotineIntake() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_NICOTINE_INTAKE);
    }

    @Test
    public void isFitnessPermission_returnsTrueForAllFitnessPermissions() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            assertThat(healthConnectMappings.isFitnessPermission(descriptor.getReadPermission()))
                    .isTrue();
            assertThat(healthConnectMappings.isFitnessPermission(descriptor.getWritePermission()))
                    .isTrue();
        }
    }

    @Test
    public void isFitnessPermission_returnsFalseForAdditionalPermissions() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        assertThat(
                        healthConnectMappings.isFitnessPermission(
                                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND))
                .isFalse();
        assertThat(
                        healthConnectMappings.isFitnessPermission(
                                HealthPermissions.READ_HEALTH_DATA_HISTORY))
                .isFalse();
        assertThat(
                        healthConnectMappings.isFitnessPermission(
                                HealthPermissions.READ_EXERCISE_ROUTES))
                .isFalse();
    }

    @Test
    public void isFitnessPermission_returnsFalseForMedicalPermissions() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (String permission : HealthPermissions.getAllMedicalPermissions()) {
            assertThat(healthConnectMappings.isFitnessPermission(permission)).isFalse();
        }
    }
}
