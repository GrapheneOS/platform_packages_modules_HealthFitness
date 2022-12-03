package android.healthconnect.aidl;

import android.healthconnect.aidl.AggregateDataResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#aggregateRecords}.
 *
 * {@hide}
 */
interface IAggregateRecordsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in AggregateDataResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
