package android.healthconnect.aidl;

import android.healthconnect.aidl.HealthConnectExceptionParcel;
import android.healthconnect.aidl.ChangeLogTokenResponseParcel;

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
