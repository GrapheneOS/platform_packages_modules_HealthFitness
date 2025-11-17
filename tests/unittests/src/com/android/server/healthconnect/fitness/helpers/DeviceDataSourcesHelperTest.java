/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.fitness.helpers;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getIntegerAndConvertToBoolean;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;

@EnableFlags({Flags.FLAG_DEVELOPMENT_DATABASE, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
@RunWith(AndroidJUnit4.class)
public class DeviceDataSourcesHelperTest {

    @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static final String TEST_APP_PACKAGE = "com.test.app";
    private static final String DISPLAY_NAME = "Test Pixel";
    private static final String DEVICE_ID = "1234";

    private DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private TransactionManager mTransactionManager;
    private HealthConnectMappings mHealthConnectMappings;
    private Device mDevice;
    private SyntheticPackageNameCreator mSyntheticPackageNameCreator;
    private AppInfoHelper mAppInfoHelper;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mHealthConnectMappings = healthConnectInjector.getHealthConnectMappings();
        mDeviceDataSourcesHelper =
                new DeviceDataSourcesHelper(
                        healthConnectInjector.getDatabaseHelpers(),
                        mTransactionManager,
                        mHealthConnectMappings);
        mDevice =
                new Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel")
                        .setType(DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        mSyntheticPackageNameCreator = healthConnectInjector.getSyntheticPackageNameCreator();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
    }

    @Test
    public void testTableName() {
        assertThat(DeviceDataSourcesHelper.TABLE_NAME).isEqualTo("device_data_sources_table");
    }

    @Test
    public void testColumnNames() {
        assertThat(DeviceDataSourcesHelper.SOURCE_PACKAGE_NAME).isEqualTo("source_package_name");
        assertThat(DeviceDataSourcesHelper.DATA_TYPE).isEqualTo("data_type");
        assertThat(DeviceDataSourcesHelper.IS_AVAILABLE).isEqualTo("is_available");
        assertThat(DeviceDataSourcesHelper.IS_USER_ENABLED).isEqualTo("is_user_enabled");
        assertThat(DeviceDataSourcesHelper.IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING)
                .isEqualTo("is_visible_by_default_in_matchmaking");
    }

    @Test
    public void testGetCreateTableRequest() {
        CreateTableRequest request = DeviceDataSourcesHelper.getCreateTableRequest();

        assertThat(request.getTableName()).isEqualTo(DeviceDataSourcesHelper.TABLE_NAME);
        assertThat(request.getCreateCommand())
                .contains(
                        " FOREIGN KEY ("
                                + RecordHelper.APP_INFO_ID_COLUMN_NAME
                                + ")"
                                + " REFERENCES "
                                + AppInfoHelper.TABLE_NAME
                                + "("
                                + RecordHelper.PRIMARY_COLUMN_NAME
                                + ") ON DELETE CASCADE");
    }

    @Test
    public void testGetColumnInfo() {
        CreateTableRequest request = DeviceDataSourcesHelper.getCreateTableRequest();
        String createCommand = request.getCreateCommand();

        assertThat(createCommand)
                .contains(RecordHelper.PRIMARY_COLUMN_NAME + " " + StorageUtils.PRIMARY);
        assertThat(createCommand)
                .contains(
                        RecordHelper.APP_INFO_ID_COLUMN_NAME + " " + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataSourcesHelper.SOURCE_PACKAGE_NAME
                                + " "
                                + StorageUtils.TEXT_NOT_NULL);
        assertThat(createCommand)
                .contains(DeviceDataSourcesHelper.DATA_TYPE + " " + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataSourcesHelper.IS_AVAILABLE + " " + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataSourcesHelper.IS_USER_ENABLED
                                + " "
                                + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataSourcesHelper.IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING
                                + " "
                                + StorageUtils.INTEGER_NOT_NULL);
    }

    @Test
    public void testGetMainTableName() {
        assertThat(mDeviceDataSourcesHelper.getMainTableName())
                .isEqualTo(DeviceDataSourcesHelper.TABLE_NAME);
    }

    @Test
    public void insertOrUpdateDatabase_insertNewEntry_entryInserted() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);

        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .setVisibleByDefaultInMatchmaking(true)
                                        .build())));

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
            cursor.moveToFirst();
            assertThat(getCursorString(cursor, DeviceDataSourcesHelper.SOURCE_PACKAGE_NAME))
                    .isEqualTo(TEST_APP_PACKAGE);
            assertThat(getCursorInt(cursor, RecordHelper.APP_INFO_ID_COLUMN_NAME))
                    .isEqualTo(deviceInfoId);
            assertThat(getCursorInt(cursor, DeviceDataSourcesHelper.DATA_TYPE))
                    .isEqualTo(mHealthConnectMappings.getRecordType(StepsRecord.class));
            assertThat(getIntegerAndConvertToBoolean(cursor, DeviceDataSourcesHelper.IS_AVAILABLE))
                    .isTrue();
            assertThat(
                            getIntegerAndConvertToBoolean(
                                    cursor, DeviceDataSourcesHelper.IS_USER_ENABLED))
                    .isTrue();
            assertThat(
                            getIntegerAndConvertToBoolean(
                                    cursor,
                                    DeviceDataSourcesHelper.IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING))
                    .isTrue();
        }
    }

    @Test
    public void insertOrUpdateDatabase_updateExistingEntry_entryUpdated() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .setVisibleByDefaultInMatchmaking(true)
                                        .build())));
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(false)
                                        .setUserEnabled(false)
                                        .setVisibleByDefaultInMatchmaking(false)
                                        .build())));
        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
            cursor.moveToFirst();
            assertThat(getCursorString(cursor, DeviceDataSourcesHelper.SOURCE_PACKAGE_NAME))
                    .isEqualTo(TEST_APP_PACKAGE);
            assertThat(getCursorLong(cursor, RecordHelper.APP_INFO_ID_COLUMN_NAME))
                    .isEqualTo(deviceInfoId);
            assertThat(getCursorInt(cursor, DeviceDataSourcesHelper.DATA_TYPE))
                    .isEqualTo(mHealthConnectMappings.getRecordType(StepsRecord.class));
            assertThat(getIntegerAndConvertToBoolean(cursor, DeviceDataSourcesHelper.IS_AVAILABLE))
                    .isFalse();
            assertThat(
                            getIntegerAndConvertToBoolean(
                                    cursor, DeviceDataSourcesHelper.IS_USER_ENABLED))
                    .isFalse();
            assertThat(
                            getIntegerAndConvertToBoolean(
                                    cursor,
                                    DeviceDataSourcesHelper.IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING))
                    .isFalse();
        }
    }

    @Test
    public void insertOrUpdateDatabase_multipleEntries_entriesInserted() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build(),
                                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(2);
        }
    }

    @Test
    public void cache_clearedOnClearData() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));

        mDeviceDataSourcesHelper.clearData(mTransactionManager);

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(0);
        }
    }

    @Test
    public void cache_clearedOnClearCache() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));

        mDeviceDataSourcesHelper.clearCache();

        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));
        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
        }
    }

    @Test
    public void populateDdpCache_readsDbAndPopulatesCache() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        ContentValues contentValues = new ContentValues();
        contentValues.put(DeviceDataSourcesHelper.SOURCE_PACKAGE_NAME, TEST_APP_PACKAGE);
        contentValues.put(RecordHelper.APP_INFO_ID_COLUMN_NAME, deviceInfoId);
        contentValues.put(
                DeviceDataSourcesHelper.DATA_TYPE,
                mHealthConnectMappings.getRecordType(StepsRecord.class));
        contentValues.put(DeviceDataSourcesHelper.IS_AVAILABLE, 1);
        contentValues.put(DeviceDataSourcesHelper.IS_USER_ENABLED, 1);
        contentValues.put(DeviceDataSourcesHelper.IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING, 1);
        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(DeviceDataSourcesHelper.TABLE_NAME, contentValues));

        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .setVisibleByDefaultInMatchmaking(true)
                                        .build())));

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
        }
    }

    @Test
    public void insertOrUpdateAdvertisement_deletesUnspecifiedDataTypes() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build(),
                                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));

        // Update with only StepsRecord, expecting DistanceRecord to be deleted
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .setUserEnabled(true)
                                        .build())));

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataSourcesHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
            cursor.moveToFirst();
            assertThat(getCursorInt(cursor, DeviceDataSourcesHelper.DATA_TYPE))
                    .isEqualTo(mHealthConnectMappings.getRecordType(StepsRecord.class));
        }
    }

    @Test
    public void getAdvertisedDataTypes_returnsCorrectDataTypes() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .build(),
                                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                        .setAvailable(true)
                                        .build())));

        List<Integer> advertisedDataTypes =
                mDeviceDataSourcesHelper.getAdvertisedDataTypes(TEST_APP_PACKAGE, deviceInfoId);

        assertThat(advertisedDataTypes)
                .containsExactly(
                        mHealthConnectMappings.getRecordType(StepsRecord.class),
                        mHealthConnectMappings.getRecordType(DistanceRecord.class));
    }

    @Test
    public void getAdvertisedDataTypes_noAdvertisements_returnsEmptyList() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        List<Integer> advertisedDataTypes =
                mDeviceDataSourcesHelper.getAdvertisedDataTypes(TEST_APP_PACKAGE, deviceInfoId);

        assertThat(advertisedDataTypes).isEmpty();
    }

    @Test
    public void getAdvertisedDataTypes_differentPackage_returnsEmptyList() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .build())));

        List<Integer> advertisedDataTypes =
                mDeviceDataSourcesHelper.getAdvertisedDataTypes("different.package", deviceInfoId);

        assertThat(advertisedDataTypes).isEmpty();
    }

    @Test
    public void getAdvertisedDataTypes_differentDevice_returnsEmptyList() {
        long deviceInfoId = insertDeviceInfo();
        String canonicalSpn = mSyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(canonicalSpn, deviceInfoId);
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                TEST_APP_PACKAGE,
                appInfoId,
                new DeviceDataAdvertisement(
                        mDevice,
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .build())));
        String otherCanonicalSpn =
                mSyntheticPackageNameCreator.createCanonical(1, "testOtherDeviceId");
        long otherAppInfoId =
                mAppInfoHelper.insertOrUpdateDeviceDataSource(otherCanonicalSpn, deviceInfoId);

        List<Integer> advertisedDataTypes =
                mDeviceDataSourcesHelper.getAdvertisedDataTypes(TEST_APP_PACKAGE, otherAppInfoId);

        assertThat(advertisedDataTypes).isEmpty();
    }

    private long insertDeviceInfo() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DeviceInfoHelper.MANUFACTURER_COLUMN_NAME, "Google");
        contentValues.put(DeviceInfoHelper.MODEL_COLUMN_NAME, "Pixel");
        contentValues.put(DeviceInfoHelper.DEVICE_TYPE_COLUMN_NAME, DEVICE_TYPE_PHONE);
        return (long)
                mTransactionManager.insertOrThrowOnConflict(
                        new UpsertTableRequest(DeviceInfoHelper.TABLE_NAME, contentValues));
    }
}
