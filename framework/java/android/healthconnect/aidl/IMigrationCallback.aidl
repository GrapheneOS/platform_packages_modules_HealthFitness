package android.healthconnect.aidl;

import android.healthconnect.migration.MigrationException;

/**
 * A callback for any error encountered by {@link HealthConnectManager#writeMigrationData}.
 * @hide
 */
interface IMigrationCallback {

    // Called when the batch is successfully saved
    oneway void onSuccess();

    // Called when an error is hit during the migration process
    oneway void onError(in MigrationException exception);
}
