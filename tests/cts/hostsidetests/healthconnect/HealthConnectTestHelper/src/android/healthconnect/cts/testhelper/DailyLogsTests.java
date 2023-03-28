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

import android.health.connect.HealthConnectManager;

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

    @Test
    public void testHealthConnectDatabaseStats() throws Exception {
        insertRecords(
                List.of(getStepsRecord(), getBloodPressureRecord(), getHeartRateRecord()),
                mHealthConnectManager);
    }

    /**
     * Deletes the records added by the test app.
     *
     * <p>Triggered in the teardown of HealthConnectDailyLogsStatsTests after database stats are
     * collected and verified.
     */
    @Test
    public void deleteAllRecordsAddedForTest() throws InterruptedException {
        deleteAllRecordsAddedByTestApp(mHealthConnectManager);
    }
}
