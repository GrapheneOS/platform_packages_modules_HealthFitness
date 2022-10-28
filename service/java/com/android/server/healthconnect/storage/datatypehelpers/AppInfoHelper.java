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

import static android.healthconnect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class to help with the DB transaction for storing Application Info. {@link AppInfoHelper} acts
 * as a layer b/w the application_info_table stored in the DB and helps perform insert and read
 * operations on the table
 *
 * @hide
 */
public class AppInfoHelper {
    private static final class AppInfo {
        private final String mAppName;
        private final byte[] mIcon;
        private long mId;

        AppInfo(long id, String appName, byte[] icon) {
            this.mId = id;
            this.mAppName = appName;
            this.mIcon = icon;
        }

        public byte[] getIcon() {
            return mIcon;
        }

        public String getAppName() {
            return mAppName;
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            this.mId = id;
        }
    }

    private static final String TABLE_NAME = "application_info_table";
    private static final String APPLICATION_COLUMN_NAME = "app_name";
    private static final String PACKAGE_COLUMN_NAME = "package_name";
    private static final String APP_ICON_COLUMN_NAME = "app_icon";
    private static final int COMPRESS_FACTOR = 100;
    private static AppInfoHelper sAppInfoHelper;

    /**
     * Map to store application package-name -> AppInfo mapping (such as packageName -> appName,
     * icon, rowId in the DB etc.)
     */
    private Map<String, AppInfo> mAppInfoMap;

    private AppInfoHelper() {}

    public static AppInfoHelper getInstance() {
        if (sAppInfoHelper == null) {
            sAppInfoHelper = new AppInfoHelper();
        }

        return sAppInfoHelper;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public final CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    /** Populates record with appInfoId */
    public void populateAppInfoId(@NonNull RecordInternal<?> recordInternal, Context context) {
        if (Objects.isNull(mAppInfoMap)) {
            populateAppInfoMap();
        }

        String packageName = recordInternal.getPackageName();
        AppInfo appInfo = mAppInfoMap.getOrDefault(packageName, null);
        if (appInfo == null) {
            appInfo = insertAndGetAppInfo(packageName, context);
        }
        recordInternal.setAppInfoId(appInfo.getId());
    }

    public void populateAppInfoMap() {
        mAppInfoMap = new ArrayMap<>();
        final TransactionManager transactionManager = TransactionManager.getInitializedInstance();
        try (SQLiteDatabase db = TransactionManager.getInitializedInstance().getReadableDb();
                Cursor cursor = transactionManager.read(db, new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId =
                        cursor.getLong(cursor.getColumnIndex(RecordHelper.PRIMARY_COLUMN_NAME));
                String packageName = cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME));
                String appName = cursor.getString(cursor.getColumnIndex(APPLICATION_COLUMN_NAME));
                byte[] icon = cursor.getBlob(cursor.getColumnIndex(APP_ICON_COLUMN_NAME));
                mAppInfoMap.put(packageName, new AppInfo(rowId, appName, icon));
            }
        }
    }

    // Called on DB update.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    public long getAppInfoId(String packageName) {
        if (Objects.isNull(mAppInfoMap)) {
            populateAppInfoMap();
        }

        AppInfo appInfo = mAppInfoMap.getOrDefault(packageName, null);
        if (appInfo == null) {
            return DEFAULT_LONG;
        }

        return appInfo.getId();
    }

    private AppInfo getAppInfo(String packageName, Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info =
                    packageManager.getApplicationInfo(
                            packageName, PackageManager.ApplicationInfoFlags.of(0));
            String appName = packageManager.getApplicationLabel(info).toString();
            Drawable icon = packageManager.getApplicationIcon(info);
            Bitmap bitmap = getBitmapFromDrawable(icon);
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_FACTOR, stream);
                byte[] bitmapData = stream.toByteArray();
                return new AppInfo(DEFAULT_LONG, appName, bitmapData);
            } catch (IOException exception) {
                throw new IllegalArgumentException(exception);
            }
        } catch (NameNotFoundException exception) {
            throw new IllegalArgumentException(
                    "Could not find package info for package", exception);
        }
    }

    private AppInfo insertAndGetAppInfo(String packageName, Context context) {
        AppInfo appInfo = getAppInfo(packageName, context);
        long rowId =
                TransactionManager.getInitializedInstance()
                        .insert(
                                new UpsertTableRequest(
                                        TABLE_NAME, getContentValues(packageName, appInfo)));
        appInfo.setId(rowId);
        mAppInfoMap.put(packageName, appInfo);
        return appInfo;
    }

    @NonNull
    private ContentValues getContentValues(String packageName, AppInfo appInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PACKAGE_COLUMN_NAME, packageName);
        contentValues.put(APPLICATION_COLUMN_NAME, appInfo.getAppName());
        contentValues.put(APP_ICON_COLUMN_NAME, appInfo.getIcon());
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
}
