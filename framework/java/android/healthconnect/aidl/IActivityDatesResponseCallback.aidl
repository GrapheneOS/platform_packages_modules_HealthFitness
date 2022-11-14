package android.healthconnect.aidl;

import android.healthconnect.aidl.ActivityDatesResponseParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getActivityDates}.
 *
 * {@hide}
 */
interface IActivityDatesResponseCallback {
    // Called on a successful operation
    oneway void onResult(in ActivityDatesResponseParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}