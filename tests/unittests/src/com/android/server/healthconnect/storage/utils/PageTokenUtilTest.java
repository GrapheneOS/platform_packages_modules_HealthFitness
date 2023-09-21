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

import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.PageTokenUtil.MAX_ALLOWED_TIME_MILLIS;
import static com.android.server.healthconnect.storage.utils.PageTokenUtil.encode;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.time.temporal.ChronoUnit.DAYS;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
public class PageTokenUtilTest {

    @Test
    public void encodeAndRetrieveIsAscending_expectCorrectResult() {
        PageTokenWrapper wrapper =
                PageTokenWrapper.of(/* isAscending= */ true, 1234, /* offset= */ 0);
        long token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token, /* defaultIsAscending= */ false).isAscending()).isTrue();

        wrapper = PageTokenWrapper.of(/* isAscending= */ false, 5678, /* offset= */ 0);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token, /* defaultIsAscending= */ true).isAscending()).isFalse();
    }

    @Test
    public void encodeAndRetrieveTimestamp_expectCorrectResult() {
        long nowTimeMillis = Instant.now().toEpochMilli();
        PageTokenWrapper wrapper =
                PageTokenWrapper.of(/* isAscending= */ false, nowTimeMillis, /* offset= */ 0);
        long token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).timeMillis()).isEqualTo(nowTimeMillis);

        long futureTimeMillis = Instant.now().plus(36500, DAYS).toEpochMilli();
        wrapper = PageTokenWrapper.of(/* isAscending= */ true, futureTimeMillis, /* offset= */ 0);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).timeMillis()).isEqualTo(futureTimeMillis);

        long pastTimeMillis = Instant.now().minus(3650, DAYS).toEpochMilli();
        wrapper = PageTokenWrapper.of(/* isAscending= */ true, pastTimeMillis, /* offset= */ 0);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).timeMillis()).isEqualTo(pastTimeMillis);

        wrapper =
                PageTokenWrapper.of(/* isAscending= */ true, /* timeMillis= */ 0, /* offset= */ 0);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).timeMillis()).isEqualTo(0);

        wrapper =
                PageTokenWrapper.of(
                        /* isAscending= */ true, MAX_ALLOWED_TIME_MILLIS, /* offset= */ 0);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).timeMillis()).isEqualTo(MAX_ALLOWED_TIME_MILLIS);
    }

    @Test
    public void encodeAndRetrieveOffset_expectCorrectResult() {
        int maxOffset = (1 << 18) - 1;
        int minOffset = 0;
        long timestamp = Instant.now().toEpochMilli();

        PageTokenWrapper wrapper =
                PageTokenWrapper.of(/* isAscending= */ false, timestamp, maxOffset);
        long token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).offset()).isEqualTo(maxOffset);

        wrapper = PageTokenWrapper.of(/* isAscending= */ true, timestamp, minOffset);
        token = encode(wrapper);
        assertThat(wrapper.isTimestampSet()).isTrue();
        assertThat(decode(token).offset()).isEqualTo(minOffset);
    }

    @Test
    public void decode_pageTokenNotSet_defaultIsAscendingUsed() {
        PageTokenWrapper wrapper = decode(DEFAULT_LONG, /* defaultIsAscending= */ true);
        assertThat(wrapper.isTimestampSet()).isFalse();
        assertThat(wrapper.isAscending()).isTrue();

        wrapper = decode(DEFAULT_LONG, /* defaultIsAscending= */ false);
        assertThat(wrapper.isTimestampSet()).isFalse();
        assertThat(wrapper.isAscending()).isFalse();
    }

    @Test
    public void decode_invalidPageToken_throws() {
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> decode(-123, /* defaultIsAscending= */ true));
        assertThat(thrown.getMessage()).isEqualTo("pageToken cannot be negative");
    }

    private static PageTokenWrapper decode(long pageToken) {
        return decode(pageToken, /* defaultIsAscending= */ true);
    }

    private static PageTokenWrapper decode(long pageToken, boolean defaultIsAscending) {
        return PageTokenUtil.decode(pageToken, defaultIsAscending);
    }
}
