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

package com.android.server.healthconnect.storage.utils;

import static com.android.server.healthconnect.storage.utils.PageTokenUtil.MAX_ALLOWED_OFFSET;
import static com.android.server.healthconnect.storage.utils.PageTokenUtil.MAX_ALLOWED_TIME_MILLIS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PageTokenWrapperTest {
    @Test
    public void of_createInstance() {
        boolean isAscending = false;
        long timeMillis = 123;
        int offset = 456;
        PageTokenWrapper wrapper = PageTokenWrapper.of(isAscending, timeMillis, offset);

        assertThat(wrapper.isAscending()).isEqualTo(isAscending);
        assertThat(wrapper.timeMillis()).isEqualTo(timeMillis);
        assertThat(wrapper.offset()).isEqualTo(offset);
        assertThat(wrapper.isTimestampSet()).isTrue();
    }

    @Test
    public void of_isAscending_timestampNotSet() {
        PageTokenWrapper wrapper = PageTokenWrapper.ofAscending(/* isAscending= */ true);
        assertThat(wrapper.isAscending()).isTrue();
        assertThat(wrapper.timeMillis()).isEqualTo(0);
        assertThat(wrapper.offset()).isEqualTo(0);
        assertThat(wrapper.isTimestampSet()).isFalse();
    }

    @Test
    public void of_offsetTooLarge_setToMax() {
        boolean isAscending = true;
        PageTokenWrapper wrapper =
                PageTokenWrapper.of(isAscending, /* timeMillis= */ 0, (int) MAX_ALLOWED_OFFSET + 1);
        assertThat(wrapper.offset()).isEqualTo(MAX_ALLOWED_OFFSET);
    }

    @Test
    public void of_invalidArgument_throws() {
        boolean isAscending = true;
        Throwable thrown;

        thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PageTokenWrapper.of(
                                        isAscending, /* timeMillis= */ -1, /* offset= */ 0));
        assertThat(thrown.getMessage()).isEqualTo("timestamp can not be negative");

        thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PageTokenWrapper.of(
                                        isAscending, /* timeMillis= */ 0, /* offset= */ -1));
        assertThat(thrown.getMessage()).isEqualTo("offset can not be negative");

        thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PageTokenWrapper.of(
                                        isAscending, MAX_ALLOWED_TIME_MILLIS + 1, /* offset= */ 0));
        assertThat(thrown.getMessage()).isEqualTo("timestamp too large");
    }

    @Test
    public void equals_sameValue_expectTrue() {
        PageTokenWrapper wrapper1 =
                PageTokenWrapper.of(
                        /* isAscending= */ false, /* timeMillis= */ 1234, /* offset= */ 567);
        PageTokenWrapper wrapper2 =
                PageTokenWrapper.of(
                        /* isAscending= */ false, /* timeMillis= */ 1234, /* offset= */ 567);

        assertThat(wrapper1.equals(wrapper2)).isTrue();
    }

    @Test
    public void equals_differentValue_expectFalse() {
        PageTokenWrapper wrapper =
                PageTokenWrapper.of(
                        /* isAscending= */ false, /* timeMillis= */ 1234, /* offset= */ 567);
        PageTokenWrapper differentIsAscending =
                PageTokenWrapper.of(
                        /* isAscending= */ true, /* timeMillis= */ 1234, /* offset= */ 567);
        PageTokenWrapper differentTime =
                PageTokenWrapper.of(
                        /* isAscending= */ false, /* timeMillis= */ 123, /* offset= */ 567);
        PageTokenWrapper differentOffset =
                PageTokenWrapper.of(
                        /* isAscending= */ false, /* timeMillis= */ 1234, /* offset= */ 5678);

        assertThat(wrapper.equals(differentIsAscending)).isFalse();
        assertThat(wrapper.equals(differentTime)).isFalse();
        assertThat(wrapper.equals(differentOffset)).isFalse();
    }
}
