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

import static com.android.internal.util.Preconditions.checkArgument;

/**
 * A util class handles encoding and decoding page token.
 *
 * @hide
 */
public final class PageTokenUtil {
    static final long MAX_ALLOWED_TIME_MILLIS = (1L << 44) - 1;
    static final long MAX_ALLOWED_OFFSET = (1 << 18) - 1;

    private static final int OFFSET_START_BIT = 45;
    private static final int TIMESTAMP_START_BIT = 1;

    /**
     * Encodes a {@link PageTokenWrapper} to page token.
     *
     * <p>Page token is structured as following from right (least significant bit) to left (most
     * significant bit):
     * <li>Least significant bit: 0 = isAscending true, 1 = isAscending false
     * <li>Next 44 bits: timestamp, represents epoch time millis
     * <li>Next 18 bits: offset, represents number of records processed in the previous page
     * <li>Sign bit: not used for encoding, page token is a signed long
     */
    public static long encode(PageTokenWrapper wrapper) {
        return ((long) wrapper.offset() << OFFSET_START_BIT)
                | (wrapper.timeMillis() << TIMESTAMP_START_BIT)
                | (wrapper.isAscending() ? 0 : 1);
    }

    /**
     * Decodes a {@code pageToken} to {@link PageTokenWrapper}.
     *
     * <p>When {@code pageToken} is not set, in which case we can not get {@code isAscending} from
     * the token, it falls back to {@code defaultIsAscending}.
     *
     * <p>{@code pageToken} must be a non-negative long number (except for using the sentinel value
     * {@code DEFAULT_LONG}, whose current value is {@code -1}, which represents page token not set)
     */
    public static PageTokenWrapper decode(long pageToken, boolean defaultIsAscending) {
        if (pageToken == DEFAULT_LONG) {
            return PageTokenWrapper.of(defaultIsAscending);
        }
        checkArgument(pageToken >= 0, "pageToken cannot be negative");
        return PageTokenWrapper.of(
                getIsAscending(pageToken), getTimestamp(pageToken), getOffset(pageToken));
    }

    /**
     * Take the least significant bit in the given {@code pageToken} to retrieve isAscending
     * information.
     *
     * <p>If the last bit of the token is 1, isAscending is false; otherwise isAscending is true.
     */
    private static boolean getIsAscending(long pageToken) {
        return (pageToken & 1) == 0;
    }

    /** Shifts bits in the given {@code pageToken} to retrieve timestamp information. */
    private static long getTimestamp(long pageToken) {
        long mask = MAX_ALLOWED_TIME_MILLIS << TIMESTAMP_START_BIT;
        return (pageToken & mask) >> TIMESTAMP_START_BIT;
    }

    /** Shifts bits in the given {@code pageToken} to retrieve offset information. */
    private static int getOffset(long pageToken) {
        return (int) (pageToken >> OFFSET_START_BIT);
    }

    private PageTokenUtil() {}
}
