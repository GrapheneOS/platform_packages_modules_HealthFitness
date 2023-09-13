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
 */
package com.android.healthconnect.controller.datasources

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment.Companion.CATEGORY_KEY
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(HealthPreferenceFragment::class)
class AddAnAppFragment : Hilt_AddAnAppFragment() {

    private val dataSourcesViewModel: DataSourcesViewModel by activityViewModels()
    @HealthDataCategoryInt
    private var category: Int = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.add_an_app_screen, rootKey)
        if (requireArguments().containsKey(CATEGORY_KEY)) {
            category = requireArguments().getInt(CATEGORY_KEY)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataSourcesViewModel.loadPotentialAppSources(category)

        dataSourcesViewModel.potentialAppSources.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DataSourcesViewModel.PotentialAppSourcesState.Loading -> {
                    setLoading(true)
                }
                is DataSourcesViewModel.PotentialAppSourcesState.LoadingFailed -> {
                    setLoading(false)
                    setError(true)
                }
                is DataSourcesViewModel.PotentialAppSourcesState.WithData -> {
                    setLoading(false)
                    updateAppsList(state.appSources)
                }
            }
        }
    }

    private fun updateAppsList(appSources: List<AppMetadata>) {
        preferenceScreen.removeAll()
        appSources.sortedBy { it.appName }
            .forEach { appMetadata ->
                preferenceScreen.addPreference(
                    HealthPreference(requireContext())
                        .also {
                            it.title = appMetadata.appName
                            it.icon = appMetadata.icon
                            it.setOnPreferenceClickListener {
                                // TODO - update priority list and return to previous fragment
                                true
                            }
                        }
                )
            }
    }

}