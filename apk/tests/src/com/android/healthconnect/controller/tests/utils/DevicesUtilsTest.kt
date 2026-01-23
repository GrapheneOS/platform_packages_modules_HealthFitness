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

import android.content.Context
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.StepsRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.utils.findCurrentDeviceId
import com.android.healthconnect.controller.utils.findSystemInfo
import com.android.healthconnect.controller.utils.isDevicePackage
import com.android.healthconnect.controller.utils.isDisabledByAllProviders
import com.android.healthconnect.controller.utils.providesNativeSteps
import com.android.healthconnect.controller.utils.toDeviceIconAttr
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicesUtilsTest {

    @get:Rule val mSetFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
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
    fun isDevicePackage_doesNotMatchRandom() {
        assertThat(isDevicePackage("com.example.app")).isFalse()
    }

    @Test
    fun findCurrentDeviceId_returnsCurrentDeviceId() {
        val currentDeviceId = TEST_DEVICE_DATA_SOURCES_INFO.findCurrentDeviceId()
        assertThat(currentDeviceId).isEqualTo(TEST_PHONE_SPN)
    }

    @Test
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
    fun findSystemInfo_returnsSystemProviderInfo() {
        val phoneInfo =
            TEST_DEVICE_DATA_SOURCES_INFO.find {
                it.deviceDataOrigin.packageName == TEST_PHONE_SPN
            }!!
        val systemInfo = phoneInfo.findSystemInfo()

        assertThat(systemInfo).isNotNull()
        assertThat(systemInfo?.packageName).isEqualTo(DEVICE_DATA_PROVIDER_PACKAGE)
    }

    @Test
    fun findSystemInfo_noSystemProvider_returnsNull() {
        val watchInfo =
            TEST_DEVICE_DATA_SOURCES_INFO.find {
                it.deviceDataOrigin.packageName == TEST_WATCH_SPN
            }!!
        // The watch info in TEST_DEVICE_DATA_SOURCES_INFO has a provider "testDdp", not the system
        // one.
        val systemInfo = watchInfo.findSystemInfo()

        assertThat(systemInfo).isNull()
    }

    @Test
    fun providesNativeSteps_trueIfAvailable() {
        val phoneInfo =
            TEST_DEVICE_DATA_SOURCES_INFO.find {
                it.deviceDataOrigin.packageName == TEST_PHONE_SPN
            }!!
        // The phone info has the system that advertises StepsRecord as available.
        assertThat(phoneInfo.providesNativeSteps()).isTrue()
    }

    @Test
    fun providesNativeSteps_falseIfNotAvailable() {
        val watchInfo =
            TEST_DEVICE_DATA_SOURCES_INFO.find {
                it.deviceDataOrigin.packageName == TEST_WATCH_SPN
            }!!
        // Watch info doesn't have system provider
        assertThat(watchInfo.providesNativeSteps()).isFalse()
    }

    @Test
    fun isDisabledByAllProviders_falseWhenEnabled() {
        val phoneInfo =
            TEST_DEVICE_DATA_SOURCES_INFO.find {
                it.deviceDataOrigin.packageName == TEST_PHONE_SPN
            }!!
        // Phone info has userEnabled = true for StepsRecord
        assertThat(phoneInfo.isDisabledByAllProviders()).isFalse()
    }

    @Test
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
}
