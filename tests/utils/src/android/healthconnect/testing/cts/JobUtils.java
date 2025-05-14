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

package android.healthconnect.testing.cts;

import android.util.Log;

import com.android.compatibility.common.util.SystemUtil;

public class JobUtils {

    private static final String TAG = "HealthConnectShellUtils";

    /** Returns whether a job with the given namespace has been scheduled. */
    public static boolean isJobScheduled(String namespace) throws Exception {
        String dumpsysOutput = SystemUtil.runShellCommand("dumpsys jobscheduler");
        return dumpsysOutput.contains(namespace + ":");
    }

    /** Runs the job with the given namespace if it has already been scheduled. */
    public static void runJobIfScheduled(String namespace) throws Exception {
        runShellCommandForHCJob(namespace, "run -f -n");
    }

    /** Cancels the job with the given namespace if it has already been scheduled. */
    public static void cancelJobIfScheduled(String namespace) throws Exception {
        runShellCommandForHCJob(namespace, "cancel -n");
    }

    private static void runShellCommandForHCJob(String namespace, String command) throws Exception {
        String dumpsysOutput = SystemUtil.runShellCommand("dumpsys jobscheduler");
        if (!isJobScheduled(namespace)) {
            Log.i(TAG, "No HC jobs scheduled for namespace: " + namespace);
            return;
        }

        String filteredOutput =
                dumpsysOutput.substring(
                        dumpsysOutput.indexOf(namespace), dumpsysOutput.indexOf(namespace) + 100);
        String jobId =
                filteredOutput.substring(
                        filteredOutput.indexOf("/") + 1, filteredOutput.indexOf(": "));
        String commandOutput =
                SystemUtil.runShellCommand(
                        String.format(
                                "cmd jobscheduler %s %s android %s", command, namespace, jobId));
        Log.i(TAG, "Run output: " + commandOutput);
    }
}
