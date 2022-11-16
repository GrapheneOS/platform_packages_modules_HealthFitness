package android.healthconnect.aidl;

import android.healthconnect.aidl.RecordTypeInfoResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#queryAllRecordTypesInfo}.
 *
 * {@hide}
 */
interface IRecordTypeInfoResponseCallback {
    // Called on a successful operation
    oneway void onResult(in RecordTypeInfoResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
