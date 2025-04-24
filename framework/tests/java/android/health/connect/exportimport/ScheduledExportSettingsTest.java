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

package android.health.connect.exportimport;

import static android.health.connect.Constants.DEFAULT_INT;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ScheduledExportSettingsTest {
    private static final Uri TEST_URI =
            Uri.parse("content://com.android.server.healthconnect/testuri");

    @Test
    public void testWithUri() {
        ScheduledExportSettings settings =
                new ScheduledExportSettings.Builder().setUri(TEST_URI).build();

        Parcel settingsParcel = writeToParcel(settings);
        settingsParcel.setDataPosition(0);
        ScheduledExportSettings deserializedSettings =
                settingsParcel.readTypedObject(ScheduledExportSettings.CREATOR);

        assertThat(deserializedSettings.getUri()).isEqualTo(TEST_URI);
        assertThat(deserializedSettings.getPeriodInDays()).isEqualTo(DEFAULT_INT);
    }

    @Test
    public void testWithPeriodInDays() {
        ScheduledExportSettings settings =
                new ScheduledExportSettings.Builder().setPeriodInDays(7).build();

        Parcel settingsParcel = writeToParcel(settings);
        settingsParcel.setDataPosition(0);
        ScheduledExportSettings deserializedSettings =
                settingsParcel.readTypedObject(ScheduledExportSettings.CREATOR);

        assertThat(deserializedSettings.getUri()).isNull();
        assertThat(deserializedSettings.getPeriodInDays()).isEqualTo(7);
    }

    @Test
    public void testWithUriAndPeriodInDays() {
        ScheduledExportSettings settings =
                new ScheduledExportSettings.Builder().setPeriodInDays(7).setUri(TEST_URI).build();

        Parcel settingsParcel = writeToParcel(settings);
        settingsParcel.setDataPosition(0);
        ScheduledExportSettings deserializedSettings =
                settingsParcel.readTypedObject(ScheduledExportSettings.CREATOR);

        assertThat(deserializedSettings.getUri()).isEqualTo(TEST_URI);
        assertThat(deserializedSettings.getPeriodInDays()).isEqualTo(7);
    }

    @Test
    public void testEquals_andHashCode_withPeriodInDays() {
        ScheduledExportSettings settingsA =
                new ScheduledExportSettings.Builder().setPeriodInDays(7).build();
        ScheduledExportSettings settingsB =
                new ScheduledExportSettings.Builder().setPeriodInDays(7).build();

        assertThat(settingsA).isEqualTo(settingsB);
        assertThat(settingsA.hashCode()).isEqualTo(settingsB.hashCode());
    }

    @Test
    public void testEquals_andHashCode_withDifferentPeriodInDays() {
        ScheduledExportSettings settingsA =
                new ScheduledExportSettings.Builder().setPeriodInDays(7).build();
        ScheduledExportSettings settingsB =
                new ScheduledExportSettings.Builder().setPeriodInDays(8).build();

        assertThat(settingsA).isNotEqualTo(settingsB);
        assertThat(settingsA.hashCode()).isNotEqualTo(settingsB.hashCode());
    }

    @Test
    public void testEquals_andHashCode_withUri() {
        ScheduledExportSettings settingsA =
                new ScheduledExportSettings.Builder().setUri(TEST_URI).build();
        ScheduledExportSettings settingsB =
                new ScheduledExportSettings.Builder().setUri(TEST_URI).build();

        assertThat(settingsA).isEqualTo(settingsB);
        assertThat(settingsA.hashCode()).isEqualTo(settingsB.hashCode());
    }

    @Test
    public void testEquals_andHashCode_withDifferentUri() {
        ScheduledExportSettings settingsA =
                new ScheduledExportSettings.Builder().setUri(TEST_URI).build();
        ScheduledExportSettings settingsB =
                new ScheduledExportSettings.Builder().setUri(Uri.EMPTY).build();

        assertThat(settingsA).isNotEqualTo(settingsB);
        assertThat(settingsA.hashCode()).isNotEqualTo(settingsB.hashCode());
    }

    private static Parcel writeToParcel(ScheduledExportSettings settings) {
        Parcel settingsParcel = Parcel.obtain();
        settingsParcel.writeTypedObject(settings, 0);
        return settingsParcel;
    }
}
