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

package android.healthconnect.internal.datatypes;

import static android.healthconnect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Base class for all health connect datatype records.
 *
 * @hide
 */
public abstract class RecordInternal<T extends Record> {
    private final int mRecordIdentifier;
    private String mUuid;
    private String mPackageName;
    private long mLastModifiedTime = DEFAULT_LONG;
    private String mClientRecordId;
    private long mClientRecordVersion = DEFAULT_LONG;
    private String mManufacturer;
    private String mModel;
    private int mDeviceType;
    private long mDeviceInfoId = DEFAULT_LONG;
    private long mAppInfoId = DEFAULT_LONG;

    RecordInternal() {
        android.healthconnect.datatypes.Identifier annotation =
                this.getClass().getAnnotation(Identifier.class);
        Objects.requireNonNull(annotation);
        mRecordIdentifier = annotation.recordIdentifier();
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordType() {
        return mRecordIdentifier;
    }

    /**
     * Populates self with the data present in {@code parcel}. Reads should be in the same order as
     * write
     */
    public final void populateUsing(@NonNull Parcel parcel) {
        mUuid = parcel.readString();
        mPackageName = parcel.readString();
        mLastModifiedTime = parcel.readLong();
        mClientRecordId = parcel.readString();
        mClientRecordVersion = parcel.readLong();
        mManufacturer = parcel.readString();
        mModel = parcel.readString();
        mDeviceType = parcel.readInt();

        populateRecordFrom(parcel);
    }

    @SuppressWarnings("unchecked")
    public final void populateUsing(@NonNull Record record) throws ClassCastException {
        populateUsingInternal((T) record);
    }

    /**
     * Populates {@code parcel} with the self information, required to reconstructor this object
     * during IPC
     */
    @NonNull
    public final void writeToParcel(@NonNull Parcel parcel) {
        parcel.writeString(mUuid);
        parcel.writeString(mPackageName);
        parcel.writeLong(mLastModifiedTime);
        parcel.writeString(mClientRecordId);
        parcel.writeLong(mClientRecordVersion);
        parcel.writeString(mManufacturer);
        parcel.writeString(mModel);
        parcel.writeInt(mDeviceType);

        populateRecordTo(parcel);
    }

    @Nullable
    public String getUuid() {
        return mUuid;
    }

    @NonNull
    public RecordInternal<T> setUuid(@Nullable String uuid) {
        this.mUuid = uuid;
        return this;
    }

    @Nullable
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public RecordInternal<T> setPackageName(@Nullable String packageName) {
        this.mPackageName = packageName;
        return this;
    }

    public long getLastModifiedTime() {
        return mLastModifiedTime;
    }

    @NonNull
    public RecordInternal<T> setLastModifiedTime(long lastModifiedTime) {
        this.mLastModifiedTime = lastModifiedTime;
        return this;
    }

    @Nullable
    public String getClientRecordId() {
        return mClientRecordId;
    }

    @NonNull
    public RecordInternal<T> setClientRecordId(@Nullable String clientRecordId) {
        this.mClientRecordId = clientRecordId;
        return this;
    }

    public long getClientRecordVersion() {
        return mClientRecordVersion;
    }

    @NonNull
    public RecordInternal<T> setClientRecordVersion(long clientRecordVersion) {
        this.mClientRecordVersion = clientRecordVersion;
        return this;
    }

    @Nullable
    public String getManufacturer() {
        return mManufacturer;
    }

    @NonNull
    public RecordInternal<T> setManufacturer(@Nullable String manufacturer) {
        this.mManufacturer = manufacturer;
        return this;
    }

    @Nullable
    public String getModel() {
        return mModel;
    }

    @NonNull
    public RecordInternal<T> setModel(@Nullable String model) {
        this.mModel = model;
        return this;
    }

    @Device.DeviceType
    public int getDeviceType() {
        return mDeviceType;
    }

    @NonNull
    public RecordInternal<T> setDeviceType(@Device.DeviceType int deviceType) {
        this.mDeviceType = deviceType;
        return this;
    }

    public long getDeviceInfoId() {
        return mDeviceInfoId;
    }

    @NonNull
    public RecordInternal<T> setDeviceInfoId(long deviceInfoId) {
        this.mDeviceInfoId = deviceInfoId;
        return this;
    }

    public long getAppInfoId() {
        return mAppInfoId;
    }

    @NonNull
    public RecordInternal<T> setAppInfoId(long appInfoId) {
        this.mAppInfoId = appInfoId;
        return this;
    }

    /** Child class must implement this method and return an external record for this record */
    public abstract T toExternalRecord();

    /** Populates self with the data present in {@code bundle} */
    final void populateUsingInternal(@NonNull T record) {
        mUuid = record.getMetadata().getId();
        mPackageName = record.getMetadata().getDataOrigin().getPackageName();
        mLastModifiedTime = record.getMetadata().getLastModifiedTime().toEpochMilli();
        mClientRecordId = record.getMetadata().getClientRecordId();
        mClientRecordVersion = record.getMetadata().getClientRecordVersion();
        mManufacturer = record.getMetadata().getDevice().getManufacturer();
        mModel = record.getMetadata().getDevice().getModel();
        mDeviceType = record.getMetadata().getDevice().getType();

        populateRecordFrom(record);
    }

    @NonNull
    Metadata buildMetaData() {
        return new Metadata.Builder()
                .setClientRecordId(getClientRecordId())
                .setClientRecordVersion(getClientRecordVersion())
                .setDataOrigin(new DataOrigin.Builder().setPackageName(getPackageName()).build())
                .setId(getUuid())
                .setLastModifiedTime(Instant.ofEpochMilli(getLastModifiedTime()))
                .setDevice(
                        new Device.Builder()
                                .setManufacturer(getManufacturer())
                                .setType(getDeviceType())
                                .setModel(getModel())
                                .build())
                .build();
    }

    /**
     * @return the {@link LocalDate} object of this activity start time.
     */
    public abstract LocalDate getLocalDate();

    /**
     * Populate {@code bundle} with the data required to un-bundle self. This is used suring IPC
     * transmissions
     */
    abstract void populateRecordTo(@NonNull Parcel bundle);

    /**
     * Child class must implement this method and populates itself with the data present in {@code
     * bundle}
     */
    abstract void populateRecordFrom(@NonNull Parcel bundle);

    /**
     * Child class must implement this method and populates itself with the data present in {@code
     * record}
     */
    abstract void populateRecordFrom(@NonNull T record);
}
