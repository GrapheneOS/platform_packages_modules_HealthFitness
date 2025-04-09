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

package android.health.connect.internal.datatypes;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.SleepSessionRecord;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.Period;

@RunWith(AndroidJUnit4.class)
public class SleepStageInternalTest {
    private final Instant mStartTime = Instant.now().minus(Period.ofDays(1));
    private final Instant mEndTime = Instant.now();

    @Test
    public void testSleepStageInternal_convertToExternalAndBack_recordsAreEqual() {
        SleepSessionRecord.Stage external =
                new SleepSessionRecord.Stage(
                        mStartTime, mEndTime, SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        SleepSessionRecord.Stage converted = external.toInternalStage().toExternalRecord();

        // Compare time in milliseconds as we store time in milliseconds in the database.
        assertThat(converted.getStartTime().toEpochMilli())
                .isEqualTo(external.getStartTime().toEpochMilli());
        assertThat(converted.getEndTime().toEpochMilli())
                .isEqualTo(external.getEndTime().toEpochMilli());

        assertThat(converted.getType()).isEqualTo(external.getType());
    }

    @Test
    public void testSleepStageInternal_writeToParcelAndBack_recordsAreEqual() {
        SleepStageInternal initial =
                new SleepStageInternal()
                        .setStartTime(mStartTime.toEpochMilli())
                        .setEndTime(mEndTime.toEpochMilli())
                        .setStageType(SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_IN_BED);
        Parcel parcel = Parcel.obtain();
        initial.writeToParcel(parcel);
        parcel.setDataPosition(0);
        SleepStageInternal restored = SleepStageInternal.readFromParcel(parcel);
        parcel.recycle();

        assertThat(restored.getStartTime()).isEqualTo(initial.getStartTime());
        assertThat(restored.getEndTime()).isEqualTo(initial.getEndTime());
        assertThat(restored.getStageType()).isEqualTo(initial.getStageType());
        assertThat(restored).isEqualTo(initial);
    }
}
