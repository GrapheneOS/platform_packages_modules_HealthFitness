/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import com.android.healthconnect.controller.permissions.GetContributorAppInfoUseCase
import com.android.healthconnect.controller.shared.AppMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoReader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val applicationsInfoUseCase: GetContributorAppInfoUseCase
) {

    private var cache: HashMap<String, AppMetadata> = HashMap()
    private val packageManager = context.packageManager

    suspend fun getAppMetadata(packageName: String): AppMetadata {
        return if (isAppInstalled(packageName)) {
            AppMetadata(
                packageName = packageName,
                appName =
                    packageManager.getApplicationLabel(getPackageInfo(packageName)).toString(),
                icon = packageManager.getApplicationIcon(packageName))
        } else {
            if (!cache.containsKey(packageName)) {
                // refresh cache
                cache.putAll(applicationsInfoUseCase.invoke())
            }
            if (cache.containsKey(packageName)) {
                cache[packageName]!!
            } else {
                AppMetadata(packageName = packageName, appName = "", icon = null)
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            getPackageInfo(packageName).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getPackageInfo(packageName: String): ApplicationInfo {
        return packageManager.getApplicationInfo(packageName, ApplicationInfoFlags.of(0))
    }
}
