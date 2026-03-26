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

package com.android.server.healthconnect.device;

import android.annotation.FlaggedApi;
import android.health.connect.device.SyntheticPackageNameMatcher;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.os.Build;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

/** @hide */
@FlaggedApi(Flags.FLAG_DDP_DEBUG_UTIL)
public class DeviceDataProviderDebugUtil {
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;

    public DeviceDataProviderDebugUtil(
            DeviceInfoHelper deviceInfoHelper, AppInfoHelper appInfoHelper) {
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
    }

    /**
     * Dumps DDP related database information in bug reports or for debugging.
     *
     * @hide
     */
    public void dump(PrintWriter pw) {
        if (!Flags.ddpDebugUtil() || "user".equals(Build.TYPE)) {
            return;
        }
        // TODO(b/452314599): Only print out data for DDP devices.
        // device info dump
        Map<Long, DeviceInfo> deviceInfoMap = mDeviceInfoHelper.getIdDeviceInfoMap();
        if (deviceInfoMap == null || deviceInfoMap.isEmpty()) {
            pw.println("No devices");

        } else {
            pw.println("Health Connect Device Info");

            // device info dump
            for (Map.Entry<Long, DeviceInfo> entry : deviceInfoMap.entrySet()) {
                pw.printf(Locale.ROOT, "  Device info id: %d%n", entry.getKey());
                pw.printf(Locale.ROOT, "  Display name: %s%n", entry.getValue().getDisplayName());
                pw.printf(Locale.ROOT, "  Model: %s%n", entry.getValue().getModel());
                pw.printf(Locale.ROOT, "  Device type: %d%n", entry.getValue().getDeviceType());
                pw.printf(Locale.ROOT, "  Manufacturer: %s%n", entry.getValue().getManufacturer());
                pw.printf(Locale.ROOT, "  Device id: %s%n%n", entry.getValue().getDeviceId());
            }
        }

        Map<String, AppInfoInternal> appInfoMap = mAppInfoHelper.getAppInfoMap();
        if (appInfoMap == null || appInfoMap.isEmpty()) {
            pw.println("No apps");
        } else {

            // app info dump
            pw.println("Health Connect App Info");
            for (Map.Entry<String, AppInfoInternal> entry : appInfoMap.entrySet()) {
                pw.printf(Locale.ROOT, "  App info id: %d%n", entry.getValue().getId());
                pw.printf(Locale.ROOT, "  App name: %s%n", entry.getValue().getName());
                pw.printf(Locale.ROOT, "  Package name: %s%n", entry.getValue().getPackageName());

                if (SyntheticPackageNameMatcher.matches(entry.getValue().getPackageName())) {
                    pw.printf(
                            Locale.ROOT,
                            "  Device info id: %s%n",
                            entry.getValue().getDeviceInfoId());
                }
                pw.printf(
                        Locale.ROOT,
                        "  Record types used: %s%n%n",
                        entry.getValue().getRecordTypesUsed());
            }
        }
    }
}
