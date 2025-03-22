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

package com.android.server.healthconnect.backuprestore;

import static com.android.server.healthconnect.backuprestore.RecordProtoConverter.PROTO_VERSION;

import android.annotation.Nullable;
import android.health.connect.backuprestore.BackupSettings;
import android.health.connect.backuprestore.RestoreChange;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Slog;

import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.logging.BackupRestoreLogger;
import com.android.server.healthconnect.proto.backuprestore.BackupData;
import com.android.server.healthconnect.proto.backuprestore.Record;
import com.android.server.healthconnect.proto.backuprestore.Settings;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages Cloud Restore operations.
 *
 * @hide
 */
public class CloudRestoreManager {

    private static final String TAG = "CloudRestoreManager";

    private final TransactionManager mTransactionManager;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final FitnessRecordReadHelper mFitnessRecordReadHelper;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final RecordProtoConverter mRecordProtoConverter = new RecordProtoConverter();
    private final HealthDataCategoryPriorityHelper mPriorityHelper;
    private final PreferenceHelper mPreferenceHelper;
    private final CloudBackupSettingsHelper mSettingsHelper;
    private final Clock mClock;
    private final BackupRestoreLogger mBackupRestoreLogger;

    public CloudRestoreManager(
            TransactionManager transactionManager,
            FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            FitnessRecordReadHelper fitnessRecordReadHelper,
            InternalHealthConnectMappings internalHealthConnectMappings,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            HealthDataCategoryPriorityHelper priorityHelper,
            PreferenceHelper preferenceHelper,
            Clock clock,
            BackupRestoreLogger backupRestoreLogger) {
        mTransactionManager = transactionManager;
        mFitnessRecordUpsertHelper = fitnessRecordUpsertHelper;
        mFitnessRecordReadHelper = fitnessRecordReadHelper;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mPriorityHelper = priorityHelper;
        mPreferenceHelper = preferenceHelper;
        mSettingsHelper =
                new CloudBackupSettingsHelper(priorityHelper, preferenceHelper, appInfoHelper);
        mClock = clock;
        mBackupRestoreLogger = backupRestoreLogger;
    }

    /** Takes the serialized user settings and overwrites existing settings. */
    public void restoreSettings(BackupSettings newSettings) {
        Slog.i(TAG, "Restoring user settings.");
        CloudBackupSettingsHelper cloudBackupSettingsHelper =
                new CloudBackupSettingsHelper(mPriorityHelper, mPreferenceHelper, mAppInfoHelper);

        byte[] data = newSettings.getData();
        Settings settings;
        try {
            settings = Settings.parseFrom(data);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to parse BackupSettings object back into"
                            + "Settings Record. Details: ",
                    e.getCause());
        }
        cloudBackupSettingsHelper.restoreUserSettings(settings);
    }

    /** Checks whether data with a certain version could be restored. */
    public boolean canRestore(int dataVersion) {
        return dataVersion <= PROTO_VERSION;
    }

    /** Restores backup data changes. */
    public void restoreChanges(List<RestoreChange> changes) {
        var protoRecords = changes.stream().map(this::toRecord).filter(Objects::nonNull).toList();

        Slog.i(TAG, "Creating app info");
        protoRecords.stream()
                .map(Record::getPackageName)
                .distinct()
                .forEach(
                        packageName ->
                                mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(packageName, null));
        Slog.i(TAG, "Created app info");

        Slog.i(TAG, "Restoring " + changes.size() + " changes");
        var internalRecords =
                protoRecords.stream().map(this::toRecordInternal).filter(Objects::nonNull).toList();
        removeNonExistentReferences(internalRecords);
        var insertedRecords =
                mFitnessRecordUpsertHelper.insertRecordsUnrestricted(
                        internalRecords, /* shouldGenerateChangeLog= */ true);

        protoRecords.stream()
                .collect(
                        Collectors.groupingBy(
                                Record::getPackageName,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        mRecordProtoConverter::getRecordTypeId,
                                        Collectors.toSet())))
                .forEach(
                        (packageName, recordTypes) ->
                                mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                                        recordTypes, packageName));
        Slog.i(TAG, "Restored " + insertedRecords.size() + " records out of " + changes.size());
    }

    /**
     * Removes references from exercise sessions to training plans that do not exist.
     *
     * <p>A training plan can be deleted before an exercise session that references it is restored.
     * In that scenario we will need to remove the reference before inserting to prevent a crash.
     */
    private void removeNonExistentReferences(List<? extends RecordInternal<?>> internalRecords) {
        Set<UUID> seenPlannedSessions = new HashSet<>();
        Set<ExerciseSessionRecordInternal> sessionsToCheck = new HashSet<>();
        for (var internalRecord : internalRecords) {
            if (internalRecord instanceof PlannedExerciseSessionRecordInternal plannedSession) {
                seenPlannedSessions.add(plannedSession.getUuid());
            } else if (internalRecord instanceof ExerciseSessionRecordInternal session) {
                var plannedSessionId = session.getPlannedExerciseSessionId();
                if (plannedSessionId != null && !seenPlannedSessions.contains(plannedSessionId)) {
                    sessionsToCheck.add(session);
                }
            }
        }
        List<RecordInternal<?>> existingPlannedSessions =
                mFitnessRecordReadHelper.readRecordsUnrestricted(
                        mTransactionManager,
                        Map.of(
                                RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION,
                                sessionsToCheck.stream()
                                        .map(
                                                ExerciseSessionRecordInternal
                                                        ::getPlannedExerciseSessionId)
                                        .filter(Objects::nonNull)
                                        .toList()));
        Set<UUID> existingPlannedSessionIds =
                existingPlannedSessions.stream()
                        .map(RecordInternal::getUuid)
                        .collect(Collectors.toSet());
        for (var session : sessionsToCheck) {
            if (!existingPlannedSessionIds.contains(session.getPlannedExerciseSessionId())) {
                session.setPlannedExerciseSessionId(null);
            }
        }
    }

    @Nullable
    private Record toRecord(RestoreChange backupChange) {
        try {
            return BackupData.parseFrom(backupChange.getData()).getRecord();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to parse record", e);
            return null;
        }
    }

    @Nullable
    private RecordInternal<?> toRecordInternal(Record record) {
        try {
            return mRecordProtoConverter.toRecordInternal(record);
        } catch (IllegalArgumentException e) {
            Slog.e(
                    TAG,
                    "Failed to convert record, likely because the record type is not supported",
                    e);
            throw e;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to convert record", e);
            return null;
        }
    }
}
