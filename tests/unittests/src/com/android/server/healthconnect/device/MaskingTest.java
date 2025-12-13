/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.healthconnect.device;

import static com.google.common.truth.Truth.assertWithMessage;

import android.annotation.IntDef;
import android.health.connect.aidl.IHealthConnectService;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
public class MaskingTest {
    /**
     * TLDR: go/hc-masking
     *
     * <p>A registry of Health Connect Service APIs that are required to apply privacy masking to
     * their responses.
     *
     * <p><strong>Security Context:</strong> Device Data Providers (DDP) introduce unique
     * identifiers for devices, implemented as synthetic package names that blend with standard app
     * package names. To prevent cross-application tracking or device fingerprinting, APIs that
     * return {@link Record} objects or package names must mask the source identity. Instead of the
     * internal device identifier package, a synthetic alias (masked name) must be returned to the
     * caller.
     *
     * <p><strong>Maintenance Guide:</strong> If you are adding a new method to {@code
     * IHealthConnectService}:
     *
     * <ol>
     *   <li><strong>Analyze:</strong> Does the response contain package names, {@code Record}
     *       objects, or any data derived from specific app identities?
     *   <li><strong>Consult:</strong> Refer to <i>go/hc-masking</i> for the implementation guide
     *       and decision tree.
     *   <li><strong>Implement:</strong> If yes, apply the masking logic in the service
     *       implementation.
     *   <li><strong>Register:</strong> Add the method name to this set.
     *   <li><strong>Verify:</strong> Add a corresponding CTS test to {@link
     *       android.healthconnect.cts.device.MaskingTest}. You must assert that the returned value
     *       matches {@link #mMaskedDeviceName} (the masked alias).
     * </ol>
     *
     * @see #DOES_NOT_REQUIRE_MASKING_LIST
     */
    private static final Set<String> REQUIRES_MASKING_LIST =
            Set.of(
                    "aggregateRecords",
                    "readRecords",
                    "getChangeLogToken",
                    "getChangeLogs",
                    "getCurrentPriority",
                    "updatePriority",
                    "getContributorApplicationsInfo",
                    "queryAllRecordTypesInfo",
                    "getDeviceDataSourceInfos",
                    "getDeviceDataSources",
                    "getCurrentDeviceDataSource",
                    "recordMatchmakingDenial",
                    "advertiseDeviceDataSources",
                    "insertDeviceRecords",
                    "updateDeviceRecords",
                    "getCurrentDeviceId",
                    "readDeviceRecords",
                    "deleteDeviceRecords");

    /**
     * Neither the method arguments nor the return values contain package names or data origin data.
     */
    private static final @SkipMaskingReason int DOES_NOT_INCLUDE_PACKAGE_NAMES = 0;

    /**
     * The API is conceptually incompatible with Device clients. These methods represent flows that
     * are specific to applications (e.g., UI permission grants) or medical data. Invoking them from
     * a "Device" context is either strictly forbidden, results in a no-op, or leads to undefined
     * behavior. Since the code path is not traversable by devices, masking is not applicable.
     */
    private static final @SkipMaskingReason int DOES_NOT_INTERACT_WITH_DEVICES = 1;

    @IntDef({DOES_NOT_INCLUDE_PACKAGE_NAMES, DOES_NOT_INTERACT_WITH_DEVICES})
    @Retention(RetentionPolicy.SOURCE)
    private @interface SkipMaskingReason {}

    /**
     * A registry of Health Connect Service APIs that are explicitly <strong>exempt</strong> from
     * privacy masking.
     *
     * <p>This map enforces an "Exhaustiveness Check." Every public method in the service interface
     * <strong>must</strong> be categorized either in {@link #REQUIRES_MASKING_LIST} or in this
     * list. This ensures no API is added without a conscious security review regarding data
     * leakage.
     *
     * <p>The value associated with each key indicates the justification for the exemption, based on
     * {@link SkipMaskingReason}.
     */
    private static final Map<String, Integer> DOES_NOT_REQUIRE_MASKING_LIST =
            Map.<String, Integer>ofEntries(
                    Map.entry("grantHealthPermission", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("revokeHealthPermission", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("revokeAllHealthPermissions", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getGrantedHealthPermissions", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getHealthPermissionsFlags", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry(
                            "setHealthPermissionsUserFixedFlagValue",
                            DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry(
                            "getHistoricalAccessStartDateInMilliseconds",
                            DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("insertRecords", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("updateRecords", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("deleteUsingFilters", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("setRecordRetentionPeriodInDays", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getRecordRetentionPeriodInDays", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("queryAccessLogs", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getActivityDates", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("startMigration", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("finishMigration", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("writeMigrationData", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry(
                            "insertMinDataMigrationSdkExtensionVersion",
                            DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("stageAllHealthConnectRemoteData", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getAllDataForBackup", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getAllBackupFileNames", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("deleteAllStagedRemoteData", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("setLowerRateLimitsForTesting", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("updateDataDownloadState", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry(
                            "updateHealthConnectBackupAndRestoreSettings",
                            DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("updateHealthConnectRestoreStatus", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getHealthConnectDataState", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getHealthConnectMigrationUiState", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("configureScheduledExport", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getScheduledExportStatus", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getScheduledExportPeriodInDays", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getImportStatus", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("runImport", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("runImmediateExport", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("queryDocumentProviders", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("createMedicalDataSource", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getMedicalDataSourcesByIds", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getMedicalDataSourcesByRequest", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("deleteMedicalDataSourceWithData", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry(
                            "upsertMedicalResourcesFromRequestsParcel",
                            DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("readMedicalResourcesByIds", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("readMedicalResourcesByRequest", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("deleteMedicalResourcesByIds", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("deleteMedicalResourcesByRequest", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("queryAllMedicalResourceTypeInfos", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("updateHealthConnectBackupStatus", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getChangesForBackup", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getLatestMetadataForBackup", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("restoreLatestMetadata", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("canRestore", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("restoreChanges", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getHealthConnectOnboardingState", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("isMatchmakingPossible", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("getMatchingDataSources", DOES_NOT_INTERACT_WITH_DEVICES),
                    Map.entry("setTrackingEnabled", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("isTrackingEnabled", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("getDeviceDataSourceCapabilities", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("dump", DOES_NOT_INCLUDE_PACKAGE_NAMES),
                    Map.entry("hasUserEnabledTracking", DOES_NOT_INCLUDE_PACKAGE_NAMES));

    /**
     * Enforces that every method in {@code IHealthConnectService} has a defined security contract.
     *
     * <p>This test acts as a build-time safety net. It reflects over all methods in the service
     * interface and ensures they are listed in either the "Required" set or the "Exempt" map.
     *
     * <p><strong>Failure Scenario:</strong> If this test fails, it means a new API was added to the
     * service but was not reviewed for masking compliance. You must classify the new method into
     * one of the two lists.
     */
    @Test
    public void testHealthConnectServiceApis_setsMaskingRequirement() {
        Method[] allMethods = IHealthConnectService.class.getDeclaredMethods();
        for (Method m : allMethods) {
            assertWithMessage(
                            "Method '%s' does not belong to either"
                                    + " REQUIRES_MASKING_LIST or"
                                    + " DOES_NOT_REQUIRE_MASKING_LIST. Make sure that responses"
                                    + " exposing package are masked in the method implementation"
                                    + " before returned to callers, then add the method to"
                                    + " REQUIRES_MASKING_LIST (check the Javadoc for this constant"
                                    + " for more details). If the method does not handle package"
                                    + " names, add it to DOES_NOT_REQUIRE_MASKING_LIST.",
                            m.getName())
                    .that(
                            REQUIRES_MASKING_LIST.contains(m.getName())
                                    || DOES_NOT_REQUIRE_MASKING_LIST.containsKey(m.getName()))
                    .isTrue();

            assertWithMessage(
                            "Method '%s' can not belong to both DOES_NOT_REQUIRE_MASKING_LIST"
                                    + " and REQUIRES_MASKING_LIST.",
                            m.getName())
                    .that(
                            REQUIRES_MASKING_LIST.contains(m.getName())
                                    && DOES_NOT_REQUIRE_MASKING_LIST.containsKey(m.getName()))
                    .isFalse();
        }
    }
}
