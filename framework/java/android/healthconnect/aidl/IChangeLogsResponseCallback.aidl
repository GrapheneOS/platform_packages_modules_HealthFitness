package android.healthconnect.aidl;

import android.healthconnect.aidl.ChangeLogsResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link HealthConnectManager#getChangeLogs}
 * {@hide}
 */
interface IChangeLogsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in ChangeLogsResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
