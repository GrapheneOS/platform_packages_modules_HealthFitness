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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_FHIR_VERSION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createVaccineMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.DATA_SOURCE_UUID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.DISPLAY_NAME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.FHIR_BASE_URI_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.FHIR_VERSION_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.MAX_ALLOWED_MEDICAL_DATA_SOURCES;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.MEDICAL_DATA_SOURCE_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.getReadQueryForDataSourcesFilterOnIdsAndAppIdsAndResourceTypes;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_UNIQUE_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getHexString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.toUuids;
import static com.android.server.healthconnect.testing.TestUtils.TEST_USER;
import static com.android.server.healthconnect.testing.storage.PhrTestUtils.ACCESS_LOG_EQUIVALENCE;
import static com.android.server.healthconnect.testing.storage.PhrTestUtils.makeUpsertRequest;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
import com.android.server.healthconnect.testing.fakes.FakeTimeSource;
import com.android.server.healthconnect.testing.storage.PhrTestUtils;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class MedicalDataSourceHelperTest {

    private static final long APP_INFO_ID = 123;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final Instant INSTANT_NOW = Instant.now();
    private static final Instant INSTANT_NOW_PLUS_TEN_SEC = INSTANT_NOW.plusSeconds(10);
    private static final Instant INSTANT_NOW_PLUS_TWENTY_SEC = INSTANT_NOW.plusSeconds(20);

    private MedicalDataSourceHelper mMedicalDataSourceHelper;
    private MedicalResourceHelper mMedicalResourceHelper;
    private TransactionManager mTransactionManager;
    private TransactionTestUtils mTransactionTestUtils;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private PhrTestUtils mUtil;
    private FakeTimeSource mFakeTimeSource;
    private UserHandle mUserHandle;

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;
    @Mock private Drawable mDrawable;

    @Before
    public void setup() throws NameNotFoundException {

        when(mContext.getUser()).thenReturn(TEST_USER);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mFakeTimeSource = new FakeTimeSource(INSTANT_NOW);
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setTimeSource(mFakeTimeSource)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mMedicalDataSourceHelper = healthConnectInjector.getMedicalDataSourceHelper();
        mMedicalResourceHelper = healthConnectInjector.getMedicalResourceHelper();
        mUserHandle = Process.myUserHandle();

        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mUtil = new PhrTestUtils(healthConnectInjector);
    }

    @After
    public void tearDown() throws Exception {
        reset(mDrawable, mContext, mPackageManager);
    }

    @Test
    public void getCreateTableRequest_correctResult() {
        List<Pair<String, String>> columnInfo =
                List.of(
                        Pair.create(MedicalDataSourceHelper.getPrimaryColumnName(), PRIMARY),
                        Pair.create(
                                MedicalDataSourceHelper.getAppInfoIdColumnName(), INTEGER_NOT_NULL),
                        Pair.create(DISPLAY_NAME_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(FHIR_BASE_URI_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(FHIR_VERSION_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(DATA_SOURCE_UUID_COLUMN_NAME, BLOB_UNIQUE_NON_NULL),
                        Pair.create(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER_NOT_NULL));
        CreateTableRequest expected =
                new CreateTableRequest(MEDICAL_DATA_SOURCE_TABLE_NAME, columnInfo)
                        .addForeignKey(
                                AppInfoHelper.TABLE_NAME,
                                List.of(MedicalDataSourceHelper.getAppInfoIdColumnName()),
                                List.of(PRIMARY_COLUMN_NAME));

        CreateTableRequest result = MedicalDataSourceHelper.getCreateTableRequest();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getUpsertTableRequest_correctResult() {
        CreateMedicalDataSourceRequest createMedicalDataSourceRequest =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build();
        UUID uuid = UUID.randomUUID();

        UpsertTableRequest upsertRequest =
                mMedicalDataSourceHelper.getUpsertTableRequest(
                        uuid, createMedicalDataSourceRequest, APP_INFO_ID, INSTANT_NOW);
        ContentValues contentValues = upsertRequest.getContentValues();

        assertThat(upsertRequest.getTable()).isEqualTo(MEDICAL_DATA_SOURCE_TABLE_NAME);
        assertThat(upsertRequest.getUniqueColumnsCount()).isEqualTo(1);
        assertThat(contentValues.size()).isEqualTo(6);
        assertThat(contentValues.get(FHIR_BASE_URI_COLUMN_NAME))
                .isEqualTo(DATA_SOURCE_FHIR_BASE_URI.toString());
        assertThat(contentValues.get(DISPLAY_NAME_COLUMN_NAME)).isEqualTo(DATA_SOURCE_DISPLAY_NAME);
        assertThat(contentValues.get(FHIR_VERSION_COLUMN_NAME))
                .isEqualTo(DATA_SOURCE_FHIR_VERSION.toString());
        assertThat(contentValues.get(DATA_SOURCE_UUID_COLUMN_NAME))
                .isEqualTo(StorageUtils.convertUUIDToBytes(uuid));
        assertThat(contentValues.get(MedicalDataSourceHelper.getAppInfoIdColumnName()))
                .isEqualTo(APP_INFO_ID);
        assertThat(contentValues.get(LAST_MODIFIED_TIME_COLUMN_NAME))
                .isEqualTo(INSTANT_NOW.toEpochMilli());
    }

    @Test
    public void getReadTableRequest_usingMedicalDataSourceId_correctQuery() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<String> hexValues = List.of(getHexString(uuid1), getHexString(uuid2));

        ReadTableRequest readRequest =
                MedicalDataSourceHelper.getReadTableRequest(List.of(uuid1, uuid2));

        assertThat(readRequest.getTableName()).isEqualTo(MEDICAL_DATA_SOURCE_TABLE_NAME);
        assertThat(readRequest.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM medical_data_source_table WHERE data_source_uuid IN ("
                                + String.join(", ", hexValues)
                                + ")");
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getReadTableRequest_noRestrictions_success() throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource expected =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        ReadTableRequest request =
                MedicalDataSourceHelper.getReadTableRequest(
                        List.of(UUID.fromString(expected.getId())), /* appInfoRestriction= */ null);

        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(getIds(cursor)).containsExactly(expected.getId());
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getReadTableRequest_packageRestrictionMatches_success()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource correctDataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        long appInfoRestriction = mAppInfoHelper.getAppInfoId(DATA_SOURCE_PACKAGE_NAME);
        ReadTableRequest request =
                MedicalDataSourceHelper.getReadTableRequest(
                        List.of(UUID.fromString(correctDataSource.getId())), appInfoRestriction);

        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(getIds(cursor)).containsExactly(correctDataSource.getId());
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getReadTableRequest_packageRestrictionDoesNotMatch_noResult()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource correctDataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DIFFERENT_DATA_SOURCE_BASE_URI,
                DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        long appInfoRestriction = mAppInfoHelper.getAppInfoId(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        ReadTableRequest request =
                MedicalDataSourceHelper.getReadTableRequest(
                        List.of(UUID.fromString(correctDataSource.getId())), appInfoRestriction);

        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(getIds(cursor)).isEmpty();
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getReadTableRequest_noIdsPackageRestrictionMatches_succeeds()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DATA_SOURCE_FHIR_BASE_URI,
                DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION,
                DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource otherDataSource =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        long appInfoRestriction = mAppInfoHelper.getAppInfoId(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        ReadTableRequest request =
                MedicalDataSourceHelper.getReadTableRequest(List.of(), appInfoRestriction);

        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(getIds(cursor)).containsExactly(otherDataSource.getId());
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getReadTableRequest_noIdsNoPackageRestrictionMatches_succeeds()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource otherDataSource =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        ReadTableRequest request =
                MedicalDataSourceHelper.getReadTableRequest(
                        List.of(), /* appInfoIdRestriction= */ null);

        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(getIds(cursor)).containsExactly(dataSource.getId(), otherDataSource.getId());
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void createAndGetSingleMedicalDataSource_packageDoesNotExist_success()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource expected =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(UUID.fromString(expected.getId())));

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void createAndGetSingleMedicalDataSource_packageAlreadyExists_success() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(UUID.fromString(dataSource1.getId())));

        assertThat(result).containsExactly(dataSource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void createMedicalDataSources_sameDisplayNamesFromSamePackage_throws() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mMedicalDataSourceHelper.createMedicalDataSource(
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                DATA_SOURCE_FHIR_VERSION)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.createMedicalDataSource(
                                new CreateMedicalDataSourceRequest.Builder(
                                                DIFFERENT_DATA_SOURCE_BASE_URI,
                                                DATA_SOURCE_DISPLAY_NAME,
                                                DATA_SOURCE_FHIR_VERSION)
                                        .build(),
                                DATA_SOURCE_PACKAGE_NAME));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void createMedicalDataSource_lastModifiedTimeIsPopulated() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DATA_SOURCE_FHIR_BASE_URI,
                DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION,
                DATA_SOURCE_PACKAGE_NAME);

        long lastModifiedTimestamp =
                mUtil.readLastModifiedTimestamp(MEDICAL_DATA_SOURCE_TABLE_NAME);

        assertThat(lastModifiedTimestamp).isEqualTo(INSTANT_NOW.toEpochMilli());
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void createAndGetMultipleMedicalDataSources_bothPackagesAlreadyExist_success() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));

        assertThat(result).containsExactly(dataSource1, dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void createAndGetMultipleMedicalDataSourcesWithSamePackage_packageDoesNotExist_success()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        List<MedicalDataSource> expected = List.of(dataSource1, dataSource2);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsIn(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void
            createAndGetMultipleMedicalDataSourcesWithDifferentPackages_packagesDoNotExist_success()
                    throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));

        assertThat(result).containsExactly(dataSource1, dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void createMultipleMedicalDataSources_maxLimitExceeded_throws()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        for (int i = 0; i < MAX_ALLOWED_MEDICAL_DATA_SOURCES; i++) {
            String suffix = String.valueOf(i);
            createDataSource(
                    Uri.withAppendedPath(DATA_SOURCE_FHIR_BASE_URI, "/" + suffix),
                    DATA_SOURCE_DISPLAY_NAME + " " + suffix,
                    DATA_SOURCE_FHIR_VERSION,
                    DATA_SOURCE_PACKAGE_NAME);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    createDataSource(
                            DATA_SOURCE_FHIR_BASE_URI,
                            DATA_SOURCE_DISPLAY_NAME,
                            DATA_SOURCE_FHIR_VERSION,
                            DATA_SOURCE_PACKAGE_NAME);
                });
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getMedicalDataSourcesByPackage_noPackages_returnsAll() throws Exception {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> dataSources =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of());

        assertThat(dataSources).containsExactly(dataSource1, dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void getMedicalDataSourcesByPackage_onePackage_filters() throws Exception {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> dataSources1 =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME));
        List<MedicalDataSource> dataSources2 =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME));

        assertThat(dataSources1).containsExactly(dataSource1);
        assertThat(dataSources2).containsExactly(dataSource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getDataSourcesByIds_noWriteOrReadPerm_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                            /* ids= */ List.of(),
                            /* grantedMedicalResourceTypes= */ Set.of(),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ false,
                            /* isCalledFromBgWithoutBgRead= */ false,
                            mAppInfoHelper);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getDataSourcesByIds_hasWritePermButNeverWrittenData_noReadPerm_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                            /* ids= */ List.of(),
                            /* grantedMedicalResourceTypes= */ Set.of(),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ true,
                            /* isCalledFromBgWithoutBgRead= */ false,
                            mAppInfoHelper);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getDataSourcesByIds_inBgWithoutBgPermHasWritePerm_canReadSelfDataSources() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource1, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(addLastDataUpdateTime(dataSource1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void createMedicalDataSource_createsAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        mMedicalDataSourceHelper.createMedicalDataSource(
                getCreateMedicalDataSourceRequest(), DATA_SOURCE_PACKAGE_NAME);

        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_UPSERT,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inBgWithoutBgPerm_hasWritePerm_noAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource1.getId())),
                /* grantedReadMedicalResourceTypes= */ Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ true,
                mAppInfoHelper);

        // We don't expect an access log for read.
        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inBgWithoutBgPerm_hasWritePerm_hasReadPermForResourceTypes_noAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource1.getId())),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ true,
                mAppInfoHelper);

        // We don't expect an access log for read.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inBgWithoutBgPerm_noWritePerm_vaccineReadPermOnly_noAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource1.getId())),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ true,
                mAppInfoHelper);

        // We don't expect an access log for read.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_expectAccessLogsWhenDataAccessedIsThroughReadPermission() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergyResourcePackage2 = createAllergyMedicalResource(dataSource2.getId());

        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(vaccineDS1)));
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                List.of(makeUpsertRequest(allergyResourcePackage2)));
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource1.getId(), dataSource2.getId())),
                Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES read permission.
        // has write permission.
        // The data that the calling app can read: dataSource1 (through selfRead)
        // dataSource1 (through read permission for allergy).
        // In this case, read access log is only created since the dataSource accessed
        // is because  of the app having allergy read permission.
        // We only expect a single access log for the createMedicalDataSource and no access log
        // for the read.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_expectAccessLogsWhenAppHasNoWritePermHasReadPermButReadOnlySelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergyPackage2 = createAllergyMedicalResource(dataSource2.getId());

        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(vaccineDS1)));
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(allergyPackage2)));
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource1.getId(), dataSource2.getId())),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // no write permission.
        // The data that the calling app can read: dataSource1 (through vaccine
        // read permission)
        // In this case, read access log is created based on the intention of the
        // app and the fact that the app has MEDICAL_RESOURCE_TYPE_VACCINES
        // even though the actual data accessed is self data.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_expectAccessLogsWhenAppHasNoWritePermHasReadPermReadNonSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        String dataSource =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource vaccineDifferentPackage = createVaccineMedicalResource(dataSource);
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                List.of(makeUpsertRequest(vaccineDifferentPackage)));
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource)),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // no write permission.
        // The data that the calling app can read: any dataSource belonging to the any vaccine
        // resources as the app has vaccine permission.
        // In this case, read access log is created.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inForegroundOrBgWithPerm_hasReadVaccine_nothingRead_noAccessLog() {
        List<UUID> dataSourceIds = List.of(UUID.fromString("a6194e35-698c-4706-918f-00bf959f123b"));
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                dataSourceIds,
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inForegroundOrBgWithPerm_hasWritePerm_noReadPerm_noAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        String dataSource = mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource)),
                Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        // No access log should be created for read,
        // since app is intending to access self data as it has
        // no read permissions.
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).doesNotContain(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_expectAccessLogsWhenAppHasWritePermHasReadPermReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                toUuids(List.of(dataSource.getId())),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false,
                mAppInfoHelper);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // has write permission.
        // The data that the calling app can read: dataSource of the vaccine
        // In this case, read access log is created based on the intention of the app
        // even though the actual data accessed is self data.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByIds_inBgWithoutBgPermHasWritePermHasReadPerm_canReadSelfDataSources() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource1, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(addLastDataUpdateTime(dataSource1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByIds_inBgWithoutBgPermNoWritePermVaccineReadPermOnly_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource1, INSTANT_NOW);
        // Even though the reader does not have Allergy read permissions, this resource needs to
        // be considered for calculating the MedicalDataSource's last data update time.
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        // App is in background without background read perm, no write permission but has
        // vaccine read permission. App can read dataSources belonging to vaccines that
        // the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(addLastDataUpdateTime(dataSource1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inBgWithoutBgPermNoWritePermBothAllergyAndVaccineReadPerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, no write permission but has
        // vaccine read permission. App can read dataSources belonging to vaccines
        // and allergy resource types that the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(),
                                        dataSource2Package1.getId(),
                                        dataSource1Package2.getId())),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getById_inForegroundOrinBgWithBgPermNoWritePermHasVaccinePerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package2,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, no write permission but has
        // vaccine read permission. App can read all dataSources belonging to vaccines.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(), dataSource2Package1.getId(),
                                        dataSource1Package2.getId(), dataSource2Package2.getId())),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByIds_inForegroundOrBgWithBgPermHasWritePermNoReadResourceTypesPerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // App can read only read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(), dataSource2Package1.getId(),
                                        dataSource1Package2.getId(), dataSource2Package2.getId())),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByIds_inForegroundOrBgWithBgPermNoWritePermHasAllergyPerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package2,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy resource type read permission.
        // App can read only read dataSources belonging to the allergy resource types.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(), dataSource2Package1.getId(),
                                        dataSource1Package2.getId(), dataSource2Package2.getId())),
                        Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByIds_inForegroundOrBgWithBgPermNoWritePermMultipleReadPerms_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package2,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy resource type and vaccine read permissions.
        // App can read dataSources belonging to allergy and vaccine resource types.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(), dataSource2Package1.getId(),
                                        dataSource1Package2.getId(), dataSource2Package2.getId())),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByIds_inForegroundOrBgWithBgPermHasWritePermHasReadVaccinePerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithPermissionChecks(
                        toUuids(
                                List.of(
                                        dataSource1Package1.getId(), dataSource2Package1.getId(),
                                        dataSource1Package2.getId(), dataSource2Package2.getId())),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false,
                        mAppInfoHelper);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackage_appHasNotCreatedDataSourcesAndNoReadPermission_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                            Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                            /* grantedMedicalResourceTypes= */ Set.of(),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ true,
                            /* isCalledFromBgWithoutBgRead= */ false);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackage_emptyPackages_inForegroundOrBgWithBgPermHasWriteAndReadPerm_success() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The packageName set is empty so no filtering based on packageNames.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfIncluded_inForegroundOrBgWithBgPermHasWriteAndReadPerm_success() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types written by any of the given packages.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_selfNotIncluded_inForegroundOrBgWithBgPermHasWriteAndReadPerm_success() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package2,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The app's package name is not included in the list of packages.
        // App can read dataSources belonging to
        // vaccine resource types written by any of the given packages.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inBgWithoutBgPermHasWritePerm_canReadSelfDataSources() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The given packageNames is empty so no filter is applied.
        // App can read dataSources they wrote themself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfIncluded_inBgWithoutBgPermHasWritePerm_canReadSelfDataSources() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfNotIncluded_inBgWithoutBgPermHasWritePerm_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The app's package name is not included in the list of packages.
        // App can read no dataSources.
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                            Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                            /* grantedMedicalResourceTypes= */ Set.of(),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ true,
                            /* isCalledFromBgWithoutBgRead= */ true);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inBgWithoutBgPermHasWriteAndReadPerm_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, has write permission and
        // has read vaccine permission.
        // The packageNames is empty so no filtering is applied.
        // App can read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfIncluded_inBgWithoutBgPermHasWriteAndReadPerm_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, has write permission and
        // has read vaccine permission.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfNotIncluded_inBgWithoutBgPermHasWriteAndReadPerm_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in background without background read perm, has write permission and
        // has read vaccine permission.
        // The app's package name is not included in the list of packages.
        // App can read not dataSources.
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                            Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                            Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ true,
                            /* isCalledFromBgWithoutBgRead= */ true);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inBgWithoutBgPermHasReadPermOnly_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, has no write permission and
        // has read vaccine permission.
        // The packageNames is empty so no filtering based on packageNames.
        // App can read dataSources belonging to vaccines the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfIncluded_inBgWithoutBgPermHasReadPermOnly_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in background without background read perm, has no write permission and
        // has read vaccine permission.
        // The app's package name is included in the list of packages.
        // App can read dataSources belonging to vaccines the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfNotIncluded_inBgWithoutBgPermHasReadPermOnly_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in background without background read perm, has no write permission and
        // has read vaccine permission.
        // The app's package name is not included in the list of packages.
        // App can read no dataSources.
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                            Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                            Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ false,
                            /* isCalledFromBgWithoutBgRead= */ true);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inBgWithoutBgPermHasMultipleReadPerms_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, no write permission but has
        // vaccine and allergy read permission.
        // PackageNames is empty so no filtering based on packageNames is applied.
        // App can read dataSources belonging to
        // vaccines and allergy resource types that the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfIncluded_inBgWithoutBgPermHasMultipleReadPerms_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in background without background read perm, no write permission but has
        // vaccine and allergy read permission. App can read dataSources belonging to
        // vaccines and allergy resource types that the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_emptyPackages_inForegroundOrBgWithBgPermOnlyWritePerm_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_selfIncluded_inForegroundOrBgWithBgPermOnlyWritePerm_canReadSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // App package name is included in the set of given packageNames.
        // App can read only read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        /* grantedMedicalResourceTypes= */ Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_selfNotIncluded_inForegroundOrBgWithBgPermOnlyWritePerm_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // App package name is not included in the set of given packageNames.
        // App can not read any dataSources.
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                            Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                            /* grantedMedicalResourceTypes= */ Set.of(),
                            DATA_SOURCE_PACKAGE_NAME,
                            /* hasWritePermission= */ true,
                            /* isCalledFromBgWithoutBgRead= */ false);
                });
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inForegroundOrBgWithBgPermHasAllergyPerm_correct() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy resource type read permission.
        // App can read only read dataSources belonging to the allergy resource types written
        // by any apps.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_inForegroundOrBgWithBgPermHasAllergyPerm_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy resource type read permission.
        // App can read only read dataSources belonging to the allergy resource types written
        // by the provided packageNames.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_emptyPackages_inForegroundOrBgWithBgPermMultipleReadPerms_correct() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package1,
                INSTANT_NOW_PLUS_TWENTY_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy and vaccine resource types read permission.
        // The packageNames is empty so no filtering applied based on that.
        // App can read only read dataSources belonging to the allergy and vaccine
        // resource types written by any apps.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW_PLUS_TWENTY_SEC),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_inForegroundOrBgWithBgPermMultipleReadPerms_correctResult() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentAllergyMedicalResource,
                dataSource2Package2,
                INSTANT_NOW_PLUS_TWENTY_SEC);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy and vaccine resource types read permission.
        // App can read only read dataSources belonging to the allergy and vaccine
        // resource types written by the provided packageNames.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource2Package2, INSTANT_NOW_PLUS_TWENTY_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_emptyPackages_inForegroundOrBgWithBgPermHasWriteAndReadPerm_correct() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine resource type read permission.
        // The packageNames is empty so no filtering is applied based on that.
        // App can read dataSources belonging to the vaccine resource types written by any
        // apps and any other dataSources that the app wrote itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        /* packageNames= */ Set.of(),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_selfIncluded_inForegroundOrBgWithBgPermHasWriteAndReadPerm_correctRes() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package1,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine resource type read permission.
        // App itself is included in the packageNames.
        // App can read dataSources belonging to the vaccine resource types written by the
        // provided packageNames and all dataSources written by the app itself.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2Package1, INSTANT_NOW),
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getByPackages_selfNotIncluded_inForegroundOrBgWithPermHasWriteAndReadPerm_correctRes() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // Insert more data with later modified time.
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1Package2,
                INSTANT_NOW_PLUS_TEN_SEC);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine resource type read permission.
        // App itself is not included in the packageNames.
        // App can read dataSources belonging to the vaccine resource types written by the
        // provided packageNames.
        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                        Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1Package2, INSTANT_NOW_PLUS_TEN_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getMDSesByIdsWithoutPermissionChecks_multipleResourcesIns_correctDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource1, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource2, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource,
                dataSource2,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource2,
                INSTANT_NOW_PLUS_TWENTY_SEC);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2, INSTANT_NOW_PLUS_TWENTY_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getMDSesByIdsWithoutPermissionChecks_deletedResource_notCountedForDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        Instant nowMinus10Seconds = INSTANT_NOW.minusSeconds(10);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource, nowMinus10Seconds);
        MedicalResource resource2 =
                upsertResourceAtTime(
                        PhrDataFactory::createDifferentAllergyMedicalResource,
                        dataSource,
                        INSTANT_NOW);

        List<MedicalDataSource> dataSourcesBeforeDelete =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource.getId())));
        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(resource2.getId()));
        List<MedicalDataSource> dataSourcesAfterDelete =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource.getId())));

        assertThat(dataSourcesBeforeDelete)
                .containsExactly(addLastDataUpdateTime(dataSource, INSTANT_NOW));
        assertThat(dataSourcesAfterDelete)
                .containsExactly(addLastDataUpdateTime(dataSource, nowMinus10Seconds));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getMDSesByIdsWithoutPermissionChecks_noResourcesInserted_nullDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId())));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastDataUpdateTime()).isNull();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getMDSesByPackageWithoutPermissionChecks_multipleResourcesIns_correctDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource1, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createDifferentVaccineMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource1,
                INSTANT_NOW_PLUS_TEN_SEC);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource2, INSTANT_NOW);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource,
                dataSource2,
                INSTANT_NOW_PLUS_TEN_SEC);
        upsertResourceAtTime(
                PhrDataFactory::createAllergyMedicalResource,
                dataSource2,
                INSTANT_NOW_PLUS_TWENTY_SEC);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(dataSource1.getPackageName(), dataSource2.getPackageName()));

        assertThat(result)
                .containsExactly(
                        addLastDataUpdateTime(dataSource1, INSTANT_NOW_PLUS_TEN_SEC),
                        addLastDataUpdateTime(dataSource2, INSTANT_NOW_PLUS_TWENTY_SEC));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            getMDSesByPackageWithoutPermissionChecks_deletedResource_notCountedForDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        Instant nowMinus10Seconds = INSTANT_NOW.minusSeconds(10);
        upsertResourceAtTime(
                PhrDataFactory::createVaccineMedicalResource, dataSource, nowMinus10Seconds);
        MedicalResource resource2 =
                upsertResourceAtTime(
                        PhrDataFactory::createDifferentAllergyMedicalResource,
                        dataSource,
                        INSTANT_NOW);

        List<MedicalDataSource> dataSourcesBeforeDelete =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(dataSource.getPackageName()));
        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(resource2.getId()));
        List<MedicalDataSource> dataSourcesAfterDelete =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(dataSource.getPackageName()));

        assertThat(dataSourcesBeforeDelete)
                .containsExactly(addLastDataUpdateTime(dataSource, INSTANT_NOW));
        assertThat(dataSourcesAfterDelete)
                .containsExactly(addLastDataUpdateTime(dataSource, nowMinus10Seconds));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getMDSesByPackageWithoutPermissionChecks_noResourcesInserted_nullDataUpdateTime() {
        mTransactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithoutPermissionChecks(
                        Set.of(dataSource.getPackageName()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastDataUpdateTime()).isNull();
    }

    @Test
    public void getReadQueryForDataSourcesFilterOnIdsAndAppIdsAndResourceTypes_correctQuery() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> dataSourceIds = List.of(uuid1, uuid2);
        List<String> dataSourceIdsHexValues = List.of(getHexString(uuid1), getHexString(uuid2));
        Set<Long> appInfoIds = Set.of(1L);
        Set<Integer> resourceTypes = Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        List<String> groupByColumnNames =
                List.of(
                        "medical_data_source_row_id",
                        "display_name",
                        "fhir_base_uri",
                        "fhir_version",
                        "data_source_uuid",
                        "package_name");
        String expectedQuery =
                "SELECT MAX(medical_resource_table.last_modified_time) AS"
                        + " last_data_update_time,inner_query_result.last_modified_time AS"
                        + " last_data_source_update_time,"
                        + String.join(",", groupByColumnNames)
                        + " FROM ( SELECT * FROM medical_data_source_table WHERE"
                        + " medical_data_source_row_id IN (SELECT medical_data_source_row_id FROM ("
                        + " SELECT * FROM medical_data_source_table WHERE data_source_uuid IN"
                        + " ("
                        + String.join(", ", dataSourceIdsHexValues)
                        + ") AND app_info_id IN (1) ) AS inner_query_result  INNER JOIN"
                        + " medical_resource_table ON inner_query_result.medical_data_source_row_id"
                        + " = medical_resource_table.data_source_id  INNER JOIN ( SELECT * FROM"
                        + " medical_resource_indices_table WHERE medical_resource_type IN (2))"
                        + " medical_resource_indices_table ON"
                        + " medical_resource_table.medical_resource_row_id ="
                        + " medical_resource_indices_table.medical_resource_id)  ) AS"
                        + " inner_query_result  INNER JOIN application_info_table ON"
                        + " inner_query_result.app_info_id = application_info_table.row_id  LEFT"
                        + " JOIN medical_resource_table ON"
                        + " inner_query_result.medical_data_source_row_id ="
                        + " medical_resource_table.data_source_id GROUP BY "
                        + String.join(",", groupByColumnNames);

        assertThat(
                        getReadQueryForDataSourcesFilterOnIdsAndAppIdsAndResourceTypes(
                                dataSourceIds, appInfoIds, resourceTypes))
                .isEqualTo(expectedQuery);
    }

    @Test
    public void getReadQueryForDataSources_withNullResourceType_correctQuery() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> dataSourceIds = List.of(uuid1, uuid2);
        List<String> dataSourceIdsHexValues = List.of(getHexString(uuid1), getHexString(uuid2));
        Set<Long> appInfoIds = Set.of(1L);
        List<String> groupByColumnNames =
                List.of(
                        "medical_data_source_row_id",
                        "display_name",
                        "fhir_base_uri",
                        "fhir_version",
                        "data_source_uuid",
                        "package_name");
        String expectedQuery =
                "SELECT MAX(medical_resource_table.last_modified_time) AS"
                        + " last_data_update_time,inner_query_result.last_modified_time AS"
                        + " last_data_source_update_time,"
                        + String.join(",", groupByColumnNames)
                        + " FROM ( SELECT * FROM medical_data_source_table WHERE"
                        + " medical_data_source_row_id IN (SELECT medical_data_source_row_id FROM"
                        + " medical_data_source_table WHERE data_source_uuid IN"
                        + " ("
                        + String.join(", ", dataSourceIdsHexValues)
                        + ")"
                        + " AND app_info_id IN (1))  ) AS inner_query_result  INNER JOIN"
                        + " application_info_table ON inner_query_result.app_info_id ="
                        + " application_info_table.row_id  LEFT JOIN medical_resource_table ON"
                        + " inner_query_result.medical_data_source_row_id ="
                        + " medical_resource_table.data_source_id GROUP BY "
                        + String.join(",", groupByColumnNames);

        assertThat(
                        getReadQueryForDataSourcesFilterOnIdsAndAppIdsAndResourceTypes(
                                dataSourceIds, appInfoIds, null))
                .isEqualTo(expectedQuery);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackage_expectAccessLogWhenDataSourceAccessedThroughReadPerm() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The packageName set is empty so no filtering based on packageNames.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        // So access log will be created based on the app permission to access dataSources
        // belonging to vaccines.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackage_expectAccessLogWhenSelfDataAccessedThroughReadPerm() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);

        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The packageName set is empty so no filtering based on packageNames.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types. In this case the only vaccine resource type is for
        // the app itself. Since the read happened through a read permission, access log will be
        // created.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackage_expectAccessLogWhenDataSourceAccessedThroughReadPermEvenSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types written by any of the given packages.
        // Since some vaccine resource types were read, an access log will be created.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                Set.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_expectAccessLogWhenDataSourceReadThroughReadPermNoSelfData() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);
        // App is in foreground or background with background read perm, has write permission and
        // has vaccine read permissions.
        // The app's package name is not included in the list of packages.
        // App can read dataSources belonging to
        // vaccine resource types written by any of the given packages.
        // Access log is created since the app has access through read permission.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                Set.of(DIFFERENT_DATA_SOURCE_PACKAGE_NAME),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_expectNoAccessLogWhenSelfReadPerm() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // App can read dataSources they wrote themself, which in this case belong to the
        // vaccine and allergy resources written. No access log should be created.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                /* grantedMedicalResourceTypes= */ Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ true);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_inForegroundOrBgWithBgPermNoReadPermCanReadSelfDataNoAccessLog() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        // No access log should be created for this.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                /* grantedMedicalResourceTypes= */ Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_expectAccessLogWhenAppAccessesDataSourceThroughReadPerm() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1Package1 =
                mUtil.insertR4MedicalDataSource("ds/1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package1 =
                mUtil.insertR4MedicalDataSource("ds/2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1Package2 =
                mUtil.insertR4MedicalDataSource("ds/3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2Package2 =
                mUtil.insertR4MedicalDataSource("ds/4", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1Package2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2Package2);

        // App is in foreground or background with background read perm, no write permission but
        // has allergy resource type read permission.
        // App can only read dataSources belonging to the allergy resource types written
        // by any apps. Access log should be created in this case.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void getByPackages_expectNoAccessLogWhenNoDataSourcesRead() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));

        // No dataSources are inserted, so no dataSources will be returned as part of this
        // API call. Hence no access log should be created.
        mMedicalDataSourceHelper.getMedicalDataSourcesByPackageWithPermissionChecks(
                /* packageNames= */ Set.of(),
                Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        AccessLog readAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(readAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalDataSourcesByIdsWithoutPermCheck_appIdDoesNotExist_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                                UUID.randomUUID()));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithoutPermCheck_deleteDataSourceThatDoesNotExist_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                                UUID.randomUUID()));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalDataSourcesByIdsWithPermCheck_appIdDoesNotExist_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                UUID.randomUUID(), DATA_SOURCE_PACKAGE_NAME));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermCheck_deleteDataSourceBelongingToAnotherApp_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                UUID.fromString(dataSource2.getId()), DATA_SOURCE_PACKAGE_NAME));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermCheck_deleteDataSourceThatDoesNotExist_throws() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                UUID.randomUUID(), DATA_SOURCE_PACKAGE_NAME));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteDataSourcesByIdsWithoutPermissionChecks_noDeleteAccessLogs() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                UUID.fromString(dataSource1.getId()));

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .doesNotContain(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermCheck_expectAccessLogForDeletedDataSourceAndResources() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                UUID.fromString(dataSource1.getId()), /* packageName= */ DATA_SOURCE_PACKAGE_NAME);

        // In this test, we have inserted two different resource types from different packages.
        // When the calling app, calls the delete API, we expect access log to be created only
        // for the deleted dataSource belonging to the app and the medical resources associated
        // with that dataSource.
        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermCheck_dataSourceHasMultipleResources_correctAccessLogs() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                UUID.fromString(dataSource1.getId()), DATA_SOURCE_PACKAGE_NAME);

        // We have inserted two different resource types from one package and one overlapping
        // resource type from another package.
        // When the calling app, calls the delete API, we expect access log to be created for
        // the deleted dataSource belonging to the app and the medical resources associated
        // with that dataSource which in this case would be vaccine and allergy.
        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermCheck_dataSourceHasNoResources_correctAccessLogs() {
        insertApps(List.of(DATA_SOURCE_PACKAGE_NAME, DIFFERENT_DATA_SOURCE_PACKAGE_NAME));
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                UUID.fromString(dataSource1.getId()), DATA_SOURCE_PACKAGE_NAME);

        // We have two dataSources but there's no data associated with the dataSource created
        // by the calling package. When the calling app, calls the delete API, we expect access log
        // to be created for the deleted dataSource belonging to the app and the
        // medicalResourceTypes to be empty since the dataSource had no resources associated
        // with it.
        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(),
                        /* isMedicalDataSourceAccessed= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(deleteAccessLog);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithoutPermCheck_badId_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                            UUID.randomUUID());
                });
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithoutPermCheck__badId_leavesRecordsUnchanged()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource existing =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                            UUID.randomUUID());
                });

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(UUID.fromString(existing.getId())));
        assertThat(result).containsExactly(existing);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithPermCheck_oneIdWrongPackage_existingDataUnchanged()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource existing =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource different =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                UUID.fromString(existing.getId()),
                                DIFFERENT_DATA_SOURCE_PACKAGE_NAME));

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(existing.getId(), different.getId())));
        assertThat(result).containsExactly(existing, different);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithoutPermCheck_oneId_existingDataDeleted()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource existing =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        UUID existingUuid = UUID.fromString(existing.getId());

        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(existingUuid);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(existingUuid));
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithoutPermCheck_multiplePresentOneIdRequested_onlyRequestedDeleted()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                UUID.fromString(dataSource1.getId()));

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));
        assertThat(result).containsExactly(dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteWithPermCheck_multiplePresentOneIdRequested_onlyRequestedDeleted()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                UUID.fromString(dataSource1.getId()), DATA_SOURCE_PACKAGE_NAME);

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));
        assertThat(result).containsExactly(dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteWithPermCheck_multiplePresentOneIdRequestedDifferentAppId_throws()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource1 =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                createDataSource(
                        DIFFERENT_DATA_SOURCE_BASE_URI,
                        DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                        DIFFERENT_DATA_SOURCE_FHIR_VERSION,
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalDataSourceHelper.deleteMedicalDataSourceWithPermissionChecks(
                                UUID.fromString(dataSource1.getId()),
                                DIFFERENT_DATA_SOURCE_PACKAGE_NAME));

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource1.getId(), dataSource2.getId())));
        assertThat(result).containsExactly(dataSource1, dataSource2);
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    public void deleteByIdsWithoutPermCheck_removesAssociatedResource()
            throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource =
                createDataSource(
                        DATA_SOURCE_FHIR_BASE_URI,
                        DATA_SOURCE_DISPLAY_NAME,
                        DATA_SOURCE_FHIR_VERSION,
                        DATA_SOURCE_PACKAGE_NAME);
        MedicalResource medicalResource = createVaccineMedicalResource(dataSource.getId());
        UpsertMedicalResourceInternalRequest upsertRequest =
                new UpsertMedicalResourceInternalRequest()
                        .setMedicalResourceType(medicalResource.getType())
                        .setFhirResourceId(medicalResource.getFhirResource().getId())
                        .setFhirResourceType(medicalResource.getFhirResource().getType())
                        .setFhirVersion(medicalResource.getFhirVersion())
                        .setData(medicalResource.getFhirResource().getData())
                        .setDataSourceId(dataSource.getId());
        MedicalResource resource =
                mMedicalResourceHelper
                        .upsertMedicalResources(DATA_SOURCE_PACKAGE_NAME, List.of(upsertRequest))
                        .get(0);

        mMedicalDataSourceHelper.deleteMedicalDataSourceWithoutPermissionChecks(
                UUID.fromString(dataSource.getId()));

        List<MedicalDataSource> result =
                mMedicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        toUuids(List.of(dataSource.getId())));
        assertThat(result).isEmpty();
        List<MedicalResource> resourceResult =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(resource.getId()));
        assertThat(resourceResult).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void testGetAllContributorAppInfoIds_success() throws NameNotFoundException {
        setUpMocksForAppInfo(DATA_SOURCE_PACKAGE_NAME);
        setUpMocksForAppInfo(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DATA_SOURCE_FHIR_BASE_URI,
                DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION,
                DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DIFFERENT_DATA_SOURCE_BASE_URI,
                DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION,
                DATA_SOURCE_PACKAGE_NAME);
        createDataSource(
                DIFFERENT_DATA_SOURCE_BASE_URI,
                DIFFERENT_DATA_SOURCE_DISPLAY_NAME,
                DATA_SOURCE_FHIR_VERSION,
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        long appInfoId = mAppInfoHelper.getAppInfoId(DATA_SOURCE_PACKAGE_NAME);
        long differentAppInfoId = mAppInfoHelper.getAppInfoId(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        Set<Long> response = mMedicalDataSourceHelper.getAllContributorAppInfoIds();

        assertThat(response).containsExactly(appInfoId, differentAppInfoId);
    }

    private void setUpMocksForAppInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo appInfo = getApplicationInfo(packageName);
        when(mPackageManager.getApplicationInfo(eq(packageName), any())).thenReturn(appInfo);
        when(mPackageManager.getApplicationLabel(eq(appInfo))).thenReturn(packageName);
        when(mPackageManager.getApplicationIcon((ApplicationInfo) any())).thenReturn(mDrawable);
        when(mDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mDrawable.getIntrinsicWidth()).thenReturn(200);
    }

    private ApplicationInfo getApplicationInfo(String packageName) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = packageName;
        return appInfo;
    }

    /**
     * Upsert a {@link MedicalResource} using the given {@link PhrTestUtils.MedicalResourceCreator}
     * and the {@link MedicalDataSource} at the specified {@code upsertTime}.
     */
    public MedicalResource upsertResourceAtTime(
            PhrTestUtils.MedicalResourceCreator creator,
            MedicalDataSource dataSource,
            Instant upsertTime) {
        Instant currentTime = mFakeTimeSource.getInstantNow();

        mFakeTimeSource.setInstant(upsertTime);
        MedicalResource resource = mUtil.upsertResource(creator, dataSource);

        // reset the mFakeTimeSource time to what it was before.
        mFakeTimeSource.setInstant(currentTime);

        return resource;
    }

    private MedicalDataSource createDataSource(
            Uri baseUri, String displayName, FhirVersion fhirVersion, String packageName) {
        CreateMedicalDataSourceRequest request =
                new CreateMedicalDataSourceRequest.Builder(baseUri, displayName, fhirVersion)
                        .build();
        return mMedicalDataSourceHelper.createMedicalDataSource(request, packageName);
    }

    private void insertApps(List<String> packageNames) {
        for (String packageName : packageNames) {
            mTransactionTestUtils.insertApp(packageName);
        }
    }

    private static List<String> getIds(Cursor cursor) {
        ArrayList<String> result = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                UUID uuid = getCursorUUID(cursor, DATA_SOURCE_UUID_COLUMN_NAME);
                result.add(uuid.toString());
            } while (cursor.moveToNext());
        }
        return result;
    }

    /**
     * Sets the {@code lastDataUpdateTime} on the provided data source at millisecond precision.
     *
     * <p>The {@code upsertTime} is converted to millisecond precision, as this is the precision
     * that is stored in the database.
     */
    private static MedicalDataSource addLastDataUpdateTime(
            MedicalDataSource dataSource, Instant lastDataUpdateTime) {
        Instant lastDataUpdateTimeMillisPrecision =
                Instant.ofEpochMilli(lastDataUpdateTime.toEpochMilli());
        return new MedicalDataSource.Builder(dataSource)
                .setLastDataUpdateTime(lastDataUpdateTimeMillisPrecision)
                .build();
    }
}
