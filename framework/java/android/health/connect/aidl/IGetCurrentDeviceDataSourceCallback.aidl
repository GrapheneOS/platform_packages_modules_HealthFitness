package android.health.connect.aidl;

import android.health.connect.DeviceDataSource;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#getCurrentDeviceDataSource}.
 * @hide
 */
oneway interface IGetCurrentDeviceDataSourceCallback {
    void onResult(in DeviceDataSource result);
    void onError(in HealthConnectExceptionParcel exception);
}
