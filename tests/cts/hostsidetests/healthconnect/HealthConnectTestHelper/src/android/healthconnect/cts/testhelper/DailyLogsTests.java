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

package android.healthconnect.cts.testhelper;

import static android.healthconnect.cts.testhelper.TestHelperUtils.deleteAllRecordsAddedByTestApp;
import static android.healthconnect.cts.testhelper.TestHelperUtils.getBloodPressureRecord;
import static android.healthconnect.cts.testhelper.TestHelperUtils.getHeartRateRecord;
import static android.healthconnect.cts.testhelper.TestHelperUtils.getStepsRecord;
import static android.healthconnect.cts.testhelper.TestHelperUtils.insertRecords;
import static android.healthconnect.cts.testhelper.TestHelperUtils.queryAccessLogs;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.TestUtils;

import androidx.test.InstrumentationRegistry;

import com.android.compatibility.common.util.NonApiTest;

import org.junit.Test;

import java.util.List;

/**
 * These tests are run by statsdatom/healthconnect to log atoms by triggering Health Connect APIs.
 *
 * <p>They only trigger the APIs, but don't test anything themselves.
 */
@NonApiTest(
        exemptionReasons = {},
        justification = "METRIC")
public class DailyLogsTests {

    private final HealthConnectManager mHealthConnectManager =
            InstrumentationRegistry.getContext().getSystemService(HealthConnectManager.class);
    private final PhrCtsTestUtils mPhrTestUtils = new PhrCtsTestUtils(mHealthConnectManager);

    @Test
    public void testUpsertMedicalResourcesThenReadSuccess() throws InterruptedException {
        // clean up before the test to make the test more realistic
        mPhrTestUtils.deleteAllMedicalData();

        MedicalDataSource medicalDataSource =
                mPhrTestUtils.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource medicalResource =
                mPhrTestUtils.upsertMedicalData(medicalDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // make a read to emulate monthly active user
        mPhrTestUtils.readMedicalResourcesByIds(List.of(medicalResource.getId()));
    }

    @Test
    public void testInsertRecordsSucceed() throws Exception {
        assertThat(
                        insertRecords(
                                mHealthConnectManager,
                                List.of(
                                        getStepsRecord(),
                                        getBloodPressureRecord(),
                                        getHeartRateRecord())))
                .hasSize(3);
    }

    @Test
    public void testHealthConnectAccessLogsEqualsZero() throws Exception {
        assertThat(queryAccessLogs(mHealthConnectManager)).hasSize(0);
    }

    @Test
    public void testHealthConnectAccessLogsEqualsOne() throws Exception {
        assertThat(queryAccessLogs(mHealthConnectManager)).hasSize(1);
    }

    @Test
    public void testHealthConnectAccessLogsEqualsTwo() throws Exception {
        assertThat(queryAccessLogs(mHealthConnectManager)).hasSize(2);
    }

    /**
     * Deletes the records added by the test app as well as the staged remote data.
     *
     * <p>Triggered in the teardown of HealthConnectDailyLogsStatsTests after database stats are
     * collected and verified.
     */
    @Test
    public void deleteAllStagedRemoteData() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
        deleteAllRecordsAddedByTestApp(mHealthConnectManager);
        mPhrTestUtils.deleteAllMedicalData();
    }
}
