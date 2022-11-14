package android.healthconnect.aidl;

import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.IReadRecordsResponseCallback;
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
     * Inserts {@param records} into the HealthConnect database.
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
}
