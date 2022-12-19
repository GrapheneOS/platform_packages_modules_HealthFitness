package android.healthconnect.aidl;

import android.healthconnect.restore.StageRemoteDataException;

/**
 * Callback for {@link IHealthConnectService#stageAllHealthConnectRemoteData}.
 * @hide
 */
interface IDataStagingFinishedCallback {
    oneway void onResult();
    oneway void onError(in StageRemoteDataException exception);
}