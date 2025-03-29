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

package com.android.server.healthconnect.fitness.aggregation;

import java.util.Collections;
import java.util.List;

/** Helper class to generate aggregated data for unit tests. */
public class PriorityAggregationTestDataFactory {
    /** Create session aggregated data. */
    public static AggregationRecordData createSessionData(
            int startTime,
            int endTime,
            int priority,
            List<Long> excludeStarts,
            List<Long> excludeEnds) {
        return createSessionData(startTime, endTime, priority, excludeStarts, excludeEnds, 0);
    }

    /** Create session aggregated data with a last modified time. */
    public static AggregationRecordData createSessionData(
            int startTime,
            int endTime,
            int priority,
            List<Long> excludeStarts,
            List<Long> excludeEnds,
            int lastModifiedTime) {
        return new SessionDurationAggregationData(
                startTime, endTime, priority, lastModifiedTime, excludeStarts, excludeEnds);
    }

    /** Create session aggregated data with no excluded intervals. */
    public static AggregationRecordData createSessionData(
            int startTime, int endTime, int priority) {
        return createSessionData(
                startTime, endTime, priority, Collections.emptyList(), Collections.emptyList());
    }

    /** Create aggregated steps data from integers. */
    public static AggregationRecordData createStepsData(
            int startTime, int endTime, int count, int priority, int lastModifiedTime) {
        return new ValueColumnAggregationData(
                startTime, endTime, priority, lastModifiedTime, count);
    }

    /** Create aggregated steps data from longs. */
    public static AggregationRecordData createStepsData(
            long startTime, long endTime, int count, int priority, int lastModifiedTime) {
        return new ValueColumnAggregationData(
                startTime, endTime, priority, lastModifiedTime, count);
    }
}
