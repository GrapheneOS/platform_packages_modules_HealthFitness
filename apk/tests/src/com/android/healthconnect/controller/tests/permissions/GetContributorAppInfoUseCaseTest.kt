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
package com.android.healthconnect.controller.tests.permissions

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.health.connect.ApplicationInfoResponse
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.AppInfo
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.GetContributorAppInfoUseCase
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.spy

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetContributorAppInfoUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val coroutineTestRule = CoroutineTestRule()

    private var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)
    private lateinit var usecase: GetContributorAppInfoUseCase
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context

        Mockito.doAnswer(prepareDeviceSourcesAnswer(emptyList()))
            .`when`(manager)
            .getDeviceDataSourceInfos(any(), any())

        usecase = GetContributorAppInfoUseCase(manager, context, Dispatchers.Main)
    }

    @Test
    fun invoke_noData_returnsEmptyMap() = runTest {
        Mockito.doAnswer(prepareAppInfoAnswer(emptyList()))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        assertThat(usecase.invoke()).isEmpty()
    }

    @Test
    fun invoke_withContributorApps_returnsAppsMap() = runTest {
        val bitmap =
            AppCompatResources.getDrawable(context, R.drawable.health_connect_logo)!!.toBitmap()
        val appInfo =
            listOf(
                AppInfo.Builder(TEST_APP_PACKAGE_NAME)
                    .setName(TEST_APP_NAME)
                    .setIcon(bitmap.toByteArray())
                    .build(),
                AppInfo.Builder(TEST_APP_PACKAGE_NAME_2)
                    .setName(TEST_APP_NAME_2)
                    .setIcon(bitmap.toByteArray())
                    .build(),
            )

        Mockito.doAnswer(prepareAppInfoAnswer(appInfo))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        val result = usecase.invoke()
        assertThat(result.size).isEqualTo(2)
        assertThat(result).containsKey(TEST_APP_PACKAGE_NAME)
        assertThat(result).containsKey(TEST_APP_PACKAGE_NAME_2)
    }

    @Test
    fun invoke_deviceDataProviderApp_returnsDeviceIcon() = runTest {
        val managerDeviceBitmap =
            AppCompatResources.getDrawable(
                    context,
                    com.android.settingslib.widget.preference.selector.R.drawable.ic_settings_accent,
                )!!
                .toBitmap()
        val deviceIconBitmap =
            AppCompatResources.getDrawable(context, R.drawable.ic_device_phone)!!.toBitmap()
        val appInfo =
            listOf(
                AppInfo.Builder(DEVICE_DATA_PROVIDER_PACKAGE)
                    .setName("Pixel 9a")
                    .setIcon(managerDeviceBitmap.toByteArray())
                    .build()
            )

        Mockito.doAnswer(prepareAppInfoAnswer(appInfo))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        val result = usecase.invoke()
        assertThat(result.size).isEqualTo(1)
        assertThat(result).containsKey(DEVICE_DATA_PROVIDER_PACKAGE)
        assertThat(result[DEVICE_DATA_PROVIDER_PACKAGE]!!.appName).isEqualTo("Pixel 9a")
        assert(result[DEVICE_DATA_PROVIDER_PACKAGE]!!.icon!!.toBitmap().sameAs(deviceIconBitmap))
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun invoke_ddpFlagsOn_syntheticPackageName_returnsDeviceInfo() = runTest {
        context = spy(object : ContextWrapper(context) {})

        val appInfo = listOf(AppInfo.Builder(TEST_PHONE_SPN).build())
        val deviceInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }

        assert(deviceInfo != null)

        Mockito.doAnswer(prepareAppInfoAnswer(appInfo))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        Mockito.doAnswer(prepareDeviceSourcesAnswer(listOf(deviceInfo!!)))
            .`when`(manager)
            .getDeviceDataSourceInfos(any(), any())

        val result = usecase.invoke()
        assertThat(result.size).isEqualTo(1)
        assertThat(result).containsKey(TEST_PHONE_SPN)
        assertThat(result[TEST_PHONE_SPN]!!.packageName).isEqualTo(TEST_PHONE_SPN)
        assertThat(result[TEST_PHONE_SPN]!!.appName).isEqualTo("Some phone")
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun invoke_ddpFlagsOff_syntheticPackageName_returnsOnlyAppInfo() = runTest {
        val appInfo = listOf(AppInfo.Builder(TEST_PHONE_SPN).build())
        val deviceInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }

        assert(deviceInfo != null)

        Mockito.doAnswer(prepareAppInfoAnswer(appInfo))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        Mockito.doAnswer(prepareDeviceSourcesAnswer(listOf(deviceInfo!!)))
            .`when`(manager)
            .getDeviceDataSourceInfos(any(), any())

        val result = usecase.invoke()
        assertThat(result.size).isEqualTo(1)
        assertThat(result).containsKey(TEST_PHONE_SPN)
        assertThat(result[TEST_PHONE_SPN]!!.packageName).isEqualTo(TEST_PHONE_SPN)
        assertThat(result[TEST_PHONE_SPN]!!.appName).isEqualTo(TEST_PHONE_SPN)
        assertThat(result[TEST_PHONE_SPN]!!.icon).isEqualTo(null)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun invoke_syntheticPackageNameAndApp_replacesSpnWithDeviceInfo() = runTest {
        context = spy(object : ContextWrapper(context) {})

        val appInfo =
            listOf(
                AppInfo.Builder(TEST_APP_PACKAGE_NAME).setName(TEST_APP_NAME).build(),
                AppInfo.Builder(TEST_PHONE_SPN).build(),
            )
        val deviceInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }

        assert(deviceInfo != null)

        Mockito.doAnswer(prepareAppInfoAnswer(appInfo))
            .`when`(manager)
            .getContributorApplicationsInfo(any(), any())

        Mockito.doAnswer(prepareDeviceSourcesAnswer(listOf(deviceInfo!!)))
            .`when`(manager)
            .getDeviceDataSourceInfos(any(), any())

        val result = usecase.invoke()
        assertThat(result.size).isEqualTo(2)
        assertThat(result).containsKey(TEST_PHONE_SPN)
        assertThat(result).containsKey(TEST_APP_PACKAGE_NAME)
    }

    private fun prepareAppInfoAnswer(apps: List<AppInfo>): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<ApplicationInfoResponse, *>
            receiver.onResult(ApplicationInfoResponse(apps))
            null
        }
        return answer
    }

    private fun prepareDeviceSourcesAnswer(
        sources: List<DeviceDataSourceInfo>
    ): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<List<DeviceDataSourceInfo>, *>
            receiver.onResult(sources)
            null
        }
        return answer
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
