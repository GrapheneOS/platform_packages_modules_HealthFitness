package android.health.connect.aidl;

import android.content.AttributionSource;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.aidl.ActivityDatesRequestParcel;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.IAccessLogsResponseCallback;
import android.health.connect.aidl.IActivityDatesResponseCallback;
import android.health.connect.aidl.IActivityDatesResponseCallback;
import android.health.connect.aidl.IAggregateRecordsResponseCallback;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetHealthConnectDataStateCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetSettingsForBackupResponseCallback;
import android.health.connect.aidl.IGetHealthConnectMigrationUiStateCallback;
import android.health.connect.aidl.IGetPriorityResponseCallback;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;
import android.health.connect.aidl.IMedicalResourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.IRecordTypeInfoResponseCallback;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.UpdatePriorityRequestParcel;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.BackupSettings;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.exportimport.IImportStatusCallback;
import android.health.connect.exportimport.IQueryDocumentProvidersCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.exportimport.IScheduledExportStatusCallback;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.migration.MigrationEntity;
import android.health.connect.migration.MigrationEntityParcel;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataRequest;
import android.health.connect.backuprestore.BackupChange;
import android.health.connect.backuprestore.RestoreChange;
import android.net.Uri;
import android.os.UserHandle;

import java.util.List;
import java.util.Map;

/**
 * Interface for {@link com.android.health.connect.HealthConnectManager}
 * {@hide}
 */
interface IHealthConnectService {
    void grantHealthPermission(String packageName, String permissionName, in UserHandle user);
    void revokeHealthPermission(String packageName, String permissionName, String reason, in UserHandle user);
    void revokeAllHealthPermissions(String packageName, String reason, in UserHandle user);
    List<String> getGrantedHealthPermissions(String packageName, in UserHandle user);

    /**
     * Returns a Map<String, Integer> from a permission name to permission flags.
     * @hide
     */
    Map getHealthPermissionsFlags(String packageName, in UserHandle user, in List<String> permissions);

    /**
     * @hide
     */
    void setHealthPermissionsUserFixedFlagValue(String packageName, in UserHandle user, in List<String> permissions, boolean value);

    /* @hide */
    long getHistoricalAccessStartDateInMilliseconds(String packageName, in UserHandle user);

    /**
     * Inserts {@code records} into the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param recordsParcel represents records to be inserted.
     * @param callback Callback to receive result of performing this operation.
     */
    void insertRecords(
        in AttributionSource attributionSource,
        in RecordsParcel recordsParcel,
        in IInsertRecordsResponseCallback callback);

    /**
     * Returns aggregation results based on the {@code request} into the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param request represents the request using which the aggregation is to be performed.
     * @param callback Callback to receive result of performing this operation.
     */
    void aggregateRecords(
        in AttributionSource attributionSource,
        in AggregateDataRequestParcel request,
        in IAggregateRecordsResponseCallback callback);

    /**
     * Reads from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param request represents the request to be read.
     * @param callback Callback to receive result of performing this operation.
     */
    void readRecords(
        in AttributionSource attributionSource,
        in ReadRecordsRequestParcel request,
        in IReadRecordsResponseCallback callback);

    /**
     * Updates {@param records} in the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param recordsParcel represents records to be updated.
     * @param callback Callback to receive result of performing this operation.
     */
    void updateRecords(
            in AttributionSource attributionSource,
            in RecordsParcel recordsParcel,
            in IEmptyResponseCallback callback);

    /**
     * @param packageName calling package name
     * @param request token request
     * @return a token that can be used with {@code getChanges(token)} to fetch the upsert and
     *     delete changes corresponding to {@code request}
     */
    void getChangeLogToken(
        in AttributionSource attributionSource,
        in ChangeLogTokenRequest request,
        in IGetChangeLogTokenCallback callback);

    /**
     * @param attributionSource attribution source for the data.
     * @param token request token from {@code getChangeLogToken}
     */
    void getChangeLogs(
        in AttributionSource attributionSource,
        in ChangeLogsRequest token,
        in IChangeLogsResponseCallback callback);

    /**
     * @param attributionSource attribution source for the data.
     * @param request Delete request using the mentioned filters
     * @param callback Callback to receive result of performing this operation
     */
    void deleteUsingFilters(
        in AttributionSource attributionSource,
        in DeleteUsingFiltersRequestParcel request,
        in IEmptyResponseCallback callback);

    /**
     * @param attributionSource attribution source for the data.
     * @param request Delete request using the mentioned filters
     * @param callback Callback to receive result of performing this operation
     */
    void deleteUsingFiltersForSelf(
        in AttributionSource attributionSource,
        in DeleteUsingFiltersRequestParcel request,
        in IEmptyResponseCallback callback);

    /**
     * @param permissionCategory PermissionCategory corresponding to which priority is requested
     * @param callback Callback to receive result of performing this operation
     */
    void getCurrentPriority(
        int permissionCategory,
        in IGetPriorityResponseCallback callback);

    /**
     * @param packageName Calling package's name
     * @param request Update request with the required priority changes
     * @param callback Callback to receive result of performing this operation
     */
    void updatePriority(
        in UpdatePriorityRequestParcel request,
        in IEmptyResponseCallback callback);

    /** Sets record rention period for HC DB */
    void setRecordRetentionPeriodInDays(
        int days,
        in UserHandle userHandle,
        in IEmptyResponseCallback callback);

    /** Gets record rention period for HC DB */
    int getRecordRetentionPeriodInDays(in UserHandle userHandle);

    /**
     * Returns information, represented by {@code ApplicationInfoResponse}, for all the
     * packages that have contributed to the health connect DB.
     *
     * @param callback Callback to receive result of performing this operation.
     */
    void getContributorApplicationsInfo(in IApplicationInfoResponseCallback callback);

    /** Returns information for each RecordType like health permission category, record category and
     * contributing packages.
     * @param callback Callback to receive result of performing this operation.
     */
    void queryAllRecordTypesInfo(in IRecordTypeInfoResponseCallback callback);

    /**
     * @param packageName name of the package reading access logs
     * @param callback Callback to receive result of performing this operation
     */
    void queryAccessLogs(
        String packageName,
        in IAccessLogsResponseCallback callback);

    /**
     * Returns a list of unique dates for which at least one record type has at least one entry.
     *
     * @param recordTypes List of record types classes for which to get the activity dates.
     * @param callback Callback to receive the result of performing this operation.
     * {@hide}
     */
    void getActivityDates(
        in ActivityDatesRequestParcel recordTypes,
        in IActivityDatesResponseCallback callback);

    /**
     * Marks the start of the migration.
     *
     * @param packageName calling package name
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void startMigration(String packageName, in IMigrationCallback callback);

    /**
     * Marks the end of the migration.
     *
     * @param packageName calling package name
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void finishMigration(String packageName, in IMigrationCallback callback);

    /**
     * Writes given entities to the module database.
     *
     * @param packageName calling package name
     * @param entities List of {@link MigrationEntity} to migrate.
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void writeMigrationData(
        String packageName,
        in MigrationEntityParcel parcel,
        in IMigrationCallback callback);

    /**
     * @param packageName calling package name
     * @param callback Callback to receive a result or an error encountered while performing this
     * operation.
     */
    void insertMinDataMigrationSdkExtensionVersion(
        String packageName,
        int requiredSdkExtension,
        in IMigrationCallback callback);

    /**
     * Stages all HealthConnect remote data and returns any errors in a callback. Errors encountered
     * for all the files are shared in the provided callback.
     *
     * @param pfdsByFileName The map of file names and their {@link ParcelFileDescriptor}s.
     * @param executor       The {@link Executor} on which to invoke the callback.
     * @param callback       The callback which will receive the outcome of this call.
     * @throws NullPointerException if null is passed for any of the required {@link NonNull}
     *                              parameters.
     * @hide
     */
    void stageAllHealthConnectRemoteData(in StageRemoteDataRequest stageRemoteDataRequest,
            in UserHandle userHandle, in IDataStagingFinishedCallback callback);

    /**
     * Copies all HealthConnect backup data in the passed FDs.
     *
     * <p>The shared data should later be sent for cloud backup or to another device for backup.
     *
     * <p>We are responsible for closing the original file descriptors. The caller must not close
     * the FD before that.
     *
     * @param pfdsByFileName The map of file names and their {@link ParcelFileDescriptor}s.
     * @hide
     */
    void getAllDataForBackup(in StageRemoteDataRequest stageRemoteDataRequest, in UserHandle userHandle);

    /**
     * Shares the names of all HealthConnect backup files
     *
     * @hide
     */
    BackupFileNamesSet getAllBackupFileNames(in boolean forDeviceToDevice);

    /**
     * Deletes all previously staged HealthConnect data from the disk.
     * For testing purposes only.
     *
     * @hide
     */
    void deleteAllStagedRemoteData(in UserHandle userHandle);

    /**
     * Updates the download state of the Health Connect data.
     *
     * @param downloadState The download state which needs to be purely one of:
     *                      {@link HealthConnectManager#CLOUD_DOWNLOAD_STARTED}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_RETRY}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_FAILED}, {@link
     *                      HealthConnectManager#CLOUD_DOWNLOAD_COMPLETE}
     * @hide
     */
     void updateDataDownloadState(int downloadState);

    /**
     * Asynchronously returns the current state of the Health Connect data as it goes through the Data-Restore and/or the Data-Migration process.
     *
     * <p>See also {@link HealthConnectDataState} object describing the HealthConnect state.
     *
     * @param callback The callback which will receive the current {@link HealthConnectDataState}.
     *
     * @hide
     */
    void getHealthConnectDataState(in IGetHealthConnectDataStateCallback callback);

    /**
     * Asynchronously returns the current UI state of Health Connect as it goes through the Data-Migration process.
     *
     * <p>See also {@link HealthConnectMigrationUiState} object describing the HealthConnect UI state.
     *
     * @param callback The callback which will receive the current {@link HealthConnectMigrationUiState}.
     *
     * @hide
     */
    void getHealthConnectMigrationUiState(in IGetHealthConnectMigrationUiStateCallback callback);

    /**
    * Configures the settings for the scheduled export of Health Connect data.
    *
    * @param settings Settings to use for the scheduled export. Use null to clear the settings.
    *
    * @hide
    */
    void configureScheduledExport(in @nullable ScheduledExportSettings settings, in UserHandle userHandle);

    /**
    * Gets the period in days between scheduled exports of Health Connect data.
    *
    * @hide
    */
    int getScheduledExportPeriodInDays(in UserHandle userHandle);

    /**
    * Queries the document providers available to be used for export/import.
    *
    * @hide
    */
    void queryDocumentProviders(in UserHandle userHandle, in IQueryDocumentProvidersCallback callback);

    /**
    * Gets the status of the currently scheduled export.
    *
    * @hide
    */
    void getScheduledExportStatus(in UserHandle userHandle, in IScheduledExportStatusCallback callback);

    /**
     * Allows setting lower rate limits in tests.
     *
     * @hide
     */
    void setLowerRateLimitsForTesting(in boolean enabled);

    /**
    * Gets the status of the ongoing data import.
    *
    * @hide
    */
    void getImportStatus(in UserHandle userHandle, in IImportStatusCallback callback);

    /**
    * Imports the given compressed database file.
    *
    * @hide
    */
    void runImport(in UserHandle userHandle, in Uri file, in IEmptyResponseCallback callback);

    /**
    * Triggers an immediate export of health connect data.
    *
    * @hide
    */
    void runImmediateExport(in Uri file, in IEmptyResponseCallback callback);

    /**
     * Creates a {@code MedicalDataSource} in HealthConnect based on the {@code request} values.
     *
     * @param attributionSource attribution source for the data.
     * @param request Creation request.
     * @param callback Callback to receive result of performing this operation.
     */
    void createMedicalDataSource(
            in AttributionSource attributionSource,
            in CreateMedicalDataSourceRequest request,
            in IMedicalDataSourceResponseCallback callback);

    /**
     * Gets {@code MedicalDataSource}s in HealthConnect matching the given ids.
     *
     * @param attributionSource attribution source for the data.
     * @param ids the ids for which datasources to fetch.
     * @param callback Callback to receive result of performing this operation.
     */
    void getMedicalDataSourcesByIds(
            in AttributionSource attributionSource,
            in List<String> ids,
            in IMedicalDataSourcesResponseCallback callback);

    /**
     * Gets {@code MedicalDataSource}s in HealthConnect based on the {@code request} values.
     *
     * @param attributionSource attribution source for the data.
     * @param request specification for which datasources to fetch.
     * @param callback Callback to receive result of performing this operation.
     */
    void getMedicalDataSourcesByRequest(
            in AttributionSource attributionSource,
            in GetMedicalDataSourcesRequest request,
            in IMedicalDataSourcesResponseCallback callback);

    /**
     * Deletes a {@code MedicalDataSource} in HealthConnect including all the data contained in it.
     *
     * <p>If the datasource does not exist, the operation will fail.
     *
     * @param attributionSource attribution source for the data.
     * @param id the datasource to delete, returned earlier from {@code createMedicalDataSource}
     * @param callback Callback to receive result of performing this operation.
     */
    void deleteMedicalDataSourceWithData(
            in AttributionSource attributionSource,
            in String id,
            in IEmptyResponseCallback callback);

    /**
     * Upserts {@link MedicalResource}s in HealthConnect based on a list of {@link
     * UpsertMedicalResourceRequest}s.
     *
     * @param attributionSource attribution source for the data.
     * @param requests A list of upsert requests.
     * @param callback Callback to receive result of performing this operation.
     */
    void upsertMedicalResources(
        in AttributionSource attributionSource,
        in List<UpsertMedicalResourceRequest> requests,
        in IMedicalResourcesResponseCallback callback);

    /**
     * Upserts {@link MedicalResource}s in HealthConnect based on a {@link
     * UpsertMedicalResourceRequestsParcel}.
     *
     * @param attributionSource attribution source for the data.
     * @param requestsParcel Contains the list of upsert requests.
     * @param callback Callback to receive result of performing this operation.
     */
    void upsertMedicalResourcesFromRequestsParcel(
        in AttributionSource attributionSource,
        in UpsertMedicalResourceRequestsParcel requestsParcel,
        in IMedicalResourceListParcelResponseCallback callback);

    /**
     * Reads from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param medicalResourceIds represents the ids to be read.
     * @param callback Callback to receive result of performing this operation.
     */
    void readMedicalResourcesByIds(
        in AttributionSource attributionSource,
        in List<MedicalResourceId> medicalResourceIds,
        in IReadMedicalResourcesResponseCallback callback);

    /**
     * Reads from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param request Read request.
     * @param callback Callback to receive result of performing this operation.
     */
    void readMedicalResourcesByRequest(
        in AttributionSource attributionSource,
        in ReadMedicalResourcesRequestParcel request,
        in IReadMedicalResourcesResponseCallback callback);

    /**
     * Delete from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param medicalResourceIds represents the ids to be deleted.
     * @param callback Callback to receive result of performing this operation.
     */
    void deleteMedicalResourcesByIds(
        in AttributionSource attributionSource,
        in List<MedicalResourceId> medicalResourceIds,
        in IEmptyResponseCallback callback);

    /**
     * Delete from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param request represents a request specifying what to delete.
     * @param callback Callback to receive result of performing this operation.
     */
    void deleteMedicalResourcesByRequest(
        in AttributionSource attributionSource,
        in DeleteMedicalResourcesRequest request,
        in IEmptyResponseCallback callback);

    /**
     * Returns information for each MedicalResourceType like medical permission category and
     * contributing data sources.
     *
     * @param callback Callback to receive result of performing this operation.
     */
    void queryAllMedicalResourceTypeInfos(in IMedicalResourceTypeInfosCallback callback);

    /**
     * Returns the paganized changes for cloud backup based on the changeToken.
     *
     * @param changeToken Indicates whether and where to resume to the data backup.
     * @param callback Callback to receive result of performing this operation.
     */
    void getChangesForBackup(in @nullable String changeToken, in IGetChangesForBackupResponseCallback callback);

    /**
     * Returns the settings for cloud backup.
     *
     * @param callback Callback to receive result of performing this operation.
     */
    void getSettingsForBackup(in IGetSettingsForBackupResponseCallback callback);

    /**
     * Restores the backed up user settings.
     *
     * @param backupSettings Settings that were previously backed up.
     * @param callback Callback to receive result of performing this operation.
     */
    void restoreSettings(in BackupSettings backupSettings, in IEmptyResponseCallback callback);

    /**
     * Returns whether the input data version can be restored.
     *
     * @param dataVersion Data version to be restored.
     * @param callback Callback to receive result of performing this operation.
     */
     void canRestore(in int dataVersion, in ICanRestoreResponseCallback callback);

    /**
     * Restores the backed up changes.
     *
     * @param changes Changes to be restored.
     * @param callback Callback to receive result of performing this operation.
     */
     void restoreChanges(in List<RestoreChange> changes, in byte[] appInfoMap, in IEmptyResponseCallback callback);
}
