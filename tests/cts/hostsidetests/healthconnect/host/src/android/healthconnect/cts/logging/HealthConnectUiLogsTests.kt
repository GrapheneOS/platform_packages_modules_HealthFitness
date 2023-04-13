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

package android.healthconnect.cts.logging

import android.cts.statsdatom.lib.AtomTestUtils
import android.cts.statsdatom.lib.ConfigUtils
import android.cts.statsdatom.lib.DeviceUtils
import android.cts.statsdatom.lib.ReportUtils
import android.healthfitness.ui.ElementId
import android.healthfitness.ui.PageId
import com.android.os.StatsLog
import com.android.os.healthfitness.ui.UiExtensionAtoms
import com.android.tradefed.build.IBuildInfo
import com.android.tradefed.testtype.DeviceTestCase
import com.android.tradefed.testtype.IBuildReceiver
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ExtensionRegistry

class HealthConnectUiLogsTests : DeviceTestCase(), IBuildReceiver {

    companion object {
        private const val TAG = "HomeFragmentHostTest"
        private const val TEST_APP_PKG_NAME = "android.healthconnect.cts.testhelper"
        private const val READ_HEART_RATE_PERMISSION = "android.permission.health.READ_HEART_RATE"
        private const val WRITE_HEART_RATE_PERMISSION = "android.permission.health.WRITE_HEART_RATE"
    }

    private lateinit var mCtsBuild: IBuildInfo
    private lateinit var packageName: String

    override fun setUp() {
        super.setUp()
        assertThat(mCtsBuild).isNotNull()
        ConfigUtils.removeConfig(device)
        ReportUtils.clearReports(device)
        val pmResult =
            device.executeShellCommand(
                "pm list packages com.google.android.healthconnect.controller")
        packageName =
            if (pmResult.isEmpty()) {
                "com.android.healthconnect.controller"
            } else {
                "com.google.android.healthconnect.controller"
            }

        ConfigUtils.createConfigBuilder(packageName)
        ConfigUtils.uploadConfigForPushedAtoms(
            device,
            packageName,
            intArrayOf(
                UiExtensionAtoms.HEALTH_CONNECT_UI_IMPRESSION_FIELD_NUMBER,
                UiExtensionAtoms.HEALTH_CONNECT_UI_INTERACTION_FIELD_NUMBER))
    }

    @Throws(Exception::class)
    override fun tearDown() {
        ConfigUtils.removeConfig(device)
        ReportUtils.clearReports(device)
        super.tearDown()
    }

    override fun setBuild(buildInfo: IBuildInfo) {
        mCtsBuild = buildInfo
    }

    fun testHomeScreenImpressions() {
        val pageId = PageId.HOME_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openHomeFragment")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)

        val data = ReportUtils.getEventMetricDataList(device, registry)
        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val appPermissionsImpression = filterLogs(data, pageId, ElementId.APP_PERMISSIONS_BUTTON)
        assertThat(appPermissionsImpression.size).isEqualTo(1)

        val dataAndAccessImpression = filterLogs(data, pageId, ElementId.DATA_AND_ACCESS_BUTTON)
        assertThat(dataAndAccessImpression.size).isEqualTo(1)

        val recentAccessDataImpression = filterLogs(data, pageId, ElementId.RECENT_ACCESS_ENTRY)
        assertThat(recentAccessDataImpression.size).isAtLeast(1)

        val seeAllRecentAccessImpression =
            filterLogs(data, pageId, ElementId.SEE_ALL_RECENT_ACCESS_BUTTON)
        assertThat(seeAllRecentAccessImpression.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testRecentAccessImpressions() {
        val pageId = PageId.RECENT_ACCESS_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openRecentAccess")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val recentAppImpression = filterLogs(data, pageId, ElementId.RECENT_ACCESS_ENTRY)
        assertThat(recentAppImpression.size).isAtLeast(1)

        val managePermissionsButtonImpression =
            filterLogs(data, pageId, ElementId.MANAGE_PERMISSIONS_FLOATING_BUTTON)
        assertThat(managePermissionsButtonImpression.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testDataAndAccessImpressions() {
        val pageId = PageId.CATEGORIES_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openCategories")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val categoryImpression = filterLogs(data, pageId, ElementId.CATEGORY_BUTTON)
        assertThat(categoryImpression.size).isEqualTo(2)

        val seeAllCategoriesImpression =
            filterLogs(data, pageId, ElementId.SEE_ALL_CATEGORIES_BUTTON)
        assertThat(seeAllCategoriesImpression.size).isEqualTo(1)

        val autoDeleteImpression = filterLogs(data, pageId, ElementId.AUTO_DELETE_BUTTON)
        assertThat(autoDeleteImpression.size).isEqualTo(1)

        val deleteAllDataImpression = filterLogs(data, pageId, ElementId.DELETE_ALL_DATA_BUTTON)
        assertThat(deleteAllDataImpression.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testAllCategoriesImpressions() {
        val pageId = PageId.ALL_CATEGORIES_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openAllCategories")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val categoryImpression = filterLogs(data, pageId, ElementId.CATEGORY_BUTTON)
        assertThat(categoryImpression.size).isEqualTo(6)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testPermissionTypesImpressions() {
        val pageId = PageId.PERMISSION_TYPES_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openPermissionTypes")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val permissionTypeImpression = filterLogs(data, pageId, ElementId.PERMISSION_TYPE_BUTTON)
        assertThat(permissionTypeImpression.size).isEqualTo(1)

        val deleteCategoryDataImpression =
            filterLogs(data, pageId, ElementId.DELETE_CATEGORY_DATA_BUTTON)
        assertThat(deleteCategoryDataImpression.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testDataAccessImpressions() {
        val pageId = PageId.DATA_ACCESS_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openDataAccess")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(2)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val seeAllEntriesImpression = filterLogs(data, pageId, ElementId.SEE_ALL_ENTRIES_BUTTON)
        assertThat(seeAllEntriesImpression.size).isEqualTo(1)

        val deletePermissionDataButton = filterLogs(data, pageId, ElementId.DELETE_THIS_DATA_BUTTON)
        assertThat(deletePermissionDataButton.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testDataEntriesImpressions() {
        val pageId = PageId.DATA_ENTRIES_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openDataEntries")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(1)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val dataEntryImpression = filterLogs(data, pageId, ElementId.DATA_ENTRY_VIEW)
        assertThat(dataEntryImpression.size).isEqualTo(1)

        val deleteDataEntryImpression = filterLogs(data, pageId, ElementId.DATA_ENTRY_DELETE_BUTTON)
        assertThat(deleteDataEntryImpression.size).isEqualTo(1)

        val toolbarImpression = filterLogs(data, pageId, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isEqualTo(1)
    }

    fun testAppPermissionsImpressions() {
        val pageId = PageId.APP_PERMISSIONS_PAGE
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openAppPermissions")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(1)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val searchButtonImpression = filterLogs(data, pageId, ElementId.SEARCH_BUTTON)
        assertThat(searchButtonImpression.size).isEqualTo(1)

        val removeAllPermissionsImpression =
            filterLogs(data, pageId, ElementId.REMOVE_ALL_APPS_PERMISSIONS_BUTTON)
        assertThat(removeAllPermissionsImpression.size).isEqualTo(1)

        val helpAndFeedbackImpression = filterLogs(data, pageId, ElementId.HELP_AND_FEEDBACK_BUTTON)
        assertThat(helpAndFeedbackImpression.size).isEqualTo(1)
    }

    fun testRequestPermissionsImpressions() {
        val pageId = PageId.REQUEST_PERMISSIONS_PAGE
        device.executeShellCommand("pm revoke $TEST_APP_PKG_NAME $READ_HEART_RATE_PERMISSION")
        device.executeShellCommand("pm revoke $TEST_APP_PKG_NAME $WRITE_HEART_RATE_PERMISSION")
        DeviceUtils.runDeviceTests(
            device, TEST_APP_PKG_NAME, ".HealthConnectUiTestHelper", "openRequestPermissions")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)
        val data = ReportUtils.getEventMetricDataList(device, registry)

        assertThat(data.size).isAtLeast(1)

        // Page impression
        val pageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(pageImpression.size).isEqualTo(1)

        val permissionSwitchImpression = filterLogs(data, pageId, ElementId.PERMISSION_SWITCH)
        assertThat(permissionSwitchImpression.size).isEqualTo(2)

        val cancelPermissionsImpression =
            filterLogs(data, pageId, ElementId.CANCEL_PERMISSIONS_BUTTON)
        assertThat(cancelPermissionsImpression.size).isEqualTo(1)

        val allowPermissionsImpression =
            filterLogs(data, pageId, ElementId.ALLOW_PERMISSIONS_BUTTON)
        assertThat(allowPermissionsImpression.size).isEqualTo(1)
    }

    private fun filterLogs(
        data: List<StatsLog.EventMetricData>,
        pageId: PageId,
        elementId: ElementId
    ): List<StatsLog.EventMetricData> {
        return data.filter {
            it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page == pageId &&
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).element ==
                    elementId
        }
    }
}
