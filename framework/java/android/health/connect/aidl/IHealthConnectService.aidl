package android.health.connect.aidl;

import android.content.AttributionSource;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.MatchmakingRequest;
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
import android.health.connect.aidl.IDeviceDataSourceCapabilitiesCallback;
import android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetDeviceDataSourcesCallback;
import android.health.connect.aidl.IGetCurrentDeviceDataSourceCallback;
import android.health.connect.aidl.IGetDeviceDataSourceInfosCallback;
import android.health.connect.aidl.IGetHealthConnectDataStateCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetLatestMetadataForBackupResponseCallback;
import android.health.connect.aidl.IGetHealthConnectMigrationUiStateCallback;
import android.health.connect.aidl.IGetHealthConnectOnboardingStateCallback;
import android.health.connect.aidl.IGetPriorityResponseCallback;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.IRecordTypeInfoResponseCallback;
import android.health.connect.aidl.IIsMatchmakingPossibleCallback;
import android.health.connect.aidl.IGetMatchingDataSourcesCallback;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.ICanRestoreResponseCallback;
import android.health.connect.aidl.UpdatePriorityRequestParcel;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.BackupMetadata;
import android.health.connect.backuprestore.UpdateHealthConnectBackupStatusRequest;
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
import android.health.connect.backuprestore.UpdateBackupAndRestoreSettingsRequest;
import android.health.connect.backuprestore.RestoreChange;
import android.net.Uri;
import android.os.UserHandle;
import android.health.connect.device.DeviceDataAdvertisement;

import java.util.List;
import java.util.Map;

/**
 * Interface for {@link com.android.health.connect.HealthConnectManager}
 * @hide
 */
interface IHealthConnectService {
    void grantHealthPermission(String packageName, String permissionName, in UserHandle user);
    List<String> grantHealthPermissions(String packageName, in List<String> permissionNames, in UserHandle user);
    void revokeHealthPermission(String packageName, String permissionName, in @nullable @JavaPassthrough(annotation="@android.annotation.Nullable") String reason, in UserHandle user);
    List<String> revokeHealthPermissions(String packageName, in List<String> permissionNames, in @nullable @JavaPassthrough(annotation="@android.annotation.Nullable") String reason, in UserHandle user);
    void revokeAllHealthPermissions(String packageName, in @nullable @JavaPassthrough(annotation="@android.annotation.Nullable") String reason, in UserHandle user);
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
     * @param attributionSource attribution source for the data
     * @param permissionCategory PermissionCategory corresponding to which priority is requested
     * @param callback Callback to receive result of performing this operation
     */
    void getCurrentPriority(
        in AttributionSource attributionSource,
        int permissionCategory,
        in IGetPriorityResponseCallback callback);

    /**
     * @param attributionSource attribution source for the data
     * @param request Update request with the required priority changes
     * @param callback Callback to receive result of performing this operation
     */
    void updatePriority(
        in AttributionSource attributionSource,
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
     * @param attributionSource attribution source for the data
     * @param callback Callback to receive result of performing this operation.
     */
    void getContributorApplicationsInfo(in AttributionSource attributionSource, in IApplicationInfoResponseCallback callback);

    /** Returns information for each RecordType like health permission category, record category and
     * contributing packages.
     *
     * @param attributionSource attribution source for the data
     * @param callback Callback to receive result of performing this operation.
     */
    void queryAllRecordTypesInfo(in AttributionSource attributionSource, in IRecordTypeInfoResponseCallback callback);

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
     * @hide
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
    void configureScheduledExport(in @JavaPassthrough(annotation="@android.annotation.Nullable") ScheduledExportSettings settings, in UserHandle userHandle);

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
    void getChangesForBackup(in @nullable @JavaPassthrough(annotation="@android.annotation.Nullable") String changeToken, in IGetChangesForBackupResponseCallback callback);

    /**
     * Returns the latest metadata for cloud backup.
     *
     * @param callback Callback to receive result of performing this operation.
     */
    void getLatestMetadataForBackup(in IGetLatestMetadataForBackupResponseCallback callback);

    /**
     * Restores the backed up metadata.
     *
     * @param backupMetadata Metadata that were previously backed up and to be restored.
     * @param callback Callback to receive result of performing this operation.
     */
    void restoreLatestMetadata(in BackupMetadata backupMetadata, in IEmptyResponseCallback callback);

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
     void restoreChanges(in List<RestoreChange> changes, in IEmptyResponseCallback callback);

    /**
     * Updates settings for Health Connect backup and restore.
     *
     * @param request The request containing the changes to Backup and restore UI settings.
     */
    void updateHealthConnectBackupAndRestoreSettings(
            in UpdateBackupAndRestoreSettingsRequest request);

    /**
     * Updates the restore status in Health Connect.
     *
     * @param request The UpdateHealthConnectRestoreStatusRequest
     */
    void updateHealthConnectRestoreStatus(
            in UpdateHealthConnectRestoreStatusRequest request);

    /**
     * Updates the backup status in Health Connect.
     *
     * @param request The UpdateHealthConnectBackupStatusRequest
     */
    void updateHealthConnectBackupStatus(
            in UpdateHealthConnectBackupStatusRequest request);

    /**
     * Asynchronously returns the current onboarding state of the Health Connect user.
     *
     * <p>See also {@link HealthConnectOnboardingState} object describing the HealthConnect state.
     *
     * @param callback The callback which will receive the current {@link HealthConnectOnboardingState}.
     *
     * @hide
     */
    void getHealthConnectOnboardingState(in IGetHealthConnectOnboardingStateCallback callback);

    /**
     * Checks if there are any other data sources (applications and devices) available on the
     * user's device that could potentially supply new data for specific Record types.
     *
     * @param attributionSource attribution source for the data.
     * @param request request containing the {@link Record} types to check for.
     * @param callback Callback to receive result of performing this operation.
     */
    void isMatchmakingPossible(
            in AttributionSource attributionSource,
            in MatchmakingRequest request,
            in IIsMatchmakingPossibleCallback callback);

    /**
     * Returns all other data sources (applications and devices) available on the user's system that
     * could potentially supply new data for specific Record types.
     *
     * @param attributionSource attribution source for the data.
     * @param request request containing the {@link Record} types to check for.
     * @param callback Callback to receive result of performing this operation.
     */
    void getMatchingDataSources(
            in AttributionSource attributionSource,
            in MatchmakingRequest request,
            in IGetMatchingDataSourcesCallback callback);

    /**
     * Records that a user has denied matchmaking for a calling package, denied packages and their
     * denied permissions.
     *
     * @param callingPackageName package name of the app that initiated matchmaking.
     * @param matchingApps map of package name to permissions of the apps that were denied.
     * @param callback Callback to receive result of performing this operation.
     */
    void recordMatchmakingDenial(
            in AttributionSource attributionSource,
            String callingPackageName,
            in Map<String, List<String>> deniedApps,
            in IEmptyResponseCallback callback);

    /**
     * Enables or disables system/native tracking for the corresponding data type.
     *
     * @param dataTypePrefKey key for the data type to enable/disable tracking for.
     * @param enabled whether to enable or disable tracking.
     * @param callback Callback to receive result of performing this operation
     *
     * @hide
     */
    void setTrackingEnabled(String dataTypePrefKey, boolean enabled, in IEmptyResponseCallback callback);

    /**
     * Returns a Map<String, Boolean> with the data types and if system/native tracking is enabled.
     *
     * @param dataTypePrefKeys list of keys of data type to check tracking for.
     *
     * @hide
     */
    Map isTrackingEnabled(in List<String> dataTypePrefKeys);

    /**
     * Returns whether the user has enabled native tracking for a record type on the device that
     * Health Connect is currently running on.
     *
     * @param attributionSource attribution source for the data.
     * @param recordTypePrefKey key of record type to check tracking for.
     *
     * @hide
     */
    boolean hasUserEnabledTracking(
        in AttributionSource attributionSource,
        String recordTypePrefKey);

    /**
     * Retrieve a unique identifier of the device that Health Connect is currently running on.
     *
     * @param attributionSource attribution source for the data.
     *
     * @hide
     */
    String getCurrentDeviceId(in AttributionSource attributionSource);

    /**
     * Notify Health Connect of devices that can provide data and the data types each of them can
     * provide.
     *
     * <p>A device data source refers to a specific device that can provide data for any data types.
     * A device data type source refers to a specific device + data type combination.
     *
     * <p>A {@link DeviceDataAdvertisement} should be provided for each device data source. Each
     * {@link DeviceDataAdvertisement} should contain a set of {@link DeviceDataTypeAdvertisement}s
     * representing each data type supported by the device and to be used as a device data type
     * source.
     *
     * <p>This method must be called before data can be written for the advertised device data type
     * source. This should be called as frequently as needed to accurately describe the current
     * devices and statuses of supported data types. Every advertisement must represent the latest
     * state of <b>all</b> device data sources and device data type sources. Every subsequent
     * advertisement will override the previous device data sources and device data type sources. If
     * a device data source or device data type source is omitted from a subsequent advertisement,
     * it will be deleted.
     *
     * @hide
     */
    void advertiseDeviceDataSources(
        in AttributionSource attributionSource,
        in List<DeviceDataAdvertisement> deviceDataAdvertisements,
        in IEmptyResponseCallback callback);

    /**
     * Inserts {@code records} from a device data source into the Health Connect database.
     *
     * <p>Upon successful completion, {@link OutcomeReceiver#onResult} will be invoked for the
     * {@code callback}. The records returned in {@link InsertRecordsResponse} contain the unique
     * IDs of the input records. The values are in same order as {@code records}. In case of an
     * error or a permission failure in the Health Connect service, {@link OutcomeReceiver#onError}
     * will be invoked with a {@link HealthConnectException}.
     *
     * <p>The {@code deviceId} must match the one used in {@link DeviceDataAdvertisement} in the
     * latest call to {@link #advertiseDeviceDataSources}. A {@link Device} does not need to be
     * populated in the {@link Metadata} for a {@link Record} as it will automatically be populated
     * based on the {@link DeviceDataAdvertisement}.
     *
     * @param attributionSource attribution source for the data.
     * @param deviceId the identifier for the device that is the source of this data.
     * @param records list of records to be inserted.
     * @param callback callback to receive the result of performing this operation.
     * @throws RuntimeException for internal errors
     * @hide
     */
    void insertDeviceRecords(
        in AttributionSource attributionSource,
        in String deviceId,
        in RecordsParcel recordsParcel,
        in IInsertRecordsResponseCallback callback);

    /**
     * Updates {@code recordsParcel} from a device data source in the Health Connect database.
     *
     * <p>Before this method is called, {@link #advertiseDeviceDataSources} must have been called.
     *
     * <p>In case of an error or a permission failure the HealthConnect service, {@link
     * IEmptyResponseCallback#onError} will be invoked with a {@link HealthConnectException}.
     *
     * @param attributionSource attribution source for the data.
     * @param deviceId the identifier for the device that is the source of this data.
     * @param recordsParcel parcel for list of records to be updated.
     * @param callback callback to receive result of performing this operation.
     * @hide
     */
    void updateDeviceRecords(
        in AttributionSource attributionSource,
        in String deviceId,
        in RecordsParcel recordsParcel,
        in IEmptyResponseCallback callback);

    /**
     * Reads all device data from a device data source from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param request represents the request to be read.
     * @param callback Callback to receive result of performing this operation.
     *
     * @hide
     */
    void readDeviceRecords(
        in AttributionSource attributionSource,
        in ReadRecordsRequestParcel request,
        in IReadRecordsResponseCallback callback);

    /**
     * Deletes device data from a device data source from the HealthConnect database.
     *
     * @param attributionSource attribution source for the data.
     * @param deviceId the identifier for the device that is the source of this data.
     * @param request represents the request to be deleted.
     * @param callback Callback to receive result of performing this operation.
     *
     * @hide
     */
    void deleteDeviceRecords(
        in AttributionSource attributionSource,
        in String deviceId,
        in DeleteUsingFiltersRequestParcel request,
        in IEmptyResponseCallback callback);

    /**
     * Returns a set of record type classes that device data sources are capable of providing. Use
     * this method to avoid making unnecessary permission requests when reading device data.
     *
     * <p>This will filter out any sensitive data types, unless the caller holds the relevant
     * permissions.
     *
     * @param attributionSource The attribution source of the caller.
     * @param callback Callback to receive result of performing this operation.
     */
    void getDeviceDataSourceCapabilities(in AttributionSource attributionSource, in IDeviceDataSourceCapabilitiesCallback callback);

    /**
     * Retrieves information about device data sources.
     *
     * @param attributionSource attribution source for the data.
     * @param callback Callback to receive result of performing this operation.
     *
     * @hide
     */
    void getDeviceDataSources(
        in AttributionSource attributionSource,
        in IGetDeviceDataSourcesCallback callback);

    /**
     * Retrieves information about the current device data source.
     *
     * @param attributionSource attribution source for the data.
     * @param callback Callback to receive result of performing this operation.
     *
     * @hide
     */
    void getCurrentDeviceDataSource(
        in AttributionSource attributionSource,
        in IGetCurrentDeviceDataSourceCallback callback);

    /**
     * Retrieves the list of all device data sources and their provider info.
     *
     * @param attributionSource attribution source for the data.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    void getDeviceDataSourceInfos(
        in AttributionSource attributionSource,
        in IGetDeviceDataSourceInfosCallback callback);
}
