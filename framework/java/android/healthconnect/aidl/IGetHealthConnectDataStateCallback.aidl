package android.healthconnect.aidl;

import android.healthconnect.HealthConnectDataState;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getHealthConnectDataState}.
 * @hide
 */
interface IGetHealthConnectDataStateCallback {
    oneway void onResult(in HealthConnectDataState healthConnectDataState);
    oneway void onError(in HealthConnectExceptionParcel exception);
}