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

package android.health.connect;

import static org.hamcrest.CoreMatchers.containsString;

import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.ratelimiter.RateLimiter.QuotaCategory;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;

@RunWith(AndroidJUnit4.class)
public class RateLimiterTest {
    private static final int UID = 1;
    private static final boolean IS_IN_FOREGROUND_TRUE = true;
    private static final boolean IS_IN_FOREGROUND_FALSE = false;
    private static final int MAX_FOREGROUND_READ_CALL_15M = 2000;
    private static final int MAX_BACKGROUND_CALL_15M = 1000;
    private static final Duration WINDOW_15M = Duration.ofMinutes(15);
    private static final int MEMORY_COST = 20000;

    @Rule public ExpectedException exception = ExpectedException.none();

    private final RateLimiter mRateLimiter = new RateLimiter();

    @Test
    public void testTryAcquireApiCallQuota_invalidQuotaCategory() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 0;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Quota category not defined.");
        mRateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, IS_IN_FOREGROUND_TRUE);
    }

    @Test
    public void testTryAcquireApiCallQuota_unmeteredForegroundCalls() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 1;
        tryAcquireCallQuotaNTimes(
                quotaCategory, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_READ_CALL_15M + 1);
    }

    @Test
    public void testTryAcquireApiCallQuota_unmeteredBackgroundCalls() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 1;
        tryAcquireCallQuotaNTimes(
                quotaCategory, IS_IN_FOREGROUND_TRUE, MAX_BACKGROUND_CALL_15M + 1);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredForegroundCallsInLimit() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryRead = 2;
        tryAcquireCallQuotaNTimes(
                quotaCategoryRead, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_READ_CALL_15M);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredBackgroundCallsInLimit() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryWrite = 3;
        tryAcquireCallQuotaNTimes(
                quotaCategoryWrite, IS_IN_FOREGROUND_FALSE, MAX_BACKGROUND_CALL_15M);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredForegroundCallsLimitExceeded() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryRead = 2;
        Instant startTime = Instant.now();
        tryAcquireCallQuotaNTimes(
                quotaCategoryRead, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_READ_CALL_15M);
        Instant endTime = Instant.now();
        int ceilQuotaAcquired =
                getCeilQuotaAcquired(startTime, endTime, WINDOW_15M, MAX_FOREGROUND_READ_CALL_15M);
        exception.expect(HealthConnectException.class);
        exception.expectMessage(containsString("API call quota exceeded"));
        tryAcquireCallQuotaNTimes(quotaCategoryRead, IS_IN_FOREGROUND_TRUE, ceilQuotaAcquired);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredBackgroundCallsLimitExceeded() {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryWrite = 3;
        Instant startTime = Instant.now();
        tryAcquireCallQuotaNTimes(
                quotaCategoryWrite, IS_IN_FOREGROUND_FALSE, MAX_BACKGROUND_CALL_15M);
        Instant endTime = Instant.now();
        int ceilQuotaAcquired =
                getCeilQuotaAcquired(startTime, endTime, WINDOW_15M, MAX_BACKGROUND_CALL_15M);
        exception.expect(HealthConnectException.class);
        exception.expectMessage(containsString("API call quota exceeded"));
        tryAcquireCallQuotaNTimes(quotaCategoryWrite, IS_IN_FOREGROUND_FALSE, ceilQuotaAcquired);
    }

    @Test
    public void testRecordMemoryRollingQuota_exceedBackgroundLimit() throws InterruptedException {
        mRateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryWrite = 3;
        exception.expect(HealthConnectException.class);
        exception.expectMessage(containsString("API call quota exceeded"));
        tryAcquireCallQuotaNTimes(
                quotaCategoryWrite, IS_IN_FOREGROUND_FALSE, MAX_BACKGROUND_CALL_15M, 40000);
    }

    @Test
    public void checkMaxChunkMemoryUsage_LimitExceeded() {
        long valueExceeding = 5000001;
        exception.expect(HealthConnectException.class);
        exception.expectMessage(
                "Records chunk size exceeded the max chunk limit: 5000000, was: 5000001");
        mRateLimiter.checkMaxChunkMemoryUsage(valueExceeding);
    }

    @Test
    public void checkMaxChunkMemoryUsage_inLimit() {
        long value = 5000000;
        mRateLimiter.checkMaxChunkMemoryUsage(value);
    }

    @Test
    public void checkMaxRecordMemoryUsage_LimitExceeded() {
        long valueExceeding = 1000001;
        exception.expect(HealthConnectException.class);
        exception.expectMessage(
                "Record size exceeded the single record size limit: 1000000, was: 1000001");
        mRateLimiter.checkMaxRecordMemoryUsage(valueExceeding);
    }

    @Test
    public void checkMaxRecordMemoryUsage_inLimit() {
        long value = 1000000;
        mRateLimiter.checkMaxRecordMemoryUsage(value);
    }

    private int getCeilQuotaAcquired(
            Instant startTime, Instant endTime, Duration window, int maxQuota) {
        Duration timeSpent = Duration.between(startTime, endTime);
        float accumulated = timeSpent.toMillis() * ((float) maxQuota / (float) window.toMillis());
        return accumulated > (int) accumulated
                ? (int) Math.ceil(accumulated)
                : (int) accumulated + 1;
    }

    private void tryAcquireCallQuotaNTimes(
            @QuotaCategory.Type int quotaCategory, boolean isInForeground, int nTimes) {

        if (quotaCategory == QuotaCategory.QUOTA_CATEGORY_WRITE) {
            for (int i = 0; i < nTimes; i++) {
                mRateLimiter.tryAcquireApiCallQuota(
                        UID, quotaCategory, isInForeground, MEMORY_COST);
            }
        } else {
            for (int i = 0; i < nTimes; i++) {
                mRateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, isInForeground);
            }
        }
    }

    private void tryAcquireCallQuotaNTimes(
            @QuotaCategory.Type int quotaCategory,
            boolean isInForeground,
            int nTimes,
            int memoryCost) {
        for (int i = 0; i < nTimes; i++) {
            mRateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, isInForeground, memoryCost);
        }
    }
}
