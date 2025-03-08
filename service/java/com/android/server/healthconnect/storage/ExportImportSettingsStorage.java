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

package com.android.server.healthconnect.storage;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_ERROR_UNSPECIFIED;

import static com.android.healthfitness.flags.Flags.exportImportFastFollow;

import android.annotation.Nullable;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.exportimport.ImportStatus;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.exportimport.ScheduledExportStatus;
import android.health.connect.exportimport.ScheduledExportStatus.DataExportError;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.time.Instant;

/**
 * Stores the settings for the scheduled export service, including settings that are exposed to the
 * UI via ScheduledExportStatus and values used exclusively by the system server for logic and
 * logging.
 *
 * @hide
 */
public final class ExportImportSettingsStorage {
    // Scheduled Export Settings
    public static final String EXPORT_URI_PREFERENCE_KEY = "export_uri_key";
    public static final String EXPORT_PERIOD_PREFERENCE_KEY = "export_period_key";

    // Scheduled Export State
    private static final String LAST_SUCCESSFUL_EXPORT_PREFERENCE_KEY =
            "last_successful_export_key";
    private static final String LAST_FAILED_EXPORT_PREFERENCE_KEY = "last_failed_export_key";
    private static final String LAST_EXPORT_ERROR_PREFERENCE_KEY = "last_export_error_key";
    private static final String LAST_SUCCESSFUL_EXPORT_URI_PREFERENCE_KEY =
            "last_successful_export_uri_key";
    private static final String NEXT_EXPORT_SEQUENTIAL_NUMBER_PREFERENCE_KEY =
            "next_export_sequential_number_key";

    // Import State
    private static final String IMPORT_STATE_PREFERENCE_KEY = "import_state_key";

    private static final String EXPORT_REPEAT_ERROR_ON_RETRY_COUNT_KEY =
            "repeat_error_on_retry_count";

    private static final String TAG = "HealthConnectExportImport";

    private final PreferenceHelper mPreferenceHelper;

    public ExportImportSettingsStorage(PreferenceHelper preferenceHelper) {
        mPreferenceHelper = preferenceHelper;
    }

    /**
     * Configures the settings for the scheduled export of Health Connect data.
     *
     * @param settings Settings to use for the scheduled export. Use null to clear the settings.
     */
    public void configure(@Nullable ScheduledExportSettings settings) {
        if (settings != null) {
            configureNonNull(settings);
        } else {
            clear();
        }
    }

    /** Configures the settings for the scheduled export of Health Connect data. */
    private void configureNonNull(ScheduledExportSettings settings) {
        if (settings.getUri() != null) {
            Uri uri = settings.getUri();
            mPreferenceHelper.insertOrReplacePreference(EXPORT_URI_PREFERENCE_KEY, uri.toString());
            String lastExportError =
                    mPreferenceHelper.getPreference(LAST_EXPORT_ERROR_PREFERENCE_KEY);
            if (lastExportError != null) {
                mPreferenceHelper.removeKey(LAST_EXPORT_ERROR_PREFERENCE_KEY);
            }
            if (exportImportFastFollow()) {
                String previousExportSequentialNumber =
                        mPreferenceHelper.getPreference(
                                NEXT_EXPORT_SEQUENTIAL_NUMBER_PREFERENCE_KEY);
                if (previousExportSequentialNumber == null) {
                    mPreferenceHelper.insertOrReplacePreference(
                            NEXT_EXPORT_SEQUENTIAL_NUMBER_PREFERENCE_KEY, String.valueOf(1));
                } else {
                    int nextSequentialNumber = Integer.parseInt(previousExportSequentialNumber) + 1;
                    mPreferenceHelper.insertOrReplacePreference(
                            NEXT_EXPORT_SEQUENTIAL_NUMBER_PREFERENCE_KEY,
                            String.valueOf(nextSequentialNumber));
                }
            }
        }

        if (settings.getPeriodInDays() != DEFAULT_INT) {
            String periodInDays = String.valueOf(settings.getPeriodInDays());
            mPreferenceHelper.insertOrReplacePreference(EXPORT_PERIOD_PREFERENCE_KEY, periodInDays);
        }
    }

    /** Clears the settings for the scheduled export of Health Connect data. */
    private void clear() {
        mPreferenceHelper.removeKey(EXPORT_URI_PREFERENCE_KEY);
        mPreferenceHelper.removeKey(EXPORT_PERIOD_PREFERENCE_KEY);
    }

    /** Gets scheduled export URI for exporting Health Connect data. */
    public Uri getUri() {
        String result = mPreferenceHelper.getPreference(EXPORT_URI_PREFERENCE_KEY);
        if (result == null) throw new IllegalArgumentException("Export URI cannot be null.");
        return Uri.parse(result);
    }

    /** Get the uri of the last successful export. */
    public @Nullable Uri getLastSuccessfulExportUri() {
        String result = mPreferenceHelper.getPreference(LAST_SUCCESSFUL_EXPORT_URI_PREFERENCE_KEY);
        return result == null ? null : Uri.parse(result);
    }

    /** Get the time of the last successful export. */
    public @Nullable Instant getLastSuccessfulExportTime() {
        String result = mPreferenceHelper.getPreference(LAST_SUCCESSFUL_EXPORT_PREFERENCE_KEY);
        return result == null ? null : Instant.ofEpochMilli(Long.parseLong(result));
    }

    /** Gets scheduled export period for exporting Health Connect data. */
    public int getScheduledExportPeriodInDays() {
        String result = mPreferenceHelper.getPreference(EXPORT_PERIOD_PREFERENCE_KEY);

        if (result == null) return 0;
        return Integer.parseInt(result);
    }

    /** Set the last successful export time for the currently configured export. */
    public void setLastSuccessfulExport(Instant instant, Uri uri) {
        mPreferenceHelper.insertOrReplacePreference(
                LAST_SUCCESSFUL_EXPORT_PREFERENCE_KEY, String.valueOf(instant.toEpochMilli()));
        mPreferenceHelper.removeKey(LAST_EXPORT_ERROR_PREFERENCE_KEY);
        mPreferenceHelper.insertOrReplacePreference(
                LAST_SUCCESSFUL_EXPORT_URI_PREFERENCE_KEY, uri.toString());
    }

    /** Convenience method for getting the last recorded Export error. */
    public @DataExportError int getLastExportError() {
        String lastError = mPreferenceHelper.getPreference(LAST_EXPORT_ERROR_PREFERENCE_KEY);
        if (lastError != null) {
            return Integer.parseInt(lastError);
        }
        return DATA_EXPORT_ERROR_UNSPECIFIED;
    }

    /** Set errors and time during the last failed export attempt. */
    public void setLastExportError(
            @ScheduledExportStatus.DataExportError int error, Instant instant) {
        mPreferenceHelper.insertOrReplacePreference(
                LAST_EXPORT_ERROR_PREFERENCE_KEY, String.valueOf(error));
        mPreferenceHelper.insertOrReplacePreference(
                LAST_FAILED_EXPORT_PREFERENCE_KEY, String.valueOf(instant.toEpochMilli()));
    }

    /** Get the status of the currently scheduled export. */
    public ScheduledExportStatus getScheduledExportStatus(Context context) {
        String lastExportTime =
                mPreferenceHelper.getPreference(LAST_SUCCESSFUL_EXPORT_PREFERENCE_KEY);
        String lastFailedExportTime =
                mPreferenceHelper.getPreference(LAST_FAILED_EXPORT_PREFERENCE_KEY);
        String lastExportError = mPreferenceHelper.getPreference(LAST_EXPORT_ERROR_PREFERENCE_KEY);
        String periodInDays = mPreferenceHelper.getPreference(EXPORT_PERIOD_PREFERENCE_KEY);
        String nextExportSequentialNumber =
                exportImportFastFollow()
                        ? mPreferenceHelper.getPreference(
                                NEXT_EXPORT_SEQUENTIAL_NUMBER_PREFERENCE_KEY)
                        : String.valueOf(0);

        String lastExportFileName = null;
        String lastExportAppName = null;
        String nextExportFileName = null;
        String nextExportAppName = null;

        String nextExportUriString = mPreferenceHelper.getPreference(EXPORT_URI_PREFERENCE_KEY);
        if (nextExportUriString != null) {
            Uri uri = Uri.parse(nextExportUriString);
            nextExportAppName = getExportAppName(context, uri);
            nextExportFileName = getExportFileName(context, uri);
        }

        String lastSuccessfulExportUriString =
                mPreferenceHelper.getPreference(LAST_SUCCESSFUL_EXPORT_URI_PREFERENCE_KEY);
        if (lastSuccessfulExportUriString != null) {
            Uri uri = Uri.parse(lastSuccessfulExportUriString);
            lastExportAppName = getExportAppName(context, uri);
            lastExportFileName = getExportFileName(context, uri);
        }

        return new ScheduledExportStatus.Builder()
                .setLastSuccessfulExportTime(
                        lastExportTime == null
                                ? null
                                : Instant.ofEpochMilli(Long.parseLong(lastExportTime)))
                .setLastFailedExportTime(
                        lastFailedExportTime == null
                                ? null
                                : Instant.ofEpochMilli(Long.parseLong(lastFailedExportTime)))
                .setDataExportError(
                        lastExportError == null
                                ? ScheduledExportStatus.DATA_EXPORT_ERROR_NONE
                                : Integer.parseInt(lastExportError))
                .setNextExportSequentialNumber(
                        nextExportSequentialNumber == null
                                ? 0
                                : Integer.parseInt(nextExportSequentialNumber))
                .setPeriodInDays(periodInDays == null ? 0 : Integer.parseInt(periodInDays))
                .setLastExportFileName(lastExportFileName)
                .setLastExportAppName(lastExportAppName)
                .setNextExportFileName(nextExportFileName)
                .setNextExportAppName(nextExportAppName)
                .build();
    }

    /**
     * Get the number of times that the current export has failed with the same error message during
     * retries.
     */
    public int getExportRepeatErrorOnRetryCount() {
            String repeatErrorOnRetry_count =
                    mPreferenceHelper.getPreference(EXPORT_REPEAT_ERROR_ON_RETRY_COUNT_KEY);
            return (repeatErrorOnRetry_count == null)
                    ? 0
                    : Integer.parseInt(repeatErrorOnRetry_count);
    }

    /**
     * Increase the number of times that the current export has failed with the same error message
     * during retries. Should be called when an export fails during retries.
     */
    public void increaseExportRepeatErrorOnRetryCount() {
            String repeatErrorOnRetry_count =
                    mPreferenceHelper.getPreference(EXPORT_REPEAT_ERROR_ON_RETRY_COUNT_KEY);
            int count =
                    (repeatErrorOnRetry_count == null)
                            ? 0
                            : Integer.parseInt(repeatErrorOnRetry_count);
            count++;
            mPreferenceHelper.insertOrReplacePreference(
                    EXPORT_REPEAT_ERROR_ON_RETRY_COUNT_KEY, String.valueOf(count));
    }

    /**
     * Reset the count of times that the current export has failed with the same error message
     * during retries. Should be called when an export succeeds, when the current error message does
     * not match the previous error message, when export settings are changed by the user and when
     * retries finish/a new regular export is scheduled.
     */
    public void resetExportRepeatErrorOnRetryCount() {
            mPreferenceHelper.removeKey(EXPORT_REPEAT_ERROR_ON_RETRY_COUNT_KEY);
    }

    /** Set the state of an import to started, success or an error of the last import attempt. */
    public void setImportState(@ImportStatus.DataImportState int state) {
        mPreferenceHelper.insertOrReplacePreference(
                IMPORT_STATE_PREFERENCE_KEY, String.valueOf(state));
    }

    /** Get the status of the last data import. */
    public ImportStatus getImportStatus() {
        String lastImportState = mPreferenceHelper.getPreference(IMPORT_STATE_PREFERENCE_KEY);

        return new ImportStatus(
                lastImportState == null
                        ? ImportStatus.DATA_IMPORT_ERROR_NONE
                        : Integer.parseInt(lastImportState));
    }

    /** Get the file name of either the last or the next export, depending on the passed uri. */
    private static @Nullable String getExportFileName(Context context, Uri destinationUri) {
        try (Cursor cursor =
                context.getContentResolver().query(destinationUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        } catch (IllegalArgumentException exception) {
            Slog.i(TAG, "Failed to get the file name", exception);
        }
        return null;
    }

    /** Get the app name of either the last or the next export, depending on the passed uri. */
    private static @Nullable String getExportAppName(Context context, Uri destinationUri) {
        try (ContentProviderClient contentProviderClient =
                context.getContentResolver().acquireUnstableContentProviderClient(destinationUri)) {
            if (contentProviderClient != null) {
                Uri rootsUri = DocumentsContract.buildRootsUri(destinationUri.getAuthority());
                try (Cursor contentProviderCursor =
                        contentProviderClient.query(rootsUri, null, null, null, null)) {
                    if (contentProviderCursor != null && contentProviderCursor.moveToFirst()) {
                        String appName =
                                contentProviderCursor.getString(
                                        contentProviderCursor.getColumnIndexOrThrow(
                                                DocumentsContract.Root.COLUMN_TITLE));
                        return appName;
                    }
                }
            }
        } catch (RemoteException | SecurityException | IllegalArgumentException exception) {
            Slog.e(TAG, "Failed to get the app name", exception);
        }
        return null;
    }
}
