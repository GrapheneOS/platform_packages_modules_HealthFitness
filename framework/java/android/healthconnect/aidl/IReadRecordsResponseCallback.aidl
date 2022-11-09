package android.healthconnect.aidl;

import android.healthconnect.aidl.ReadRecordsResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#readRecord}.
 *
 * {@hide}
 */
interface IReadRecordsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in ReadRecordsResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
