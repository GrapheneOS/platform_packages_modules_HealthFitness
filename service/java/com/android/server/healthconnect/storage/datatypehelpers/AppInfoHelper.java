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

import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorBlob;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.ApplicationInfoFlags;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.health.connect.Constants;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A class to help with the DB transaction for storing Application Info. {@link AppInfoHelper} acts
 * as a layer b/w the application_igenfo_table stored in the DB and helps perform insert and read
 * operations on the table
 *
 * @hide
 */
public final class AppInfoHelper {
    private static final String TABLE_NAME = "application_info_table";
    private static final String APPLICATION_COLUMN_NAME = "app_name";
    private static final String PACKAGE_COLUMN_NAME = "package_name";
    private static final String APP_ICON_COLUMN_NAME = "app_icon";
    private static final int COMPRESS_FACTOR = 100;
    private static volatile AppInfoHelper sAppInfoHelper;
    /**
     * Map to store appInfoId -> packageName mapping for populating record for read
     *
     * <p>TO HAVE THREAD SAFETY DON'T USE THESE VARIABLES DIRECTLY, INSTEAD USE ITS GETTER
     */
    private ConcurrentHashMap<Long, String> mIdPackageNameMap;
    /**
     * Map to store application package-name -> AppInfo mapping (such as packageName -> appName,
     * icon, rowId in the DB etc.)
     *
     * <p>TO HAVE THREAD SAFETY DON'T USE THESE VARIABLES DIRECTLY, INSTEAD USE ITS GETTER
     */
    private ConcurrentHashMap<String, AppInfoInternal> mAppInfoMap;

    private AppInfoHelper() {}

    public static synchronized AppInfoHelper getInstance() {
        if (sAppInfoHelper == null) {
            sAppInfoHelper = new AppInfoHelper();
        }

        return sAppInfoHelper;
    }

    @Nullable
    private static byte[] encodeBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_FACTOR, stream);
            return stream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    @Nullable
    private static Bitmap decodeBitmap(@Nullable byte[] bytes) {
        return bytes != null ? BitmapFactory.decodeByteArray(bytes, 0, bytes.length) : null;
    }

    @NonNull
    private static Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp =
                Bitmap.createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    /** Deletes all entries from the database and clears the cache. */
    public synchronized void clearData(TransactionManager transactionManager) {
        transactionManager.delete(new DeleteTableRequest(TABLE_NAME));
        clearCache();
    }

    public synchronized void clearCache() {
        mAppInfoMap = null;
        mIdPackageNameMap = null;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    /** Populates record with appInfoId */
    public void populateAppInfoId(
            @NonNull RecordInternal<?> record, @NonNull Context context, boolean requireAllFields) {
        final String packageName = requireNonNull(record.getPackageName());
        AppInfoInternal appInfo = getAppInfoMap().get(packageName);

        if (appInfo == null) {
            try {
                appInfo = getAppInfo(packageName, context);
            } catch (NameNotFoundException e) {
                if (requireAllFields) {
                    throw new IllegalArgumentException("Could not find package info", e);
                }

                appInfo = new AppInfoInternal(DEFAULT_LONG, packageName, record.getAppName(), null);
            }

            upsertAppInfo(packageName, appInfo);
        }

        record.setAppInfoId(appInfo.getId());
    }

    /**
     * Inserts or replaces the application info of the specified {@code packageName} with the
     * specified {@code name} and {@code icon}, only if the corresponding application is not
     * currently installed.
     */
    public void updateAppInfoIfNotInstalled(
            @NonNull Context context,
            @NonNull String packageName,
            @Nullable String name,
            @Nullable byte[] icon) {
        if (!isAppInstalled(context, packageName) && containsAppInfo(packageName)) {
            upsertAppInfo(
                    packageName,
                    new AppInfoInternal(DEFAULT_LONG, packageName, name, decodeBitmap(icon)));
        }
    }

    private boolean isAppInstalled(@NonNull Context context, @NonNull String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, ApplicationInfoFlags.of(0));
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Populates record with package name
     *
     * @param appInfoId rowId from {@code application_info_table }
     * @param record The record to be populated with package name
     */
    public void populateRecordWithValue(long appInfoId, @NonNull RecordInternal<?> record) {
        record.setPackageName(getIdPackageNameMap().get(appInfoId));
    }

    // Called on DB update.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /**
     * @return id of {@code packageName} or {@link Constants#DEFAULT_LONG} if the id is not found
     */
    public long getAppInfoId(String packageName) {
        AppInfoInternal appInfo = getAppInfoMap().getOrDefault(packageName, null);

        if (appInfo == null) {
            return DEFAULT_LONG;
        }

        return appInfo.getId();
    }

    private boolean containsAppInfo(String packageName) {
        return getAppInfoMap().containsKey(packageName);
    }

    /**
     * @param packageNames List of package names
     * @return A list of appinfo ids from the application_info_table.
     */
    public List<Long> getAppInfoIds(List<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(packageNames.size());
        packageNames.forEach(packageName -> result.add(getAppInfoId(packageName)));

        return result;
    }

    @NonNull
    public String getPackageName(long packageId) {
        return getIdPackageNameMap().get(packageId);
    }

    @NonNull
    public List<String> getPackageNames(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> packageNames = new ArrayList<>();
        packageIds.forEach(
                (packageId) -> {
                    String packageName = getIdPackageNameMap().get(packageId);
                    requireNonNull(packageName);

                    packageNames.add(packageName);
                });

        return packageNames;
    }

    /** Returns a list of AppInfo objects */
    public List<AppInfo> getApplicationInfos() {
        return getAppInfoMap().values().stream()
                .map(AppInfoInternal::toExternal)
                .collect(Collectors.toList());
    }

    /** Returns AppInfo id for the provided {@code packageName}, creating it if needed. */
    public long getOrInsertAppInfoId(@NonNull String packageName, @NonNull Context context) {
        AppInfoInternal appInfoInternal = getAppInfoMap().get(packageName);

        if (appInfoInternal == null) {
            try {
                appInfoInternal = getAppInfo(packageName, context);
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException("Could not find package info for package", e);
            }

            upsertAppInfo(packageName, appInfoInternal);
        }

        return appInfoInternal.getId();
    }

    private synchronized void populateAppInfoMap() {
        if (mAppInfoMap != null) {
            return;
        }

        ConcurrentHashMap<String, AppInfoInternal> appInfoMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, String> idPackageNameMap = new ConcurrentHashMap<>();
        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        final SQLiteDatabase db = transactionManager.getReadableDb();
        try (Cursor cursor = transactionManager.read(db, new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId = getCursorLong(cursor, RecordHelper.PRIMARY_COLUMN_NAME);
                String packageName = getCursorString(cursor, PACKAGE_COLUMN_NAME);
                String appName = getCursorString(cursor, APPLICATION_COLUMN_NAME);
                byte[] icon = getCursorBlob(cursor, APP_ICON_COLUMN_NAME);
                Bitmap bitmap = decodeBitmap(icon);
                appInfoMap.put(
                        packageName, new AppInfoInternal(rowId, packageName, appName, bitmap));
                idPackageNameMap.put(rowId, packageName);
            }
        }
        mAppInfoMap = appInfoMap;
        mIdPackageNameMap = idPackageNameMap;
    }

    private Map<String, AppInfoInternal> getAppInfoMap() {
        if (Objects.isNull(mAppInfoMap)) {
            populateAppInfoMap();
        }

        return mAppInfoMap;
    }

    private Map<Long, String> getIdPackageNameMap() {
        if (mIdPackageNameMap == null) {
            populateAppInfoMap();
        }

        return mIdPackageNameMap;
    }

    private AppInfoInternal getAppInfo(@NonNull String packageName, @NonNull Context context)
            throws NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo info =
                packageManager.getApplicationInfo(
                        packageName, PackageManager.ApplicationInfoFlags.of(0));
        String appName = packageManager.getApplicationLabel(info).toString();
        Drawable icon = packageManager.getApplicationIcon(info);
        Bitmap bitmap = getBitmapFromDrawable(icon);
        return new AppInfoInternal(DEFAULT_LONG, packageName, appName, bitmap);
    }

    private void upsertAppInfo(@NonNull String packageName, @NonNull AppInfoInternal appInfo) {
        long rowId =
                TransactionManager.getInitialisedInstance()
                        .insertOrReplace(
                                new UpsertTableRequest(
                                        TABLE_NAME, getContentValues(packageName, appInfo)));
        appInfo.setId(rowId);
        getAppInfoMap().put(packageName, appInfo);
        getIdPackageNameMap().put(appInfo.getId(), packageName);
    }

    @NonNull
    private ContentValues getContentValues(String packageName, AppInfoInternal appInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PACKAGE_COLUMN_NAME, packageName);
        contentValues.put(APPLICATION_COLUMN_NAME, appInfo.getName());
        contentValues.put(APP_ICON_COLUMN_NAME, encodeBitmap(appInfo.getIcon()));
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
    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(PACKAGE_COLUMN_NAME, TEXT_NOT_NULL_UNIQUE));
        columnInfo.add(new Pair<>(APPLICATION_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(APP_ICON_COLUMN_NAME, BLOB));

        return columnInfo;
    }
}
