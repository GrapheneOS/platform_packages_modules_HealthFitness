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

package android.healthconnect.cts.database;

import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DataBaseTestUtils {
    public static final String INSTALL_ARG_FORCE_QUERYABLE = "--force-queryable";

    public static final String HC_APEX_RESOURCE_PATH_PREFIX = "/HealthConnectApexFiles/";
    public static final String HC_CTS_RESOURCE_PATH_PREFIX = "/HealthConnectCtsApkFiles/";
    public static final String TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner";

    public static int getCurrentHcDatabaseVersion(ITestDevice device)
            throws DeviceNotAvailableException {
        String currentDbVersion =
                device.executeShellCommand(
                        "cd ~; sqlite3 data/system_ce/0/healthconnect/healthconnect.db"
                                + " \"PRAGMA user_version;\"");
        // To remove any extra white spaces on the sides.
        currentDbVersion = currentDbVersion.strip();
        LogUtil.CLog.d("Current Database version  " + currentDbVersion);
        return Integer.parseInt(currentDbVersion);
    }

    public static void deleteHcDatabase(ITestDevice device) throws DeviceNotAvailableException {
        String result =
                device.executeShellCommand(
                        "cd ~; rm /data/system_ce/0/healthconnect/healthconnect.db;");
        // Deleted successfully.
        if (!result.isBlank()) {
            throw new IllegalArgumentException("Failed to remove healthconnect.db : " + result);
        }
    }

    /**
     * Installs package using the packageFilename in Resources.
     *
     * <p>Since this method can be used for both HC apex files and CTS apk files, pass true in
     * {@code isHcApex} to notify that the resource to be installed is an apex otherwise pass false.
     */
    public static void assertInstallSucceeds(
            ITestDevice device, String packageFilenameInResources, boolean isHcApex)
            throws Exception {
        String installResult =
                installPackageFromResource(device, packageFilenameInResources, isHcApex);
        if (installResult != null) {
            throw new IllegalArgumentException(
                    "Failed to install " + packageFilenameInResources + ": " + installResult);
        }
    }

    public static void assertUninstallSucceeds(ITestDevice device, String packageName)
            throws DeviceNotAvailableException {
        String uninstallResult = device.uninstallPackage(packageName);
        if (uninstallResult != null) {
            throw new IllegalArgumentException(
                    "Failed to install " + uninstallResult + ": " + uninstallResult);
        }
    }

    /** Fetches the package from resources and installs it for the current user. */
    public static String installPackageFromResource(
            ITestDevice device, String apkFilenameInResources, boolean isHcApex)
            throws IOException {
        // ITestDevice.installPackage API requires the APK to be installed to be a File. We thus
        // copy the requested resource into a temporary file, attempt to install it, and delete the
        // file during cleanup.
        File apkFile = null;
        try {
            apkFile = getFileFromResource(apkFilenameInResources, isHcApex);
            // Install package for current user.
            return device.installPackageForUser(
                    apkFile, true, device.getCurrentUser(), INSTALL_ARG_FORCE_QUERYABLE);
        } catch (DeviceNotAvailableException e) {
            throw new RemoteException("Device is not available, please connect a device.", e);
        } finally {
            cleanUpFile(apkFile);
        }
    }

    public static File getFileFromResource(String filenameInResources, boolean isHcApex)
            throws IOException, IllegalArgumentException {
        final String fullResourceName;
        if (isHcApex) {
            fullResourceName = HC_APEX_RESOURCE_PATH_PREFIX + filenameInResources;
        } else {
            fullResourceName = HC_CTS_RESOURCE_PATH_PREFIX + filenameInResources;
        }
        File tempDir = FileUtil.createTempDir("HcHostSideTests");
        File file = new File(tempDir, filenameInResources);
        InputStream in = DataBaseTestUtils.class.getResourceAsStream(fullResourceName);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found: " + fullResourceName);
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        byte[] buf = new byte[65536];
        int chunkSize;
        while ((chunkSize = in.read(buf)) != -1) {
            out.write(buf, 0, chunkSize);
        }
        out.close();
        return file;
    }

    static void cleanUpFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    /**
     * Run a device side test.
     *
     * @param pkgName Test package name, such as "android.healthconnect.cts".
     * @param testClassName Test class name; either a fully qualified name, or "." + a class name.
     * @param testMethodName Test method name.
     */
    public static String runDeviceTests(
            ITestDevice device, String pkgName, String testClassName, String testMethodName)
            throws DeviceNotAvailableException {
        if (testClassName != null && testClassName.startsWith(".")) {
            testClassName = pkgName + testClassName;
        }

        RemoteAndroidTestRunner testRunner =
                new RemoteAndroidTestRunner(pkgName, TEST_RUNNER, device.getIDevice());
        testRunner.setMaxTimeout(1800, TimeUnit.SECONDS);
        if (testClassName != null && testMethodName != null) {
            testRunner.setMethodName(testClassName, testMethodName);
        } else if (testClassName != null) {
            testRunner.setClassName(testClassName);
        }

        CollectingTestListener listener = new CollectingTestListener();
        assert (device.runInstrumentationTests(testRunner, listener));

        final TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            throw new AssertionError(
                    "Failed to successfully run device tests for "
                            + result.getName()
                            + ": "
                            + result.getRunFailureMessage());
        }
        if (result.getNumTests() == 0) {
            throw new AssertionError("No tests were run on the device");
        }

        if (result.hasFailedTests()) {
            // build a meaningful error message
            StringBuilder errorBuilder = new StringBuilder("On-device tests failed:\n");
            for (Map.Entry<TestDescription, TestResult> resultEntry :
                    result.getTestResults().entrySet()) {
                if (!resultEntry
                        .getValue()
                        .getStatus()
                        .equals(com.android.ddmlib.testrunner.TestResult.TestStatus.PASSED)) {
                    errorBuilder.append(resultEntry.getKey().toString());
                    errorBuilder.append(":\n");
                    errorBuilder.append(resultEntry.getValue().getStackTrace());
                }
            }
            throw new AssertionError(errorBuilder.toString());
        }
        return result.getTextSummary();
    }
}
