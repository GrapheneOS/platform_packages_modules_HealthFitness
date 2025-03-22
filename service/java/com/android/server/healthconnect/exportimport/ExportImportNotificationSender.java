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

import android.annotation.IntDef;
import android.content.Context;

import com.android.server.healthconnect.notifications.HealthConnectNotificationSender;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Export-import specific implementation of the HealthConnectNotificationSender.
 *
 * @hide
 */
@HealthConnectNotificationSender.HealthConnectNotificationType
public class ExportImportNotificationSender {

    private static final String TAG = "ExportImportNotificationSender";
    private static final int FIXED_NOTIFICATION_ID = 9877;
    private static final String NOTIFICATION_TAG = "HealthConnectTag";
    private static final String CHANNEL_ID = "healthconnect-channel";
    private static final String CHANNEL_GROUP_ID = "healthconnect-channel-group";
    private static final String CHANNEL_NAME_RESOURCE = "app_label";
    private static final String CHANNEL_GROUP_NAME_RESOURCE = "app_label";

    /** Create an instance of HealthConnectNotificationSender, setup for export-import. */
    public static HealthConnectNotificationSender createSender(Context context) {
        return new HealthConnectNotificationSender.Builder()
                .setContext(context)
                .setNotificationFactory(new ExportImportNotificationFactory(context, CHANNEL_ID))
                .setChannelGroupId(CHANNEL_GROUP_ID)
                .setChannelNameResource(CHANNEL_NAME_RESOURCE)
                .setChannelGroupNameResource(CHANNEL_GROUP_NAME_RESOURCE)
                .setChannelId(CHANNEL_ID)
                .setFixedNotificationId(FIXED_NOTIFICATION_ID)
                .setNotificationTag(NOTIFICATION_TAG)
                .setIsEnabled(true)
                .build();
    }

    public static final int NOTIFICATION_TYPE_IMPORT_IN_PROGRESS = 0;
    public static final int NOTIFICATION_TYPE_IMPORT_COMPLETE = 1;
    public static final int NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR = 2;
    public static final int NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE = 3;
    public static final int NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE = 4;
    public static final int NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH = 5;
    public static final int NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR = 6;
    public static final int NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE = 7;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        NOTIFICATION_TYPE_IMPORT_IN_PROGRESS,
        NOTIFICATION_TYPE_IMPORT_COMPLETE,
        NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_GENERIC_ERROR,
        NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_INVALID_FILE,
        NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE,
        NOTIFICATION_TYPE_IMPORT_UNSUCCESSFUL_VERSION_MISMATCH,
        NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_GENERIC_ERROR,
        NOTIFICATION_TYPE_EXPORT_UNSUCCESSFUL_NOT_ENOUGH_SPACE
    })
    public @interface ExportImportNotificationType {}
}
