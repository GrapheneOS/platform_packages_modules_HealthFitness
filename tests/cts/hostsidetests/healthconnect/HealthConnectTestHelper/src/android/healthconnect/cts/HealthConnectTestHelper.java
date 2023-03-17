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

package android.healthconnect.cts;

import static android.healthconnect.cts.lib.TestUtils.INSERT_RECORD_QUERY;
import static android.healthconnect.cts.lib.TestUtils.INTENT_EXCEPTION;
import static android.healthconnect.cts.lib.TestUtils.QUERY_TYPE;
import static android.healthconnect.cts.lib.TestUtils.SUCCESS;
import static android.healthconnect.cts.lib.TestUtils.getTestRecords;
import static android.healthconnect.cts.lib.TestUtils.insertRecords;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.health.connect.datatypes.Record;
import android.os.Bundle;

import java.util.List;

public class HealthConnectTestHelper extends Activity {
    private static final String TAG = "HealthConnectTestHelper";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        String queryType = getIntent().getStringExtra(QUERY_TYPE);
        Intent returnIntent;
        try {
            switch (queryType) {
                case INSERT_RECORD_QUERY:
                    returnIntent = insertRecord(queryType, context);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown query received from launcher app: " + queryType);
            }
        } catch (Exception e) {
            returnIntent = new Intent(queryType);
            returnIntent.putExtra(INTENT_EXCEPTION, e);
        }

        sendBroadcast(returnIntent);
    }

    private Intent insertRecord(String queryType, Context context) {
        List<Record> records = getTestRecords(context.getPackageName());
        final Intent intent = new Intent(queryType);
        try {
            insertRecords(records, context);
            intent.putExtra(SUCCESS, true);
        } catch (InterruptedException e) {
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }
}
