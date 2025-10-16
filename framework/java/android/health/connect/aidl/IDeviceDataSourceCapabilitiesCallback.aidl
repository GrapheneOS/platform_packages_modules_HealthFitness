package android.health.connect.aidl;

import android.health.connect.aidl.DeviceDataSourceCapabilities;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link HealthConnectService#getDeviceDataSourceCapabilites}
 * @hide
 */
oneway interface IDeviceDataSourceCapabilitiesCallback {
    void onResult(in DeviceDataSourceCapabilities capabilities);
    oneway void onError(in HealthConnectExceptionParcel exception);
}
