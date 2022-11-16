package android.healthconnect.aidl;

import android.healthconnect.aidl.ChangeLogTokenRequestParcel;
import android.healthconnect.aidl.DeleteUsingFiltersRequestParcel;
import android.healthconnect.aidl.IChangeLogsResponseCallback;
import android.healthconnect.aidl.IEmptyResponseCallback;
import android.healthconnect.aidl.IGetChangeLogTokenCallback;
import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.aidl.IApplicationInfoResponseCallback;
import android.healthconnect.aidl.IEmptyResponseCallback;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.IReadRecordsResponseCallback;
import android.healthconnect.aidl.IRecordTypeInfoResponseCallback;
import android.healthconnect.aidl.ReadRecordsRequestParcel;

import android.os.UserHandle;

import java.util.List;

/**
 * Interface for {@link com.android.healthconnect.HealthConnectManager}
 * {@hide}
 */
interface IHealthConnectService {
    /* @hide */
    void grantHealthPermission(String packageName, String permissionName, in UserHandle user);
    /* @hide */
    void revokeHealthPermission(String packageName, String permissionName, String reason, in UserHandle user);
    /* @hide */
    void revokeAllHealthPermissions(String packageName, String reason, in UserHandle user);
    /* @hide */
    List<String> getGrantedHealthPermissions(String packageName, in UserHandle user);
    /**
     * Inserts {@code records} into the HealthConnect database.
     *
     * @param packageName name of the package inserting the record.
     * @param recordsParcel represents records to be inserted.
     * @param callback Callback to receive result of performing this operation.
     * {@hide}
     */
    void insertRecords(
        String packageName,
        in RecordsParcel recordsParcel,
        in IInsertRecordsResponseCallback callback);

    /**
     * Reads from the HealthConnect database.
     *
     * @param packageName name of the package reading the record.
     * @param request represents the request to be read.
     * @param callback Callback to receive result of performing this operation.
     * {@hide}
     */
    void readRecords(
        in String packageName,
        in ReadRecordsRequestParcel request,
        in IReadRecordsResponseCallback callback);

    /**
     * Updates {@param records} in the HealthConnect database.
     *
     * @param packageName name of the package updating the record.
     * @param recordsParcel represents records to be updated.
     * @param callback Callback to receive result of performing this operation.
     * {@hide}
     */
    void updateRecords(
            String packageName,
            in RecordsParcel recordsParcel,
            in IEmptyResponseCallback callback);

    /**
     * @param packageName calling package name
     * @param request token request
     * @return a token that can be used with {@code getChanges(token)} to fetch the upsert and
     *     delete changes corresponding to {@code request}
     */
    void getChangeLogToken(
        String packageName,
        in ChangeLogTokenRequestParcel request,
        in IGetChangeLogTokenCallback callback);

    /**
     * @param packageName calling package name
     * @param token request token from {@code getChangeLogToken}
     */
    void getChangeLogs(
        String packageName,
        long token,
        in IChangeLogsResponseCallback callback);

    /**
     * @param packageName Calling package's name
     * @param request Delete request using the mentioned filters
     * @param callback Callback to receive result of performing this operation
     */
    void deleteUsingFilters(
        String packageName,
        in DeleteUsingFiltersRequestParcel request,
        in IEmptyResponseCallback callback);

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
     * {@hide}
     */
    void queryAllRecordTypesInfo(in IRecordTypeInfoResponseCallback callback);
}
