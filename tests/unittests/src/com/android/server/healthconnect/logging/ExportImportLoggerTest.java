/*
 * Copyright (C) 2024 The Android Open Source Project
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

package healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_STARTED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_VERSION_MISMATCH;
import static android.health.connect.exportimport.ImportStatus.DATA_IMPORT_ERROR_VERSION_MISMATCH;
import static android.health.connect.exportimport.ScheduledExportStatus.DATA_EXPORT_STARTED;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.health.HealthFitnessStatsLog;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.logging.ExportImportLogger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class ExportImportLoggerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;

    @Test
    public void test_LogExportStatus() {

        new ExportImportLogger(mHealthFitnessStatsLog)
                .logExportStatus(DATA_EXPORT_STARTED, 1, 2, 3);
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_EXPORT_INVOKED),
                        eq(HEALTH_CONNECT_EXPORT_INVOKED__STATUS__EXPORT_STATUS_STARTED),
                        eq(1),
                        eq(2),
                        eq(3));
    }

    @Test
    public void test_logImportStatus() {
        // Create variable to comply with java formatter line length.
        int error = HEALTH_CONNECT_IMPORT_INVOKED__STATUS__IMPORT_STATUS_ERROR_VERSION_MISMATCH;
        new ExportImportLogger(mHealthFitnessStatsLog)
                .logImportStatus(DATA_IMPORT_ERROR_VERSION_MISMATCH, 2, 3, 4);
        verify(mHealthFitnessStatsLog, times(1))
                .write(eq(HEALTH_CONNECT_IMPORT_INVOKED), eq(error), eq(2), eq(3), eq(4));
    }
}
