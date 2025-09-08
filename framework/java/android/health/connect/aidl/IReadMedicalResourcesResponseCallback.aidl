package android.health.connect.aidl;

import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#readMedicalResources}.
 *
 * @hide
 */
interface IReadMedicalResourcesResponseCallback {
    // Called on a successful operation
    oneway void onResult(in ReadMedicalResourcesResponse parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
