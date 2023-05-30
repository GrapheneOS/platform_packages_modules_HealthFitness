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

package android.healthconnect;

import static android.health.connect.ratelimiter.RateLimiter.CHUNK_SIZE_LIMIT_IN_BYTES;
import static android.health.connect.ratelimiter.RateLimiter.RECORD_SIZE_LIMIT_IN_BYTES;

import static org.hamcrest.CoreMatchers.containsString;

import android.health.connect.HealthConnectException;
import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.ratelimiter.RateLimiter.QuotaCategory;

import com.android.server.healthconnect.HealthConnectDeviceConfigManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RateLimiterTest {
    private static final int UID = 1;
    private static final boolean IS_IN_FOREGROUND_TRUE = true;
    private static final boolean IS_IN_FOREGROUND_FALSE = false;
    private static final int MAX_FOREGROUND_CALL_15M = 1000;
    private static final int MAX_BACKGROUND_CALL_15M = 1000;
    private static final Duration WINDOW_15M = Duration.ofMinutes(15);
    private static final int MEMORY_COST = 20000;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        Map<Integer, Integer> quotaBucketToMaxRollingQuotaMap = new HashMap<>();
        Map<String, Integer> quotaBucketToMaxMemoryQuotaMap = new HashMap<>();
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_15M_FOREGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_15M_BACKGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_FOREGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_BACKGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_24H_FOREGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_24H_BACKGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_FOREGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_BACKGROUND,
                HealthConnectDeviceConfigManager
                        .QUOTA_BUCKET_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_DATA_PUSH_LIMIT_PER_APP_15M,
                HealthConnectDeviceConfigManager.DATA_PUSH_LIMIT_PER_APP_15M_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxRollingQuotaMap.put(
                RateLimiter.QuotaBucket.QUOTA_BUCKET_DATA_PUSH_LIMIT_ACROSS_APPS_15M,
                HealthConnectDeviceConfigManager
                        .DATA_PUSH_LIMIT_ACROSS_APPS_15M_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxMemoryQuotaMap.put(
                CHUNK_SIZE_LIMIT_IN_BYTES,
                HealthConnectDeviceConfigManager.CHUNK_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE);
        quotaBucketToMaxMemoryQuotaMap.put(
                RECORD_SIZE_LIMIT_IN_BYTES,
                HealthConnectDeviceConfigManager.RECORD_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE);
        RateLimiter.updateMaxRollingQuotaMap(quotaBucketToMaxRollingQuotaMap);
        RateLimiter.updateMemoryQuotaMap(quotaBucketToMaxMemoryQuotaMap);
        RateLimiter.updateEnableRateLimiterFlag(true);
    }

    @Test
    public void testTryAcquireApiCallQuota_invalidQuotaCategory() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 0;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Quota category not defined.");
        RateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, IS_IN_FOREGROUND_TRUE);
    }

    @Test
    public void testTryAcquireApiCallQuota_unmeteredForegroundCalls() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 1;
        tryAcquireCallQuotaNTimes(
                quotaCategory, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_CALL_15M + 1);
    }

    @Test
    public void testTryAcquireApiCallQuota_unmeteredBackgroundCalls() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategory = 1;
        tryAcquireCallQuotaNTimes(
                quotaCategory, IS_IN_FOREGROUND_TRUE, MAX_BACKGROUND_CALL_15M + 1);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredForegroundCallsInLimit() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryRead = 2;
        tryAcquireCallQuotaNTimes(
                quotaCategoryRead, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_CALL_15M);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredBackgroundCallsInLimit() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryWrite = 3;
        tryAcquireCallQuotaNTimes(
                quotaCategoryWrite, IS_IN_FOREGROUND_FALSE, MAX_BACKGROUND_CALL_15M);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredForegroundCallsLimitExceeded() {
        RateLimiter.clearCache();
        @QuotaCategory.Type int quotaCategoryRead = 2;
        Instant startTime = Instant.now();
        tryAcquireCallQuotaNTimes(
                quotaCategoryRead, IS_IN_FOREGROUND_TRUE, MAX_FOREGROUND_CALL_15M);
        Instant endTime = Instant.now();
        int ceilQuotaAcquired =
                getCeilQuotaAcquired(startTime, endTime, WINDOW_15M, MAX_FOREGROUND_CALL_15M);
        exception.expect(HealthConnectException.class);
        exception.expectMessage(containsString("API call quota exceeded"));
        tryAcquireCallQuotaNTimes(quotaCategoryRead, IS_IN_FOREGROUND_TRUE, ceilQuotaAcquired);
    }

    @Test
    public void testTryAcquireApiCallQuota_meteredBackgroundCallsLimitExceeded() {
        RateLimiter.clearCache();
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
        RateLimiter.clearCache();
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
        RateLimiter.checkMaxChunkMemoryUsage(valueExceeding);
    }

    @Test
    public void checkMaxChunkMemoryUsage_inLimit() {
        long value = 5000000;
        RateLimiter.checkMaxChunkMemoryUsage(value);
    }

    @Test
    public void checkMaxRecordMemoryUsage_LimitExceeded() {
        long valueExceeding = 1000001;
        exception.expect(HealthConnectException.class);
        exception.expectMessage(
                "Record size exceeded the single record size limit: 1000000, was: 1000001");
        RateLimiter.checkMaxRecordMemoryUsage(valueExceeding);
    }

    @Test
    public void checkMaxRecordMemoryUsage_inLimit() {
        long value = 1000000;
        RateLimiter.checkMaxRecordMemoryUsage(value);
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
                RateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, isInForeground, MEMORY_COST);
            }
        } else {
            for (int i = 0; i < nTimes; i++) {
                RateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, isInForeground);
            }
        }
    }

    private void tryAcquireCallQuotaNTimes(
            @QuotaCategory.Type int quotaCategory,
            boolean isInForeground,
            int nTimes,
            int memoryCost) {
        for (int i = 0; i < nTimes; i++) {
            RateLimiter.tryAcquireApiCallQuota(UID, quotaCategory, isInForeground, memoryCost);
        }
    }
}
