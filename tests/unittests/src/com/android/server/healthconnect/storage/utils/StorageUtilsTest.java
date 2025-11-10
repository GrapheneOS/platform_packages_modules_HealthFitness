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

package healthconnect.storage.utils;

import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getFhirResourceId;
import static android.healthconnect.testing.unittest.StorageUtils.createEmptyDatabase;

import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.UUID_BYTE_SIZE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.bytesToUuids;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkColumnExists;
import static com.android.server.healthconnect.storage.utils.StorageUtils.generateMedicalResourceUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getDedupeByteBuffer;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getNormalisedString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getSingleByteArray;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.internal.datatypes.CyclePhasesRecordInternal;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.fitness.recordhelpers.StepsRecordHelper;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.request.CreateTableRequest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class StorageUtilsTest {
    @Test
    public void uuidToBytesAndBack_emptyList() {
        byte[] bytes = getSingleByteArray(List.of());
        assertThat(bytes.length).isEqualTo(0);
        assertThat(bytesToUuids(bytes)).isEmpty();
    }

    @Test
    public void uuidToBytesAndBack_oneUuid() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        byte[] bytes = getSingleByteArray(List.of(uuid1, uuid2));
        assertThat(bytes.length).isEqualTo(UUID_BYTE_SIZE * 2);
        assertThat(bytesToUuids(bytes)).containsExactly(uuid1, uuid2);
    }

    @Test
    public void generateMedicalResourceUuid_correctResult() throws JSONException {
        byte[] resourceIdBytes = getFhirResourceId(FHIR_DATA_IMMUNIZATION).getBytes();
        byte[] dataSourceIdBytes = DATA_SOURCE_ID.getBytes();
        byte[] bytes =
                ByteBuffer.allocate(
                                resourceIdBytes.length + Integer.BYTES + dataSourceIdBytes.length)
                        .put(resourceIdBytes)
                        .putInt(FHIR_RESOURCE_TYPE_IMMUNIZATION)
                        .put(dataSourceIdBytes)
                        .array();
        UUID expected = UUID.nameUUIDFromBytes(bytes);

        UUID result =
                generateMedicalResourceUUID(
                        getFhirResourceId(FHIR_DATA_IMMUNIZATION),
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        DATA_SOURCE_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getNormalisedString_quotedId_returnsQuotedId() {
        String id = "'id'";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("'id'");
    }

    @Test
    public void getNormalisedString_xQuotedId_returnsQuotedId() {
        String id = "x'id'";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("x'id'");
    }

    @Test
    public void getNormalisedString_unquotedId_returnsQuotedId() {
        String id = "id";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("'id'");
    }

    @Test
    public void getNormalisedString_quotedIdWithEscapedQuotes_returnsQuotedId() {
        String id = "'id with 'escaped' quotes'";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("'id with ''escaped'' quotes'");
    }

    @Test
    public void getNormalisedString_xQuotedIdWithEscapedQuotes_returnsQuotedId() {
        String id = "x'id with 'escaped' quotes'";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("x'id with ''escaped'' quotes'");
    }

    @Test
    public void getNormalisedString_unquotedIdWithEscapedQuotes_returnsQuotedId() {
        String id = "id with 'escaped' quotes";
        String result = getNormalisedString(id);
        assertThat(result).isEqualTo("'id with ''escaped'' quotes'");
    }

    @Test
    public void getDedupeByteBuffer_cyclePhases_usesLocalDate() {
        LocalDate date = LocalDate.of(2025, 11, 5);
        CyclePhasesRecordInternal record1 = new CyclePhasesRecordInternal();
        record1.setAppInfoId(1);
        record1.setStartTime(date.atStartOfDay().toInstant(ZoneOffset.ofHours(2)).toEpochMilli());
        record1.setStartZoneOffset(ZoneOffset.ofHours(2).getTotalSeconds());

        CyclePhasesRecordInternal record2 = new CyclePhasesRecordInternal();
        record2.setAppInfoId(1);
        record2.setStartTime(date.atStartOfDay().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
        record2.setStartZoneOffset(ZoneOffset.ofHours(8).getTotalSeconds());

        CyclePhasesRecordInternal record3 = new CyclePhasesRecordInternal();
        record3.setAppInfoId(1);
        record3.setStartTime(
                date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.ofHours(2)).toEpochMilli());
        record3.setStartZoneOffset(ZoneOffset.ofHours(2).getTotalSeconds());

        byte[] hash1 = getDedupeByteBuffer(record1);
        byte[] hash2 = getDedupeByteBuffer(record2);
        byte[] hash3 = getDedupeByteBuffer(record3);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    public void checkColumnExists_whenColumnExists_returnsTrue() {
        try (var db = createEmptyDatabase()) {
            HealthConnectDatabase.createTable(
                    db,
                    new CreateTableRequest(
                            "tableName", List.of(new Pair<>("columnName", TEXT_NULL))));

            assertThat(checkColumnExists(db, "tableName", "columnName")).isTrue();
        }
    }

    @Test
    public void checkColumnExists_whenColumnDoesNotExist_returnsFalse() {
        try (var db = createEmptyDatabase()) {
            HealthConnectDatabase.createTable(
                    db,
                    new CreateTableRequest(
                            "tableName", List.of(new Pair<>("columnName", TEXT_NULL))));

            assertThat(
                            checkColumnExists(
                                    db, StepsRecordHelper.STEPS_TABLE_NAME, "non_existent_column"))
                    .isFalse();
        }
    }

    @Test
    public void checkColumnExists_whenTableDoesNotExist_returnsFalse() {
        try (var db = createEmptyDatabase()) {
            HealthConnectDatabase.createTable(
                    db,
                    new CreateTableRequest(
                            "tableName", List.of(new Pair<>("columnName", TEXT_NULL))));

            assertThat(checkColumnExists(db, "non_existent_table", "columnName")).isFalse();
        }
    }
}
