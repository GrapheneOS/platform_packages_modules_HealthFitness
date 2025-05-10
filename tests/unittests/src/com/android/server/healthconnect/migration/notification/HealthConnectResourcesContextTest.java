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

package com.android.server.healthconnect.migration.notification;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.exportimport.ExportImportNotificationFactory;

import org.junit.Before;
import org.junit.Test;

/**
 * A unit test to ensure that resources that are needed from the Health Connect controller APK are
 * indeed present.
 *
 * <p>THis test is not expected to be bivalent.
 */
public class HealthConnectResourcesContextTest {

    private Context mContext;
    private HealthConnectResourcesContext mResourcesContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mResourcesContext = new HealthConnectResourcesContext(mContext);
    }

    @Test
    public void testAllMigrationNotificationStringsExist() {
        String[] expected = MigrationNotificationFactory.getNotificationStringResources();
        for (String s : expected) {
            String fetched = mResourcesContext.getStringByNameOrThrow(s);
            String failMessage = "String resource with name " + s + " cannot be found.";
            assertWithMessage(failMessage).that(fetched).isNotNull();
        }
    }

    @Test
    public void testAppIconDrawableExists() {
        assertThat(
                        mResourcesContext.getIconByDrawableName(
                                MigrationNotificationFactory.APP_ICON_DRAWABLE_NAME))
                .isNotNull();
    }

    @Test
    public void testAllExportImportNotificationStringsExist() {
        ExportImportNotificationFactory factory =
                new ExportImportNotificationFactory(
                        mContext, mResourcesContext, "healthconnect-channel");
        String[] expectedStrings = factory.getNotificationStringResources();
        for (String s : expectedStrings) {
            String fetched = mResourcesContext.getStringByNameOrThrow(s);
            String failMessage = "String resource with name " + s + " cannot be found.";
            assertWithMessage(failMessage).that(fetched).isNotNull();
        }
    }
}
