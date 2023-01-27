package android.health.connect.aidl;

import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.ChangeLogTokenResponseParcel;

/**
 * Callback for {@link IHealthConnectService#getChangeLogToken}
 *
 * {@hide}
 */
interface IGetChangeLogTokenCallback {
    // Called on a successful operation
    oneway void onResult(in ChangeLogTokenResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
