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

import static android.healthconnect.testing.shared.DataFactory.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@EnableFlags({
    Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
    Flags.FLAG_DEVELOPMENT_DATABASE,
    Flags.FLAG_DEVICE_DATA_PROVIDERS_DB
})
@RunWith(AndroidJUnit4.class)
public class DeviceDataProviderMetadataHelperTest {

    @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static final String TEST_DDP_PACKAGE = "test.package";

    private DeviceDataProviderMetadataHelper mDeviceDataProviderMetadataHelper;
    private TransactionManager mTransactionManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = Mockito.spy(healthConnectInjector.getTransactionManager());
        mDeviceDataProviderMetadataHelper =
                new DeviceDataProviderMetadataHelper(
                        healthConnectInjector.getDatabaseHelpers(), mTransactionManager);
    }

    @Test
    public void testTableName() {
        assertThat(DeviceDataProviderMetadataHelper.TABLE_NAME)
                .isEqualTo("device_data_provider_metadata_table");
    }

    @Test
    public void testColumnNames() {
        assertThat(DeviceDataProviderMetadataHelper.SOURCE_PACKAGE_COLUMN_NAME)
                .isEqualTo("source_package_name");
    }

    @Test
    public void testGetCreateTableRequest() {
        CreateTableRequest request = DeviceDataProviderMetadataHelper.getCreateTableRequest();

        assertThat(request.getTableName()).isEqualTo(DeviceDataProviderMetadataHelper.TABLE_NAME);
    }

    @Test
    public void testGetColumnInfo() {
        CreateTableRequest request = DeviceDataProviderMetadataHelper.getCreateTableRequest();
        String createCommand = request.getCreateCommand();

        assertThat(createCommand)
                .contains(RecordHelper.PRIMARY_COLUMN_NAME + " " + StorageUtils.PRIMARY);
        assertThat(createCommand)
                .contains(
                        DeviceDataProviderHelper.SOURCE_PACKAGE_NAME
                                + " "
                                + StorageUtils.TEXT_NOT_NULL);
    }

    @Test
    public void testGetMainTableName() {
        assertThat(mDeviceDataProviderMetadataHelper.getMainTableName())
                .isEqualTo(DeviceDataProviderMetadataHelper.TABLE_NAME);
    }

    @Test
    public void withNewEntry_insertIfNotPresent_entryInserted() {
        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataProviderMetadataHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(1);
            cursor.moveToFirst();
            assertThat(getCursorString(cursor, DeviceDataProviderHelper.SOURCE_PACKAGE_NAME))
                    .isEqualTo(TEST_DDP_PACKAGE);
        }
    }

    @Test
    public void withNewEntry_insertIfNotPresent_entryAddedToCache() {
        assertThat(mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().size())
                .isEqualTo(0);
        assertThat(mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataIdMap().size())
                .isEqualTo(0);

        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);

        assertThat(mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().size())
                .isEqualTo(1);
        assertThat(mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataIdMap().size())
                .isEqualTo(1);
    }

    @Test
    public void withMultipleEntries_insertIfNotPresent_entriesInserted() {
        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);
        mDeviceDataProviderMetadataHelper.insertIfNotPresent("Another Package");

        try (Cursor cursor =
                mTransactionManager.read(
                        new ReadTableRequest(DeviceDataProviderMetadataHelper.TABLE_NAME))) {
            assertThat(cursor.getCount()).isEqualTo(2);
        }
    }

    @Test
    public void withExistingEntry_insertIfNotPresent_returnsEarly() {
        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);
        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);

        verify(mTransactionManager, times(1)).read(any());
    }

    @Test
    public void withEmptyState_getIdDeviceDataProviderMetadataMap_readsDbAndPopulatesCache() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                DeviceDataProviderMetadataHelper.SOURCE_PACKAGE_COLUMN_NAME, TEST_DDP_PACKAGE);

        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(DeviceDataProviderMetadataHelper.TABLE_NAME, contentValues));

        assertThat(mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().size())
                .isEqualTo(1);
        assertThat(mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataIdMap().size())
                .isEqualTo(1);

        assertThat(mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().get(1L))
                .isEqualTo(
                        new DeviceDataProviderMetadataHelper.DeviceDataProviderMetadata(
                                TEST_DDP_PACKAGE));
        assertThat(
                        mDeviceDataProviderMetadataHelper
                                .getDeviceDataProviderMetadataIdMap()
                                .get(
                                        new DeviceDataProviderMetadataHelper
                                                .DeviceDataProviderMetadata(TEST_DDP_PACKAGE)))
                .isEqualTo(1L);
    }

    @Test
    public void withEmptyState_getDeviceDataProviderMetadataId_returnsDefaultLong() {
        assertThat(
                        mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataId(
                                TEST_DDP_PACKAGE))
                .isEqualTo(DEFAULT_LONG);
    }

    @Test
    public void withInsertedRecord_getDeviceDataProviderMetadataId_returnsRightId() {
        mDeviceDataProviderMetadataHelper.insertIfNotPresent(TEST_DDP_PACKAGE);

        assertThat(
                        mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataId(
                                TEST_DDP_PACKAGE))
                .isEqualTo(1L);
    }
}
