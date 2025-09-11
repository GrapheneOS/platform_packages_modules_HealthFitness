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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class DeviceDataProviderHelperTest {

    @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private DeviceDataProviderHelper mDeviceDataProviderHelper;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mDeviceDataProviderHelper =
                new DeviceDataProviderHelper(healthConnectInjector.getDatabaseHelpers());
    }

    @Test
    public void testTableName() {
        assertThat(DeviceDataProviderHelper.TABLE_NAME).isEqualTo("device_data_provider_table");
    }

    @Test
    public void testColumnNames() {
        assertThat(DeviceDataProviderHelper.SOURCE_PACKAGE_NAME).isEqualTo("source_package_name");
        assertThat(DeviceDataProviderHelper.DATA_TYPE).isEqualTo("data_type");
        assertThat(DeviceDataProviderHelper.IS_AVAILABLE).isEqualTo("is_available");
        assertThat(DeviceDataProviderHelper.IS_USER_ENABLED).isEqualTo("is_user_enabled");
    }

    @Test
    public void testGetCreateTableRequest() {
        CreateTableRequest request = DeviceDataProviderHelper.getCreateTableRequest();

        assertThat(request.getTableName()).isEqualTo(DeviceDataProviderHelper.TABLE_NAME);
        assertThat(request.getCreateCommand())
                .contains(
                        " FOREIGN KEY ("
                                + RecordHelper.DEVICE_INFO_ID_COLUMN_NAME
                                + ")"
                                + " REFERENCES "
                                + DeviceInfoHelper.TABLE_NAME
                                + "("
                                + RecordHelper.PRIMARY_COLUMN_NAME
                                + ") ON DELETE CASCADE");
    }

    @Test
    public void testGetColumnInfo() {
        CreateTableRequest request = DeviceDataProviderHelper.getCreateTableRequest();
        String createCommand = request.getCreateCommand();

        assertThat(createCommand)
                .contains(RecordHelper.PRIMARY_COLUMN_NAME + " " + StorageUtils.PRIMARY);
        assertThat(createCommand)
                .contains(
                        RecordHelper.DEVICE_INFO_ID_COLUMN_NAME
                                + " "
                                + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataProviderHelper.SOURCE_PACKAGE_NAME
                                + " "
                                + StorageUtils.TEXT_NOT_NULL);
        assertThat(createCommand)
                .contains(DeviceDataProviderHelper.DATA_TYPE + " " + StorageUtils.TEXT_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataProviderHelper.IS_AVAILABLE
                                + " "
                                + StorageUtils.INTEGER_NOT_NULL);
        assertThat(createCommand)
                .contains(
                        DeviceDataProviderHelper.IS_USER_ENABLED
                                + " "
                                + StorageUtils.INTEGER_NOT_NULL);
    }

    @Test
    public void testGetMainTableName() {
        assertThat(mDeviceDataProviderHelper.getMainTableName())
                .isEqualTo(DeviceDataProviderHelper.TABLE_NAME);
    }
}
