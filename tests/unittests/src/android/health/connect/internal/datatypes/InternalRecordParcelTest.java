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

import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.testing.RecordFactory;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.base.Preconditions;
import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/** This is a test across all internal records, rather than across any particular record. */
@RunWith(AndroidJUnit4.class)
public class InternalRecordParcelTest {
    @Rule public final Expect expect = Expect.create();

    /** Test that all internal records have a constructor for the parcel path. */
    @Test
    public void testAllInternalRecords_haveParcelConstructor() throws NoSuchMethodException {
        HealthConnectMappings mappings = HealthConnectMappings.getInstance();
        Collection<Class<? extends RecordInternal<?>>> internalClasses =
                mappings.getRecordIdToInternalRecordClassMap().values();

        for (Class<? extends RecordInternal<?>> clazz : internalClasses) {
            // Check for a constructor that takes no arguments. If it fails throws
            // NoSuchMethodException.
            assertThat(clazz.getConstructor()).isNotNull();
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
     *   <li>How do we get a fully populated instance? This is delegated to a test utility method
     *       {@link RecordFactory} that is tested by this class exercising it.
     * </ul>
     *
     * Note: this test is not a replacement for unit tests. It does not test all edge conditions. It
     * is an integration test that all the serialization infrastructure works together for one
     * instance.
     */
    @Test
    public void testAllInternalRecords_serializeToAndFromParcels() throws Exception {
        HealthConnectMappings mappings = HealthConnectMappings.getInstance();
        Map<Integer, Class<? extends RecordInternal<?>>> recordIdToInternalRecord =
                mappings.getRecordIdToInternalRecordClassMap();

        for (Map.Entry<Integer, Class<? extends RecordInternal<?>>> entry :
                recordIdToInternalRecord.entrySet()) {
            int recordType = entry.getKey();
            Class<? extends RecordInternal<?>> recordInternalClass = entry.getValue();
            // Create a fully populated Record of every type (so we can use it for equality
            // testing later).
            Record record = RecordFactory.makePopulatedRecord(recordType);
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
                    internalRecord.getClass().getConstructor();
            RecordInternal<?> internalRecordCopy = parcelConstructor.newInstance();
            internalRecordCopy.populateUsing(parcel);
            parcel.recycle();

            // Unfortunately do a hack to normalize the sample order.
            normalizeSampleOrder(internalRecordCopy);

            // Convert the deserialized RecordInternal back to a record
            Record recordCopy = internalRecordCopy.toExternalRecord();

            // Check nothing has been lost
            expect.withMessage("Failed parcel conversion for %s, %s", record.getClass(), recordType)
                    .that(recordCopy)
                    .isEqualTo(record);
        }
    }

    /**
     * Sort the samples if appropriate.
     *
     * <p>At the moment for the following 4 data types, the ordering of the samples affects
     * .equals(), but read then write doesn't guarantee preservation of this. So before doing the
     * comparison, normalize the order.
     *
     * <p>At the moment this method makes a number of assumptions which happen to be true at the
     * moment but need not be in future:
     *
     * <ul>
     *   <li>RecordFactory returns the samples in time order.
     *   <li>The various RecordInternals are mutable
     *   <li>When calling setSamples() the set passed is kept rather than copied
     *   <li>The iteration order for the set is used when creating the list for the Record
     * </ul>
     */
    private static void normalizeSampleOrder(RecordInternal<?> internalRecordCopy) {
        switch (internalRecordCopy) {
            case HeartRateRecordInternal heartRateRecordInternal -> {
                TreeSet<HeartRateRecordInternal.HeartRateSample> sortedSamples =
                        new TreeSet<>(
                                (sample1, sample2) -> {
                                    int result1 =
                                            Long.compare(
                                                    sample1.getEpochMillis(),
                                                    sample2.getEpochMillis());
                                    if (result1 != 0) {
                                        return result1;
                                    } else {
                                        return Integer.compare(
                                                sample1.getBeatsPerMinute(),
                                                sample2.getBeatsPerMinute());
                                    }
                                });
                sortedSamples.addAll(heartRateRecordInternal.getSamples());
                heartRateRecordInternal.setSamples(sortedSamples);
            }
            case PowerRecordInternal powerRecordInternal -> {
                TreeSet<PowerRecordInternal.PowerRecordSample> sortedSamples =
                        new TreeSet<>(
                                (sample1, sample2) -> {
                                    int result1 =
                                            Long.compare(
                                                    sample1.getEpochMillis(),
                                                    sample2.getEpochMillis());
                                    if (result1 != 0) {
                                        return result1;
                                    } else {
                                        return Double.compare(
                                                sample1.getPower(), sample2.getPower());
                                    }
                                });
                sortedSamples.addAll(powerRecordInternal.getSamples());
                powerRecordInternal.setSamples(sortedSamples);
            }
            case SpeedRecordInternal speedRecordInternal -> {
                TreeSet<SpeedRecordInternal.SpeedRecordSample> sortedSamples =
                        new TreeSet<>(
                                (sample1, sample2) -> {
                                    int result1 =
                                            Long.compare(
                                                    sample1.getEpochMillis(),
                                                    sample2.getEpochMillis());
                                    if (result1 != 0) {
                                        return result1;
                                    } else {
                                        return Double.compare(
                                                sample1.getSpeed(), sample2.getSpeed());
                                    }
                                });
                sortedSamples.addAll(speedRecordInternal.getSamples());
                speedRecordInternal.setSamples(sortedSamples);
            }
            case StepsCadenceRecordInternal stepsCadenceRecordInternal -> {
                TreeSet<StepsCadenceRecordInternal.StepsCadenceRecordSample> sortedSamples =
                        new TreeSet<>(
                                (sample1, sample2) -> {
                                    int result1 =
                                            Long.compare(
                                                    sample1.getEpochMillis(),
                                                    sample2.getEpochMillis());
                                    if (result1 != 0) {
                                        return result1;
                                    } else {
                                        return Double.compare(sample1.getRate(), sample2.getRate());
                                    }
                                });
                sortedSamples.addAll(stepsCadenceRecordInternal.getSamples());
                stepsCadenceRecordInternal.setSamples(sortedSamples);
            }
            default -> {
                // All other cases leave unchanged.
            }
        }
    }
}
