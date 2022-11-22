package android.healthconnect.aidl;

import android.healthconnect.aidl.AccessLogsResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link HealthConnectManager#queryAccessLogs}
 * {@hide}
 */
interface IAccessLogsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in AccessLogsResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
