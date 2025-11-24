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

package android.health.connect.internal.datatypes;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.SuppressLint;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.healthconnect.testing.shared.recordfactory.RecordFactory;
import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.base.Preconditions;
import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;

/** This is a test across all internal records, rather than across any particular record. */
@RunWith(AndroidJUnit4.class)
public class InternalRecordParcelTest {
    @Rule public final Expect expect = Expect.create();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    @SuppressLint("VisibleForTests") // this is indeed a test file
    public void setup() {
        HealthConnectMappings.resetInstanceForTesting();
    }

    /** Test that all internal records have a constructor for the parcel path. */
    @Test
    public void testAllInternalRecords_haveParcelConstructor() throws NoSuchMethodException {
        HealthConnectMappings mappings = HealthConnectMappings.getInstance();
        Collection<Class<? extends RecordInternal<?>>> internalClasses =
                mappings.getRecordIdToInternalRecordClassMap().values();

        for (Class<? extends RecordInternal<?>> clazz : internalClasses) {
            // Check for a constructor that takes no arguments. If it fails throws
            // NoSuchMethodException.
            assertThat(clazz.getConstructor(Parcel.class)).isNotNull();
        }
    }

    /**
     * Test that all internal records correctly serialize an instance to and from parcels. This acts
     * more like an integration test.
     *
     * <p>While these tests should be on the unit tests for the RecordInternal themselves, it is
     * important for system wide code health that this works for any RecordInternal. There are some
     * difficult problems with this test:
     *
     * <ul>
     *   <li>How do we get a list of all record classes? We get this from HealthConnectMappings.
     *   <li>How do we test equality between two {@link RecordInternal}s? We actually test equality
     *       on the equivalent records themselves
     * </ul>
     *
     * Note: this test is not a replacement for unit tests. It does not test all edge conditions. It
     * is an integration test that all the serialization infrastructure works together for one
     * instance.
     */
    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testAllInternalRecords_serializeToAndFromParcels() throws Exception {
        HealthConnectMappings mappings = HealthConnectMappings.getInstance();
        Map<Integer, Class<? extends RecordInternal<?>>> recordIdToInternalRecord =
                mappings.getRecordIdToInternalRecordClassMap();

        for (Map.Entry<Integer, Class<? extends RecordInternal<?>>> entry :
                recordIdToInternalRecord.entrySet()) {
            int recordType = entry.getKey();
            Class<? extends RecordInternal<?>> recordInternalClass = entry.getValue();
            Class<? extends Record> recordExternalClass =
                    mappings.getRecordIdToExternalRecordClassMap().get(recordType);
            // Create a fully populated Record of every type (so we can use it for equality
            // testing later).
            Record record = RecordFactory.newFullRecordForType(recordExternalClass);
            // Convert to an internal record.
            RecordInternal<?> internalRecord = record.toRecordInternal();
            // Check that the factory was working properly and gave us a record of the type
            // we want to test.
            Preconditions.checkState(recordInternalClass.isInstance(internalRecord));

            // Send the internal record to a parcel.
            Parcel parcel = Parcel.obtain();
            internalRecord.writeToParcel(parcel);
            parcel.setDataPosition(0);

            // Make sure we can bring back from a parcel via a constructor
            Constructor<? extends RecordInternal> parcelConstructor =
                    internalRecord.getClass().getConstructor(Parcel.class);
            RecordInternal<?> internalRecordCopy = parcelConstructor.newInstance(parcel);
            parcel.recycle();
            // It would be nice to check equality for the internal record here, but most internal
            // records don't have equals() and hashcode() implementations.

            // Convert the deserialized RecordInternal back to a record
            Record recordCopy = internalRecordCopy.toExternalRecord();

            // Check nothing has been lost
            expect.withMessage(
                            "Failed parcel conversion for %s, not equal to %s", record, recordCopy)
                    .that(recordCopy)
                    .isEqualTo(record);
        }
    }
}
