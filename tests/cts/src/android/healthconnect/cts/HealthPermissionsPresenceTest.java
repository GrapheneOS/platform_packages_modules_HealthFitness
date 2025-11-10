/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.cts;

import static android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP;
import static android.health.connect.HealthPermissions.READ_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_ALCOHOL_CONSUMPTION;
import static android.health.connect.HealthPermissions.READ_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.READ_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.READ_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.READ_BODY_FAT;
import static android.health.connect.HealthPermissions.READ_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.READ_BONE_MASS;
import static android.health.connect.HealthPermissions.READ_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.READ_HEIGHT;
import static android.health.connect.HealthPermissions.READ_HYDRATION;
import static android.health.connect.HealthPermissions.READ_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.READ_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.READ_MENSTRUATION;
import static android.health.connect.HealthPermissions.READ_NICOTINE_INTAKE;
import static android.health.connect.HealthPermissions.READ_NUTRITION;
import static android.health.connect.HealthPermissions.READ_OVULATION_TEST;
import static android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.READ_POWER;
import static android.health.connect.HealthPermissions.READ_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.READ_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_SPEED;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_ABDOMINAL_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_ACNE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BACK_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BLOATING;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BRAIN_FOG;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BREAST_TENDERNESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BRITTLE_NAILS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_BURNING_MOUTH;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CHEST_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CHEST_TIGHTNESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CHILLS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CONSTIPATION;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_COUGH;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CRAMPS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_CRAVINGS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_DEHYDRATION;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_DIARRHEA;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_DIFFICULTY_SWALLOWING;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_DIZZINESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_DRY_SKIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_EARACHES;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_FATIGUE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_FEVER;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_GENERALIZED_BODY_ACHE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_HAIR_LOSS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_HEADACHE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_HEARTBURN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_HEART_PALPITATIONS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_HOT_FLASHES;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_INSOMNIA;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_JOINT_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_JOINT_STIFFNESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_LOSS_OF_APPETITE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_LOSS_OF_CONSCIOUSNESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_LOWER_BACK_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_MEMORY_LAPSE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_MOOD_CHANGE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_MUSCLE_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_NAUSEA;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_NIGHT_SWEATS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_PELVIC_PAIN;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_RUNNY_NOSE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SHORTNESS_OF_BREATH;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SKIPPED_HEARTBEAT;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SLEEPINESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SLEEP_CHANGES;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SNEEZING;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SNORE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_SORE_THROAT;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_STOMACH_ACHE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_STUFFY_NOSE;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_VAGINAL_DRYNESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_VAGINAL_ITCHINESS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_VOMITING;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_WATER_RETENTION;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_WHEEZING;
import static android.health.connect.HealthPermissions.READ_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_VO2_MAX;
import static android.health.connect.HealthPermissions.READ_WEIGHT;
import static android.health.connect.HealthPermissions.READ_WHEELCHAIR_PUSHES;
import static android.health.connect.HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_ALCOHOL_CONSUMPTION;
import static android.health.connect.HealthPermissions.WRITE_BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.WRITE_BLOOD_PRESSURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_FAT;
import static android.health.connect.HealthPermissions.WRITE_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_BODY_WATER_MASS;
import static android.health.connect.HealthPermissions.WRITE_BONE_MASS;
import static android.health.connect.HealthPermissions.WRITE_CERVICAL_MUCUS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_ELEVATION_GAINED;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.health.connect.HealthPermissions.WRITE_FLOORS_CLIMBED;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissions.WRITE_HEIGHT;
import static android.health.connect.HealthPermissions.WRITE_HYDRATION;
import static android.health.connect.HealthPermissions.WRITE_INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissions.WRITE_LEAN_BODY_MASS;
import static android.health.connect.HealthPermissions.WRITE_MENSTRUATION;
import static android.health.connect.HealthPermissions.WRITE_NICOTINE_INTAKE;
import static android.health.connect.HealthPermissions.WRITE_NUTRITION;
import static android.health.connect.HealthPermissions.WRITE_OVULATION_TEST;
import static android.health.connect.HealthPermissions.WRITE_OXYGEN_SATURATION;
import static android.health.connect.HealthPermissions.WRITE_POWER;
import static android.health.connect.HealthPermissions.WRITE_RESPIRATORY_RATE;
import static android.health.connect.HealthPermissions.WRITE_RESTING_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_SPEED;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_ABDOMINAL_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_ACNE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BACK_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BLOATING;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BRAIN_FOG;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BREAST_TENDERNESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BRITTLE_NAILS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_BURNING_MOUTH;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CHEST_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CHEST_TIGHTNESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CHILLS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CONSTIPATION;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_COUGH;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CRAMPS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_CRAVINGS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_DEHYDRATION;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_DIARRHEA;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_DIFFICULTY_SWALLOWING;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_DIZZINESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_DRY_SKIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_EARACHES;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_FATIGUE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_FEVER;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_GENERALIZED_BODY_ACHE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_HAIR_LOSS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_HEADACHE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_HEARTBURN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_HEART_PALPITATIONS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_HOT_FLASHES;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_INSOMNIA;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_JOINT_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_JOINT_STIFFNESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_LOSS_OF_APPETITE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_LOSS_OF_CONSCIOUSNESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_LOWER_BACK_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_MEMORY_LAPSE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_MOOD_CHANGE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_MUSCLE_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_NAUSEA;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_NIGHT_SWEATS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_PELVIC_PAIN;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_RUNNY_NOSE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SHORTNESS_OF_BREATH;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SKIPPED_HEARTBEAT;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SLEEPINESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SLEEP_CHANGES;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SNEEZING;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SNORE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_SORE_THROAT;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_STOMACH_ACHE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_STUFFY_NOSE;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_VAGINAL_DRYNESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_VAGINAL_ITCHINESS;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_VOMITING;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_WATER_RETENTION;
import static android.health.connect.HealthPermissions.WRITE_SYMPTOM_WHEEZING;
import static android.health.connect.HealthPermissions.WRITE_TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_VO2_MAX;
import static android.health.connect.HealthPermissions.WRITE_WEIGHT;
import static android.health.connect.HealthPermissions.WRITE_WHEELCHAIR_PUSHES;

import static com.android.healthfitness.flags.Flags.FLAG_ALCOHOL_CONSUMPTION;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.health.connect.HealthConnectManager;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/*
 * Configuration test to check that all health permissions are defined.
 */
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(value = {FLAG_SYMPTOMS, FLAG_SMOKING, FLAG_ALCOHOL_CONSUMPTION})
public class HealthPermissionsPresenceTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    // TODO(b/452289293): Add cycle phases permissions after the API is not annotated with @hide
    private static final Set<String> HEALTH_PERMISSIONS =
            Set.of(
                    READ_ACTIVE_CALORIES_BURNED,
                    READ_ALCOHOL_CONSUMPTION,
                    READ_BASAL_BODY_TEMPERATURE,
                    READ_BASAL_METABOLIC_RATE,
                    READ_BLOOD_GLUCOSE,
                    READ_BLOOD_PRESSURE,
                    READ_BODY_FAT,
                    READ_BODY_TEMPERATURE,
                    READ_BODY_WATER_MASS,
                    READ_BONE_MASS,
                    READ_CERVICAL_MUCUS,
                    READ_DISTANCE,
                    READ_ELEVATION_GAINED,
                    READ_EXERCISE,
                    READ_FLOORS_CLIMBED,
                    READ_HEALTH_DATA_HISTORY,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEART_RATE,
                    READ_HEART_RATE_VARIABILITY,
                    READ_HEIGHT,
                    READ_HYDRATION,
                    READ_INTERMENSTRUAL_BLEEDING,
                    READ_LEAN_BODY_MASS,
                    READ_MENSTRUATION,
                    READ_NICOTINE_INTAKE,
                    READ_NUTRITION,
                    READ_OVULATION_TEST,
                    READ_OXYGEN_SATURATION,
                    READ_POWER,
                    READ_RESPIRATORY_RATE,
                    READ_RESTING_HEART_RATE,
                    READ_SEXUAL_ACTIVITY,
                    READ_SKIN_TEMPERATURE,
                    READ_SLEEP,
                    READ_SPEED,
                    READ_STEPS,
                    READ_SYMPTOM_ABDOMINAL_PAIN,
                    READ_SYMPTOM_ACNE,
                    READ_SYMPTOM_BACK_PAIN,
                    READ_SYMPTOM_BLOATING,
                    READ_SYMPTOM_BRAIN_FOG,
                    READ_SYMPTOM_BREAST_TENDERNESS,
                    READ_SYMPTOM_BRITTLE_NAILS,
                    READ_SYMPTOM_BURNING_MOUTH,
                    READ_SYMPTOM_CHEST_PAIN,
                    READ_SYMPTOM_CHEST_TIGHTNESS,
                    READ_SYMPTOM_CHILLS,
                    READ_SYMPTOM_CONSTIPATION,
                    READ_SYMPTOM_COUGH,
                    READ_SYMPTOM_CRAMPS,
                    READ_SYMPTOM_CRAVINGS,
                    READ_SYMPTOM_DEHYDRATION,
                    READ_SYMPTOM_DIARRHEA,
                    READ_SYMPTOM_DIFFICULTY_SWALLOWING,
                    READ_SYMPTOM_DIZZINESS,
                    READ_SYMPTOM_DRY_SKIN,
                    READ_SYMPTOM_EARACHES,
                    READ_SYMPTOM_FATIGUE,
                    READ_SYMPTOM_FEVER,
                    READ_SYMPTOM_GENERALIZED_BODY_ACHE,
                    READ_SYMPTOM_HAIR_LOSS,
                    READ_SYMPTOM_HEADACHE,
                    READ_SYMPTOM_HEARTBURN,
                    READ_SYMPTOM_HEART_PALPITATIONS,
                    READ_SYMPTOM_HOT_FLASHES,
                    READ_SYMPTOM_INSOMNIA,
                    READ_SYMPTOM_JOINT_PAIN,
                    READ_SYMPTOM_JOINT_STIFFNESS,
                    READ_SYMPTOM_LOSS_OF_APPETITE,
                    READ_SYMPTOM_LOSS_OF_CONSCIOUSNESS,
                    READ_SYMPTOM_LOWER_BACK_PAIN,
                    READ_SYMPTOM_MEMORY_LAPSE,
                    READ_SYMPTOM_MOOD_CHANGE,
                    READ_SYMPTOM_MUSCLE_PAIN,
                    READ_SYMPTOM_NAUSEA,
                    READ_SYMPTOM_NIGHT_SWEATS,
                    READ_SYMPTOM_PELVIC_PAIN,
                    READ_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT,
                    READ_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE,
                    READ_SYMPTOM_RUNNY_NOSE,
                    READ_SYMPTOM_SHORTNESS_OF_BREATH,
                    READ_SYMPTOM_SKIPPED_HEARTBEAT,
                    READ_SYMPTOM_SLEEPINESS,
                    READ_SYMPTOM_SLEEP_CHANGES,
                    READ_SYMPTOM_SNEEZING,
                    READ_SYMPTOM_SNORE,
                    READ_SYMPTOM_SORE_THROAT,
                    READ_SYMPTOM_STOMACH_ACHE,
                    READ_SYMPTOM_STUFFY_NOSE,
                    READ_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES,
                    READ_SYMPTOM_VAGINAL_DRYNESS,
                    READ_SYMPTOM_VAGINAL_ITCHINESS,
                    READ_SYMPTOM_VOMITING,
                    READ_SYMPTOM_WATER_RETENTION,
                    READ_SYMPTOM_WHEEZING,
                    READ_TOTAL_CALORIES_BURNED,
                    READ_VO2_MAX,
                    READ_WEIGHT,
                    READ_WHEELCHAIR_PUSHES,
                    WRITE_ACTIVE_CALORIES_BURNED,
                    WRITE_ALCOHOL_CONSUMPTION,
                    WRITE_BASAL_BODY_TEMPERATURE,
                    WRITE_BASAL_METABOLIC_RATE,
                    WRITE_BLOOD_GLUCOSE,
                    WRITE_BLOOD_PRESSURE,
                    WRITE_BODY_FAT,
                    WRITE_BODY_TEMPERATURE,
                    WRITE_BODY_WATER_MASS,
                    WRITE_BONE_MASS,
                    WRITE_CERVICAL_MUCUS,
                    WRITE_DISTANCE,
                    WRITE_ELEVATION_GAINED,
                    WRITE_EXERCISE,
                    WRITE_EXERCISE_ROUTE,
                    WRITE_FLOORS_CLIMBED,
                    WRITE_HEART_RATE,
                    WRITE_HEART_RATE_VARIABILITY,
                    WRITE_HEIGHT,
                    WRITE_HYDRATION,
                    WRITE_INTERMENSTRUAL_BLEEDING,
                    WRITE_LEAN_BODY_MASS,
                    WRITE_MENSTRUATION,
                    WRITE_NICOTINE_INTAKE,
                    WRITE_NUTRITION,
                    WRITE_OVULATION_TEST,
                    WRITE_OXYGEN_SATURATION,
                    WRITE_POWER,
                    WRITE_RESPIRATORY_RATE,
                    WRITE_RESTING_HEART_RATE,
                    WRITE_SEXUAL_ACTIVITY,
                    WRITE_SKIN_TEMPERATURE,
                    WRITE_SLEEP,
                    WRITE_SPEED,
                    WRITE_STEPS,
                    WRITE_SYMPTOM_ABDOMINAL_PAIN,
                    WRITE_SYMPTOM_ACNE,
                    WRITE_SYMPTOM_BACK_PAIN,
                    WRITE_SYMPTOM_BLOATING,
                    WRITE_SYMPTOM_BRAIN_FOG,
                    WRITE_SYMPTOM_BREAST_TENDERNESS,
                    WRITE_SYMPTOM_BRITTLE_NAILS,
                    WRITE_SYMPTOM_BURNING_MOUTH,
                    WRITE_SYMPTOM_CHEST_PAIN,
                    WRITE_SYMPTOM_CHEST_TIGHTNESS,
                    WRITE_SYMPTOM_CHILLS,
                    WRITE_SYMPTOM_CONSTIPATION,
                    WRITE_SYMPTOM_COUGH,
                    WRITE_SYMPTOM_CRAMPS,
                    WRITE_SYMPTOM_CRAVINGS,
                    WRITE_SYMPTOM_DEHYDRATION,
                    WRITE_SYMPTOM_DIARRHEA,
                    WRITE_SYMPTOM_DIFFICULTY_SWALLOWING,
                    WRITE_SYMPTOM_DIZZINESS,
                    WRITE_SYMPTOM_DRY_SKIN,
                    WRITE_SYMPTOM_EARACHES,
                    WRITE_SYMPTOM_FATIGUE,
                    WRITE_SYMPTOM_FEVER,
                    WRITE_SYMPTOM_GENERALIZED_BODY_ACHE,
                    WRITE_SYMPTOM_HAIR_LOSS,
                    WRITE_SYMPTOM_HEADACHE,
                    WRITE_SYMPTOM_HEARTBURN,
                    WRITE_SYMPTOM_HEART_PALPITATIONS,
                    WRITE_SYMPTOM_HOT_FLASHES,
                    WRITE_SYMPTOM_INSOMNIA,
                    WRITE_SYMPTOM_JOINT_PAIN,
                    WRITE_SYMPTOM_JOINT_STIFFNESS,
                    WRITE_SYMPTOM_LOSS_OF_APPETITE,
                    WRITE_SYMPTOM_LOSS_OF_CONSCIOUSNESS,
                    WRITE_SYMPTOM_LOWER_BACK_PAIN,
                    WRITE_SYMPTOM_MEMORY_LAPSE,
                    WRITE_SYMPTOM_MOOD_CHANGE,
                    WRITE_SYMPTOM_MUSCLE_PAIN,
                    WRITE_SYMPTOM_NAUSEA,
                    WRITE_SYMPTOM_NIGHT_SWEATS,
                    WRITE_SYMPTOM_PELVIC_PAIN,
                    WRITE_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT,
                    WRITE_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE,
                    WRITE_SYMPTOM_RUNNY_NOSE,
                    WRITE_SYMPTOM_SHORTNESS_OF_BREATH,
                    WRITE_SYMPTOM_SKIPPED_HEARTBEAT,
                    WRITE_SYMPTOM_SLEEPINESS,
                    WRITE_SYMPTOM_SLEEP_CHANGES,
                    WRITE_SYMPTOM_SNEEZING,
                    WRITE_SYMPTOM_SNORE,
                    WRITE_SYMPTOM_SORE_THROAT,
                    WRITE_SYMPTOM_STOMACH_ACHE,
                    WRITE_SYMPTOM_STUFFY_NOSE,
                    WRITE_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES,
                    WRITE_SYMPTOM_VAGINAL_DRYNESS,
                    WRITE_SYMPTOM_VAGINAL_ITCHINESS,
                    WRITE_SYMPTOM_VOMITING,
                    WRITE_SYMPTOM_WATER_RETENTION,
                    WRITE_SYMPTOM_WHEEZING,
                    WRITE_TOTAL_CALORIES_BURNED,
                    WRITE_VO2_MAX,
                    WRITE_WEIGHT,
                    WRITE_WHEELCHAIR_PUSHES);

    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mPackageManager = InstrumentationRegistry.getTargetContext().getPackageManager();
    }

    @Test
    public void testHealthPermissionGroup_isDefined() throws Exception {
        PermissionGroupInfo info =
                mPackageManager.getPermissionGroupInfo(HEALTH_PERMISSION_GROUP, /* flags= */ 0);
        assertThat(info).isNotNull();
    }

    @Test
    public void testHealthPermissions_isDefined() throws Exception {
        for (String permissionName : HEALTH_PERMISSIONS) {
            assertHealthPermissionIsDefined(permissionName);
        }
    }

    @Test
    public void testGetHealthPermissions_returns_allHealthPermissions() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        Set<String> healthPermissions = HealthConnectManager.getHealthPermissions(context);
        for (String permission : HEALTH_PERMISSIONS) {
            assertThat(healthPermissions.contains(permission)).isTrue();
        }
    }

    private void assertHealthPermissionIsDefined(String permissionName) throws Exception {
        PermissionInfo info =
                mPackageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
        assertThat(info.getProtection()).isEqualTo(PermissionInfo.PROTECTION_DANGEROUS);
    }
}
