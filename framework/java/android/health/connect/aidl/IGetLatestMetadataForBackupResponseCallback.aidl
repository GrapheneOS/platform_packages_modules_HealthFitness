package android.health.connect.aidl;

import android.health.connect.backuprestore.GetLatestMetadataForBackupResponse;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link HealthConnectManager#getLatestMetadataForBackup}
 * {@hide}
 */
interface IGetLatestMetadataForBackupResponseCallback {
    // Called on a successful operation
    oneway void onResult(in GetLatestMetadataForBackupResponse parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
