package android.healthconnect.aidl;

import android.healthconnect.aidl.InsertRecordsResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#insertRecords}.
 *
 * {@hide}
 */
interface IInsertRecordsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in InsertRecordsResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
