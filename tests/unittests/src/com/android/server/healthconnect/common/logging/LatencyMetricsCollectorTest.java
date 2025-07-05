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

package com.android.server.healthconnect.common.logging;

import static com.android.server.healthconnect.common.logging.LatencyMetricsCollector.LatencyMetricsData;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SleepSessionRecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class LatencyMetricsCollectorTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private TransactionManager mTransactionManager;
    @Mock private AppInfoHelper mAppInfoHelper;

    private LatencyMetricsCollector mLatencyMetricsCollector;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setTransactionManager(mTransactionManager)
                        .setAppInfoHelper(mAppInfoHelper)
                        .setEnvironmentDataDirectory(mTemporaryFolder.getRoot())
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .build();
        mLatencyMetricsCollector =
                new LatencyMetricsCollector(
                        healthConnectInjector.getTransactionManager(),
                        healthConnectInjector.getAppInfoHelper());
    }

    @Test
    public void testReadLastWeekExerciseSessions_returnsCorrectData() throws Exception {
        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            RecordHelper.APP_INFO_ID_COLUMN_NAME,
                            IntervalRecordHelper.END_TIME_COLUMN_NAME,
                            RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME
                        });
        cursor.addRow(new Object[] {1, 1000, 2000});
        cursor.addRow(new Object[] {2, 1500, 2500});

        when(mTransactionManager.read(
                        argThat(
                                readTableRequestMatcher(
                                        ExerciseSessionRecordHelper
                                                .EXERCISE_SESSION_RECORD_TABLE_NAME))))
                .thenReturn(cursor);
        when(mAppInfoHelper.getPackageName(1)).thenReturn("com.example.app1");
        when(mAppInfoHelper.getPackageName(2)).thenReturn("com.example.app2");

        List<LatencyMetricsData> result = mLatencyMetricsCollector.readLastWeekExerciseSessions();

        assertThat(result)
                .containsExactly(
                        new LatencyMetricsData(
                                "com.example.app1", /* latency= */ Duration.ofMillis(1000)),
                        new LatencyMetricsData(
                                "com.example.app2", /* latency= */ Duration.ofMillis(1000)));
    }

    @Test
    public void testReadLastWeekSleepSessions_returnsCorrectData() throws Exception {
        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            RecordHelper.APP_INFO_ID_COLUMN_NAME,
                            IntervalRecordHelper.END_TIME_COLUMN_NAME,
                            RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME
                        });
        cursor.addRow(new Object[] {1, 3000, 4000});
        cursor.addRow(new Object[] {2, 3500, 4500});

        when(mTransactionManager.read(
                        argThat(
                                readTableRequestMatcher(
                                        SleepSessionRecordHelper.SLEEP_SESSION_RECORD_TABLE_NAME))))
                .thenReturn(cursor);
        when(mAppInfoHelper.getPackageName(1)).thenReturn("com.example.app1");
        when(mAppInfoHelper.getPackageName(2)).thenReturn("com.example.app2");

        List<LatencyMetricsData> result = mLatencyMetricsCollector.readLastWeekSleepSessions();

        assertThat(result)
                .containsExactly(
                        new LatencyMetricsData(
                                "com.example.app1", /* latency= */ Duration.ofMillis(1000)),
                        new LatencyMetricsData(
                                "com.example.app2", /* latency= */ Duration.ofMillis(1000)));
    }

    @Test
    public void testReadLastWeekSessions_nameNotFoundException_skipsRecord() throws Exception {
        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            RecordHelper.APP_INFO_ID_COLUMN_NAME,
                            IntervalRecordHelper.END_TIME_COLUMN_NAME,
                            RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME
                        });
        cursor.addRow(new Object[] {1, 1000, 2000});
        cursor.addRow(new Object[] {2, 1500, 2500});

        when(mTransactionManager.read(
                        argThat(
                                readTableRequestMatcher(
                                        ExerciseSessionRecordHelper
                                                .EXERCISE_SESSION_RECORD_TABLE_NAME))))
                .thenReturn(cursor);
        when(mAppInfoHelper.getPackageName(1)).thenReturn("com.example.app1");
        when(mAppInfoHelper.getPackageName(2))
                .thenThrow(new PackageManager.NameNotFoundException());

        List<LatencyMetricsData> result = mLatencyMetricsCollector.readLastWeekExerciseSessions();

        assertThat(result)
                .containsExactly(
                        new LatencyMetricsData(
                                "com.example.app1", /* latency= */ Duration.ofMillis(1000)));
    }

    @Test
    public void testReadLastWeekSessions_emptyCursor_returnsEmptyList() {
        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            RecordHelper.APP_INFO_ID_COLUMN_NAME,
                            IntervalRecordHelper.END_TIME_COLUMN_NAME,
                            RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME
                        });

        when(mTransactionManager.read(
                        argThat(
                                readTableRequestMatcher(
                                        ExerciseSessionRecordHelper
                                                .EXERCISE_SESSION_RECORD_TABLE_NAME))))
                .thenReturn(cursor);

        List<LatencyMetricsData> result = mLatencyMetricsCollector.readLastWeekExerciseSessions();

        assertThat(result).isEmpty();
    }

    private ArgumentMatcher<ReadTableRequest> readTableRequestMatcher(String tableName) {
        return request -> request.getTableName().equals(tableName);
    }
}
