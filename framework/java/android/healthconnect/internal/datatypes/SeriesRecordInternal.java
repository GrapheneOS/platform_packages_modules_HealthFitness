/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.internal.datatypes;

import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_SAMPLES;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.healthconnect.datatypes.IntervalRecord;
import android.os.Bundle;

import java.util.List;

/**
 * Parent class for all the Series type records.
 *
 * <p>U -> Sample type for series record
 *
 * @hide
 */
public abstract class SeriesRecordInternal<T extends IntervalRecord, U>
        extends IntervalRecordInternal<T> {
    @NonNull
    public abstract List<? extends Sample> getSamples();

    @NonNull
    public abstract SeriesRecordInternal setSamples(List<? extends Sample> samples);

    @Override
    final void populateIntervalRecordFrom(@NonNull Bundle payload) {
        final List<Bundle> samples =
                requireNonNull(payload.getParcelableArrayList(DM_RECORD_SAMPLES, Bundle.class));
        populateSamplesFrom(samples);
    }

    /** Populates samples using the provided data migration payloads. */
    void populateSamplesFrom(@NonNull List<Bundle> payloads) {
        // TODO(b/263571058): Make abstract when implemented for all records
    }

    /** Base class for the series data stored in {@link SeriesRecordInternal} types */
    public interface Sample {}
}
