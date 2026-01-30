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

package com.android.healthconnect.controller.utils

import android.content.Context
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.Device.DEVICE_TYPE_CHEST_STRAP
import android.health.connect.datatypes.Device.DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE
import android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_BAND
import android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_EQUIPMENT
import android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_MACHINE
import android.health.connect.datatypes.Device.DEVICE_TYPE_GLASSES
import android.health.connect.datatypes.Device.DEVICE_TYPE_HEAD_MOUNTED
import android.health.connect.datatypes.Device.DEVICE_TYPE_HEARABLE
import android.health.connect.datatypes.Device.DEVICE_TYPE_METER
import android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE
import android.health.connect.datatypes.Device.DEVICE_TYPE_PORTABLE_COMPUTER
import android.health.connect.datatypes.Device.DEVICE_TYPE_RING
import android.health.connect.datatypes.Device.DEVICE_TYPE_SCALE
import android.health.connect.datatypes.Device.DEVICE_TYPE_SMART_DISPLAY
import android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN
import android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH
import android.health.connect.datatypes.StepsRecord
import android.health.connect.device.SyntheticPackageNameMatcher
import androidx.appcompat.content.res.AppCompatResources
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi

fun isDevicePackage(packageName: String): Boolean {
    return packageName == DEVICE_DATA_PROVIDER_PACKAGE ||
        deviceDataProvidersApi() && SyntheticPackageNameMatcher.matches(packageName)
}

fun Set<DeviceDataSourceInfo>.findCurrentDeviceId(): String? =
    this.find { it.isCurrentDevice }?.deviceDataOrigin?.packageName

fun DeviceDataSourceInfo.findSystemInfo(): DeviceDataProviderInfo? =
    this.deviceDataProviderInfos.find { providerInfo ->
        providerInfo.packageName.equals(DEVICE_DATA_PROVIDER_PACKAGE)
    }

fun DeviceDataSourceInfo.providesNativeSteps(): Boolean =
    this.findSystemInfo()
        ?.deviceDataTypeAdvertisements
        ?.find { typeAd -> typeAd.dataType == StepsRecord::class.java }
        ?.isAvailable == true

fun DeviceDataSourceInfo.asAppMetadata(context: Context) =
    AppMetadata(
        packageName = this.deviceDataOrigin.packageName,
        appName = this.device.displayName ?: this.device.type.toDeviceTypeString(context),
        icon =
            try {
                AttributeResolver.getDrawable(context, this.device.type.toDeviceIconAttr())
            } catch (e: Exception) {
                // If the context is not that of a fragment's (e.g. AppInfoReader), the
                // AttributeResolver will fail.
                // TODO(b/433184152): Return with unknown icon once we can ensure that the
                // passed context is always that of a fragment

                // Manual resolving is sup-optimal in terms of theming, but as device icons
                // are static, this is acceptable for now.
                AppCompatResources.getDrawable(context, this.device.type.toDeviceIconRes())
            },
    )

fun Int.toDeviceIconAttr(): Int {
    return when (this) {
        DEVICE_TYPE_WATCH -> R.attr.deviceWatchIcon
        DEVICE_TYPE_PHONE -> R.attr.devicePhoneIcon
        DEVICE_TYPE_SCALE -> R.attr.deviceScaleIcon
        DEVICE_TYPE_RING -> R.attr.deviceGenericIcon
        DEVICE_TYPE_HEAD_MOUNTED -> R.attr.deviceGenericIcon
        DEVICE_TYPE_FITNESS_BAND -> R.attr.deviceFitnessBandIcon
        DEVICE_TYPE_CHEST_STRAP -> R.attr.deviceGenericIcon
        DEVICE_TYPE_SMART_DISPLAY -> R.attr.devicePortableComputerIcon
        DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE -> R.attr.deviceConsumerMedicalDeviceIcon
        DEVICE_TYPE_GLASSES -> R.attr.deviceGenericIcon
        DEVICE_TYPE_HEARABLE -> R.attr.deviceHearableIcon
        DEVICE_TYPE_FITNESS_MACHINE -> R.attr.deviceGenericIcon
        DEVICE_TYPE_FITNESS_EQUIPMENT -> R.attr.deviceFitnessEquipmentIcon
        DEVICE_TYPE_PORTABLE_COMPUTER -> R.attr.devicePortableComputerIcon
        DEVICE_TYPE_METER -> R.attr.deviceGenericIcon
        else -> R.attr.deviceGenericIcon
    }
}

fun Int.toDeviceIconRes(): Int {
    return when (this) {
        DEVICE_TYPE_WATCH -> R.drawable.ic_device_watch
        DEVICE_TYPE_PHONE -> R.drawable.ic_device_phone
        DEVICE_TYPE_SCALE -> R.drawable.ic_device_scale
        DEVICE_TYPE_RING -> R.drawable.ic_device_generic
        DEVICE_TYPE_HEAD_MOUNTED -> R.drawable.ic_device_generic
        DEVICE_TYPE_FITNESS_BAND -> R.drawable.ic_device_fitness_band
        DEVICE_TYPE_CHEST_STRAP -> R.drawable.ic_device_generic
        DEVICE_TYPE_SMART_DISPLAY -> R.drawable.ic_device_portable_computer
        DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE -> R.drawable.ic_device_consumer_medical_device
        DEVICE_TYPE_GLASSES -> R.drawable.ic_device_generic
        DEVICE_TYPE_HEARABLE -> R.drawable.ic_device_hearable
        DEVICE_TYPE_FITNESS_MACHINE -> R.drawable.ic_device_generic
        DEVICE_TYPE_FITNESS_EQUIPMENT -> R.drawable.ic_device_fitness_equipment
        DEVICE_TYPE_PORTABLE_COMPUTER -> R.drawable.ic_device_portable_computer
        DEVICE_TYPE_METER -> R.drawable.ic_device_generic
        else -> R.drawable.ic_device_generic
    }
}

fun Int.toDeviceTypeString(context: Context): String {
    val resId =
        when (this) {
            DEVICE_TYPE_UNKNOWN -> R.string.device_type_unknown
            DEVICE_TYPE_WATCH -> R.string.device_type_watch
            DEVICE_TYPE_PHONE -> R.string.device_type_phone
            DEVICE_TYPE_SCALE -> R.string.device_type_scale
            DEVICE_TYPE_RING -> R.string.device_type_ring
            DEVICE_TYPE_HEAD_MOUNTED -> R.string.device_type_head_mounted
            DEVICE_TYPE_FITNESS_BAND -> R.string.device_type_fitness_band
            DEVICE_TYPE_CHEST_STRAP -> R.string.device_type_chest_strap
            DEVICE_TYPE_SMART_DISPLAY -> R.string.device_type_smart_display
            DEVICE_TYPE_CONSUMER_MEDICAL_DEVICE -> R.string.device_type_consumer_medical_device
            DEVICE_TYPE_GLASSES -> R.string.device_type_glasses
            DEVICE_TYPE_HEARABLE -> R.string.device_type_hearable
            DEVICE_TYPE_FITNESS_MACHINE -> R.string.device_type_fitness_machine
            DEVICE_TYPE_FITNESS_EQUIPMENT -> R.string.device_type_fitness_equipment
            DEVICE_TYPE_PORTABLE_COMPUTER -> R.string.device_type_portable_computer
            DEVICE_TYPE_METER -> R.string.device_type_meter
            else -> R.string.device_type_unknown
        }

    return context.getString(resId)
}

fun DeviceDataSourceInfo.isDisabledByAllProviders() =
    this.deviceDataProviderInfos.all { providerInfo ->
        providerInfo.deviceDataTypeAdvertisements.none { typeAd -> typeAd.isUserEnabled }
    }

fun DeviceDataProviderInfo.enabledAdsCount() =
    this.deviceDataTypeAdvertisements.count { typeAd -> typeAd.isUserEnabled }
