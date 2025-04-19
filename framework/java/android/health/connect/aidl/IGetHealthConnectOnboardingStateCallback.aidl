package android.health.connect.aidl;

import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.HealthConnectOnboardingState;

/**
 * Callback for {@link IHealthConnectService#getHealthConnectOnboardingState}.
 * @hide
 */
interface IGetHealthConnectOnboardingStateCallback {
    oneway void onResult(in HealthConnectOnboardingState healthConnectOnboardingState);
    oneway void onError(in HealthConnectExceptionParcel exception);
}