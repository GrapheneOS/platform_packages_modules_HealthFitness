package android.healthconnect.aidl;

import android.healthconnect.aidl.GetPriorityResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getCurrentPriority}.
 *
 * {@hide}
 */
interface IGetPriorityResponseCallback {
    // Called on a successful operation
    oneway void onResult(in GetPriorityResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
