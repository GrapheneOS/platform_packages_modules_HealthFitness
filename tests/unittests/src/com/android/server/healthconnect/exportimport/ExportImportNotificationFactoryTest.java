/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.exportimport;

import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_COMPLETE;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE;
import static com.android.server.healthconnect.exportimport.ExportImportNotificationSender.NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.health.connect.HealthConnectManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.migration.notification.HealthConnectResourcesContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class ExportImportNotificationFactoryTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    @Mock private HealthConnectResourcesContext mResourcesContext;

    private static final Bitmap BITMAP = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    private static final Icon APP_ICON = Icon.createWithBitmap(BITMAP);
    private static final String NOTIFICATION_CHANNEL_ID = "healthconnect-channel";

    private static final String HEALTH_CONNECT_RESTART_IMPORT_ACTION =
            "android.health.connect.action.START_IMPORT_FLOW";
    private static final String HEALTH_CONNECT_RESTART_EXPORT_SETUP =
            "android.health.connect.action.START_EXPORT_SETUP";
    private static final String HEALTH_CONNECT_UPDATE_ACTION =
            "android.settings.SYSTEM_UPDATE_SETTINGS";

    private ExportImportNotificationFactory mFactory;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mFactory =
                new ExportImportNotificationFactory(
                        mContext, mResourcesContext, NOTIFICATION_CHANNEL_ID);
        // Return the requested name as the string resource
        when(mResourcesContext.getStringByName(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mResourcesContext.getStringByNameWithArgs(any(), any()))
                .thenAnswer(
                        invocation -> invocation.getArgument(0) + "," + invocation.getArgument(1));
        when(mResourcesContext.getIconByDrawableName(
                        ExportImportNotificationFactory.APP_ICON_DRAWABLE_NAME))
                .thenReturn(APP_ICON);
    }

    @Test
    public void testAllNotificationStringsExist() {
        String[] expectedStrings = mFactory.getNotificationStringResources();
        for (String s : expectedStrings) {
            String fetched = mFactory.getStringResource(s);
            assertThat(fetched).isEqualTo(s);
        }
    }

    @Test
    public void testAppIconDrawableExists() {
        assertThat(mFactory.getAppIcon()).hasValue(APP_ICON);
    }

    @Test
    public void importCompletesSuccessfully_notificationDisplayedCorrectly() {
        Notification result = mFactory.createNotification(NOTIFICATION_TYPE_IMPORT_COMPLETE);
        Intent expectedIntent = new Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_DATA);
        PendingIntent expectedPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, expectedIntent, PendingIntent.FLAG_IMMUTABLE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();

        assertThat(result.actions).hasLength(1);

        Notification.Action action = result.actions[0];
        assertThat(action.title.toString()).isEqualTo("import_notification_open_intent_button");

        PendingIntent pendingIntent = action.actionIntent;
        assertThat(pendingIntent.getCreatorPackage())
                .isEqualTo(expectedPendingIntent.getCreatorPackage());
    }

    @Test
    public void importCompletesUnsuccessfully_invalidFile_notificationDisplayedCorrectly() {
        Notification result =
                mFactory.createNotification(NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE);
        Intent expectedIntent = new Intent(HEALTH_CONNECT_RESTART_IMPORT_ACTION);
        PendingIntent expectedPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, expectedIntent, PendingIntent.FLAG_IMMUTABLE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();

        assertThat(result.actions).hasLength(1);

        Notification.Action action = result.actions[0];
        assertThat(action.title.toString())
                .isEqualTo("import_notification_choose_file_intent_button");

        PendingIntent pendingIntent = action.actionIntent;
        assertThat(pendingIntent.getCreatorPackage())
                .isEqualTo(expectedPendingIntent.getCreatorPackage());
    }

    @Test
    public void importCompletesUnsuccessfully_versionMismatch_notificationDisplayedCorrectly() {
        Notification result =
                mFactory.createNotification(NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH);
        Intent expectedIntent = new Intent(HEALTH_CONNECT_UPDATE_ACTION);
        PendingIntent expectedPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, expectedIntent, PendingIntent.FLAG_IMMUTABLE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();

        assertThat(result.actions).hasLength(1);

        Notification.Action action = result.actions[0];
        assertThat(action.title.toString())
                .isEqualTo("import_notification_update_now_intent_button");

        PendingIntent pendingIntent = action.actionIntent;
        assertThat(pendingIntent.getCreatorPackage())
                .isEqualTo(expectedPendingIntent.getCreatorPackage());
    }

    @Test
    public void importCompletesUnsuccessfully_unknownError_notificationDisplayedCorrectly() {
        Notification result =
                mFactory.createNotification(NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR);
        Intent expectedIntent = new Intent(HEALTH_CONNECT_RESTART_IMPORT_ACTION);
        PendingIntent expectedPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, expectedIntent, PendingIntent.FLAG_IMMUTABLE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();

        assertThat(result.actions).hasLength(1);

        Notification.Action action = result.actions[0];
        assertThat(action.title.toString())
                .isEqualTo("import_notification_try_again_intent_button");

        PendingIntent pendingIntent = action.actionIntent;

        assertThat(pendingIntent.getCreatorPackage())
                .isEqualTo(expectedPendingIntent.getCreatorPackage());
    }

    @Test
    @EnableFlags(Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW)
    public void exportCompletesUnsuccessfully_unknownError_notificationDisplayedCorrectly() {
        Notification result =
                mFactory.createNotification(NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR);
        Intent expectedIntent = new Intent(HEALTH_CONNECT_RESTART_EXPORT_SETUP);
        PendingIntent expectedPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, expectedIntent, PendingIntent.FLAG_IMMUTABLE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();

        assertThat(result.actions).hasLength(1);

        Notification.Action action = result.actions[0];
        assertThat(action.title.toString()).isEqualTo("export_notification_set_up_intent_button");

        PendingIntent pendingIntent = action.actionIntent;

        assertThat(pendingIntent.getCreatorPackage())
                .isEqualTo(expectedPendingIntent.getCreatorPackage());
    }

    @Test
    @EnableFlags(Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW)
    public void exportCompletesUnsuccessfully_moreSpaceNeeded_notificationDisplayedCorrectly() {
        Notification result =
                mFactory.createNotification(NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE);

        String failMessage = "Notification could not be created";
        assertWithMessage(failMessage).that(result).isNotNull();

        assertThat(result.getChannelId()).isEqualTo(NOTIFICATION_CHANNEL_ID);
        assertThat(result.extras.getString(Notification.EXTRA_TITLE)).isNotNull();
        assertThat(result.extras.getString(Notification.EXTRA_TITLE))
                .contains("export_notification_error_more_space_needed_title");
    }
}
