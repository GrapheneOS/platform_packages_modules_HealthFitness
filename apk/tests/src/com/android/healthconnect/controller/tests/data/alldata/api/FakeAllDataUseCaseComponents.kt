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

package com.android.healthconnect.controller.tests.data.alldata.api

import com.android.healthconnect.controller.data.alldata.api.IGetFitnessPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.data.alldata.api.IGetMedicalPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.data.alldata.api.IHasFitnessDataUseCase
import com.android.healthconnect.controller.data.alldata.api.IHasMedicalDataUseCase
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlinx.coroutines.Dispatchers

class FakeHasFitnessDataUseCase :
    FakeUseCase<Unit, Boolean>(dispatcher = Dispatchers.Unconfined), IHasFitnessDataUseCase {

    private var hasFitnessData = false

    override suspend fun successValue(input: Unit): Boolean {
        return hasFitnessData
    }

    fun setHasFitnessData(hasData: Boolean) {
        hasFitnessData = hasData
    }

    override fun reset() {
        super.reset()
        hasFitnessData = false
    }
}

class FakeHasMedicalDataUseCase :
    FakeUseCase<Unit, Boolean>(dispatcher = Dispatchers.Unconfined), IHasMedicalDataUseCase {

    private var hasMedicalData = false

    override suspend fun successValue(input: Unit): Boolean {
        return hasMedicalData
    }

    fun setHasMedicalData(hasData: Boolean) {
        hasMedicalData = hasData
    }

    override fun reset() {
        super.reset()
        hasMedicalData = false
    }
}

class FakeGetFitnessPermissionTypesWithDataUseCase :
    FakeUseCase<Unit, List<PermissionTypesPerCategory>>(dispatcher = Dispatchers.Unconfined),
    IGetFitnessPermissionTypesWithDataUseCase {
    private var result: List<PermissionTypesPerCategory> = emptyList()

    override suspend fun successValue(input: Unit): List<PermissionTypesPerCategory> {
        return result
    }

    fun setPermissionTypesPerCategory(list: List<PermissionTypesPerCategory>) {
        result = list
    }

    override fun reset() {
        super.reset()
        result = emptyList()
    }
}

class FakeGetMedicalPermissionTypesWithDataUseCase :
    FakeUseCase<Unit, List<PermissionTypesPerCategory>>(dispatcher = Dispatchers.Unconfined),
    IGetMedicalPermissionTypesWithDataUseCase {
    private var result: List<PermissionTypesPerCategory> = emptyList()

    override suspend fun successValue(input: Unit): List<PermissionTypesPerCategory> {
        return result
    }

    fun setPermissionTypesPerCategory(list: List<PermissionTypesPerCategory>) {
        result = list
    }

    override fun reset() {
        super.reset()
        result = emptyList()
    }
}
