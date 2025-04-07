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

package android.healthconnect.internal.datatypes.utils;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.InternalExternalRecordConverter;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InternalExternalRecordConverterTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock RecordInternal<?> mRecordInternal;

    @Test
    public void testGetExternalRecords_iaeWithNullMessage_rethrown() {
        when(mRecordInternal.toExternalRecord()).thenThrow(new IllegalArgumentException());
        InternalExternalRecordConverter converter = InternalExternalRecordConverter.getInstance();

        assertThrows(
                IllegalArgumentException.class,
                () -> converter.getExternalRecords(List.of(mRecordInternal)));
    }

    @Test
    public void testGetExternalRecords_iaeWithRandomMessage_rethrown() {
        when(mRecordInternal.toExternalRecord()).thenThrow(new IllegalArgumentException("foo"));
        InternalExternalRecordConverter converter = InternalExternalRecordConverter.getInstance();

        assertThrows(
                IllegalArgumentException.class,
                () -> converter.getExternalRecords(List.of(mRecordInternal)));
    }

    @Test
    public void testGetExternalRecords_iaeWithIllegalIntdefMessage_swallowed() {
        when(mRecordInternal.toExternalRecord())
                .thenThrow(new IllegalArgumentException("Unknown Intdef value: foo"));
        InternalExternalRecordConverter converter = InternalExternalRecordConverter.getInstance();

        assertThat(converter.getExternalRecords(List.of(mRecordInternal))).isEmpty();
    }
}
