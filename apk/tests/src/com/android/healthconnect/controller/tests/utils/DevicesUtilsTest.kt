/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.utils.categorizeAndInsertToAppAccessState
import com.android.healthconnect.controller.utils.findCurrentDeviceId
import com.android.healthconnect.controller.utils.findSystemInfo
import com.android.healthconnect.controller.utils.isDevicePackage
import com.android.healthconnect.controller.utils.isDisabledByAllProviders
import com.android.healthconnect.controller.utils.providesNativeSteps
import com.android.healthconnect.controller.utils.shouldNavigateToCurrentDeviceManagement
import com.android.healthconnect.controller.utils.toDeviceIconAttr
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicesUtilsTest {
    @get:Rule val mSetFlagsRule = SetFlagsRule()

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun shouldNavigateToCurrentDeviceManagement_systemAdvertisedOnly_returnsTrue() {
        val currentDevice =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                buildDevice(),
                true,
                listOf(
                    DeviceDataProviderInfo(DEVICE_DATA_PROVIDER_PACKAGE, "phoneId", "", "", setOf())
                ),
            )
        assertThat(currentDevice.shouldNavigateToCurrentDeviceManagement()).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun shouldNavigateToCurrentDeviceManagement_notCurrentDevice_returnsFalse() {
        val someDevice =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                buildDevice(),
                false,
                listOf(
                    DeviceDataProviderInfo(DEVICE_DATA_PROVIDER_PACKAGE, "phoneId", "", "", setOf())
                ),
            )
        assertThat(someDevice.shouldNavigateToCurrentDeviceManagement()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun shouldNavigateToCurrentDeviceManagement_multipleProviders_returnsFalse() {
        val multipleProvidersInfo =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                buildDevice(),
                true,
                listOf(
                    DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE,
                        "phoneId",
                        "",
                        "",
                        setOf(),
                    ),
                    DeviceDataProviderInfo("some.other.provider", "phoneId", "", "", setOf()),
                ),
            )
        assertThat(multipleProvidersInfo.shouldNavigateToCurrentDeviceManagement()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun shouldNavigateToCurrentDeviceManagement_noSystemProvider_returnsFalse() {
        val noSystemProviderInfo =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                buildDevice(),
                true,
                listOf(DeviceDataProviderInfo("some.other.provider", "phoneId", "", "", setOf())),
            )
        assertThat(noSystemProviderInfo.shouldNavigateToCurrentDeviceManagement()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun categorizeAndInsertToAppAccessState_categorizesCorrectly() {
        val writeSet = mutableSetOf<AppAccessMetadata>()
        val inactiveSet = mutableSetOf<AppAccessMetadata>()
        val dataSources = getDeviceDataSourcesInfo()
        val phoneInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }

        // TEST_PHONE_SPN can write StepsRecord (enabled in getDeviceDataSourcesInfo)
        // TEST_WATCH_SPN has SleepSessionRecord, but we are looking for StepsRecord.

        dataSources.categorizeAndInsertToAppAccessState(
            listOf(StepsRecord::class.java),
            emptyList(),
            writeSet,
            inactiveSet,
            context,
        )

        assertThat(writeSet).hasSize(1)
        assertThat(writeSet.first().appMetadata.packageName).isEqualTo(TEST_PHONE_SPN)
        assertThat(writeSet.first().deviceDataSourceInfo).isEqualTo(phoneInfo)
        assertThat(inactiveSet).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun categorizeAndInsertToAppAccessState_contributingApp_categorizesAsInactive() {
        val writeSet = mutableSetOf<AppAccessMetadata>()
        val inactiveSet = mutableSetOf<AppAccessMetadata>()
        val dataSources = getDeviceDataSourcesInfo()

        // TEST_WATCH_SPN does not write StepsRecord.
        // But we add it as a contributing app to mock that it inserted data.

        dataSources.categorizeAndInsertToAppAccessState(
            listOf(StepsRecord::class.java),
            listOf(AppMetadata(TEST_WATCH_SPN, "Watch", null)),
            writeSet,
            inactiveSet,
            context,
        )

        assertThat(writeSet).hasSize(1) // Phone is still writing
        assertThat(writeSet.first().appMetadata.packageName).isEqualTo(TEST_PHONE_SPN)

        assertThat(inactiveSet).hasSize(1)
        assertThat(inactiveSet.first().appMetadata.packageName).isEqualTo(TEST_WATCH_SPN)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun categorizeAndInsertToAppAccessState_neitherWritingNorContributing_ignored() {
        val writeSet = mutableSetOf<AppAccessMetadata>()
        val inactiveSet = mutableSetOf<AppAccessMetadata>()
        val dataSources = getDeviceDataSourcesInfo()

        // Searching for DistanceRecord. Neither phone nor watch advertises it.
        // And no contributing apps.

        dataSources.categorizeAndInsertToAppAccessState(
            listOf(DistanceRecord::class.java),
            emptyList(),
            writeSet,
            inactiveSet,
            context,
        )

        assertThat(writeSet).isEmpty()
        assertThat(inactiveSet).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDevicePackage_matchesConstant() {
        assertThat(isDevicePackage(DEVICE_DATA_PROVIDER_PACKAGE)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDevicePackage_flagEnabled_matchesSynthetic() {
        assertThat(isDevicePackage(TEST_PHONE_SPN)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDevicePackage_flagDisabled_doesNotMatchSynthetic() {
        assertThat(isDevicePackage(TEST_PHONE_SPN)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDevicePackage_doesNotMatchRandom() {
        assertThat(isDevicePackage("com.example.app")).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun findCurrentDeviceId_returnsCurrentDeviceId() {
        val currentDeviceId = getDeviceDataSourcesInfo().findCurrentDeviceId()
        assertThat(currentDeviceId).isEqualTo(TEST_PHONE_SPN)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun findCurrentDeviceId_noCurrentDevice_returnsNull() {
        val info =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                Device.Builder().build(),
                false, // isCurrentDevice
                emptyList(),
            )
        assertThat(setOf(info).findCurrentDeviceId()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun findSystemInfo_returnsSystemProviderInfo() {
        val phoneInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }!!
        val systemInfo = phoneInfo.findSystemInfo()

        assertThat(systemInfo).isNotNull()
        assertThat(systemInfo?.packageName).isEqualTo(DEVICE_DATA_PROVIDER_PACKAGE)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun findSystemInfo_noSystemProvider_returnsNull() {
        val watchInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_WATCH_SPN }!!
        // The watch info in getDeviceDataSourcesInfo() has a provider "testDdp", not the system
        // one.
        val systemInfo = watchInfo.findSystemInfo()

        assertThat(systemInfo).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun providesNativeSteps_trueIfAvailable() {
        val phoneInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }!!
        // The phone info has the system that advertises StepsRecord as available.
        assertThat(phoneInfo.providesNativeSteps()).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun providesNativeSteps_falseIfNotAvailable() {
        val watchInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_WATCH_SPN }!!
        // Watch info doesn't have system provider
        assertThat(watchInfo.providesNativeSteps()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDisabledByAllProviders_falseWhenEnabled() {
        val phoneInfo =
            getDeviceDataSourcesInfo().find { it.deviceDataOrigin.packageName == TEST_PHONE_SPN }!!
        // Phone info has userEnabled = true for StepsRecord
        assertThat(phoneInfo.isDisabledByAllProviders()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun isDisabledByAllProviders_trueWhenAllDisabled() {
        // Create a custom info with disabled provider
        val disabledProvider =
            DeviceDataProviderInfo(
                "com.example.provider",
                "id",
                "",
                "",
                setOf(
                    DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                        .setAvailable(true)
                        .setUserEnabled(false)
                        .build()
                ),
            )
        val info =
            DeviceDataSourceInfo(
                DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                Device.Builder().build(),
                false,
                listOf(disabledProvider),
            )

        assertThat(info.isDisabledByAllProviders()).isTrue()
    }

    @Test
    fun toDeviceIconAttr_unknown_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_UNKNOWN.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_watch_returnsWatchIcon() {
        val attr = Device.DEVICE_TYPE_WATCH.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceWatchIcon)
    }

    @Test
    fun toDeviceIconAttr_phone_returnsPhoneIcon() {
        val attr = Device.DEVICE_TYPE_PHONE.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.devicePhoneIcon)
    }

    @Test
    fun toDeviceIconAttr_scale_returnsScaleIcon() {
        val attr = Device.DEVICE_TYPE_SCALE.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceScaleIcon)
    }

    @Test
    fun toDeviceIconAttr_ring_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_RING.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_headMounted_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_HEAD_MOUNTED.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_fitnessBand_returnsFitnessBandIcon() {
        val attr = Device.DEVICE_TYPE_FITNESS_BAND.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceFitnessBandIcon)
    }

    @Test
    fun toDeviceIconAttr_chestStrap_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_CHEST_STRAP.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_smartDisplay_returnsSmartDisplayIcon() {
        val attr = Device.DEVICE_TYPE_SMART_DISPLAY.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.devicePortableComputerIcon)
    }

    @Test
    fun toDeviceIconAttr_consumerMedicalDevice_returnsConsumerMedicalDeviceIcon() {
        val attr = Device.DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceConsumerMedicalDeviceIcon)
    }

    @Test
    fun toDeviceIconAttr_glasses_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_GLASSES.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_hearable_returnsHearableIcon() {
        val attr = Device.DEVICE_TYPE_HEARABLE.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceHearableIcon)
    }

    @Test
    fun toDeviceIconAttr_fitnessMachine_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_FITNESS_MACHINE.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    @Test
    fun toDeviceIconAttr_fitnessEquipment_returnsFitnessEquipmentIcon() {
        val attr = Device.DEVICE_TYPE_FITNESS_EQUIPMENT.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceFitnessEquipmentIcon)
    }

    @Test
    fun toDeviceIconAttr_portableComputer_returnsPortableComputerIcon() {
        val attr = Device.DEVICE_TYPE_PORTABLE_COMPUTER.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.devicePortableComputerIcon)
    }

    @Test
    fun toDeviceIconAttr_meter_returnsGenericIcon() {
        val attr = Device.DEVICE_TYPE_METER.toDeviceIconAttr()
        assertThat(attr).isEqualTo(R.attr.deviceGenericIcon)
    }

    private fun buildDevice(): Device {
        return Device.Builder()
            .setDisplayName("Some phone")
            .setModel("Some model")
            .setManufacturer("Some manufacturer")
            .setType(Device.DEVICE_TYPE_PHONE)
            .build()
    }
}
