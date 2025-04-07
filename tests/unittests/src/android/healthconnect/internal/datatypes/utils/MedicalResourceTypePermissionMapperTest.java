/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.internal.datatypes.utils;

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthPermissions;
import android.health.connect.internal.datatypes.utils.MedicalResourceTypePermissionMapper;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
@EnableFlags({FLAG_PERSONAL_HEALTH_RECORD})
public class MedicalResourceTypePermissionMapperTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void testGetMedicalReadPermissionForResourceType_vaccineType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_VACCINES);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_VACCINES);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_allergyType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);

        assertThat(readPermission)
                .isEqualTo(HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_labsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS);

        assertThat(readPermission)
                .isEqualTo(HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_medicationsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_MEDICATIONS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_personalDetailsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_practitionerDetailsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS);

        assertThat(readPermission)
                .isEqualTo(HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_pregnancyType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_PREGNANCY);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_PREGNANCY);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_conditionsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_CONDITIONS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_CONDITIONS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_proceduresType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_PROCEDURES);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_PROCEDURES);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_socialHistoryType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_visitsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_VISITS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_VISITS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_vitalSignsType_returns() {
        String readPermission =
                MedicalResourceTypePermissionMapper.getMedicalReadPermission(
                        MEDICAL_RESOURCE_TYPE_VITAL_SIGNS);

        assertThat(readPermission).isEqualTo(HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS);
    }

    @Test
    public void testGetMedicalReadPermissionForResourceType_unknownType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MedicalResourceTypePermissionMapper.getMedicalReadPermission(0));
    }

    @Test
    public void testGetMedicalResourceTypeForReadPermission_vaccineType_returns() {
        int medicalResourceType =
                MedicalResourceTypePermissionMapper.getMedicalResourceType(
                        HealthPermissions.READ_MEDICAL_DATA_VACCINES);

        assertThat(medicalResourceType).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    public void testGetMedicalResourceTypeForReadPermission_coversAllPermissions() {
        Set<String> medicalReadPermissions =
                HealthPermissions.getAllMedicalPermissions().stream()
                        .filter(
                                permissionString ->
                                        !permissionString.equals(
                                                HealthPermissions.WRITE_MEDICAL_DATA))
                        .collect(Collectors.toSet());
        Set<Integer> medicalResourceTypes =
                medicalReadPermissions.stream()
                        .map(MedicalResourceTypePermissionMapper::getMedicalResourceType)
                        .collect(Collectors.toSet());

        assertThat(medicalResourceTypes.size()).isEqualTo(medicalReadPermissions.size());
        assertThat(medicalResourceTypes.size()).isEqualTo(12);
    }

    @Test
    public void testGetMedicalResourceTypeForReadPermission_fitnessDataType_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        MedicalResourceTypePermissionMapper.getMedicalResourceType(
                                HealthPermissions.READ_STEPS));
    }
}
