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

package android.healthconnect.test.app;

import static android.health.connect.datatypes.ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.units.Energy;
import android.os.Bundle;

import java.time.Instant;

/** Receives requests from test cases. Required to perform API calls in background. */
public class TestAppReceiver extends BroadcastReceiver {
    public static final String ACTION_READ_RECORDS_FOR_OTHER_APP =
            "action.READ_RECORDS_FOR_OTHER_APP";
    public static final String ACTION_AGGREGATE = "action.AGGREGATE";
    public static final String ACTION_GET_CHANGE_LOG_TOKEN = "action.GET_CHANGE_LOG_TOKEN";
    public static final String ACTION_GET_CHANGE_LOGS = "action.GET_CHANGE_LOGS";
    public static final String ACTION_RESULT_SUCCESS = "action.SUCCESS";
    public static final String ACTION_RESULT_ERROR = "action.ERROR";
    public static final String EXTRA_RESULT_ERROR_CODE = "extra.ERROR_CODE";
    public static final String EXTRA_RESULT_ERROR_MESSAGE = "extra.ERROR_MESSAGE";
    public static final String EXTRA_RECORD_COUNT = "extra.RECORD_COUNT";
    public static final String EXTRA_TOKEN = "extra.TOKEN";
    private static final String PKG_TEST_SUITE = "android.healthconnect.integrationtests";
    private static final String TEST_SUITE_RECEIVER = "android.healthconnect.tests.TestReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_READ_RECORDS_FOR_OTHER_APP:
                readRecordsForOtherApp(context);
                break;
            case ACTION_AGGREGATE:
                aggregate(context);
                break;
            case ACTION_GET_CHANGE_LOG_TOKEN:
                getChangeLogToken(context);
                break;
            case ACTION_GET_CHANGE_LOGS:
                getChangeLogs(context, intent.getStringExtra(EXTRA_TOKEN));
                break;
            default:
                throw new IllegalStateException("Unsupported command: " + intent.getAction());
        }
    }

    private void readRecordsForOtherApp(Context context) {
        final BlockingOutcomeReceiver<ReadRecordsResponse<ActiveCaloriesBurnedRecord>> outcome =
                new BlockingOutcomeReceiver<>();

        getHealthConnectManager(context)
                .readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(
                                        ActiveCaloriesBurnedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(PKG_TEST_SUITE)
                                                .build())
                                .build(),
                        newSingleThreadExecutor(),
                        outcome);

        final HealthConnectException error = outcome.getError();
        if (error == null) {
            final Bundle extras = new Bundle();
            extras.putInt(EXTRA_RECORD_COUNT, outcome.getResult().getRecords().size());
            sendSuccess(context, extras);
        } else {
            sendError(context, error);
        }
    }

    private void aggregate(Context context) {
        final BlockingOutcomeReceiver<AggregateRecordsResponse<Energy>> outcome =
                new BlockingOutcomeReceiver<>();

        getHealthConnectManager(context)
                .aggregate(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setEndTime(Instant.now())
                                                .setStartTime(Instant.now().minusSeconds(10))
                                                .build())
                                .addAggregationType(ACTIVE_CALORIES_TOTAL)
                                .build(),
                        newSingleThreadExecutor(),
                        outcome);

        sendResult(context, outcome);
    }

    private void getChangeLogToken(Context context) {
        final BlockingOutcomeReceiver<ChangeLogTokenResponse> outcome =
                new BlockingOutcomeReceiver<>();

        getHealthConnectManager(context)
                .getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(ActiveCaloriesBurnedRecord.class)
                                .build(),
                        newSingleThreadExecutor(),
                        outcome);

        final HealthConnectException error = outcome.getError();
        if (error == null) {
            final Bundle extras = new Bundle();
            extras.putString(EXTRA_TOKEN, outcome.getResult().getToken());
            sendSuccess(context, extras);
        } else {
            sendError(context, error);
        }
    }

    private void getChangeLogs(Context context, String token) {
        final BlockingOutcomeReceiver<ChangeLogsResponse> outcome = new BlockingOutcomeReceiver<>();

        getHealthConnectManager(context)
                .getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(),
                        newSingleThreadExecutor(),
                        outcome);

        sendResult(context, outcome);
    }

    private HealthConnectManager getHealthConnectManager(Context context) {
        return requireNonNull(context.getSystemService(HealthConnectManager.class));
    }

    private void sendResult(Context context, BlockingOutcomeReceiver<?> outcomeReceiver) {
        final HealthConnectException error = outcomeReceiver.getError();
        if (error == null) {
            sendSuccess(context);
        } else {
            sendError(context, error);
        }
    }

    private void sendSuccess(Context context) {
        context.sendBroadcast(getSuccessIntent());
    }

    private void sendSuccess(Context context, Bundle extras) {
        context.sendBroadcast(getSuccessIntent().putExtras(extras));
    }

    private Intent getSuccessIntent() {
        return new Intent(ACTION_RESULT_SUCCESS).setClassName(PKG_TEST_SUITE, TEST_SUITE_RECEIVER);
    }

    private void sendError(Context context, HealthConnectException error) {
        context.sendBroadcast(
                new Intent(ACTION_RESULT_ERROR)
                        .setClassName(PKG_TEST_SUITE, TEST_SUITE_RECEIVER)
                        .putExtra(EXTRA_RESULT_ERROR_CODE, error.getErrorCode())
                        .putExtra(EXTRA_RESULT_ERROR_MESSAGE, error.getMessage()));
    }
}
