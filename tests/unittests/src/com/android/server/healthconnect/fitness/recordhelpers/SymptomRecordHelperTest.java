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
package com.android.server.healthconnect.fitness.recordhelpers;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
public class SymptomRecordHelperTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private SymptomRecordHelper mSymptomRecordHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSymptomRecordHelper = new SymptomRecordHelper();
    }

    @Test
    public void getGranularWritePermissions_abdominalPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_ABDOMINAL_PAIN);
    }

    @Test
    public void getGranularWritePermissions_acne_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_ACNE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_ACNE);
    }

    @Test
    public void getGranularWritePermissions_backPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BACK_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BACK_PAIN);
    }

    @Test
    public void getGranularWritePermissions_bloating_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BLOATING);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BLOATING);
    }

    @Test
    public void getGranularWritePermissions_brainFog_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BRAIN_FOG);
    }

    @Test
    public void getGranularWritePermissions_breastTenderness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BREAST_TENDERNESS);
    }

    @Test
    public void getGranularWritePermissions_brittleNails_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BRITTLE_NAILS);
    }

    @Test
    public void getGranularWritePermissions_burningMouth_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_BURNING_MOUTH);
    }

    @Test
    public void getGranularWritePermissions_chestPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CHEST_PAIN);
    }

    @Test
    public void getGranularWritePermissions_chestTightness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CHEST_TIGHTNESS);
    }

    @Test
    public void getGranularWritePermissions_chills_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CHILLS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CHILLS);
    }

    @Test
    public void getGranularWritePermissions_constipation_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CONSTIPATION);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CONSTIPATION);
    }

    @Test
    public void getGranularWritePermissions_cough_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_COUGH);
    }

    @Test
    public void getGranularWritePermissions_cramps_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CRAMPS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CRAMPS);
    }

    @Test
    public void getGranularWritePermissions_cravings_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_CRAVINGS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_CRAVINGS);
    }

    @Test
    public void getGranularWritePermissions_dehydration_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_DEHYDRATION);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_DEHYDRATION);
    }

    @Test
    public void getGranularWritePermissions_diarrhea_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_DIARRHEA);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_DIARRHEA);
    }

    @Test
    public void getGranularWritePermissions_difficultySwallowing_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_DIFFICULTY_SWALLOWING);
    }

    @Test
    public void getGranularWritePermissions_dizziness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_DIZZINESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_DIZZINESS);
    }

    @Test
    public void getGranularWritePermissions_drySkin_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_DRY_SKIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_DRY_SKIN);
    }

    @Test
    public void getGranularWritePermissions_earaches_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_EARACHES);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_EARACHES);
    }

    @Test
    public void getGranularWritePermissions_fatigue_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_FATIGUE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_FATIGUE);
    }

    @Test
    public void getGranularWritePermissions_fever_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_FEVER);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_FEVER);
    }

    @Test
    public void getGranularWritePermissions_generalizedBodyAche_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_GENERALIZED_BODY_ACHE);
    }

    @Test
    public void getGranularWritePermissions_hairLoss_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_HAIR_LOSS);
    }

    @Test
    public void getGranularWritePermissions_headache_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_HEADACHE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_HEADACHE);
    }

    @Test
    public void getGranularWritePermissions_heartburn_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_HEARTBURN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_HEARTBURN);
    }

    @Test
    public void getGranularWritePermissions_heartPalpitations_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_HEART_PALPITATIONS);
    }

    @Test
    public void getGranularWritePermissions_hotFlashes_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_HOT_FLASHES);
    }

    @Test
    public void getGranularWritePermissions_insomnia_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_INSOMNIA);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_INSOMNIA);
    }

    @Test
    public void getGranularWritePermissions_jointPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_JOINT_PAIN);
    }

    @Test
    public void getGranularWritePermissions_jointStiffness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_JOINT_STIFFNESS);
    }

    @Test
    public void getGranularWritePermissions_lossOfAppetite_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_LOSS_OF_APPETITE);
    }

    @Test
    public void getGranularWritePermissions_lossOfConsciousness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_LOSS_OF_CONSCIOUSNESS);
    }

    @Test
    public void getGranularWritePermissions_lowerBackPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_LOWER_BACK_PAIN);
    }

    @Test
    public void getGranularWritePermissions_memoryLapse_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_MEMORY_LAPSE);
    }

    @Test
    public void getGranularWritePermissions_moodChange_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_MOOD_CHANGE);
    }

    @Test
    public void getGranularWritePermissions_musclePain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_MUSCLE_PAIN);
    }

    @Test
    public void getGranularWritePermissions_nausea_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_NAUSEA);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_NAUSEA);
    }

    @Test
    public void getGranularWritePermissions_nightSweats_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_NIGHT_SWEATS);
    }

    @Test
    public void getGranularWritePermissions_pelvicPain_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_PELVIC_PAIN);
    }

    @Test
    public void
            getGranularWritePermissions_rapidPoundingOrFlutteringHeartbeat_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(
                        HealthPermissions.WRITE_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT);
    }

    @Test
    public void getGranularWritePermissions_reducedCapacityForExercise_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE);
    }

    @Test
    public void getGranularWritePermissions_runnyNose_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_RUNNY_NOSE);
    }

    @Test
    public void getGranularWritePermissions_shortnessOfBreath_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SHORTNESS_OF_BREATH);
    }

    @Test
    public void getGranularWritePermissions_skippedHeartbeat_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SKIPPED_HEARTBEAT);
    }

    @Test
    public void getGranularWritePermissions_sleepiness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SLEEPINESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SLEEPINESS);
    }

    @Test
    public void getGranularWritePermissions_sleepChanges_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SLEEP_CHANGES);
    }

    @Test
    public void getGranularWritePermissions_sneezing_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SNEEZING);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SNEEZING);
    }

    @Test
    public void getGranularWritePermissions_snore_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SNORE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SNORE);
    }

    @Test
    public void getGranularWritePermissions_soreThroat_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_SORE_THROAT);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_SORE_THROAT);
    }

    @Test
    public void getGranularWritePermissions_stomachAche_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_STOMACH_ACHE);
    }

    @Test
    public void getGranularWritePermissions_stuffyNose_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_STUFFY_NOSE);
    }

    @Test
    public void getGranularWritePermissions_unexplainedWeightChanges_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES);
    }

    @Test
    public void getGranularWritePermissions_vaginalDryness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_VAGINAL_DRYNESS);
    }

    @Test
    public void getGranularWritePermissions_vaginalItchiness_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_VAGINAL_ITCHINESS);
    }

    @Test
    public void getGranularWritePermissions_vomiting_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_VOMITING);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_VOMITING);
    }

    @Test
    public void getGranularWritePermissions_waterRetention_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_WATER_RETENTION);
    }

    @Test
    public void getGranularWritePermissions_wheezing_returnsCorrectPermission() {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(SymptomRecord.SYMPTOM_TYPE_WHEEZING);
        assertThat(mSymptomRecordHelper.getGranularWritePermissions(record))
                .containsExactly(HealthPermissions.WRITE_SYMPTOM_WHEEZING);
    }

    @Test
    public void getGranularReadPermissions_returnsAllReadPermissions() {
        assertThat(mSymptomRecordHelper.getGranularReadPermissions())
                .containsExactly(
                        HealthPermissions.READ_SYMPTOM_ABDOMINAL_PAIN,
                        HealthPermissions.READ_SYMPTOM_ACNE,
                        HealthPermissions.READ_SYMPTOM_BACK_PAIN,
                        HealthPermissions.READ_SYMPTOM_BLOATING,
                        HealthPermissions.READ_SYMPTOM_BRAIN_FOG,
                        HealthPermissions.READ_SYMPTOM_BREAST_TENDERNESS,
                        HealthPermissions.READ_SYMPTOM_BRITTLE_NAILS,
                        HealthPermissions.READ_SYMPTOM_BURNING_MOUTH,
                        HealthPermissions.READ_SYMPTOM_CHEST_PAIN,
                        HealthPermissions.READ_SYMPTOM_CHEST_TIGHTNESS,
                        HealthPermissions.READ_SYMPTOM_CHILLS,
                        HealthPermissions.READ_SYMPTOM_CONSTIPATION,
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.READ_SYMPTOM_CRAMPS,
                        HealthPermissions.READ_SYMPTOM_CRAVINGS,
                        HealthPermissions.READ_SYMPTOM_DEHYDRATION,
                        HealthPermissions.READ_SYMPTOM_DIARRHEA,
                        HealthPermissions.READ_SYMPTOM_DIFFICULTY_SWALLOWING,
                        HealthPermissions.READ_SYMPTOM_DIZZINESS,
                        HealthPermissions.READ_SYMPTOM_DRY_SKIN,
                        HealthPermissions.READ_SYMPTOM_EARACHES,
                        HealthPermissions.READ_SYMPTOM_FATIGUE,
                        HealthPermissions.READ_SYMPTOM_FEVER,
                        HealthPermissions.READ_SYMPTOM_GENERALIZED_BODY_ACHE,
                        HealthPermissions.READ_SYMPTOM_HAIR_LOSS,
                        HealthPermissions.READ_SYMPTOM_HEADACHE,
                        HealthPermissions.READ_SYMPTOM_HEARTBURN,
                        HealthPermissions.READ_SYMPTOM_HEART_PALPITATIONS,
                        HealthPermissions.READ_SYMPTOM_HOT_FLASHES,
                        HealthPermissions.READ_SYMPTOM_INSOMNIA,
                        HealthPermissions.READ_SYMPTOM_JOINT_PAIN,
                        HealthPermissions.READ_SYMPTOM_JOINT_STIFFNESS,
                        HealthPermissions.READ_SYMPTOM_LOSS_OF_APPETITE,
                        HealthPermissions.READ_SYMPTOM_LOSS_OF_CONSCIOUSNESS,
                        HealthPermissions.READ_SYMPTOM_LOWER_BACK_PAIN,
                        HealthPermissions.READ_SYMPTOM_MEMORY_LAPSE,
                        HealthPermissions.READ_SYMPTOM_MOOD_CHANGE,
                        HealthPermissions.READ_SYMPTOM_MUSCLE_PAIN,
                        HealthPermissions.READ_SYMPTOM_NAUSEA,
                        HealthPermissions.READ_SYMPTOM_NIGHT_SWEATS,
                        HealthPermissions.READ_SYMPTOM_PELVIC_PAIN,
                        HealthPermissions.READ_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT,
                        HealthPermissions.READ_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE,
                        HealthPermissions.READ_SYMPTOM_RUNNY_NOSE,
                        HealthPermissions.READ_SYMPTOM_SHORTNESS_OF_BREATH,
                        HealthPermissions.READ_SYMPTOM_SKIPPED_HEARTBEAT,
                        HealthPermissions.READ_SYMPTOM_SLEEPINESS,
                        HealthPermissions.READ_SYMPTOM_SLEEP_CHANGES,
                        HealthPermissions.READ_SYMPTOM_SNEEZING,
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.READ_SYMPTOM_SORE_THROAT,
                        HealthPermissions.READ_SYMPTOM_STOMACH_ACHE,
                        HealthPermissions.READ_SYMPTOM_STUFFY_NOSE,
                        HealthPermissions.READ_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES,
                        HealthPermissions.READ_SYMPTOM_VAGINAL_DRYNESS,
                        HealthPermissions.READ_SYMPTOM_VAGINAL_ITCHINESS,
                        HealthPermissions.READ_SYMPTOM_VOMITING,
                        HealthPermissions.READ_SYMPTOM_WATER_RETENTION,
                        HealthPermissions.READ_SYMPTOM_WHEEZING);
    }
}
