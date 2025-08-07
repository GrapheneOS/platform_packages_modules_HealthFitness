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
 *
 *
 */
package com.android.healthconnect.controller.shared.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.provider.Settings
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoReader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val applicationsInfoUseCase: IGetContributorAppInfoUseCase,
) {

    private var cache: HashMap<String, AppMetadata> = HashMap()
    private val packageManager = context.packageManager

    suspend fun getAppMetadata(packageName: String, isSystem: Boolean = false): AppMetadata {
        cache[packageName]?.let {
            // TODO(b/422986550): Remove special casing when DDP name updates in service
            if (packageName == DEVICE_DATA_PROVIDER_PACKAGE) {
                return getWithCurrentDeviceName(it)
            }

            return if (it.isSystem == isSystem) {
                it
            } else {
                AppMetadata(it.packageName, it.appName, it.icon, isSystem)
            }
        }
        // Always read the DDP package directly from the service - package manager will return
        // something like "Android System" which we don't want to display.
        if (packageName != DEVICE_DATA_PROVIDER_PACKAGE) {
            try {
                val app =
                    AppMetadata(
                        packageName = packageName,
                        appName =
                            packageManager
                                .getApplicationLabel(getPackageInfo(packageName))
                                .toString(),
                        icon = packageManager.getApplicationIcon(packageName),
                        isSystem = isSystem,
                    )
                cache[packageName] = app
                return app
            } catch (e: PackageManager.NameNotFoundException) {
                // Fallthrough to reading from storage.
            }
        }
        val contributorApps = applicationsInfoUseCase.invoke()
        // TODO(b/422986550): Do not cache DDP packages
        cache.putAll(contributorApps)
        return if (contributorApps.containsKey(packageName)) {
            contributorApps[packageName]!!
        } else {
            AppMetadata(packageName = packageName, appName = "", icon = null)
        }
    }

    fun isAppEnabled(packageName: String): Boolean {
        return try {
            getPackageInfo(packageName).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getPackageInfo(packageName: String): ApplicationInfo {
        return packageManager.getApplicationInfo(packageName, ApplicationInfoFlags.of(0))
    }

    private fun getWithCurrentDeviceName(dataDeviceProviderPackage: AppMetadata): AppMetadata =
        AppMetadata(
            dataDeviceProviderPackage.packageName,
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: dataDeviceProviderPackage.appName,
            dataDeviceProviderPackage.icon,
            dataDeviceProviderPackage.isSystem,
        )
}
