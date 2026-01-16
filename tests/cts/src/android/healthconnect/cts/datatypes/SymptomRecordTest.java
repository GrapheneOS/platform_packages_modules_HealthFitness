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

package android.healthconnect.cts.datatypes;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SYMPTOM;
import static android.health.connect.datatypes.SymptomRecord.SEVERITY_MILD;
import static android.health.connect.datatypes.SymptomRecord.SEVERITY_MODERATE;
import static android.health.connect.datatypes.SymptomRecord.SYMPTOM_TYPE_COUGH;

import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.SymptomRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB})
public class SymptomRecordTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Before
    public void setUp() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test(expected = IllegalArgumentException.class)
    public void intervalBuilder_startTimeAfterEndTime_throws() {
        Instant startTime = Instant.now();
        Instant endTime = startTime.minusSeconds(60);
        Metadata metadata = new Metadata.Builder().build();
        new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata);
    }

    @Test
    public void instantBuilder_allFieldsSet() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata)
                        .setSeverity(SEVERITY_MILD)
                        .setNotes("notes")
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_SYMPTOM);
        assertThat(record.getSymptomType()).isEqualTo(SYMPTOM_TYPE_COUGH);
        assertThat(record.getSeverity()).isEqualTo(SEVERITY_MILD);
        assertThat(record.getNotes()).isEqualTo("notes");
        assertThat(record.getStartTime()).isEqualTo(time);
        assertThat(record.getEndTime()).isEqualTo(time);
        assertThat(record.getStartZoneOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(record.getEndZoneOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(record.getDate()).isNull();
    }

    @Test
    public void intervalBuilder_allFieldsSet() {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(60);
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                        .setSeverity(SEVERITY_MODERATE)
                        .setNotes("notes")
                        .setCount(2)
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_SYMPTOM);
        assertThat(record.getSymptomType()).isEqualTo(SYMPTOM_TYPE_COUGH);
        assertThat(record.getSeverity()).isEqualTo(SEVERITY_MODERATE);
        assertThat(record.getNotes()).isEqualTo("notes");
        assertThat(record.getCount()).isEqualTo(2);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(record.getEndZoneOffset()).isEqualTo(ZoneOffset.ofHours(2));
        assertThat(record.getDate()).isNull();
    }

    @Test
    public void localDateBuilder_allFieldsSet() {
        LocalDate date = LocalDate.now(ZoneId.systemDefault());
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, date, metadata)
                        .setSeverity(SEVERITY_MILD)
                        .setNotes("notes")
                        .setCount(5)
                        .build();
        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_SYMPTOM);
        assertThat(record.getSymptomType()).isEqualTo(SYMPTOM_TYPE_COUGH);
        assertThat(record.getSeverity()).isEqualTo(SEVERITY_MILD);
        assertThat(record.getNotes()).isEqualTo("notes");
        assertThat(record.getCount()).isEqualTo(5);
        assertThat(record.getStartTime())
                .isEqualTo(
                        date.atStartOfDay()
                                .toInstant(
                                        ZoneOffset.systemDefault()
                                                .getRules()
                                                .getOffset(date.atStartOfDay())));
        assertThat(record.getEndTime())
                .isEqualTo(
                        date.atTime(23, 59, 59, 999_999_999)
                                .toInstant(
                                        ZoneOffset.systemDefault()
                                                .getRules()
                                                .getOffset(date.atTime(23, 59, 59, 999_999_999))));
        assertThat(record.getStartZoneOffset())
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(date.atStartOfDay()));
        assertThat(record.getEndZoneOffset())
                .isEqualTo(
                        ZoneOffset.systemDefault()
                                .getRules()
                                .getOffset(date.atTime(23, 59, 59, 999_999_999)));
        assertThat(record.getDate()).isEqualTo(date);
    }

    @Test
    public void equals_allFieldsMatch_recordsAreEqual() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record1 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata)
                        .setSeverity(SEVERITY_MILD)
                        .setNotes("notes")
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        SymptomRecord record2 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata)
                        .setSeverity(SEVERITY_MILD)
                        .setNotes("notes")
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        assertThat(record1).isEqualTo(record2);
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    public void equals_oneFieldDiffers_recordsAreNotEqual() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record1 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata)
                        .setSeverity(SEVERITY_MILD)
                        .setNotes("notes")
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        SymptomRecord record2 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata)
                        .setSeverity(SEVERITY_MODERATE) // Different severity
                        .setNotes("notes")
                        .setStartZoneOffset(ZoneOffset.ofHours(1))
                        .setEndZoneOffset(ZoneOffset.ofHours(2))
                        .build();
        assertThat(record1).isNotEqualTo(record2);
        assertThat(record1.hashCode()).isNotEqualTo(record2.hashCode());
    }

    @Test
    public void equals_null_returnsFalse() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record1 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata).build();
        assertThat(record1.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        SymptomRecord record1 =
                new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata).build();
        assertThat(record1.equals(new Object())).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void instantBuilder_setCount_throws() {
        Instant time = Instant.now();
        Metadata metadata = new Metadata.Builder().build();
        new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, time, metadata).setCount(5);
    }

    @Test(expected = IllegalStateException.class)
    public void localDateBuilder_setStartZoneOffset_throws() {
        LocalDate date = LocalDate.now(ZoneId.systemDefault());
        Metadata metadata = new Metadata.Builder().build();
        new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, date, metadata)
                .setStartZoneOffset(ZoneOffset.ofHours(1));
    }

    @Test(expected = IllegalStateException.class)
    public void localDateBuilder_setEndZoneOffset_throws() {
        LocalDate date = LocalDate.now(ZoneId.systemDefault());
        Metadata metadata = new Metadata.Builder().build();
        new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, date, metadata)
                .setEndZoneOffset(ZoneOffset.ofHours(1));
    }
}
