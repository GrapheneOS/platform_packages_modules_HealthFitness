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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Receives requests from test cases and forwards to Health Connect.
 *
 * <p>Used for testing HC API calls on behalf of other apps in the foreground.
 */
public class TestAppActivity extends Activity {
    private static final String TAG = TestAppActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We call finish here to tell the system we've successfully handled the
        // intent that started this activity. This must be done before carrying
        // out the action requested by the intent, as that action may involve
        // killing the app. See b/348171256 for background. Note: in the case of
        // a request to self-kill, sending the return broadcast is not required.
        finish();
        Intent intent = getIntent();
        Log.i(TAG, TAG + " onCreate(): " + intent);
        Intent returnIntent =
                TestAppHelper.handleRequest(getApplicationContext(), intent.getExtras());

        sendBroadcast(returnIntent);
    }
}
