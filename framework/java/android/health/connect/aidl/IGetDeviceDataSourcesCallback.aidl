package android.health.connect.aidl;

import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getDeviceDataSources}.
 * @hide
 */
oneway interface IGetDeviceDataSourcesCallback {
    void onResult(in GetDeviceDataSourcesResponse result);
    void onError(in HealthConnectExceptionParcel exception);
}
