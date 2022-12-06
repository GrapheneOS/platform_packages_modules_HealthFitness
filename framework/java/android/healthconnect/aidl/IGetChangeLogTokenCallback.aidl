package android.healthconnect.aidl;

import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getChangeLogToken}
 *
 * {@hide}
 */
interface IGetChangeLogTokenCallback {
    // Called on a successful operation
    oneway void onResult(String token);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
