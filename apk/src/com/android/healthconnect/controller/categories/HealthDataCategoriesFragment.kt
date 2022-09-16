/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.categories

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels

/** Fragment for health data categories. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HealthDataCategoriesFragment : Hilt_HealthDataCategoriesFragment(){

  companion object {
    private const val BROWSE_DATA_CATEGORY = "browse_data_category"
    private const val DELETE_ALL_DATA_BUTTON = "delete_all_data"
  }

  private val viewModel: HealthDataCategoryViewModel by viewModels()

  private val mBrowseDataCategory: PreferenceGroup? by lazy {
    preferenceScreen.findPreference(BROWSE_DATA_CATEGORY)
  }
  private val mDeleteAllData: Preference? by lazy {
    preferenceScreen.findPreference(DELETE_ALL_DATA_BUTTON)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.health_data_categories_screen, rootKey)
    mDeleteAllData?.setOnPreferenceClickListener {
      //TODO(b/246161850) implement delete all data flow
      true
    }
  }

  override fun onResume() {
    super.onResume()
    setTitle(R.string.data_title)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.categoriesData.observe(viewLifecycleOwner) { categoriesList ->
      updateDataList(categoriesList)
    }
  }

  private fun updateDataList(categoriesList: List<HealthDataCategory>) {
    mBrowseDataCategory?.removeAll()
    if (categoriesList.isEmpty()) {
      mBrowseDataCategory?.addPreference(
        Preference(requireContext()).also {
          it.setSummary(R.string.no_categories)
        }
      )
    } else {
      categoriesList.forEach { category ->
        mBrowseDataCategory?.addPreference(
          Preference(requireContext()).also {
            it.setTitle(category.title)
            it.setIcon(category.icon)
          }
        )
      }
    }

    mDeleteAllData?.isEnabled = categoriesList.isNotEmpty()
  }
}