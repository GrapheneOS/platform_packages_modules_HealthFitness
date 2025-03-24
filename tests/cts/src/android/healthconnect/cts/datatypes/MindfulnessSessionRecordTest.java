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

package android.healthconnect.cts.datatypes;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_BAND;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class MindfulnessSessionRecordTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                SkinTemperatureRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Before
    public void setUp() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void mindfulnessSessionRecordBuilder_allFieldsSet() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata =
                new Metadata.Builder()
                        .setClientRecordId("clientRecordId")
                        .setClientRecordVersion(123)
                        .setDataOrigin(
                                new DataOrigin.Builder().setPackageName("package.name").build())
                        .setId("id-foo-bar")
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                        .setDevice(
                                new Device.Builder()
                                        .setType(DEVICE_TYPE_FITNESS_BAND)
                                        .setManufacturer("manufacturer")
                                        .setModel("model")
                                        .build())
                        .setLastModifiedTime(endTime.minusSeconds(15))
                        .build();

        MindfulnessSessionRecord record =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_MINDFULNESS_SESSION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(startZoneOffset);
        assertThat(record.getEndZoneOffset()).isEqualTo(endZoneOffset);
        assertThat(record.getMindfulnessSessionType()).isEqualTo(MINDFULNESS_SESSION_TYPE_OTHER);
        assertThat(record.getTitle()).isEqualTo("title");
        assertThat(record.getNotes()).isEqualTo("notes");
    }

    @Test
    public void mindfulnessSessionRecordBuilder_optionalFieldsUnset() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord record =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_MEDITATION)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_MINDFULNESS_SESSION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(startTime));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(endTime));
        assertThat(record.getMindfulnessSessionType())
                .isEqualTo(MINDFULNESS_SESSION_TYPE_MEDITATION);
        assertThat(record.getTitle()).isNull();
        assertThat(record.getNotes()).isNull();
    }

    @Test
    public void mindfulnessSessionRecordBuilder_invalidMindfulnessSessionType() {
        MindfulnessSessionRecord.Builder builder =
                new MindfulnessSessionRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minusSeconds(60),
                        Instant.now(),
                        54);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void equals_hashCode_allFieldsEqual_recordsEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isEqualTo(recordB);
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_metadataNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadataA = new Metadata.Builder().setId("id-a").build();
        Metadata metadataB = new Metadata.Builder().setId("id-b").build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadataA, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadataB, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_startTimeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTimeA = endTime.minusSeconds(60);
        Instant startTimeB = endTime.minusSeconds(120);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTimeA, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTimeB, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_endTimeNotEqual_recordsNotEqual() {
        Instant endTimeA = Instant.now();
        Instant endTimeB = endTimeA.minusSeconds(1);
        Instant startTime = endTimeA.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTimeA, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTimeB, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_sessionTypeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_titleNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("titleA")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("titleB")
                        .setNotes("notes")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_notesNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesA")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesB")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_startZoneOffsetNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffsetA = ZoneOffset.ofHours(2);
        ZoneOffset startZoneOffsetB = ZoneOffset.ofHours(1);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesA")
                        .setStartZoneOffset(startZoneOffsetA)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesA")
                        .setStartZoneOffset(startZoneOffsetB)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_endZoneOffsetNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffsetA = ZoneOffset.ofHours(-3);
        ZoneOffset endZoneOffsetB = ZoneOffset.ofHours(-2);
        Metadata metadata = new Metadata.Builder().build();

        MindfulnessSessionRecord recordA =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesA")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetA)
                        .build();

        MindfulnessSessionRecord recordB =
                new MindfulnessSessionRecord.Builder(
                                metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_BREATHING)
                        .setTitle("title")
                        .setNotes("notesA")
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetB)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    private static ZoneOffset getDefaultZoneOffset(Instant instant) {
        return ZoneOffset.systemDefault().getRules().getOffset(instant);
    }
}
