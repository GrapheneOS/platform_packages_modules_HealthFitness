package android.healthconnect.aidl;

import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback to for {@link HealthConnect} APIs with no result object
 * {@hide}
 */
interface IEmptyResponseCallback {
    // Called on a successful operation
    oneway void onResult();
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
