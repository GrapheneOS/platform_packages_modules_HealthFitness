package android.health.connect.aidl;

import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link HealthConnectService#canRestore}
 * @hide
 */
interface ICanRestoreResponseCallback {
    // Called on a successful operation
    oneway void onResult(in boolean canRestore);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
