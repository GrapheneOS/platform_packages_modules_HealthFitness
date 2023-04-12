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

import static com.google.common.truth.Truth.assertThat;

import android.cts.statsdatom.lib.ConfigUtils;
import android.cts.statsdatom.lib.DeviceUtils;
import android.cts.statsdatom.lib.ReportUtils;
import android.healthfitness.api.ApiMethod;
import android.healthfitness.api.ApiStatus;
import android.healthfitness.api.RateLimit;

import com.android.os.StatsLog;
import com.android.os.healthfitness.api.ApiExtensionAtoms;
import com.android.os.healthfitness.api.HealthConnectApiCalled;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import com.google.protobuf.ExtensionRegistry;

import java.util.List;

public class HealthConnectServiceStatsTests extends DeviceTestCase implements IBuildReceiver {

    public static final String TEST_APP_PKG_NAME = "android.healthconnect.cts.testhelper";
    private IBuildInfo mCtsBuild;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertThat(mCtsBuild).isNotNull();
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
        super.tearDown();
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = buildInfo;
    }

    public void testInsertRecords() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectInsertRecords");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.INSERT_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testInsertRecordsError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectInsertRecordsError");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.INSERT_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testUpdateRecords() throws Exception {

        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectUpdateRecords");

        assertThat(data.size()).isAtLeast(3);
        HealthConnectApiCalled atom =
                data.get(2).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.UPDATE_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(3);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testUpdateRecordsError() throws Exception {

        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectUpdateRecordsError");

        assertThat(data.size()).isAtLeast(3);
        HealthConnectApiCalled atom =
                data.get(2).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.UPDATE_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testDeleteRecords() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectDeleteRecords");
        assertThat(data.size()).isAtLeast(3);
        HealthConnectApiCalled atom =
                data.get(2).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.DELETE_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(2);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testDeleteRecordsError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectDeleteRecordsError");
        assertThat(data.size()).isAtLeast(3);
        HealthConnectApiCalled atom =
                data.get(2).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.DELETE_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testReadRecords() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectReadRecords");
        assertThat(data.size()).isAtLeast(3);
        HealthConnectApiCalled atom =
                data.get(2).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.READ_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testReadRecordsError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectReadRecordsError");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.READ_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testChangeLogTokenRequest() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogToken");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.GET_CHANGES_TOKEN);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testChangeLogTokenRequestError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogTokenError");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.GET_CHANGES_TOKEN);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testChangeLogsRequest() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogs");
        assertThat(data.size()).isAtLeast(5);
        HealthConnectApiCalled atom =
                data.get(4).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.GET_CHANGES);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testChangeLogsRequestError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectGetChangeLogsError");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.GET_CHANGES);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testAggregatedDataRequest() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectAggregatedData");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.READ_AGGREGATED_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.SUCCESS);
        assertThat(atom.getErrorCode()).isEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
    }

    public void testAggregatedDataRequestError() throws Exception {
        List<StatsLog.EventMetricData> data =
                uploadAtomConfigAndTriggerTest("testHealthConnectAggregatedDataError");
        assertThat(data.size()).isAtLeast(2);
        HealthConnectApiCalled atom =
                data.get(1).getAtom().getExtension(ApiExtensionAtoms.healthConnectApiCalled);
        assertThat(atom.getApiMethod()).isEqualTo(ApiMethod.READ_AGGREGATED_DATA);
        assertThat(atom.getApiStatus()).isEqualTo(ApiStatus.ERROR);
        assertThat(atom.getErrorCode()).isNotEqualTo(0);
        assertThat(atom.getDurationMillis()).isAtLeast(0);
        assertThat(atom.getNumberOfRecords()).isEqualTo(1);
        assertThat(atom.getRateLimit()).isEqualTo(RateLimit.NOT_USED);
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
}
