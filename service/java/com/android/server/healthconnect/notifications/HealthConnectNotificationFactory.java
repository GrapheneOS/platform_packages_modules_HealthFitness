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

package com.android.server.healthconnect.notifications;

import android.annotation.Nullable;
import android.app.Notification;
import android.graphics.drawable.Icon;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Optional;

/**
 * Interface for the HealthConnect NotificationFactory
 *
 * @hide
 */
public interface HealthConnectNotificationFactory {

    /** Returns a {@link Notification} depending on the specified type. */
    @Nullable
    Notification createNotification(int notificationType);

    /** Returns an {@link Icon} to be displayed on the notification. */
    @VisibleForTesting
    Optional<Icon> getAppIcon();

    /** Returns a list of string identifiers associated with a notification - e.g. the title. */
    @VisibleForTesting
    String[] getNotificationStringResources();
}
