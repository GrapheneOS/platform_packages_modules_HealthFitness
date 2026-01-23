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
import static android.health.connect.HealthPermissions.WRITE_ACTIVITY_INTENSITY;
import static android.health.connect.HealthPermissions.WRITE_NICOTINE_INTAKE;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SYMPTOM;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;
import static android.health.connect.internal.datatypes.utils.DataTypeDescriptors.getAllDataTypeDescriptors;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.RecordInternal;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

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

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

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
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertThat(
                                healthConnectMappings.getHealthReadPermission(
                                        category.permissionCategoryId()))
                        .isEqualTo(category.readPermission());
            }
        }
    }

    @Test
    public void getHealthPermissions_exerciseCategory_returnsCorrectPermissions() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        String readPermission =
                healthConnectMappings.getHealthReadPermission(HealthPermissionCategory.EXERCISE);
        String writePermission =
                healthConnectMappings.getHealthWritePermission(HealthPermissionCategory.EXERCISE);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_EXERCISE);
        assertThat(writePermission).isEqualTo(HealthPermissions.WRITE_EXERCISE);
    }

    @Test
    public void getHealthPermissions_stepsCategory_returnsCorrectPermissions() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        String readPermission =
                healthConnectMappings.getHealthReadPermission(HealthPermissionCategory.STEPS);
        String writePermission =
                healthConnectMappings.getHealthWritePermission(HealthPermissionCategory.STEPS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_STEPS);
        assertThat(writePermission).isEqualTo(HealthPermissions.WRITE_STEPS);
        assertThat(
                        healthConnectMappings
                                .getHealthPermissionCategoriesForRecordType(RECORD_TYPE_STEPS)
                                .iterator()
                                .next())
                .isEqualTo(HealthPermissionCategory.STEPS);
        assertThat(
                        healthConnectMappings
                                .getHealthPermissionCategoriesForRecordType(
                                        RECORD_TYPE_STEPS_CADENCE)
                                .iterator()
                                .next())
                .isEqualTo(HealthPermissionCategory.STEPS);
    }

    @Test
    public void getHealthWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertWithMessage(descriptor.getRecordClass().getSimpleName())
                        .that(
                                healthConnectMappings.getHealthWritePermission(
                                        category.permissionCategoryId()))
                        .isEqualTo(category.writePermission());
            }
        }
    }

    @Test
    public void isWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertWithMessage(category.writePermission())
                        .that(healthConnectMappings.isWritePermission(category.writePermission()))
                        .isTrue();
                assertWithMessage(category.readPermission())
                        .that(healthConnectMappings.isWritePermission(category.readPermission()))
                        .isFalse();
            }
        }
    }

    @Test
    public void isReadPermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertWithMessage(category.writePermission())
                        .that(healthConnectMappings.isReadPermission(category.writePermission()))
                        .isFalse();
                assertWithMessage(category.readPermission())
                        .that(healthConnectMappings.isReadPermission(category.readPermission()))
                        .isTrue();
            }
        }
    }

    @Test
    public void getHealthDataCategoryForWritePermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertWithMessage(category.writePermission())
                        .that(
                                healthConnectMappings.getHealthDataCategoryForWritePermission(
                                        category.writePermission()))
                        .isEqualTo(descriptor.getDataCategory());
                assertWithMessage(category.readPermission())
                        .that(
                                healthConnectMappings.getHealthDataCategoryForWritePermission(
                                        category.readPermission()))
                        .isEqualTo(DEFAULT_INT);
            }
        }

        assertThat(healthConnectMappings.getHealthDataCategoryForWritePermission(null))
                .isEqualTo(DEFAULT_INT);
        assertThat(healthConnectMappings.getHealthDataCategoryForWritePermission("foo.bar"))
                .isEqualTo(DEFAULT_INT);
    }

    @Test
    public void getHealthPermissionCategoryForReadPermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertWithMessage(category.writePermission())
                        .that(
                                healthConnectMappings.getHealthPermissionCategoryForReadPermission(
                                        category.writePermission()))
                        .isEqualTo(DEFAULT_INT);
                assertWithMessage(category.readPermission())
                        .that(
                                healthConnectMappings.getHealthPermissionCategoryForReadPermission(
                                        category.readPermission()))
                        .isEqualTo(category.permissionCategoryId());
            }
        }

        assertThat(healthConnectMappings.getHealthPermissionCategoryForReadPermission(null))
                .isEqualTo(DEFAULT_INT);
        assertThat(healthConnectMappings.getHealthPermissionCategoryForReadPermission("foo.bar"))
                .isEqualTo(DEFAULT_INT);
    }

    @RequiresFlagsEnabled({Flags.FLAG_SMOKING, Flags.FLAG_SMOKING_DB})
    @Test
    public void getHealthDataCategoryForWritePermission_supportsNewDataTypes() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();
        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .containsAtLeast(RECORD_TYPE_ACTIVITY_INTENSITY, RECORD_TYPE_NICOTINE_INTAKE);

        assertThat(
                        healthConnectMappings.getHealthDataCategoryForWritePermission(
                                WRITE_ACTIVITY_INTENSITY))
                .isEqualTo(HealthDataCategory.ACTIVITY);
        assertThat(
                        healthConnectMappings.getHealthDataCategoryForWritePermission(
                                WRITE_NICOTINE_INTAKE))
                .isEqualTo(HealthDataCategory.WELLNESS);
    }

    @Test
    public void getWritePermissionForReadPermission_validReadPermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        for (DataTypeDescriptor descriptor : getAllDataTypeDescriptors()) {
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertThat(
                                healthConnectMappings.getWritePermissionForReadPermission(
                                        category.readPermission()))
                        .isEqualTo(category.writePermission());
            }
        }
    }

    @Test
    public void getWritePermissionForReadPermission_mapsExerciseRoutesCorrectly() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(
                        healthConnectMappings.getWritePermissionForReadPermission(
                                HealthPermissions.READ_EXERCISE_ROUTES))
                .isEqualTo(HealthPermissions.WRITE_EXERCISE_ROUTE);
    }

    @Test
    public void getWritePermissionForReadPermission_invalidReadPermission() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(
                        healthConnectMappings.getWritePermissionForReadPermission(
                                "com.some.app.READ_UNKNOWN"))
                .isNull();
        assertThat(
                        healthConnectMappings.getWritePermissionForReadPermission(
                                "com.android.health.NOT_A_PERMISSION"))
                .isNull();
    }

    @Test
    public void getWritePermissionForReadPermission_nullInput() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getWritePermissionForReadPermission(null)).isNull();
    }

    @Test
    public void getWritePermissionForReadPermission_writePermissionAsInput() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getWritePermissionForReadPermission(WRITE_STEPS)).isNull();
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

        assertThat(healthConnectMappings.getWriteHealthPermissionsFor(HealthDataCategory.UNKNOWN))
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

    @Test
    public void activityIntensityFlagsEnabled_containsActivityIntensity() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .contains(RECORD_TYPE_ACTIVITY_INTENSITY);
    }

    @RequiresFlagsEnabled({
        Flags.FLAG_SMOKING,
        Flags.FLAG_SMOKING_DB,
    })
    @Test
    public void nicotineIntakeFlagEnabled_containsNicotineIntake() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .contains(RECORD_TYPE_NICOTINE_INTAKE);
    }

    @RequiresFlagsEnabled(Flags.FLAG_SMOKING_DB)
    @RequiresFlagsDisabled(Flags.FLAG_SMOKING)
    @Test
    public void nicotineIntakeFlagDisabled_doesNotContainsNicotineIntake() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_NICOTINE_INTAKE);
    }

    @RequiresFlagsEnabled(Flags.FLAG_SMOKING)
    @RequiresFlagsDisabled(Flags.FLAG_SMOKING_DB)
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
            for (DataTypeDescriptor.PermissionCategory category :
                    descriptor.getPermissionCategories()) {
                assertThat(healthConnectMappings.isFitnessPermission(category.readPermission()))
                        .isTrue();
                assertThat(healthConnectMappings.isFitnessPermission(category.writePermission()))
                        .isTrue();
            }
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

    @RequiresFlagsEnabled({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    @Test
    public void symptomsFlagEnabled_containsSymptoms() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .contains(RECORD_TYPE_SYMPTOM);
    }

    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS_DB)
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS)
    @Test
    public void symptomsFlagDisabled_doesNotContainSymptoms() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_SYMPTOM);
    }

    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS)
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS_DB)
    @Test
    public void symptomsDbFlagDisabled_doesNotContainSymptoms() {
        HealthConnectMappings healthConnectMappings = new HealthConnectMappings();

        assertThat(healthConnectMappings.getAllRecordTypeIdentifiers())
                .doesNotContain(RECORD_TYPE_SYMPTOM);
    }
}
