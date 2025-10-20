package android.health.connect.aidl;

import android.health.connect.MedicalResourceTypeInfo;
import android.health.connect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#queryAllMedicalResourceTypeInfos}.
 *
 * @hide
 */
interface IMedicalResourceTypeInfosCallback {
    // Called on a successful operation
    oneway void onResult(in List<MedicalResourceTypeInfo> response);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
