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
package com.android.healthconnect.controller.utils.logging

import android.health.HealthFitnessStatsLog
import android.health.HealthFitnessStatsLog.*
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Helper class for logging UI Impressions and Interactions. */
@Singleton
class HealthConnectLogger @Inject constructor() {

    private var pageName = PageName.UNKNOWN_PAGE

    /**
     * Sets the page ID which will be used for all impressions and interaction logging on this page.
     */
    fun setPageId(pageName: PageName) {
        this.pageName = pageName
    }

    /** Logs the impression of a page. */
    fun logPageImpression() {
        HealthFitnessStatsLog.write(HEALTH_CONNECT_UI_IMPRESSION, pageName.impressionId)
    }

    /** Logs the impression of an element. */
    fun logImpression(element: ElementName) {
        HealthFitnessStatsLog.write(
            HEALTH_CONNECT_UI_IMPRESSION, pageName.impressionId, element.impressionId)
    }

    /** Logs the interaction with an element. */
    fun logInteraction(element: ElementName, action: UIAction = UIAction.ACTION_CLICK) {
        HealthFitnessStatsLog.write(
            HEALTH_CONNECT_UI_INTERACTION, pageName.interactionId, element.interactionId, action.id)
    }
}

/** Enum class for UI Actions, used to specify whether we wish to log a click or a toggle. */
enum class UIAction(val id: Int) {
    ACTION_CLICK(HEALTH_CONNECT_UI_INTERACTION__ACTION__ACTION_CLICK),
    ACTION_TOGGLE_ON(HEALTH_CONNECT_UI_INTERACTION__ACTION__ACTION_TOGGLE_ON),
    ACTION_TOGGLE_OFF(HEALTH_CONNECT_UI_INTERACTION__ACTION__ACTION_TOGGLE_OFF),
    ACTION_UNKNOWN(HEALTH_CONNECT_UI_INTERACTION__ACTION__ACTION_UNKNOWN)
}

/** Enum class for PageName. Each loggable fragment in the application should define a page name. */
enum class PageName(val impressionId: Int, val interactionId: Int) {
    HOME_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__HOME_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__HOME_PAGE),
    ONBOARDING_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__ONBOARDING_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__ONBOARDING_PAGE),
    RECENT_ACCESS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__RECENT_ACCESS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__RECENT_ACCESS_PAGE),
    APP_PERMISSIONS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__APP_PERMISSIONS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__APP_PERMISSIONS_PAGE),
    APP_PERMISSIONS_EMPTY_STATE_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__APP_PERMISSIONS_EMPTY_STATE_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__APP_PERMISSIONS_EMPTY_STATE_PAGE),
    HELP_AND_FEEDBACK_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__HELP_AND_FEEDBACK_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__HELP_AND_FEEDBACK_PAGE),
    CATEGORIES_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__CATEGORIES_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__CATEGORIES_PAGE),
    AUTO_DELETE_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__AUTO_DELETE_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__AUTO_DELETE_PAGE),
    PERMISSION_TYPES_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__PERMISSION_TYPES_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__PERMISSION_TYPES_PAGE),
    DATA_ACCESS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__DATA_ACCESS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__DATA_ACCESS_PAGE),
    DATA_ENTRIES_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__DATA_ENTRIES_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__DATA_ENTRIES_PAGE),
    ENTRY_DETAILS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__ENTRY_DETAILS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__ENTRY_DETAILS_PAGE),
    APP_ACCESS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__APP_ACCESS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__APP_ACCESS_PAGE),
    UNITS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__UNITS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__UNITS_PAGE),
    ALL_CATEGORIES_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__ALL_CATEGORIES_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__ALL_CATEGORIES_PAGE),
    REQUEST_PERMISSIONS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__REQUEST_PERMISSIONS_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__REQUEST_PERMISSIONS_PAGE),
    ERROR_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__ERROR_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__ERROR_PAGE),
    LOADING_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__LOADING_PAGE,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__LOADING_PAGE),
    // TODO add
    MANAGE_PERMISSIONS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__PAGE_UNKNOWN,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__PAGE_UNKNOWN),
    SETTINGS_MANAGE_PERMISSIONS_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__PAGE_UNKNOWN,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__PAGE_UNKNOWN),
    UNKNOWN_PAGE(
        HEALTH_CONNECT_UI_IMPRESSION__PAGE__PAGE_UNKNOWN,
        HEALTH_CONNECT_UI_INTERACTION__PAGE__PAGE_UNKNOWN)
}

/** Common interface for loggable elements. */
interface ElementName {
    val impressionId: Int
    val interactionId: Int
}

/** Loggable elements in the Home page. */
enum class HomePageElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    APP_PERMISSIONS_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__APP_PERMISSIONS_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__APP_PERMISSIONS_BUTTON),
    DATA_AND_ACCESS_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__DATA_AND_ACCESS_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__DATA_AND_ACCESS_BUTTON),
    SEE_ALL_RECENT_ACCESS_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__SEE_ALL_RECENT_ACCESS_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__SEE_ALL_RECENT_ACCESS_BUTTON),
}

/** Loggable elements in the Onboarding page. */
enum class OnboardingElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    ONBOARDING_COMPLETED_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ONBOARDING_COMPLETED_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ONBOARDING_COMPLETED_BUTTON),
    ONBOARDING_GO_BACK_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ONBOARDING_GO_BACK_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ONBOARDING_GO_BACK_BUTTON),
}

/** Loggable elements in the Recent Access page. */
enum class RecentAccessElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    RECENT_ACCESS_ENTRY_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__RECENT_ACCESS_ENTRY,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__RECENT_ACCESS_ENTRY),
    MANAGE_PERMISSIONS_FAB(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__MANAGE_PERMISSIONS_FLOATING_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__MANAGE_PERMISSIONS_FLOATING_BUTTON),
}

/** Loggable elements in the Category and All categories pages. */
enum class CategoriesElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    CATEGORY_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__CATEGORY_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__CATEGORY_BUTTON),
    SEE_ALL_CATEGORIES_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__SEE_ALL_CATEGORIES_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__SEE_ALL_CATEGORIES_BUTTON),
    EXPORT_DATA_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__EXPORT_DATA_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__EXPORT_DATA_BUTTON),
    AUTO_DELETE_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__AUTO_DELETE_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__AUTO_DELETE_BUTTON),
    DELETE_ALL_DATA_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__DELETE_ALL_DATA_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__DELETE_ALL_DATA_BUTTON),
}

/** Loggable elements in the toolbar. */
enum class ToolbarElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    // Represents main menu
    TOOLBAR_SETTINGS_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__TOOLBAR_SETTINGS_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__TOOLBAR_SETTINGS_BUTTON),
    TOOLBAR_HELP_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__TOOLBAR_HELP_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__TOOLBAR_HELP_BUTTON),
    // TODO update
    TOOLBAR_UNITS_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ELEMENT_UNKNOWN,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ELEMENT_UNKNOWN),
    // TODO not sure if this will be needed
    TOOLBAR_OPEN_SOURCE_LICENSE_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__TOOLBAR_OPEN_SOURCE_LICENSE_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__TOOLBAR_OPEN_SOURCE_LICENSE_BUTTON),
}

/** Loggable elements belonging to the error page, and the unknown element. */
enum class ErrorPageElement(override val impressionId: Int, override val interactionId: Int) :
    ElementName {
    ERROR_PAGE_GO_BACK_BUTTON(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ERROR_PAGE_GO_BACK_BUTTON,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ERROR_PAGE_GO_BACK_BUTTON),
    ERROR_PAGE_TOAST(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ERROR_PAGE_TOAST,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ERROR_PAGE_TOAST),
    UNKNOWN_ELEMENT(
        HEALTH_CONNECT_UI_IMPRESSION__ELEMENT__ELEMENT_UNKNOWN,
        HEALTH_CONNECT_UI_INTERACTION__ELEMENT__ELEMENT_UNKNOWN)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HealthConnectLoggerEntryPoint {
    fun logger(): HealthConnectLogger
}
