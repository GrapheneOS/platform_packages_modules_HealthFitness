/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.categories

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadCategoriesUseCase @Inject constructor() {
    /** Returns list of available data categories. */
    suspend operator fun invoke(): List<HealthDataCategory> =
        listOf(HealthDataCategory.ACTIVITY, HealthDataCategory.CYCLE_TRACKING)
}

@Singleton
class LoadAllCategoriesUseCase
@Inject
constructor(private val categoriesUseCase: LoadCategoriesUseCase) {
    /** Returns list of all available data categories. */
    suspend operator fun invoke(): List<AllCategoriesScreenHealthDataCategory> {
        val categoriesWithData = categoriesUseCase()
        return HEALTH_DATA_CATEGORIES.map { category ->
            AllCategoriesScreenHealthDataCategory(category, category !in categoriesWithData)
        }
    }
}
