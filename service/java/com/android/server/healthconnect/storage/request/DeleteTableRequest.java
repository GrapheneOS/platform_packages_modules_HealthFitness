/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.storage.request;

import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.annotation.Nullable;
import android.health.connect.Constants;
import android.util.Slog;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request object used in @{@link com.android.server.healthconnect.storage.TransactionManager} to
 * deleted entries from a table.
 *
 * <p>There is no need to have delete-requests for child tables they should be following cascaded
 * deletes. If not please rethink the table structure and if possible remove the parent-child
 * relationship.
 *
 * @hide
 */
public class DeleteTableRequest {

    private static final String TAG = "HealthConnectDelete";
    private final String mTableName;

    @Nullable private String mIdColumnName;
    @Nullable private String mPackageColumnName;
    @Nullable private String mTimeColumnName;
    @Nullable private String mDeviceDataProviderIdColumnName;
    private long mDeviceDataProviderId = DEFAULT_LONG;
    @Nullable private List<Long> mPackageFilters;
    private long mStartTime = DEFAULT_LONG;
    private long mEndTime = DEFAULT_LONG;
    @Nullable private List<String> mIds;
    private final WhereClauses mExtraWhereClauses = new WhereClauses(AND);

    public DeleteTableRequest(String tableName) {
        mTableName = tableName;
    }

    public String getTableName() {
        return mTableName;
    }

    public DeleteTableRequest setIdColumnName(String idColumnName) {
        mIdColumnName = idColumnName;
        return this;
    }

    public DeleteTableRequest setIds(String idColumnName, List<String> ids) {
        mIds = ids.stream().map(StorageUtils::getNormalisedString).toList();
        mIdColumnName = idColumnName;
        return this;
    }

    public DeleteTableRequest setId(String idColumnName, String id) {
        mIds = Collections.singletonList(StorageUtils.getNormalisedString(id));
        mIdColumnName = idColumnName;
        return this;
    }

    @Nullable
    public String getIdColumnName() {
        return mIdColumnName;
    }

    public DeleteTableRequest setPackageColumnName(String packageColumnName) {
        mPackageColumnName = packageColumnName;
        return this;
    }

    public DeleteTableRequest setPackageFilter(
            String packageColumnName, List<Long> packageFilters) {
        mPackageFilters = packageFilters;
        mPackageColumnName = packageColumnName;
        return this;
    }

    @Nullable
    public String getPackageColumnName() {
        return mPackageColumnName;
    }

    @Nullable
    public String getDeviceDataProviderIdColumnName() {
        return mDeviceDataProviderIdColumnName;
    }

    /**
     * Set the device data provider ID associated with the delete request. If {@code DEFAULT_LONG},
     * the ddp ID will be ignored in the request.
     */
    public DeleteTableRequest setDeviceDataProviderId(
            String deviceDataProviderIdColumnName, long deviceDataProviderId) {
        mDeviceDataProviderIdColumnName = deviceDataProviderIdColumnName;
        mDeviceDataProviderId = deviceDataProviderId;
        return this;
    }

    /** Adds an extra {@link WhereClauses} that filters the rows to be deleted. */
    public DeleteTableRequest addExtraWhereClauses(WhereClauses whereClauses) {
        mExtraWhereClauses.addNestedWhereClauses(whereClauses);
        return this;
    }

    public String getDeleteCommand() {
        return "DELETE FROM " + mTableName + getWhereCommand();
    }

    public String getWhereCommand() {
        WhereClauses whereClauses = new WhereClauses(AND);
        whereClauses.addNestedWhereClauses(mExtraWhereClauses);
        whereClauses.addWhereInLongsClause(mPackageColumnName, mPackageFilters);
        whereClauses.addWhereBetweenTimeClause(mTimeColumnName, mStartTime, mEndTime);
        whereClauses.addWhereInClauseWithoutQuotes(mIdColumnName, mIds);

        // SQLite starts ids at 1 (see https://sqlite.org/autoinc.html), any other value means the
        // ddp ID has not been set and should be ignored
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled() && mDeviceDataProviderId > 0) {
            Objects.requireNonNull(mDeviceDataProviderIdColumnName);
            whereClauses.addWhereEqualsClause(
                    mDeviceDataProviderIdColumnName, String.valueOf(mDeviceDataProviderId));
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "delete query: tableName: "
                            + mTableName
                            + " whereClause: "
                            + whereClauses.get(true));
        }

        return whereClauses.get(true);
    }

    public DeleteTableRequest setTimeFilter(String timeColumnName, long startTime, long endTime) {
        // Return if the params will result in no impact on the query
        if (startTime < 0 || endTime < startTime) {
            return this;
        }

        mStartTime = startTime;
        mEndTime = endTime;
        mTimeColumnName = timeColumnName;

        return this;
    }
}
