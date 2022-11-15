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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.Deletion
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.utils.setTitle
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for health data categories. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HealthDataCategoriesFragment : Hilt_HealthDataCategoriesFragment() {

    companion object {
        const val CATEGORY_NAME_KEY = "category_name_key"
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

        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }

        mDeleteAllData?.setOnPreferenceClickListener {
            val deletionFragment =
                childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) as DeletionFragment
            val deleteAllData =
                Deletion(
                    deletionType = DeletionType.DeletionTypeAllData(),
                    showTimeRangePickerDialog = true)
            deletionFragment.startDataDeletion(deleteAllData)
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
        val sortedCategoriesList: List<HealthDataCategory> =
            categoriesList.sortedBy { getString(it.title) }
        mBrowseDataCategory?.removeAll()
        if (sortedCategoriesList.isEmpty()) {
            mBrowseDataCategory?.addPreference(
                Preference(requireContext()).also { it.setSummary(R.string.no_categories) })
        } else {
            sortedCategoriesList.forEach { category ->
                mBrowseDataCategory?.addPreference(
                    Preference(requireContext()).also {
                        it.setTitle(category.title)
                        it.setIcon(category.icon)
                        it.onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                findNavController()
                                    .navigate(
                                        R.id.action_healthDataCategories_to_healthPermissionTypes,
                                        getBundle(category.name))
                                true
                            }
                    })
            }
        }

        viewModel.allCategoriesData.observe(viewLifecycleOwner) { allCategoriesList ->
            if (sortedCategoriesList.size < allCategoriesList.size) {
                mBrowseDataCategory?.addPreference(
                    Preference(requireContext()).also {
                        it.setTitle(R.string.see_all_categories)
                        it.setIcon(R.drawable.ic_arrow_forward)
                        it.onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                findNavController()
                                    .navigate(
                                        R.id.action_healthDataCategories_to_healthDataAllCategories)
                                true
                            }
                    })
            }
        }

        mDeleteAllData?.isEnabled = categoriesList.isNotEmpty()
    }
}

fun getBundle(string: String): Bundle {
    val bundle = Bundle()
    bundle.putString(HealthDataCategoriesFragment.CATEGORY_NAME_KEY, string)
    return bundle
}
