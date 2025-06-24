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
package com.android.healthconnect.controller.tests.utils

import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

suspend fun createFakeAppInfoReader(): AppInfoReader {
    val appInfoReader: AppInfoReader = mock()
    val packages =
        listOf(
            TEST_APP_PACKAGE_NAME to TEST_APP_NAME,
            TEST_APP_PACKAGE_NAME_2 to TEST_APP_NAME_2,
            TEST_APP_PACKAGE_NAME_3 to TEST_APP_NAME_3,
            "com.android.healthconnect.controller.tests" to
                "dagger.hilt.android.testing.HiltTestApplication",
        )
    for ((packageName, appName) in packages) {
        whenever(appInfoReader.getAppMetadata(eq(packageName), any()))
            .thenReturn(
                AppMetadata(
                    packageName = packageName,
                    appName = appName,
                    icon = null,
                    isSystem = false,
                )
            )
    }
    return appInfoReader
}
