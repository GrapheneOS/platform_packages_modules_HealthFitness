/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.cts.logging;

import static android.healthconnect.cts.HostSideTestUtil.grantPermissionsWithAdb;
import static android.healthconnect.cts.HostSideTestUtil.isHardwareSupported;
import static android.healthfitness.api.ApiMethod.CREATE_MEDICAL_DATA_SOURCE;
import static android.healthfitness.api.ApiMethod.DELETE_MEDICAL_DATA_SOURCE_WITH_DATA;
import static android.healthfitness.api.ApiMethod.DELETE_MEDICAL_RESOURCES_BY_IDS;
import static android.healthfitness.api.ApiMethod.DELETE_MEDICAL_RESOURCES_BY_REQUESTS;
import static android.healthfitness.api.ApiMethod.GET_MEDICAL_DATA_SOURCES_BY_IDS;
import static android.healthfitness.api.ApiMethod.GET_MEDICAL_DATA_SOURCES_BY_REQUESTS;
import static android.healthfitness.api.ApiMethod.READ_MEDICAL_RESOURCES_BY_IDS;
import static android.healthfitness.api.ApiMethod.READ_MEDICAL_RESOURCES_BY_REQUESTS;
import static android.healthfitness.api.ApiMethod.UPSERT_MEDICAL_RESOURCES;
import static android.healthfitness.api.ApiStatus.ERROR;
import static android.healthfitness.api.ApiStatus.SUCCESS;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.cts.statsdatom.lib.ConfigUtils;
import android.cts.statsdatom.lib.DeviceUtils;
import android.cts.statsdatom.lib.ReportUtils;
import android.healthconnect.cts.HostSideTestUtil;
import android.healthfitness.api.ApiMethod;
import android.healthfitness.api.ApiStatus;
import android.healthfitness.api.ForegroundState;
import android.healthfitness.api.RateLimit;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.host.HostFlagsValueProvider;

import com.android.os.StatsLog;
import com.android.os.healthfitness.api.ApiExtensionAtoms;
import com.android.os.healthfitness.api.HealthConnectApiCalled;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import com.google.protobuf.ExtensionRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(DeviceJUnit4ClassRunner.class)
public class HealthConnectServiceStatsTests extends BaseHostJUnit4Test implements IBuildReceiver {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule =
            HostFlagsValueProvider.createCheckFlagsRule(this::getDevice);

    private static final String TEST_APP_PKG_NAME = "android.healthconnect.cts.testhelper";
    private static final List<String> TEST_APP_PERMISSIONS =
            List.of(
                    "android.permission.health.WRITE_BLOOD_PRESSURE",
                    "android.permission.health.WRITE_HEART_RATE",
                    "android.permission.health.WRITE_STEPS",
                    "android.permission.health.READ_BLOOD_PRESSURE",
                    "android.permission.health.READ_HEART_RATE",
                    "android.permission.health.WRITE_MEDICAL_DATA",
                    "android.permission.health.READ_MEDICAL_DATA_VACCINES",
                    "android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES",
                    "android.permission.health.READ_MEDICAL_DATA_CONDITIONS",
                    "android.permission.health.READ_MEDICAL_DATA_LABORATORY_RESULTS",
                    "android.permission.health.READ_MEDICAL_DATA_MEDICATIONS",
                    "android.permission.health.READ_MEDICAL_DATA_PERSONAL_DETAILS",
                    "android.permission.health.READ_MEDICAL_DATA_PRACTITIONER_DETAILS",
                    "android.permission.health.READ_MEDICAL_DATA_PREGNANCY",
                    "android.permission.health.READ_MEDICAL_DATA_PROCEDURES",
                    "android.permission.health.READ_MEDICAL_DATA_SOCIAL_HISTORY",
                    "android.permission.health.READ_MEDICAL_DATA_VISITS",
                    "android.permission.health.READ_MEDICAL_DATA_VITAL_SIGNS");
    private IBuildInfo mCtsBuild;

    @Before
    public void setUp() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        // TODO(b/313055175): Do not disable rate limiting once b/300238889 is resolved.
        HostSideTestUtil.setupRateLimitingFeatureFlag(getDevice());
        assertThat(mCtsBuild).isNotNull();
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
        // b/396574091: Grant all permissions that the test helper app needs.
        // This is another attempt to pre-grant permissions to the test helper app, another attempt
        // was made in ag/31589102 but didn't seem to fix the problem.
        grantPermissionsWithAdb(getDevice(), TEST_APP_PKG_NAME, TEST_APP_PERMISSIONS);
    }

    @After
    public void tearDown() throws Exception {
        // TODO(b/313055175): Do not disable rate limiting once b/300238889 is resolved.
        HostSideTestUtil.restoreRateLimitingFeatureFlag(getDevice());
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = buildInfo;
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testCreateMedicalDataSourceSuccess() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testCreateMedicalDataSourceSuccess");

        assertApiStatus(data, CREATE_MEDICAL_DATA_SOURCE, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testCreateMedicalDataSourceError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testCreateMedicalDataSourceError");

        assertApiStatus(data, CREATE_MEDICAL_DATA_SOURCE, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testGetMedicalDataSourcesByIdsSuccess() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testGetMedicalDataSourcesByIdsSuccess");

        assertApiStatus(data, GET_MEDICAL_DATA_SOURCES_BY_IDS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testGetMedicalDataSourcesByIdsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testGetMedicalDataSourcesByIdsError");

        assertApiStatus(data, GET_MEDICAL_DATA_SOURCES_BY_IDS, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testGetMedicalDataSourcesByRequestSuccess()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testGetMedicalDataSourcesByRequestSuccess");

        assertApiStatus(data, GET_MEDICAL_DATA_SOURCES_BY_REQUESTS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testGetMedicalDataSourcesByRequestError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testGetMedicalDataSourcesByRequestError");

        assertApiStatus(data, GET_MEDICAL_DATA_SOURCES_BY_REQUESTS, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalDataSourceWithDataSuccess()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalDataSourceWithDataSuccess");

        assertApiStatus(data, DELETE_MEDICAL_DATA_SOURCE_WITH_DATA, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalDataSourceWithDataError()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalDataSourceWithDataError");

        assertApiStatus(data, DELETE_MEDICAL_DATA_SOURCE_WITH_DATA, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testUpsertMedicalResourcesSuccess() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testUpsertMedicalResourcesSuccess");

        assertApiStatus(data, UPSERT_MEDICAL_RESOURCES, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testUpsertMedicalResourcesError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testUpsertMedicalResourcesError");

        assertApiStatus(data, UPSERT_MEDICAL_RESOURCES, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testReadMedicalResourcesByIdsSuccess() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testReadMedicalResourcesByIdsSuccess");

        assertApiStatus(data, READ_MEDICAL_RESOURCES_BY_IDS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testReadMedicalResourcesByIdsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testReadMedicalResourcesByIdsError");

        assertApiStatus(data, READ_MEDICAL_RESOURCES_BY_IDS, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testReadMedicalResourcesByRequestsSuccess()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testReadMedicalResourcesByRequestsSuccess");

        assertApiStatus(data, READ_MEDICAL_RESOURCES_BY_REQUESTS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testReadMedicalResourcesByRequestsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testReadMedicalResourcesByRequestsError");

        assertApiStatus(data, READ_MEDICAL_RESOURCES_BY_REQUESTS, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalResourcesByIdsSuccess() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalResourcesByIdsSuccess");

        assertApiStatus(data, DELETE_MEDICAL_RESOURCES_BY_IDS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalResourcesByIdsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalResourcesByIdsError");

        assertApiStatus(data, DELETE_MEDICAL_RESOURCES_BY_IDS, ERROR);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalResourcesByRequestSuccess()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalResourcesByRequestSuccess");

        assertApiStatus(data, DELETE_MEDICAL_RESOURCES_BY_REQUESTS, SUCCESS);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void testPhrApiAndStatusLogs_testDeleteMedicalResourcesByRequestError()
            throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testDeleteMedicalResourcesByRequestError");

        assertApiStatus(data, DELETE_MEDICAL_RESOURCES_BY_REQUESTS, ERROR);
    }

    @Test
    public void testInsertRecords() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectInsertRecords");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.INSERT_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testInsertRecordsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectInsertRecordsError");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.INSERT_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testUpdateRecords() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectUpdateRecords");

        assertThat(data.size()).isAtLeast(3);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.UPDATE_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(3);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testUpdateRecordsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectUpdateRecordsError");

        assertThat(data.size()).isAtLeast(3);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.UPDATE_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testDeleteRecords() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectDeleteRecords");
        assertThat(data.size()).isAtLeast(3);
        int deletedRecords = 0;
        for (StatsLog.EventMetricData datum : data) {
            HealthConnectApiCalled atom =
                    datum.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);

            if (atom.getApiMethod().equals(ApiMethod.DELETE_DATA)
                    && atom.getNumberOfRecords() == 1) {
                deletedRecords++;
                assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
                assertThat(atom.getErrorCode()).isEqualTo(0);
                assertThat(atom.getDurationMillis()).isAtLeast(0);
                assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
                assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
            }
        }
        assertThat(deletedRecords).isAtLeast(2);
    }

    @Test
    public void testDeleteRecordsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectDeleteRecordsError");
        assertThat(data.size()).isAtLeast(3);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.DELETE_DATA, ERROR);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testReadRecords() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectReadRecords");
        assertThat(data.size()).isAtLeast(3);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.READ_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testReadRecordsError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectReadRecordsError");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.READ_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testChangeLogTokenRequest() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogToken");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.GET_CHANGES_TOKEN);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testChangeLogTokenRequestError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogTokenError");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.GET_CHANGES_TOKEN);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testChangeLogsRequest() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogs");
        assertThat(data.size()).isAtLeast(5);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.GET_CHANGES);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testChangeLogsRequestError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogsError");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.GET_CHANGES);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testAggregatedDataRequest() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectAggregatedData");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.READ_AGGREGATED_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    @Test
    public void testAggregatedDataRequestError() throws Exception {
        if (!isHardwareSupported(getDevice())) {
            return;
        }
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectAggregatedDataError");
        assertThat(data.size()).isAtLeast(2);
        StatsLog.EventMetricData event = getEventForApiMethod(data, ApiMethod.READ_AGGREGATED_DATA);
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiStatus()).isEqualTo(ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
        assertThat(atom.getCallerForegroundState()).isEqualTo(ForegroundState.FOREGROUND);
        assertThat(atom.getPackageName()).isEqualTo(TEST_APP_PKG_NAME);
    }

    private List<StatsLog.EventMetricData> uploadAtomConfigAndTriggerTest(String testName)
            throws Exception {
        ConfigUtils.uploadConfigForPushedAtoms(
                getDevice(),
                TEST_APP_PKG_NAME,
                new int[] {ApiExtensionAtoms.HEALTH_CONNECT_API_CALLED_FIELD_NUMBER});

        DeviceUtils.runDeviceTests(
                getDevice(), TEST_APP_PKG_NAME, ".HealthConnectServiceLogsTests", testName);
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        ApiExtensionAtoms.registerAllExtensions(registry);

        return ReportUtils.getEventMetricDataList(getDevice(), registry);
    }

    private static StatsLog.EventMetricData getEventForApiMethod(
            List<StatsLog.EventMetricData> data, ApiMethod apiMethod, ApiStatus status) {
        for (StatsLog.EventMetricData datum : data) {
            HealthConnectApiCalled atom =
                    datum.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);

            if (atom.getApiMethod().equals(apiMethod) && atom.getApiStatus().equals(status)) {
                return datum;
            }
        }
        return null;
    }

    private static StatsLog.EventMetricData getEventForApiMethod(
            List<StatsLog.EventMetricData> data, ApiMethod apiMethod) {
        boolean isFirstCall = true;
        for (StatsLog.EventMetricData datum : data) {
            HealthConnectApiCalled atom =
                    datum.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);

            if (atom.getApiMethod().equals(apiMethod)) {
                if (ApiMethod.INSERT_DATA.equals(apiMethod) && isFirstCall) {
                    // skip the insert api call in setup
                    isFirstCall = false;
                    continue;
                }
                return datum;
            }
        }
        return null;
    }

    private static void assertApiStatus(
            List<StatsLog.EventMetricData> data, ApiMethod apiMethod, ApiStatus apiStatus) {
        StatsLog.EventMetricData event = getEventForApiMethod(data, apiMethod);
        assertWithMessage("Asserting API %s with status %s", apiMethod.name(), apiStatus.name())
                .that(event)
                .isNotNull();
        assertThat(event).isNotNull();
        HealthConnectApiCalled atom =
                event.getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertWithMessage("Asserting API %s with status %s", apiMethod.name(), apiStatus.name())
                .that(atom.getApiStatus())
                .isEqualTo(apiStatus);
        assertWithMessage("Asserting API %s with status %s", apiMethod.name(), apiStatus.name())
                .that(atom.getPackageName())
                .isEqualTo(TEST_APP_PKG_NAME);
    }

    private static AssertApiCallParams createAssertApiCallParams(
            String methodToTriggerApi, ApiMethod apiMethod, ApiStatus apiStatus) {
        return new AssertApiCallParams(methodToTriggerApi, apiMethod, apiStatus);
    }

    private static class AssertApiCallParams {
        public final String methodNameToTriggerApi;
        public final ApiMethod apiMethod;
        public final ApiStatus apiStatus;

        AssertApiCallParams(String methodToTriggerApi, ApiMethod apiMethod, ApiStatus apiStatus) {
            this.methodNameToTriggerApi = methodToTriggerApi;
            this.apiMethod = apiMethod;
            this.apiStatus = apiStatus;
        }
    }
}
