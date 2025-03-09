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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.HealthPermissions.getDataCategoriesWithWritePermissionsForPackage;
import static android.health.connect.HealthPermissions.getPackageHasWriteHealthPermissionsForCategory;

import static com.android.server.healthconnect.storage.request.UpsertTableRequest.TYPE_STRING;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.health.connect.HealthDataCategory;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.UserHandle;
import android.util.Pair;
import android.util.Slog;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Helper class to get priority of the apps for each {@link HealthDataCategory}
 *
 * @hide
 */
public class HealthDataCategoryPriorityHelper extends DatabaseHelper {
    public static final String PRIORITY_TABLE_NAME = "health_data_category_priority_table";
    public static final String HEALTH_DATA_CATEGORY_COLUMN_NAME = "health_data_category";
    public static final List<Pair<String, Integer>> UNIQUE_COLUMN_INFO =
            Collections.singletonList(new Pair<>(HEALTH_DATA_CATEGORY_COLUMN_NAME, TYPE_STRING));
    public static final String APP_ID_PRIORITY_ORDER_COLUMN_NAME = "app_id_priority_order";
    private static final String TAG = "HealthConnectPrioHelper";
    private static final String DEFAULT_APP_RESOURCE_NAME =
            "android:string/config_defaultHealthConnectApp";
    public static final String INACTIVE_APPS_ADDED = "inactive_apps_added";

    private HealthConnectContext mUserContext;
    private final AppInfoHelper mAppInfoHelper;
    private final PackageInfoUtils mPackageInfoUtils;
    private final TransactionManager mTransactionManager;
    private final PreferenceHelper mPreferenceHelper;
    private final HealthConnectMappings mHealthConnectMappings;
    private final HealthConnectThreadScheduler mThreadScheduler;

    /**
     * map of {@link HealthDataCategory} to list of app ids from {@link AppInfoHelper}, in the order
     * of their priority
     */
    @Nullable
    private volatile ConcurrentHashMap<Integer, List<Long>> mHealthDataCategoryToAppIdPriorityMap;

    public HealthDataCategoryPriorityHelper(
            HealthConnectContext userContext,
            AppInfoHelper appInfoHelper,
            TransactionManager transactionManager,
            PreferenceHelper preferenceHelper,
            PackageInfoUtils packageInfoUtils,
            HealthConnectMappings healthConnectMappings,
            DatabaseHelpers databaseHelpers,
            HealthConnectThreadScheduler threadScheduler) {
        super(databaseHelpers);
        mUserContext = userContext;
        mAppInfoHelper = appInfoHelper;
        mPackageInfoUtils = packageInfoUtils;
        mTransactionManager = transactionManager;
        mPreferenceHelper = preferenceHelper;
        mHealthConnectMappings = healthConnectMappings;
        mThreadScheduler = threadScheduler;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(PRIORITY_TABLE_NAME, getColumnInfo());
    }

    @Override
    public synchronized void clearCache() {
        mHealthDataCategoryToAppIdPriorityMap = null;
    }

    /** Setup HealthDataCategoryPriorityHelper for the given user. */
    public synchronized void setupForUser(HealthConnectContext userContext) {
        mUserContext = userContext;
        // While we already call clearCache() in HCManager.onUserSwitching(), calling this again
        // here in case any of the methods below was called in between that initialized the cache
        // with the wrong context.
        clearCache();
        mThreadScheduler.scheduleInternalTask(
                () -> {
                    reSyncHealthDataPriorityTable();
                    addInactiveAppsWhenFirstMigratingToNewAggregationControl();
                });
    }

    @Override
    protected String getMainTableName() {
        return PRIORITY_TABLE_NAME;
    }

    /** See appendToPriorityList below */
    public synchronized void appendToPriorityList(
            String packageName, @HealthDataCategory.Type int dataCategory, UserHandle user) {
        if (!mUserContext.getUser().equals(user)) {
            // We are currently limited to be able to update the priority list for the foreground
            // user only. User will need to manually add the app to the priority list later.
            return;
        }
        appendToPriorityList(packageName, dataCategory, /* isInactiveApp */ false);
    }

    /** See maybeRemoveAppFromPriorityList below */
    public synchronized void maybeRemoveAppFromPriorityList(
            String packageName, @HealthDataCategory.Type int dataCategory, UserHandle user) {
        if (!mUserContext.getUser().equals(user)) {
            // We are currently limited to be able to update the priority list for the foreground
            // user only. Apps will be removed from the priority list when the device switches to
            // this user  if they no longer have permissions.
            return;
        }
        maybeRemoveAppFromPriorityList(packageName, dataCategory);
    }

    /** See maybeRemoveAppFromPriorityList below */
    public synchronized void maybeRemoveAppFromPriorityList(String packageName, UserHandle user) {
        if (!mUserContext.getUser().equals(user)) {
            // We are currently limited to be able to update the priority list for the foreground
            // user only. Apps will be removed from the priority list when the device switches to
            // this user  if they no longer have permissions.
            return;
        }
        maybeRemoveAppFromPriorityList(packageName);
    }

    /**
     * Appends a packageName to the priority list for this category when an app gets write
     * permissions or during the one-time operation to add inactive apps.
     *
     * <p>Inactive apps are added at the bottom of the priority list even if they are the default
     * app.
     */
    public synchronized void appendToPriorityList(
            String packageName, @HealthDataCategory.Type int dataCategory, boolean isInactiveApp) {
        List<Long> newPriorityOrder;
        getHealthDataCategoryToAppIdPriorityMap().putIfAbsent(dataCategory, new ArrayList<>());
        long appInfoId = mAppInfoHelper.getOrInsertAppInfoId(packageName);
        if (getHealthDataCategoryToAppIdPriorityMap().get(dataCategory).contains(appInfoId)) {
            return;
        }
        newPriorityOrder =
                new ArrayList<>(getHealthDataCategoryToAppIdPriorityMap().get(dataCategory));

        if (isDefaultApp(packageName) && !isInactiveApp) {
            newPriorityOrder.add(0, appInfoId);
        } else {
            newPriorityOrder.add(appInfoId);
        }
        safelyUpdateDBAndUpdateCache(
                new UpsertTableRequest(
                        PRIORITY_TABLE_NAME,
                        getContentValuesFor(dataCategory, newPriorityOrder),
                        UNIQUE_COLUMN_INFO),
                dataCategory,
                newPriorityOrder);
    }

    @VisibleForTesting
    boolean isDefaultApp(String packageName) {
        String defaultApp =
                mUserContext
                        .getResources()
                        .getString(
                                Resources.getSystem()
                                        .getIdentifier(DEFAULT_APP_RESOURCE_NAME, null, null));

        return Objects.equals(packageName, defaultApp);
    }

    /**
     * Removes a packageName from the priority list of a particular category if the package name
     * does not have any granted write permissions and has no data.
     */
    public synchronized void maybeRemoveAppFromPriorityList(
            String packageName, @HealthDataCategory.Type int dataCategory) {
        PackageInfo packageInfo =
                mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(
                        packageName, mUserContext.getUser(), mUserContext);

        // If package is not found, assume no permissions are granted.
        if (packageInfo == null
                || !getPackageHasWriteHealthPermissionsForCategory(
                        packageInfo, dataCategory, mUserContext)) {
            removeAppFromPriorityListIfNoDataExists(dataCategory, packageName);
        }
    }

    /**
     * Removes a packageName from the priority list of all categories if the package name does not
     * have any granted write permissions and has no data.
     */
    public synchronized void maybeRemoveAppFromPriorityList(String packageName) {
        for (Integer dataCategory : getHealthDataCategoryToAppIdPriorityMap().keySet()) {
            maybeRemoveAppFromPriorityList(packageName, dataCategory);
        }
    }

    /**
     * Removes a packageName from the priority list of a particular category if the package name has
     * no data.
     *
     * <p>Assumes that the app has no write permission.
     */
    public synchronized void maybeRemoveAppWithoutWritePermissionsFromPriorityList(
            String packageName) {
        for (Integer dataCategory : getHealthDataCategoryToAppIdPriorityMap().keySet()) {
            removeAppFromPriorityListIfNoDataExists(dataCategory, packageName);
        }
    }

    /**
     * Refreshes the priority list and returns the list of package names based on priority for the
     * input {@link HealthDataCategory}
     */
    public List<String> syncAndGetPriorityOrder(@HealthDataCategory.Type int type) {
        reSyncHealthDataPriorityTable();
        return mAppInfoHelper.getPackageNames(getAppIdPriorityOrder(type));
    }

    /** Returns list of App ids based on priority for the input {@link HealthDataCategory} */
    public List<Long> getAppIdPriorityOrder(@HealthDataCategory.Type int type) {
        List<Long> packageIds = getHealthDataCategoryToAppIdPriorityMap().get(type);
        if (packageIds == null) {
            return Collections.emptyList();
        }

        return packageIds;
    }

    /**
     * Sets a new priority order for the given category, and allows adding and removing packages
     * from the priority list.
     *
     * <p>In the old behaviour it is not allowed to add or remove packages so the new priority order
     * needs to be sanitised before applying the operation.
     */
    public void setPriorityOrder(int dataCategory, List<String> packagePriorityOrder) {
        List<Long> newPriorityOrder = mAppInfoHelper.getAppInfoIds(packagePriorityOrder);
        safelyUpdateDBAndUpdateCache(
                new UpsertTableRequest(
                        PRIORITY_TABLE_NAME,
                        getContentValuesFor(dataCategory, newPriorityOrder),
                        UNIQUE_COLUMN_INFO),
                dataCategory,
                newPriorityOrder);
    }

    private Map<Integer, List<Long>> getHealthDataCategoryToAppIdPriorityMap() {
        if (mHealthDataCategoryToAppIdPriorityMap == null) {
            populateDataCategoryToAppIdPriorityMap();
        }

        return Objects.requireNonNull(mHealthDataCategoryToAppIdPriorityMap);
    }

    /** Returns an immutable map of data categories along with their priority order. */
    public Map<Integer, List<Long>> getHealthDataCategoryToAppIdPriorityMapImmutable() {
        return Collections.unmodifiableMap(getHealthDataCategoryToAppIdPriorityMap());
    }

    private synchronized void populateDataCategoryToAppIdPriorityMap() {
        if (mHealthDataCategoryToAppIdPriorityMap != null) {
            return;
        }

        ConcurrentHashMap<Integer, List<Long>> healthDataCategoryToAppIdPriorityMap =
                new ConcurrentHashMap<>();
        try (Cursor cursor = mTransactionManager.read(new ReadTableRequest(PRIORITY_TABLE_NAME))) {
            while (cursor.moveToNext()) {
                int dataCategory =
                        cursor.getInt(cursor.getColumnIndex(HEALTH_DATA_CATEGORY_COLUMN_NAME));
                List<Long> appIdsInOrder =
                        StorageUtils.getCursorLongList(
                                cursor, APP_ID_PRIORITY_ORDER_COLUMN_NAME, DELIMITER);

                healthDataCategoryToAppIdPriorityMap.put(dataCategory, appIdsInOrder);
            }
        }

        mHealthDataCategoryToAppIdPriorityMap = healthDataCategoryToAppIdPriorityMap;
    }

    private synchronized void safelyUpdateDBAndUpdateCache(
            UpsertTableRequest request,
            @HealthDataCategory.Type int dataCategory,
            List<Long> newList) {
        try {
            mTransactionManager.insertOrReplaceOnConflict(request);
            getHealthDataCategoryToAppIdPriorityMap().put(dataCategory, newList);
        } catch (Exception e) {
            Slog.e(TAG, "Priority update failed", e);
            throw e;
        }
    }

    private synchronized void safelyUpdateDBAndUpdateCache(
            DeleteTableRequest request, @HealthDataCategory.Type int dataCategory) {
        try {
            mTransactionManager.delete(request);
            getHealthDataCategoryToAppIdPriorityMap().remove(dataCategory);
        } catch (Exception e) {
            Slog.e(TAG, "Delete from priority DB failed: ", e);
            throw e;
        }
    }

    private ContentValues getContentValuesFor(
            @HealthDataCategory.Type int dataCategory, List<Long> priorityList) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(HEALTH_DATA_CATEGORY_COLUMN_NAME, dataCategory);
        contentValues.put(
                APP_ID_PRIORITY_ORDER_COLUMN_NAME, StorageUtils.flattenLongList(priorityList));

        return contentValues;
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    private static List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(HEALTH_DATA_CATEGORY_COLUMN_NAME, INTEGER_UNIQUE));
        columnInfo.add(new Pair<>(APP_ID_PRIORITY_ORDER_COLUMN_NAME, TEXT_NOT_NULL));

        return columnInfo;
    }

    /** Syncs priority table with the permissions and data. */
    public synchronized void reSyncHealthDataPriorityTable() {
        // Candidates to be removed from the priority list
        Map<Integer, Set<Long>> dataCategoryToAppIdMapWithoutPermission =
                getHealthDataCategoryToAppIdPriorityMap().entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey, e -> new HashSet<>(e.getValue())));

        List<PackageInfo> validHealthApps = getValidHealthApps();
        for (PackageInfo packageInfo : validHealthApps) {
            Set<Integer> dataCategoriesWithWritePermissionsForThisPackage =
                    getDataCategoriesWithWritePermissionsForPackage(packageInfo, mUserContext);
            long appInfoId = mAppInfoHelper.getOrInsertAppInfoId(packageInfo.packageName);

            for (int dataCategory : dataCategoriesWithWritePermissionsForThisPackage) {
                Set<Long> appIdsWithoutPermission =
                        dataCategoryToAppIdMapWithoutPermission.getOrDefault(
                                dataCategory, new HashSet<>());
                if (appIdsWithoutPermission.remove(appInfoId)) {
                    dataCategoryToAppIdMapWithoutPermission.put(
                            dataCategory, appIdsWithoutPermission);
                }
            }
        }

        // Remove any apps without any permission for the category, if they have no data present.
        for (Map.Entry<Integer, Set<Long>> entry :
                dataCategoryToAppIdMapWithoutPermission.entrySet()) {
            for (Long appInfoId : entry.getValue()) {
                try {
                    removeAppFromPriorityListIfNoDataExists(
                            entry.getKey(), mAppInfoHelper.getPackageName(appInfoId));
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "Package name not found while syncing priority table", e);
                }
            }
        }
        addContributingAppsIfCategoryListIsEmpty();
    }

    /** Returns a list of PackageInfos holding health permissions for this user. */
    private List<PackageInfo> getValidHealthApps() {
        return mPackageInfoUtils.getPackagesHoldingHealthPermissions(
                mUserContext.getUser(), mUserContext);
    }

    /**
     * Removes a packageName from the priority list of a category. The package name is not removed
     * if it has data in that category.
     */
    private synchronized void removeAppFromPriorityListIfNoDataExists(
            @HealthDataCategory.Type int dataCategory, String packageName) {
        boolean dataExistsForPackageName = appHasDataInCategory(packageName, dataCategory);
        if (dataExistsForPackageName) {
            return;
        }

        List<Long> newPriorityList =
                new ArrayList<>(
                        getHealthDataCategoryToAppIdPriorityMap()
                                .getOrDefault(dataCategory, Collections.emptyList()));
        if (newPriorityList.isEmpty()) {
            return;
        }

        newPriorityList.remove(mAppInfoHelper.getAppInfoId(packageName));
        if (newPriorityList.isEmpty()) {
            safelyUpdateDBAndUpdateCache(
                    new DeleteTableRequest(PRIORITY_TABLE_NAME)
                            .setId(HEALTH_DATA_CATEGORY_COLUMN_NAME, String.valueOf(dataCategory)),
                    dataCategory);
            return;
        }

        safelyUpdateDBAndUpdateCache(
                new UpsertTableRequest(
                        PRIORITY_TABLE_NAME,
                        getContentValuesFor(dataCategory, newPriorityList),
                        UNIQUE_COLUMN_INFO),
                dataCategory,
                newPriorityList);
    }

    /**
     * If the priority list is empty for a {@link HealthDataCategory}, add the contributing apps.
     *
     * <p>This is necessary because the priority list should never be empty if there are
     * contributing apps present.
     */
    private synchronized void addContributingAppsIfCategoryListIsEmpty() {
        mHealthConnectMappings
                .getAllHealthDataCategories()
                .forEach(
                        (category) ->
                                getHealthDataCategoryToAppIdPriorityMap()
                                        .putIfAbsent(category, new ArrayList<>()));
        Map<Integer, List<Long>> healthDataCategoryToAppIdPriorityMap =
                getHealthDataCategoryToAppIdPriorityMap();
        for (int dataCategory : healthDataCategoryToAppIdPriorityMap.keySet()) {
            List<Long> appIdsInPriorityOrder =
                    healthDataCategoryToAppIdPriorityMap.getOrDefault(dataCategory, List.of());
            if (appIdsInPriorityOrder.isEmpty()) {
                getAllContributorApps().getOrDefault(dataCategory, new HashSet<>()).stream()
                        .sorted()
                        .forEach(
                                (contributingApp) ->
                                        appendToPriorityList(
                                                contributingApp,
                                                dataCategory,
                                                isInactiveApp(dataCategory, contributingApp)));
            }
        }
    }

    /**
     * A one-time operation which adds contributing inactive apps to the priority list.
     *
     * <p>The contributing apps are added in ascending order of their package names.
     */
    public void addInactiveAppsWhenFirstMigratingToNewAggregationControl() {
        if (!shouldAddInactiveApps()) {
            return;
        }

        Map<Integer, Set<String>> inactiveApps = getAllInactiveApps();

        for (Map.Entry<Integer, Set<String>> entry : inactiveApps.entrySet()) {
            int category = entry.getKey();
            entry.getValue().stream()
                    .sorted()
                    .forEach(
                            packageName ->
                                    appendToPriorityList(
                                            packageName, category, /* isInactiveApp= */ true));
        }

        mPreferenceHelper.insertOrReplacePreference(INACTIVE_APPS_ADDED, String.valueOf(true));
    }

    private boolean isInactiveApp(@HealthDataCategory.Type int dataCategory, String packageName) {
        Map<Integer, Set<String>> inactiveApps = getAllInactiveApps();
        return inactiveApps.getOrDefault(dataCategory, new HashSet<>()).contains(packageName);
    }

    private boolean shouldAddInactiveApps() {
        String haveInactiveAppsBeenAddedString =
                mPreferenceHelper.getPreference(INACTIVE_APPS_ADDED);

        return haveInactiveAppsBeenAddedString == null
                || !Boolean.parseBoolean(haveInactiveAppsBeenAddedString);
    }

    @VisibleForTesting
    boolean appHasDataInCategory(String packageName, int category) {
        return getDataCategoriesWithDataForPackage(packageName).contains(category);
    }

    @VisibleForTesting
    Set<Integer> getDataCategoriesWithDataForPackage(String packageName) {
        Map<Integer, Set<String>> recordTypeToContributingPackages =
                mAppInfoHelper.getRecordTypesToContributingPackagesMap();
        Set<Integer> dataCategoriesWithData = new HashSet<>();

        for (Map.Entry<Integer, Set<String>> entry : recordTypeToContributingPackages.entrySet()) {
            Integer recordType = entry.getKey();
            Set<String> contributingPackages = entry.getValue();
            int recordCategory = mHealthConnectMappings.getRecordCategoryForRecordType(recordType);
            boolean isPackageNameContributor = contributingPackages.contains(packageName);
            if (isPackageNameContributor) {
                dataCategoriesWithData.add(recordCategory);
            }
        }
        return dataCategoriesWithData;
    }

    /**
     * Returns a set of contributing apps for each dataCategory. If a dataCategory does not have any
     * data it will not be present in the map.
     */
    @VisibleForTesting
    Map<Integer, Set<String>> getAllContributorApps() {
        Map<Integer, Set<String>> recordTypeToContributingPackages =
                mAppInfoHelper.getRecordTypesToContributingPackagesMap();

        Map<Integer, Set<String>> allContributorApps = new HashMap<>();

        for (Map.Entry<Integer, Set<String>> entry : recordTypeToContributingPackages.entrySet()) {
            int recordCategory =
                    mHealthConnectMappings.getRecordCategoryForRecordType(entry.getKey());
            Set<String> contributingPackages = entry.getValue();

            Set<String> currentPackages =
                    allContributorApps.getOrDefault(recordCategory, new HashSet<>());
            currentPackages.addAll(contributingPackages);
            allContributorApps.put(recordCategory, currentPackages);
        }

        return allContributorApps;
    }

    /**
     * Returns a map of dataCategory to sets of packageNames that are inactive.
     *
     * <p>An inactive app is one that has data for the dataCategory but no write permissions.
     */
    @VisibleForTesting
    Map<Integer, Set<String>> getAllInactiveApps() {
        Map<Integer, Set<String>> allContributorApps = getAllContributorApps();
        Map<Integer, Set<String>> inactiveApps = new HashMap<>();

        for (Map.Entry<Integer, Set<String>> entry : allContributorApps.entrySet()) {
            int category = entry.getKey();
            Set<String> contributorApps = entry.getValue();

            for (String packageName : contributorApps) {
                PackageInfo packageInfo =
                        mPackageInfoUtils.getPackageInfoWithPermissionsAsUser(
                                packageName, mUserContext.getUser(), mUserContext);
                if (packageInfo == null
                        || !getPackageHasWriteHealthPermissionsForCategory(
                                packageInfo, category, mUserContext)) {
                    Set<String> currentPackages =
                            inactiveApps.getOrDefault(category, new HashSet<>());
                    if (currentPackages.add(packageName)) {
                        inactiveApps.put(category, currentPackages);
                    }
                }
            }
        }

        return inactiveApps;
    }
}
