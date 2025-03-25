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

import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class MigrationNotificationFactoryTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private Context mContext;
    @Mock private HealthConnectResourcesContext mResourcesContext;

    @Before
    public void setUp() {}

    @Test
    public void testAllNotificationStringsFetchFromResources() {
        // Mock String resource loading by just returning the name as the resource.
        when(mResourcesContext.getStringByName(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MigrationNotificationFactory factory =
                new MigrationNotificationFactory(mContext, mResourcesContext);
        String[] expected = MigrationNotificationFactory.getNotificationStringResources();

        for (String s : expected) {
            String fetched = factory.getStringResource(s);
            String failMessage = "String resource with name " + s + " cannot be found.";
            assertWithMessage(failMessage).that(fetched).isEqualTo(s);
        }
    }

    @Test
    public void testAppIconDrawableFetchesFromResources() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Icon icon = Icon.createWithBitmap(bitmap);
        when(mResourcesContext.getIconByDrawableName(
                        eq(MigrationNotificationFactory.APP_ICON_DRAWABLE_NAME)))
                .thenReturn(icon);
        MigrationNotificationFactory factory =
                new MigrationNotificationFactory(mContext, mResourcesContext);

        Icon fetched = factory.getAppIcon();

        String failMessage =
                "Drawable resource with name "
                        + MigrationNotificationFactory.APP_ICON_DRAWABLE_NAME
                        + " cannot be found.";
        assertWithMessage(failMessage).that(fetched).isNotNull();
    }
}
