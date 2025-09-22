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
import android.healthconnect.cts.HostSideTestUtil.TEST_APP_PERMISSIONS
import android.healthconnect.cts.HostSideTestUtil.TEST_APP_PKG_NAME
import android.healthconnect.cts.HostSideTestUtil.UI_TESTS_HELPER
import android.healthconnect.cts.HostSideTestUtil.grantPermissionsWithAdb
import android.healthconnect.cts.HostSideTestUtil.isHardwareSupported
import android.healthfitness.ui.ElementId
import android.healthfitness.ui.PageId
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.host.HostFlagsValueProvider
import com.android.healthfitness.flags.Flags.newHomeScreen
import com.android.os.StatsLog
import com.android.os.healthfitness.ui.UiExtensionAtoms
import com.android.tradefed.build.IBuildInfo
import com.android.tradefed.testtype.DeviceTestCase
import com.android.tradefed.testtype.IBuildReceiver
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ExtensionRegistry
import org.junit.Rule

class HealthConnectUiLogsTests : DeviceTestCase(), IBuildReceiver {

    @Rule
    val mCheckFlagsRule: CheckFlagsRule =
        HostFlagsValueProvider.createCheckFlagsRule { this.getDevice() }

    companion object {
        private const val TAG = "HomeFragmentHostTest"
    }

    private lateinit var mCtsBuild: IBuildInfo
    private lateinit var packageName: String

    override fun setUp() {
        super.setUp()
        if (!isHardwareSupported(device)) {
            return
        }
        assertThat(mCtsBuild).isNotNull()
        ConfigUtils.removeConfig(device)
        ReportUtils.clearReports(device)
        val pmResult =
            device.executeShellCommand(
                "pm list packages com.google.android.healthconnect.controller"
            )
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
                UiExtensionAtoms.HEALTH_CONNECT_UI_INTERACTION_FIELD_NUMBER,
            ),
        )

        // Permissions declared in the TEST_APP's manifest should be automatically granted, however
        // that seems flaky which led to issues like b/396574091, b/384734147.
        // ag/31764622 which explicitly grants permissions via ADB seems to work so far, so we
        // should do the same for this test file.
        grantPermissionsWithAdb(device, TEST_APP_PKG_NAME, TEST_APP_PERMISSIONS)
    }

    @Throws(Exception::class)
    override fun tearDown() {
        if (!isHardwareSupported(device)) {
            return
        }
        ConfigUtils.removeConfig(device)
        ReportUtils.clearReports(device)
        super.tearDown()
    }

    override fun setBuild(buildInfo: IBuildInfo) {
        mCtsBuild = buildInfo
    }

    fun testImpressionsAndInteractionsSent() {
        if (!isHardwareSupported(device)) {
            return
        }
        DeviceUtils.runDeviceTests(device, TEST_APP_PKG_NAME, UI_TESTS_HELPER, "openHomeFragment")
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG.toLong())
        val registry = ExtensionRegistry.newInstance()
        UiExtensionAtoms.registerAllExtensions(registry)

        val data = ReportUtils.getEventMetricDataList(device, registry)
        assertThat(data.size).isAtLeast(2)

        val homePageId =
            if (newHomeScreen()) {
                PageId.NEW_HOME_PAGE
            } else {
                PageId.HOME_PAGE
            }
        val manageDataPageId = PageId.MANAGE_DATA_PAGE
        val homePageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page ==
                    homePageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(homePageImpression.size).isAtLeast(1)

        val manageDataPageImpression =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).page ==
                    manageDataPageId &&
                    !it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).hasElement()
            }
        assertThat(manageDataPageImpression.size).isAtLeast(1)

        val manageDataInteraction =
            data.filter {
                it.atom.getExtension(UiExtensionAtoms.healthConnectUiInteraction).page ==
                    homePageId &&
                    it.atom.getExtension(UiExtensionAtoms.healthConnectUiInteraction).element ==
                        ElementId.MANAGE_DATA_BUTTON
            }
        assertThat(manageDataInteraction.size).isAtLeast(1)

        // Home page impressions
        val appPermissionsImpression =
            if (newHomeScreen()) {
                filterImpressionLogs(data, ElementId.SEE_ALL_CONNECTED_APPS_HOME_SCREEN_BUTTON)
            } else {
                filterImpressionLogs(data, ElementId.APP_PERMISSIONS_BUTTON)
            }
        assertThat(appPermissionsImpression.size).isAtLeast(1)

        if (newHomeScreen()) {
            val recentAccessButtonImpression =
                filterImpressionLogs(data, ElementId.RECENT_ACCESS_BUTTON)
            assertThat(recentAccessButtonImpression.size).isAtLeast(1)
        } else {
            val recentAccessDataImpression =
                filterImpressionLogs(data, ElementId.RECENT_ACCESS_ENTRY)
            assertThat(recentAccessDataImpression.size).isAtLeast(1)

            val seeAllRecentAccessImpression =
                filterImpressionLogs(data, ElementId.SEE_ALL_RECENT_ACCESS_BUTTON)
            assertThat(seeAllRecentAccessImpression.size).isAtLeast(1)
        }

        val toolbarImpression = filterImpressionLogs(data, ElementId.TOOLBAR_SETTINGS_BUTTON)
        assertThat(toolbarImpression.size).isAtLeast(1)

        // Manage data page impressions
        val autoDeleteImpression = filterImpressionLogs(data, ElementId.AUTO_DELETE_BUTTON)
        assertThat(autoDeleteImpression.size).isAtLeast(1)

        val dataSourcesAndPriorityImpression =
            filterImpressionLogs(data, ElementId.DATA_SOURCES_AND_PRIORITY_BUTTON)
        assertThat(dataSourcesAndPriorityImpression.size).isAtLeast(1)

        val setUnitsImpression = filterImpressionLogs(data, ElementId.SET_UNITS_BUTTON)
        assertThat(setUnitsImpression.size).isAtLeast(1)
    }

    private fun filterImpressionLogs(
        data: List<StatsLog.EventMetricData>,
        elementId: ElementId,
    ): List<StatsLog.EventMetricData> {
        return data.filter {
            it.atom.getExtension(UiExtensionAtoms.healthConnectUiImpression).element == elementId
        }
    }
}
