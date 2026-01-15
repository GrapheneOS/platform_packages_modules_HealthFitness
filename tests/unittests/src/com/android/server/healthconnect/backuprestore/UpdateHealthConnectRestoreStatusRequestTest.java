/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.backuprestore;

import static android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest.RESTORE_STATUS_ERROR_UNKNOWN;
import static android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest.RESTORE_STATUS_SUCCESS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.backuprestore.UpdateHealthConnectRestoreStatusRequest;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UpdateHealthConnectRestoreStatusRequestTest {
    @Test
    public void testBuild_withOnlyRequiredFields_buildsRequest() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .build();

        assertThat(request.getStatusCode()).isEqualTo(RESTORE_STATUS_SUCCESS);
        assertThat(request.getTimestampInEpochMillis()).isEqualTo(now);
        assertThat(request.getStatusTitle()).isNull();
        assertThat(request.getStatusMessage()).isNull();
    }

    @Test
    public void testBuild_withAllFields_buildsRequest() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();

        assertThat(request.getStatusCode()).isEqualTo(RESTORE_STATUS_SUCCESS);
        assertThat(request.getTimestampInEpochMillis()).isEqualTo(now);
        assertThat(request.getStatusTitle()).isEqualTo("title");
        assertThat(request.getStatusMessage()).isEqualTo("message");
    }

    @Test
    public void testParcel_unparcel_equals() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        UpdateHealthConnectRestoreStatusRequest unparceledRequest =
                UpdateHealthConnectRestoreStatusRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledRequest).isEqualTo(request);
    }

    @Test
    public void testEquals_sameContent_returnsTrue() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request1 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();
        UpdateHealthConnectRestoreStatusRequest request2 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();

        assertThat(request1.equals(request2)).isTrue();
    }

    @Test
    public void testEquals_differentContent_returnsFalse() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request1 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();
        UpdateHealthConnectRestoreStatusRequest request2 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(
                                RESTORE_STATUS_ERROR_UNKNOWN, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();

        assertThat(request1.equals(request2)).isFalse();
    }

    @Test
    public void testHashCode_sameContent_returnsSameHashCode() {
        long now = System.currentTimeMillis();
        UpdateHealthConnectRestoreStatusRequest request1 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();
        UpdateHealthConnectRestoreStatusRequest request2 =
                new UpdateHealthConnectRestoreStatusRequest.Builder(RESTORE_STATUS_SUCCESS, now)
                        .setStatusTitle("title")
                        .setStatusMessage("message")
                        .build();

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }
}
