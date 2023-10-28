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

import static com.android.internal.util.Preconditions.checkArgument;
import static com.android.server.healthconnect.storage.utils.PageTokenUtil.MAX_ALLOWED_OFFSET;
import static com.android.server.healthconnect.storage.utils.PageTokenUtil.MAX_ALLOWED_TIME_MILLIS;

import static java.lang.Integer.min;

import java.util.Objects;

/**
 * A wrapper object contains information encoded in the {@code long} page token.
 *
 * @hide
 */
public final class PageTokenWrapper {
    private final boolean mIsAscending;
    private final long mTimeMillis;
    private final int mOffset;
    private final boolean mIsTimestampSet;

    /** isAscending stored in the page token. */
    public boolean isAscending() {
        return mIsAscending;
    }

    /** Timestamp stored in the page token. */
    public long timeMillis() {
        return mTimeMillis;
    }

    /** Offset stored in the page token. */
    public int offset() {
        return mOffset;
    }

    /** Whether or not the timestamp is set. */
    public boolean isTimestampSet() {
        return mIsTimestampSet;
    }

    /**
     * Both {@code timeMillis} and {@code offset} have to be non-negative; {@code timeMillis} cannot
     * exceed 2^44-1.
     *
     * <p>Note that due to space constraints, {@code offset} cannot exceed 2^18-1 (262143). If the
     * {@code offset} parameter exceeds the maximum allowed value, it'll fallback to the max value.
     *
     * <p>More details see go/hc-page-token
     */
    public static PageTokenWrapper of(boolean isAscending, long timeMillis, int offset) {
        checkArgument(timeMillis >= 0, "timestamp can not be negative");
        checkArgument(timeMillis <= MAX_ALLOWED_TIME_MILLIS, "timestamp too large");
        checkArgument(offset >= 0, "offset can not be negative");
        int boundedOffset = min((int) MAX_ALLOWED_OFFSET, offset);
        return new PageTokenWrapper(
                isAscending, timeMillis, boundedOffset, /* isTimestampSet= */ true);
    }

    /**
     * Generate a page token that contains only {@code isAscending} information. Timestamp and
     * offset are not set.
     */
    public static PageTokenWrapper ofAscending(boolean isAscending) {
        return new PageTokenWrapper(
                isAscending, /* timeMillis= */ 0, /* offset= */ 0, /* isTimestampSet= */ false);
    }

    @Override
    public String toString() {
        return "PageTokenWrapper{"
                + "isAscending = "
                + mIsAscending
                + ", timeMillis = "
                + mTimeMillis
                + ", offset = "
                + mOffset
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageTokenWrapper that)) return false;
        return mIsAscending == that.mIsAscending
                && mTimeMillis == that.mTimeMillis
                && mOffset == that.mOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsAscending, mOffset, mTimeMillis);
    }

    private PageTokenWrapper(
            boolean isAscending, long timeMillis, int offset, boolean isTimestampSet) {
        this.mIsAscending = isAscending;
        this.mTimeMillis = timeMillis;
        this.mOffset = offset;
        this.mIsTimestampSet = isTimestampSet;
    }
}
