/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.datasources.appsources

import android.content.Context
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppUtils
import com.android.healthconnect.controller.shared.preference.RankedActionPreference
import com.android.healthconnect.controller.utils.logging.DataSourcesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger

class AppSourcesPreferenceCategory(
    context: Context,
    private val logger: HealthConnectLogger,
    private val appUtils: AppUtils,
    private val viewModel: DataSourcesViewModel,
    private val category: @HealthDataCategoryInt Int,
) : PreferenceCategory(context, null) {

    override fun onAttached() {
        super.onAttached()
        updatePreferences()
    }

    private var priorityList: List<AppMetadata> = emptyList()

    private fun updatePreferences() {
        removeAll()

        priorityList = viewModel.getPriorityList()
        val showActionButtons = priorityList.size > 1
        priorityList.forEachIndexed { index, appMetadata ->
            addPreference(rankedActionPreference(index, appMetadata, showActionButtons))
        }
    }

    private fun showPopupMenu(view: View, position: Int) =
        PopupMenu(ContextThemeWrapper(context, R.style.Widget_HealthConnect_PopUpMenu), view)
            .apply {
                menuInflater.inflate(R.menu.app_source_menu, menu)
                logger.logInteraction(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
                setupMenuItems(position)
                setOnMenuItemClickListener { handleMenuClick(it, position) }
                show()
            }

    private fun PopupMenu.setupMenuItems(position: Int) {
        menu
            .findItem(R.id.move_up)
            .setVisibilityAndLog(
                isVisible = position > 0,
                logElement = DataSourcesElement.MOVE_APP_SOURCE_UP_MENU_BUTTON,
            )
        menu
            .findItem(R.id.move_down)
            .setVisibilityAndLog(
                isVisible = position < priorityList.size - 1,
                logElement = DataSourcesElement.MOVE_APP_SOURCE_DOWN_MENU_BUTTON,
            )
        menu
            .findItem(R.id.remove)
            .setVisibilityAndLog(
                isVisible = priorityList.size > 1,
                logElement = DataSourcesElement.REMOVE_APP_SOURCE_MENU_BUTTON,
            )
    }

    private fun handleMenuClick(item: MenuItem, position: Int): Boolean =
        when (item.itemId) {
            R.id.move_up -> {
                logger.logInteraction(DataSourcesElement.MOVE_APP_SOURCE_UP_MENU_BUTTON)
                swapListItems(position - 1, position)
                viewModel.updatePriorityList(priorityListPackages(), category)
                true
            }
            R.id.move_down -> {
                logger.logInteraction(DataSourcesElement.MOVE_APP_SOURCE_DOWN_MENU_BUTTON)
                swapListItems(position, position + 1)
                viewModel.updatePriorityList(priorityListPackages(), category)
                true
            }
            R.id.remove -> {
                logger.logInteraction(DataSourcesElement.REMOVE_APP_SOURCE_MENU_BUTTON)
                removeListItem(position)
                viewModel.updatePriorityList(priorityListPackages(), category)
                viewModel.showAddAnAppButton()
                true
            }
            else -> false
        }

    private fun swapListItems(firstPosition: Int, secondPosition: Int) {
        if (outOfRange(firstPosition) || outOfRange(secondPosition)) {
            return
        }
        swapPreferences(firstPosition, secondPosition)

        priorityList = priorityList.toMutableList().apply { swap(firstPosition, secondPosition) }
    }

    private fun swapPreferences(firstPosition: Int, secondPosition: Int) {
        // Simply setting new order for the preferences does not update the expressive background,
        // hence we need to remove and re-add them.
        val firstPreference = this.getPreference(firstPosition)
        val secondPreference = this.getPreference(secondPosition)
        val firstAppMetaData = priorityList[firstPosition]
        val secondAppMetadata = priorityList[secondPosition]
        val newFirstPreference = rankedActionPreference(firstPosition, secondAppMetadata)
        val newSecondPreference = rankedActionPreference(secondPosition, firstAppMetaData)

        this.removePreference(firstPreference)
        this.removePreference(secondPreference)
        this.addPreference(newFirstPreference)
        this.addPreference(newSecondPreference)
    }

    private fun removeListItem(position: Int) {
        if (outOfRange(position)) {
            return
        }
        updateRemainingPreferences(position)
        this.removePreference(this.getPreference(position))
        priorityList = priorityList.toMutableList().apply { removeAt(position) }
    }

    private fun updateRemainingPreferences(indexToRemove: Int) {
        if (preferenceCount == 2) {
            val indexOfRemaining = if (indexToRemove == 0) 1 else 0
            (getPreference(indexOfRemaining) as RankedActionPreference).hideActionButton()
        }
        preferencesInRange(indexToRemove + 1, preferenceCount).forEach { it.reduceIndex() }
    }

    private fun preferencesInRange(start: Int, end: Int): List<RankedActionPreference> =
        (start until end).map { getPreference(it) as RankedActionPreference }

    private fun priorityListPackages() = priorityList.map { it.packageName }

    private fun outOfRange(position: Int) = position !in priorityList.indices

    private fun rankedActionPreference(
        position: Int,
        appMetadata: AppMetadata,
        showActionButtons: Boolean = true,
    ): RankedActionPreference =
        RankedActionPreference(
                context,
                appMetadata,
                appUtils,
                position,
                ::showPopupMenu,
                showActionButtons,
            )
            .also {
                logger.logImpression(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
                it.logName = DataSourcesElement.APP_SOURCE_BUTTON
                order = position
                isSelectable = false
            }

    private fun MenuItem?.setVisibilityAndLog(isVisible: Boolean, logElement: DataSourcesElement) {
        this?.apply {
            this.isVisible = isVisible
            if (isVisible) logger.logImpression(logElement)
        }
    }

    private fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }
}
