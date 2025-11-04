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
package android.health.connect.aidl;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.StepsRecord;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
public class ReadRecordsRequestParcelTest {
    @Test
    public void withDeviceId_ReadRecordsRequestParcelWithFilters_setsPackageFiltersToDeviceId() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("Foo")
                        .build();

        ReadRecordsRequestParcel parcel = new ReadRecordsRequestParcel(request);

        assertThat(parcel.getPackageFilters()).containsExactly("Foo");
    }

    @Test
    public void
            withDataOrigins_ReadRecordsRequestParcelWithFilters_setsPackageFiltersToDataOrigins() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(new DataOrigin.Builder().setPackageName("Bar").build())
                        .build();

        ReadRecordsRequestParcel parcel = new ReadRecordsRequestParcel(request);

        assertThat(parcel.getPackageFilters()).containsExactly("Bar");
    }

    @Test
    public void
            withNeitherDataOriginsOrDeviceId_ReadRecordsRequestParcelWithFilters_filtersEmpty() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build();

        ReadRecordsRequestParcel parcel = new ReadRecordsRequestParcel(request);

        assertThat(parcel.getPackageFilters()).isEmpty();
    }

    @Test
    public void withRequestUsingFilter_toDdpRequestParcel_replacesPackageFilter() {
        ReadRecordsRequestParcel originalParcel =
                new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                        .setDeviceId("some device id")
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochMilli(1000))
                                        .build())
                        .setPageSize(2)
                        .setAscending(false)
                        .build()
                        .toReadRecordsRequestParcel();
        ReadRecordsRequestParcel ddpParcel =
                originalParcel.toDdpRequestParcel("internal device spn");

        // Creates a copy
        assertThat(ddpParcel).isNotSameInstanceAs(originalParcel);

        // Overwrites package filters
        assertThat(ddpParcel.getPackageFilters()).containsExactly("internal device spn");

        // Other fields are the same
        assertEquals(
                originalParcel.getRecordIdFiltersParcel(), ddpParcel.getRecordIdFiltersParcel());
        assertEquals(originalParcel.getRecordType(), ddpParcel.getRecordType());
        assertEquals(originalParcel.getStartTime(), ddpParcel.getStartTime());
        assertEquals(originalParcel.getEndTime(), ddpParcel.getEndTime());
        assertEquals(originalParcel.getPageSize(), ddpParcel.getPageSize());
        assertEquals(originalParcel.getPageToken(), ddpParcel.getPageToken());
        assertEquals(originalParcel.isAscending(), ddpParcel.isAscending());
        assertEquals(originalParcel.usesLocalTimeFilter(), ddpParcel.usesLocalTimeFilter());
    }

    @Test
    public void withRequestUsingIds_toDdpRequestParcel_returnsOriginal() {
        ReadRecordsRequestParcel originalParcel =
                new ReadRecordsRequestUsingIds.Builder<>(DistanceRecord.class)
                        .addId("Some id")
                        .build()
                        .toReadRecordsRequestParcel();

        ReadRecordsRequestParcel ddpParcel =
                originalParcel.toDdpRequestParcel("internal device spn");

        assertThat(ddpParcel).isSameInstanceAs(originalParcel);
        assertThat(ddpParcel.getPackageFilters()).isEmpty();
    }

    @Test
    public void withRequestUsingEmptyFiltersAndNoDeviceId_toDdpRequestParcel_returnsOriginal() {
        ReadRecordsRequestParcel originalParcel =
                new ReadRecordsRequestUsingFilters.Builder<>(DistanceRecord.class)
                        .build()
                        .toReadRecordsRequestParcel();

        ReadRecordsRequestParcel ddpParcel =
                originalParcel.toDdpRequestParcel("internal device spn");

        assertThat(ddpParcel).isSameInstanceAs(originalParcel);
        assertThat(ddpParcel.getPackageFilters()).isEmpty();
    }
}
