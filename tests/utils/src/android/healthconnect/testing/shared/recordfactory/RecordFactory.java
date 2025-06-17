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

package android.healthconnect.testing.shared.recordfactory;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_RING;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;

import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.healthfitness.flags.Flags;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public abstract class RecordFactory<T extends Record> {

    static final String PREFIX = "android.healthconnect.testing.shared";

    public static final ZonedDateTime YESTERDAY_11AM =
            LocalDate.now(ZoneId.systemDefault())
                    .minusDays(1)
                    .atTime(11, 0)
                    .atZone(ZoneId.systemDefault());

    public static final ZonedDateTime MIDNIGHT_ONE_WEEK_AGO =
            YESTERDAY_11AM.truncatedTo(ChronoUnit.DAYS).minusDays(7);

    public static final LocalDateTime YESTERDAY_10AM_LOCAL =
            LocalDate.now(ZoneId.systemDefault()).minusDays(1).atTime(LocalTime.parse("10:00"));

    /**
     * Returns a record with all possible fields of this data type set to non-default values.
     *
     * <p>Used to make sure all fields get successfully written and returned from Health Connect end
     * to end.
     */
    public abstract T newFullRecord(Metadata metadata, Instant startTime, Instant endTime);

    /**
     * Returns a record with all possible fields of this data type set to non-default values.
     *
     * <p>This is similar to {@link #newFullRecord(Metadata, Instant, Instant)} however every field
     * should have a different value. This is used to test that all fields successfully get updated
     * on the server side.
     */
    public abstract T anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime);

    /**
     * Returns a record with all optional fields of this data type unset or set to default values.
     *
     * <p>Used to make sure null/default values are handled correctly by the server side.
     */
    public abstract T newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime);

    /** Returns a copy of the given record with the metadata set to the provided one. */
    protected abstract T recordWithMetadata(T record, Metadata metadata);

    /**
     * Returns a bundle containing the values specific to this data type.
     *
     * <p>The returned bundle is then used by {@link #newRecordFromValuesBundle} to recreate the
     * record.
     *
     * <p>Metadata, start & end time, start & end zone offset should not be populated as they are
     * handled separately.
     *
     * <p>Used by multi-app tests to send the record across the test apps.
     */
    protected abstract Bundle getValuesBundleForRecord(T record);

    /** Returns a bundle containing the values specific to this data type. */
    public final Bundle getValuesBundle(Record record) {
        return getValuesBundleForRecord((T) record);
    }

    /** Recreates the record from the given values bundle and metadata. */
    public abstract T newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle);

    /** Converts the given record into a string representation */
    public String recordToString(T record) {
        // TODO(b/424728751): Implement and make this abstract.
        throw new UnsupportedOperationException(
                "recordToString not implemented for " + record.getClass());
    }

    /** Converts the given record into a string representation */
    public String toString(Record record) {
        return recordToString((T) record);
    }

    /** Returns a string representation of the given metadata. */
    public static String metadataToString(Metadata metadata) {
        return "Metadata {"
                + "\n\tid = "
                + metadata.getId()
                + ",\n\tclientRecordId = "
                + metadata.getClientRecordId()
                + ",\n\tclientRecordVersion = "
                + metadata.getClientRecordVersion()
                + ",\n\tdataOrigin = "
                + metadata.getDataOrigin().getPackageName()
                + ",\n\tlastModifiedTime = "
                + metadata.getLastModifiedTime()
                + ",\n\trecordingMethod = "
                + metadata.getRecordingMethod()
                + ",\n\tdevice = "
                + deviceToString(metadata.getDevice())
                + "\n}";
    }

    private static String deviceToString(Device device) {
        if (device == null) {
            return "null";
        }

        return "Device {"
                + "\n\t\ttype = "
                + device.getType()
                + ",\n\t\tmanufacturer = "
                + device.getManufacturer()
                + ",\n\t\tmodel = "
                + device.getModel()
                + "\n\t}";
    }

    /** Returns the record with id and package name overridden by the given ones. */
    public final T recordWithIdAndPackageName(Record record, String id, String packageName) {
        return recordWithMetadata(
                (T) record,
                toBuilder(record.getMetadata())
                        .setId(id)
                        .setDataOrigin(new DataOrigin.Builder().setPackageName(packageName).build())
                        .build());
    }

    /** Returns a default instance of metadata with no fields populated. */
    public static Metadata newEmptyMetadata() {
        return new Metadata.Builder().build();
    }

    /** Returns a default instance of metadata with no fields populated except the id. */
    public static Metadata newEmptyMetadataWithId(String id) {
        return new Metadata.Builder().setId(id).build();
    }

    /** Returns a default instance of metadata with no fields populated except the client id. */
    public static Metadata newEmptyMetadataWithClientId(String clientId) {
        return new Metadata.Builder().setClientRecordId(clientId).build();
    }

    /** Returns a default instance of metadata with no fields populated except id and client id. */
    public static Metadata newEmptyMetadataWithIdClientIdAndVersion(
            String id, String clientId, int version) {
        return new Metadata.Builder()
                .setId(id)
                .setClientRecordId(clientId)
                .setClientRecordVersion(version)
                .build();
    }

    /**
     * Returns an instance of metadata with all possible fields populated except ids and version.
     */
    public static Metadata newFullMetadataWithoutIds() {
        return newFullMetadataBuilderWithoutIds().build();
    }

    /**
     * Returns an instance of metadata builder with all possible fields populated except ids and
     * version.
     */
    private static Metadata.Builder newFullMetadataBuilderWithoutIds() {
        return new Metadata.Builder()
                .setDataOrigin(new DataOrigin.Builder().setPackageName("foo.package.name").build())
                .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                .setDevice(
                        new Device.Builder()
                                .setType(DEVICE_TYPE_WATCH)
                                .setManufacturer("foo-manufacturer")
                                .setModel("foo-model")
                                .build())
                .setLastModifiedTime(Instant.now().minusSeconds(120));
    }

    /**
     * Returns an instance of metadata with all possible fields populated except client id and
     * version.
     */
    public static Metadata newFullMetadataWithId(String id) {
        return newFullMetadataBuilderWithoutIds().setId(id).build();
    }

    /** Returns an instance of metadata with all possible fields populated except id and version. */
    public static Metadata newFullMetadataWithClientId(String clientId) {
        return newFullMetadataBuilderWithoutIds().setClientRecordId(clientId).build();
    }

    /** Returns an instance of metadata with all possible fields populated except id. */
    public static Metadata newFullMetadataWithClientIdAndVersion(String clientId, int version) {
        return newFullMetadataBuilderWithoutIds()
                .setClientRecordId(clientId)
                .setClientRecordVersion(version)
                .build();
    }

    /**
     * Same as {@link #newFullMetadataWithId(String)} but all fields are set to different values.
     */
    public static Metadata newAnotherFullMetadataWithId(String id) {
        return newAnotherFullMetadataBuilderWithoutIds().setId(id).build();
    }

    /**
     * Same as {@link #newFullMetadataWithClientId(String)} but all fields are set to different
     * values.
     */
    public static Metadata newAnotherFullMetadataWithClientId(String clientId) {
        return newAnotherFullMetadataBuilderWithoutIds().setClientRecordId(clientId).build();
    }

    /**
     * Same as {@link #newFullMetadataWithClientIdAndVersion(String, int)} but all fields are set to
     * different values.
     */
    public static Metadata newAnotherFullMetadataWithClientIdAndVersion(
            String clientId, int version) {
        return newAnotherFullMetadataBuilderWithoutIds()
                .setClientRecordId(clientId)
                .setClientRecordVersion(version)
                .build();
    }

    /**
     * Same as {@link #newFullMetadataBuilderWithoutIds()} but all fields are set to different
     * values.
     */
    private static Metadata.Builder newAnotherFullMetadataBuilderWithoutIds() {
        return new Metadata.Builder()
                .setDataOrigin(new DataOrigin.Builder().setPackageName("bar.package.name").build())
                .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                .setDevice(
                        new Device.Builder()
                                .setType(DEVICE_TYPE_RING)
                                .setManufacturer("bar-manufacturer")
                                .setModel("bar-model")
                                .build())
                .setLastModifiedTime(Instant.now().minusSeconds(234));
    }

    /** Converts the given metadata to the corresponding builder. */
    private static Metadata.Builder toBuilder(Metadata metadata) {
        return new Metadata.Builder()
                .setId(metadata.getId())
                .setLastModifiedTime(metadata.getLastModifiedTime())
                .setRecordingMethod(metadata.getRecordingMethod())
                .setDataOrigin(metadata.getDataOrigin())
                .setDevice(metadata.getDevice())
                .setClientRecordVersion(metadata.getClientRecordVersion())
                .setClientRecordId(metadata.getClientRecordId());
    }

    /** Returns a record test helper for given data type. */
    @Nullable
    public static RecordFactory<? extends Record> forDataType(Class<? extends Record> recordClass) {
        if (recordClass.equals(ActivityIntensityRecord.class)) {
            return new ActivityIntensityRecordFactory();
        } else if (recordClass.equals(MindfulnessSessionRecord.class)) {
            return new MindfulnessSessionRecordFactory();
        } else if (Flags.smoking() && recordClass.equals(NicotineIntakeRecord.class)) {
            return new NicotineIntakeRecordFactory();
        } else if (recordClass.equals(StepsRecord.class)) {
            return new StepsRecordFactory();
        }
        return null;
    }

    /** Returns a full record for given data type. */
    public static Record newFullRecordForType(Class<? extends Record> recordClass) {
        RecordFactory<? extends Record> factory = forDataType(recordClass);
        if (factory == null) {
            throw new UnsupportedOperationException(
                    "Record class is not supported by record factory: " + recordClass);
        }
        return factory.newFullRecord(
                RecordFactory.newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                Instant.now().minusSeconds(200),
                Instant.now().minusSeconds(100));
    }
}
