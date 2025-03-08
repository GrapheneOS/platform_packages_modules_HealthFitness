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

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_PACKAGE_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4B;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createDifferentVaccineMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createUpdatedAllergyMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createUpdatedVaccineMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createVaccineMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createVaccineMedicalResources;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getFhirResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;

import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.DATA_SOURCE_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_DATA_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_RESOURCE_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.FHIR_RESOURCE_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.MEDICAL_RESOURCE_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.getCreateTableRequest;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.getPrimaryColumn;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper.getReadQueryForMedicalResourceTypeToDataSourceIdsMap;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getParentColumnReference;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getTableName;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.generateMedicalResourceUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getHexString;
import static com.android.server.healthconnect.testing.storage.PhrTestUtils.ACCESS_LOG_EQUIVALENCE;
import static com.android.server.healthconnect.testing.storage.PhrTestUtils.makeUpsertRequest;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesPageRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.testing.fakes.FakeTimeSource;
import com.android.server.healthconnect.testing.storage.PhrTestUtils;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.json.JSONException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(AndroidJUnit4.class)
public class MedicalResourceHelperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final long DATA_SOURCE_ROW_ID = 1234;
    private static final String INVALID_PAGE_TOKEN = "aw==";
    private static final Instant INSTANT_NOW = Instant.now();

    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private MedicalResourceHelper mMedicalResourceHelper;
    private TransactionManager mTransactionManager;
    private AccessLogsHelper mAccessLogsHelper;
    private PhrTestUtils mUtil;
    private FakeTimeSource mFakeTimeSource;
    private UserHandle mUserHandle;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mFakeTimeSource = new FakeTimeSource(INSTANT_NOW);
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setTimeSource(mFakeTimeSource)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mMedicalResourceHelper = healthConnectInjector.getMedicalResourceHelper();
        mUtil = new PhrTestUtils(healthConnectInjector);
        mUserHandle = context.getUser();

        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        transactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
        transactionTestUtils.insertApp(DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
    }

    @Test
    public void getCreateTableRequest_correctResult() {
        List<Pair<String, String>> columnInfoMedicalResource =
                List.of(
                        Pair.create(getPrimaryColumn(), PRIMARY_AUTOINCREMENT),
                        Pair.create(FHIR_RESOURCE_TYPE_COLUMN_NAME, INTEGER_NOT_NULL),
                        Pair.create(FHIR_RESOURCE_ID_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(FHIR_DATA_COLUMN_NAME, TEXT_NOT_NULL),
                        Pair.create(DATA_SOURCE_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                        Pair.create(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER_NOT_NULL));
        List<Pair<String, String>> columnInfoMedicalResourceIndices =
                List.of(
                        Pair.create(getParentColumnReference(), INTEGER_NOT_NULL),
                        Pair.create(getMedicalResourceTypeColumnName(), INTEGER_NOT_NULL));
        CreateTableRequest childTableRequest =
                new CreateTableRequest(getTableName(), columnInfoMedicalResourceIndices)
                        .addForeignKey(
                                MEDICAL_RESOURCE_TABLE_NAME,
                                Collections.singletonList(getParentColumnReference()),
                                Collections.singletonList(getPrimaryColumn()));
        CreateTableRequest expected =
                new CreateTableRequest(MEDICAL_RESOURCE_TABLE_NAME, columnInfoMedicalResource)
                        .addForeignKey(
                                MedicalDataSourceHelper.getMainTableName(),
                                Collections.singletonList(DATA_SOURCE_ID_COLUMN_NAME),
                                Collections.singletonList(
                                        MedicalDataSourceHelper.getPrimaryColumnName()))
                        .createIndexOn(LAST_MODIFIED_TIME_COLUMN_NAME)
                        .setChildTableRequests(List.of(childTableRequest));

        CreateTableRequest result = getCreateTableRequest();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getUpsertContentValues_correctResult() {
        FhirResource fhirResource = getFhirResource();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        FHIR_VERSION_R4,
                        DATA_SOURCE_ID);

        ContentValues contentValues =
                MedicalResourceHelper.getContentValues(
                        DATA_SOURCE_ROW_ID, upsertMedicalResourceInternalRequest, INSTANT_NOW);

        assertThat(contentValues.size()).isEqualTo(5);
        assertThat(contentValues.get(FHIR_RESOURCE_TYPE_COLUMN_NAME))
                .isEqualTo(fhirResource.getType());
        assertThat(contentValues.get(DATA_SOURCE_ID_COLUMN_NAME)).isEqualTo(DATA_SOURCE_ROW_ID);
        assertThat(contentValues.get(FHIR_DATA_COLUMN_NAME)).isEqualTo(fhirResource.getData());
        assertThat(contentValues.get(LAST_MODIFIED_TIME_COLUMN_NAME))
                .isEqualTo(INSTANT_NOW.toEpochMilli());
    }

    @Test
    public void getReadTableRequest_distinctResourceTypesUsingAppIdAndDataSourceIds_correctQuery() {
        List<UUID> dataSourceIds = List.of(UUID.fromString("a6194e35-698c-4706-918f-00bf959f123b"));
        long appId = 123L;
        List<String> hexValues = StorageUtils.getListOfHexStrings(dataSourceIds);

        ReadTableRequest request =
                MedicalResourceHelper.getFilteredReadRequestForDistinctResourceTypes(
                        dataSourceIds, new HashSet<>(), appId);

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT DISTINCT medical_resource_type FROM ( SELECT * FROM"
                                + " medical_resource_table ) AS inner_query_result"
                                + "  INNER JOIN ( SELECT"
                                + " * FROM medical_data_source_table WHERE app_info_id = '"
                                + appId
                                + "'"
                                + " AND data_source_uuid IN ("
                                + String.join(", ", hexValues)
                                + ")) medical_data_source_table ON"
                                + " inner_query_result.data_source_id ="
                                + " medical_data_source_table.medical_data_source_row_id"
                                + "  INNER JOIN ( SELECT * FROM medical_resource_indices_table)"
                                + " medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id");
    }

    @Test
    public void getReadTableRequest_distinctResourceTypesUsingAppIdAndResourceTypes_correctQuery() {
        long appId = 123L;

        ReadTableRequest request =
                MedicalResourceHelper.getFilteredReadRequestForDistinctResourceTypes(
                        List.of(), Set.of(MEDICAL_RESOURCE_TYPE_VACCINES), appId);

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT DISTINCT medical_resource_type FROM ( SELECT * FROM"
                                + " medical_resource_table ) AS inner_query_result"
                                + "  INNER JOIN ( SELECT"
                                + " * FROM medical_data_source_table WHERE app_info_id = '"
                                + appId
                                + "'"
                                + ") medical_data_source_table ON"
                                + " inner_query_result.data_source_id ="
                                + " medical_data_source_table.medical_data_source_row_id"
                                + "  INNER JOIN ( SELECT * FROM medical_resource_indices_table"
                                + " WHERE medical_resource_type IN (1))"
                                + " medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id");
    }

    @Test
    public void getReadTableRequest_distinctResourceTypesUsingAppIdResourceTypesAndDataSourceIds() {
        List<UUID> dataSourceIds = List.of(UUID.fromString("a6194e35-698c-4706-918f-00bf959f123b"));
        long appId = 123L;
        List<String> hexValues = StorageUtils.getListOfHexStrings(dataSourceIds);

        ReadTableRequest request =
                MedicalResourceHelper.getFilteredReadRequestForDistinctResourceTypes(
                        dataSourceIds, Set.of(MEDICAL_RESOURCE_TYPE_VACCINES), appId);

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT DISTINCT medical_resource_type FROM ( SELECT * FROM"
                                + " medical_resource_table ) AS inner_query_result"
                                + "  INNER JOIN ( SELECT"
                                + " * FROM medical_data_source_table WHERE app_info_id = '"
                                + appId
                                + "'"
                                + " AND data_source_uuid IN ("
                                + String.join(", ", hexValues)
                                + ")) medical_data_source_table ON"
                                + " inner_query_result.data_source_id ="
                                + " medical_data_source_table.medical_data_source_row_id"
                                + "  INNER JOIN ( SELECT * FROM medical_resource_indices_table"
                                + " WHERE medical_resource_type IN (1))"
                                + " medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id");
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertAndUpdateResource_lastModifiedTimeIsUpdated() throws JSONException {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource = createVaccineMedicalResource(dataSource.getId());
        UpsertMedicalResourceInternalRequest insertRequest = makeUpsertRequest(resource);
        MedicalResource updatedResource = createUpdatedVaccineMedicalResource(dataSource.getId());
        UpsertMedicalResourceInternalRequest updateRequest = makeUpsertRequest(updatedResource);

        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(insertRequest));
        long lastModifiedTimeOriginal =
                mUtil.readLastModifiedTimestamp(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(lastModifiedTimeOriginal).isEqualTo(INSTANT_NOW.toEpochMilli());

        Instant upadatedInstant = Instant.now();
        mFakeTimeSource.setInstant(upadatedInstant);

        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(updateRequest));
        long lastModifiedTimeUpdated = mUtil.readLastModifiedTimestamp(MEDICAL_RESOURCE_TABLE_NAME);

        assertThat(lastModifiedTimeUpdated).isEqualTo(upadatedInstant.toEpochMilli());
    }

    @Test
    @EnableFlags({Flags.FLAG_PHR_READ_MEDICAL_RESOURCES_FIX_QUERY_LIMIT})
    public void getReadTableRequest_usingRequest_correctQuery() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadTableRequest readRequest =
                MedicalResourceHelper.getReadTableRequestUsingRequestFilters(
                        PhrPageTokenWrapper.from(request.toParcel()), request.getPageSize());

        // TODO(b/352546342): Explore improving the query building logic, so the query below
        // is simpler to read, for context: http://shortn/_2YCniY49K6
        assertThat(readRequest.getTableName()).isEqualTo(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(readRequest.getReadCommand())
                .isEqualTo(
                        "SELECT medical_resource_row_id,fhir_resource_type,fhir_resource_id,"
                                + "fhir_data,fhir_version,medical_resource_type,"
                                + "data_source_uuid,inner_query_result.last_modified_time"
                                + " AS medical_resource_last_modified_time FROM ( SELECT * FROM"
                                + " medical_resource_table ) AS"
                                + " inner_query_result  INNER JOIN ( SELECT * FROM"
                                + " medical_resource_indices_table WHERE medical_resource_type IN "
                                + "("
                                + MEDICAL_RESOURCE_TYPE_VACCINES
                                + ")"
                                + ") medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id  INNER JOIN"
                                + " medical_data_source_table ON inner_query_result.data_source_id"
                                + " = medical_data_source_table.medical_data_source_row_id"
                                + " ORDER BY medical_resource_row_id"
                                + " LIMIT "
                                + (DEFAULT_PAGE_SIZE + 1));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_PERSONAL_HEALTH_RECORD,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_PHR_READ_MEDICAL_RESOURCES_FIX_QUERY_LIMIT
    })
    public void getReadTableRequest_usingRequestWithDataSourceIds_correctQuery() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("id1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("id2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build();
        ReadTableRequest readRequest =
                MedicalResourceHelper.getReadTableRequestUsingRequestFilters(
                        PhrPageTokenWrapper.from(request.toParcel()), request.getPageSize());
        List<String> dataSourceIdHexValues =
                StorageUtils.toUuids(request.getDataSourceIds()).stream()
                        .map(StorageUtils::getHexString)
                        .toList();

        assertThat(readRequest.getTableName()).isEqualTo(MEDICAL_RESOURCE_TABLE_NAME);
        assertThat(readRequest.getReadCommand())
                .isEqualTo(
                        "SELECT medical_resource_row_id,fhir_resource_type,fhir_resource_id,"
                                + "fhir_data,fhir_version,medical_resource_type,data_source_uuid,"
                                + "inner_query_result.last_modified_time"
                                + " AS medical_resource_last_modified_time FROM ( SELECT * FROM"
                                + " medical_resource_table ) AS"
                                + " inner_query_result  INNER JOIN ( SELECT * FROM"
                                + " medical_resource_indices_table WHERE medical_resource_type IN "
                                + "("
                                + MEDICAL_RESOURCE_TYPE_VACCINES
                                + ")) medical_resource_indices_table ON"
                                + " inner_query_result.medical_resource_row_id ="
                                + " medical_resource_indices_table.medical_resource_id  INNER JOIN"
                                + " ( SELECT * FROM medical_data_source_table WHERE"
                                + " data_source_uuid IN ("
                                + String.join(", ", dataSourceIdHexValues)
                                + ")) medical_data_source_table ON"
                                + " inner_query_result.data_source_id ="
                                + " medical_data_source_table.medical_data_source_row_id"
                                + " ORDER BY medical_resource_row_id"
                                + " LIMIT "
                                + (DEFAULT_PAGE_SIZE + 1));
    }

    @Test
    public void getReadQuery_forMedicalResourceTypeToDataSourceIdsMap_correctQuery() {
        String readQuery = getReadQueryForMedicalResourceTypeToDataSourceIdsMap();

        assertThat(readQuery)
                .isEqualTo(
                        "SELECT medical_resource_type, "
                                + "GROUP_CONCAT(data_source_id, ',') AS data_source_id "
                                + "FROM ("
                                + "SELECT DISTINCT medical_resource_type,data_source_id "
                                + "FROM ( SELECT * FROM medical_resource_table )"
                                + " AS inner_query_result "
                                + " INNER JOIN medical_resource_indices_table"
                                + " ON inner_query_result.medical_resource_row_id"
                                + " = medical_resource_indices_table.medical_resource_id)"
                                + " GROUP BY medical_resource_type");
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readMedicalResourcesByIds_dbEmpty_returnsEmpty() {
        List<MedicalResourceId> medicalResourceIds = List.of(getMedicalResourceId());

        List<MedicalResource> resources =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        medicalResourceIds);

        assertThat(resources).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readMedicalResourcesByRequest_dbEmpty_returnsEmpty() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(request.toParcel()), request.getPageSize());

        assertThat(result.getMedicalResources()).isEmpty();
        assertThat(result.getPageToken()).isEqualTo(null);
        assertThat(result.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void upsertAndReadMedicalResourcesByRequest_MedicalResourceTypeDoesNotExist_success() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine = createVaccineMedicalResource(dataSource.getId());

        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(vaccine)));
        ReadMedicalResourcesInitialRequest readAllAllergiesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readAllAllergiesRequest.toParcel()),
                        readAllAllergiesRequest.getPageSize());

        assertThat(upsertedResources).containsExactly(vaccine);
        assertThat(result.getMedicalResources()).isEmpty();
        assertThat(result.getPageToken()).isEqualTo(null);
        assertThat(result.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void upsertMedicalResourcesSameDataSource_readMedicalResourcesByRequest_success() {
        // Upsert 3 resources in this test: vaccine, differentVaccine and allergy, all
        // with the same data source.
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine = createVaccineMedicalResource(dataSource.getId());
        MedicalResource differentVaccine =
                createDifferentVaccineMedicalResource(dataSource.getId());
        MedicalResource allergy = createAllergyMedicalResource(dataSource.getId());
        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(
                                makeUpsertRequest(vaccine),
                                makeUpsertRequest(differentVaccine),
                                makeUpsertRequest(allergy)));

        ReadMedicalResourcesInitialRequest readAllAllergiesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesInitialRequest readAllVaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesInitialRequest readVaccinesFromSameDataSourceRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource.getId())
                        .build();
        ReadMedicalResourcesInitialRequest readVaccinesFromDifferentDataSourceRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();
        ReadMedicalResourcesInternalResponse allAllergiesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readAllAllergiesRequest.toParcel()),
                        readAllAllergiesRequest.getPageSize());
        ReadMedicalResourcesInternalResponse allVaccinesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readAllVaccinesRequest.toParcel()),
                        readAllVaccinesRequest.getPageSize());
        ReadMedicalResourcesInternalResponse vaccinesFromSameDataSourceResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readVaccinesFromSameDataSourceRequest.toParcel()),
                        readVaccinesFromSameDataSourceRequest.getPageSize());
        ReadMedicalResourcesInternalResponse vaccinesFromDifferentDataSourceResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(
                                readVaccinesFromDifferentDataSourceRequest.toParcel()),
                        readVaccinesFromDifferentDataSourceRequest.getPageSize());

        assertThat(upsertedResources).containsExactly(vaccine, differentVaccine, allergy);
        assertThat(allAllergiesResult.getMedicalResources()).containsExactly(allergy);
        assertThat(allAllergiesResult.getPageToken()).isEqualTo(null);
        assertThat(allAllergiesResult.getRemainingCount()).isEqualTo(0);
        assertThat(allVaccinesResult.getMedicalResources())
                .containsExactly(vaccine, differentVaccine);
        assertThat(allVaccinesResult.getPageToken()).isEqualTo(null);
        assertThat(allVaccinesResult.getRemainingCount()).isEqualTo(0);
        assertThat(vaccinesFromSameDataSourceResult.getMedicalResources())
                .containsExactly(vaccine, differentVaccine);
        assertThat(vaccinesFromSameDataSourceResult.getPageToken()).isEqualTo(null);
        assertThat(vaccinesFromDifferentDataSourceResult.getMedicalResources()).isEmpty();
        assertThat(vaccinesFromDifferentDataSourceResult.getPageToken()).isEqualTo(null);
        assertThat(vaccinesFromDifferentDataSourceResult.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void upsertMedicalResourcesDifferentDataSources_readMedicalResourcesByRequest_success() {
        // Upsert 3 resources in this test: vaccine, differentVaccine and allergy. Among
        // which vaccine and allergy are from data source 1 and the differentVaccine is
        // from data source 2.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("id1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("id2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource differentVaccineDS2 =
                createDifferentVaccineMedicalResource(dataSource2.getId());
        MedicalResource allergyDS1 = createAllergyMedicalResource(dataSource1.getId());
        List<MedicalResource> upsertedResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(vaccineDS1), makeUpsertRequest(allergyDS1)));
        upsertedResources.addAll(
                mMedicalResourceHelper.upsertMedicalResources(
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(differentVaccineDS2))));

        ReadMedicalResourcesInitialRequest readAllAllergiesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesInitialRequest readAllVaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesInitialRequest readVaccinesFromDataSource1Request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource1.getId())
                        .build();
        ReadMedicalResourcesInitialRequest readVaccinesFromDataSource2Request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource2.getId())
                        .build();
        ReadMedicalResourcesInternalResponse allAllergiesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readAllAllergiesRequest.toParcel()),
                        readAllAllergiesRequest.getPageSize());
        ReadMedicalResourcesInternalResponse allVaccinesResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readAllVaccinesRequest.toParcel()),
                        readAllVaccinesRequest.getPageSize());
        ReadMedicalResourcesInternalResponse vaccinesFromDataSource1Result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readVaccinesFromDataSource1Request.toParcel()),
                        readVaccinesFromDataSource1Request.getPageSize());
        ReadMedicalResourcesInternalResponse vaccinesFromDataSource2Result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readVaccinesFromDataSource2Request.toParcel()),
                        readVaccinesFromDataSource2Request.getPageSize());

        assertThat(upsertedResources).containsExactly(vaccineDS1, differentVaccineDS2, allergyDS1);
        assertThat(allAllergiesResult.getMedicalResources()).containsExactly(allergyDS1);
        assertThat(allAllergiesResult.getPageToken()).isEqualTo(null);
        assertThat(allAllergiesResult.getRemainingCount()).isEqualTo(0);
        assertThat(allVaccinesResult.getMedicalResources())
                .containsExactly(vaccineDS1, differentVaccineDS2);
        assertThat(allVaccinesResult.getPageToken()).isEqualTo(null);
        assertThat(allVaccinesResult.getRemainingCount()).isEqualTo(0);
        assertThat(vaccinesFromDataSource1Result.getMedicalResources()).containsExactly(vaccineDS1);
        assertThat(vaccinesFromDataSource1Result.getPageToken()).isEqualTo(null);
        assertThat(vaccinesFromDataSource1Result.getRemainingCount()).isEqualTo(0);
        assertThat(vaccinesFromDataSource2Result.getMedicalResources())
                .containsExactly(differentVaccineDS2);
        assertThat(vaccinesFromDataSource2Result.getPageToken()).isEqualTo(null);
        assertThat(vaccinesFromDataSource2Result.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMedicalResources_dataSourceNotInserted_exceptionThrown() {
        FhirResource fhirResource = getFhirResource();
        String datasourceId = "acc6c726-b7ea-42f1-a063-e34f5b4e6247";
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        FHIR_VERSION_R4,
                        datasourceId);

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mMedicalResourceHelper.upsertMedicalResources(
                                        DATA_SOURCE_PACKAGE_NAME,
                                        List.of(upsertMedicalResourceInternalRequest)));
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Invalid data source id: "
                                + upsertMedicalResourceInternalRequest.getDataSourceId());
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMedicalResources_dataSourceBelongsToDifferentApp_exceptionThrownNoWrite() {
        MedicalDataSource ownDataSource =
                mUtil.insertR4MedicalDataSource("id1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource differentDataSource =
                mUtil.insertR4MedicalDataSource("id2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        UpsertMedicalResourceInternalRequest upsertRequestOwnDataSource =
                makeUpsertRequest(
                        getFhirResource(),
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        FHIR_VERSION_R4,
                        ownDataSource.getId());
        UpsertMedicalResourceInternalRequest upsertRequestDifferentDataSource =
                makeUpsertRequest(
                        getFhirResource(),
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        FHIR_VERSION_R4,
                        differentDataSource.getId());

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mMedicalResourceHelper.upsertMedicalResources(
                                        DATA_SOURCE_PACKAGE_NAME,
                                        List.of(
                                                upsertRequestOwnDataSource,
                                                upsertRequestDifferentDataSource)));
        assertThat(thrown)
                .hasMessageThat()
                .contains("Invalid data source id: " + differentDataSource.getId());
        assertThat(getMedicalResourcesTableRowCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void upsertMedicalResources_fhirVersionDoesNotMatchDataSource_exceptionThrown() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("id1", DATA_SOURCE_PACKAGE_NAME);
        FhirResource fhirResource = getFhirResource();
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(
                        fhirResource,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        FHIR_VERSION_R4B,
                        dataSource.getId());

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mMedicalResourceHelper.upsertMedicalResources(
                                        DATA_SOURCE_PACKAGE_NAME,
                                        List.of(upsertMedicalResourceInternalRequest)));
        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        String.format(
                                "Invalid fhir version: %s. It did not match the data source's fhir"
                                        + " version",
                                FHIR_VERSION_R4B));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readSubsetOfResourcesByIds_multipleResourcesUpserted_success() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource1 = createVaccineMedicalResource(dataSource.getId());
        MedicalResource resource2 = createAllergyMedicalResource(dataSource.getId());
        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(resource1), makeUpsertRequest(resource2)));
        List<MedicalResource> readResource1Result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(resource1.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        assertThat(readResource1Result).containsExactly(resource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMedicalResources_returnsMedicalResources() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource expectedResource = createVaccineMedicalResource(dataSource.getId());
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(expectedResource);

        List<MedicalResource> result =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, List.of(upsertMedicalResourceInternalRequest));

        assertThat(result).containsExactly(expectedResource);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertSingleMedicalResource_readSingleResource() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource expectedResource = createVaccineMedicalResource(dataSource.getId());
        UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest =
                makeUpsertRequest(expectedResource);
        List<MedicalResourceId> ids = List.of(expectedResource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, List.of(upsertMedicalResourceInternalRequest));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(ids);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).isEqualTo(upsertedMedicalResources);
        assertThat(result).containsExactly(expectedResource);
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_noReadOrWritePermissions_throws() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                                List.of(),
                                /* grantedReadMedicalResourceTypes= */ Set.of(),
                                DATA_SOURCE_PACKAGE_NAME,
                                /* hasWritePermission= */ false,
                                /* isCalledFromBgWithoutBgRead= */ false));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_inBgWithoutBgPerm_hasWritePerm_noAccessLog() {
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                ids,
                Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_inBgWithoutBgPerm_hasWritePerm_hasReadPermForResourceTypes_noAccessLog() {
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                ids,
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_inBgWithoutBgPerm_noWritePerm_vaccineReadPermOnly_noAccessLog() {
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                ids,
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_expectAccessLogsOnlyContainsNonSelfRead() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccinePackage1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource vaccinePackage2 = createVaccineMedicalResource(dataSource2.getId());
        MedicalResource allergyResourcePackage2 = createAllergyMedicalResource(dataSource2.getId());
        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(vaccinePackage1)));
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                List.of(
                        makeUpsertRequest(vaccinePackage2),
                        makeUpsertRequest(allergyResourcePackage2)));
        // Clear access logs table, so that only the access logs from read will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                List.of(
                        vaccinePackage1.getId(),
                        vaccinePackage2.getId(),
                        allergyResourcePackage2.getId()),
                Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES read permission.
        // has write permission.
        // The data that the calling app can read: vaccinePackage1 (through selfRead)
        // allergyResourcePackage2 (through read permission)
        // In this case, read access log is only created for non self read data:
        // MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_expectAccessLogsWhenAppHasNoWritePermHasReadPermButReadOnlySelfData() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccinePackage1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergyResourcePackage2 = createAllergyMedicalResource(dataSource2.getId());
        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(vaccinePackage1)));
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                List.of(makeUpsertRequest(allergyResourcePackage2)));
        // Clear access logs table, so that only the access logs from read will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccinePackage1.getId(), allergyResourcePackage2.getId()),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ false);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // no write permission.
        // The data that the calling app can read: vaccinePackage1 (through read permission)
        // In this case, read access log is created based on the intention of the app
        // even though the actual data accessed is self data: MEDICAL_RESOURCE_TYPE_VACCINES.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_expectAccessLogsWhenAppHasNoWritePermHasReadPermReadNonSelfData() {
        String dataSource =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource vaccineDifferentPackage = createVaccineMedicalResource(dataSource);
        mMedicalResourceHelper.upsertMedicalResources(
                DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                List.of(makeUpsertRequest(vaccineDifferentPackage)));
        // Clear access logs table, so that only the access logs from read will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccineDifferentPackage.getId()),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ false,
                /* isCalledFromBgWithoutBgRead= */ false);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // no write permission.
        // The data that the calling app can read: vaccine (through read permission)
        // In this case, read access log is created: MEDICAL_RESOURCE_TYPE_VACCINES.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_inForegroundOrBgWithPerm_hasReadVaccine_noResourceRead_noAccessLog() {
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                ids,
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_inForegroundOrBgWithPerm_hasWritePerm_noReadPerm_noAccessLog() {
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());
        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                ids,
                Set.of(),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        // No access log should be created since app is intending to access self data as it has
        // no read permissions.
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_expectAccessLogsWhenAppHasWritePermHasReadPermReadSelfData() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        // Clear access logs table, so that only the access logs from read will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccine.getId()),
                Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES read permission.
        // has write permission.
        // The data that the calling app can read: vaccine (through read permission)
        // In this case, read access log is created based on the intention of the app
        // even though the actual data accessed is self data: MEDICAL_RESOURCE_TYPE_VACCINES.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readById_expectAccessLogsForEachResourceTypeReadBasedOnReadPerm() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        // Clear access logs table, so that only the access logs from read will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccine.getId()),
                Set.of(
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        MEDICAL_RESOURCE_TYPE_VACCINES),
                DATA_SOURCE_PACKAGE_NAME,
                /* hasWritePermission= */ true,
                /* isCalledFromBgWithoutBgRead= */ false);

        // Testing the case where calling app:
        // is calling from foreground or background with permission.
        // has MEDICAL_RESOURCE_TYPE_VACCINES and MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
        // read permission.
        // has write permission.
        // The data that the calling app reads: vaccine (through read permission)
        // In this case, read access log is created only for: MEDICAL_RESOURCE_TYPE_VACCINES.
        // Even though the app has read permission for MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
        // the app did
        // not read any data of that type, so no access logs added for that.
        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inBgWithoutBgPerm_hasWritePerm_success() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(vaccineDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inBgWithoutBgPerm_hasWritePerm_hasReadPermForResourceTypes_success() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(vaccineDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inBgWithoutBgPerm_noWritePerm_vaccineReadPermOnly_success() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(vaccineDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inBgWithoutBgPerm_noWritePerm_bothAllergyAndVaccineReadPerm_success() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ true);

        assertThat(result).containsExactly(vaccineDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForegroundOrinBgWithBgPerm_noWritePerm_success() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DIFFERENT_DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(vaccineDatasource1, vaccineDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForeground_hasWritePerm_noReadResourceTypesPerm_canOnlyReadSelfData() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(vaccineDatasource1, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForeground_noWritePerm_readVaccinePerm_canOnlyReadVaccine() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(vaccineDatasource1, vaccineDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForeground_noWritePerm_readAllergyPerm_canOnlyReadAllergy() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result).containsExactly(allergyDatasource1, allergyDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForeground_noWritePerm_readVaccineAndAllergyPerm_canReadBoth() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ false,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(
                        vaccineDatasource1,
                        vaccineDatasource2,
                        allergyDatasource1,
                        allergyDatasource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readById_inForeground_hasWritePermAndReadVaccine_readsSelfDataAndVaccines() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource allergyDatasource1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource vaccineDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDatasource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        List<MedicalResourceId> ids =
                List.of(
                        vaccineDatasource1.getId(),
                        vaccineDatasource2.getId(),
                        allergyDatasource1.getId(),
                        allergyDatasource2.getId());

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithPermissionChecks(
                        ids,
                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* hasWritePermission= */ true,
                        /* isCalledFromBgWithoutBgRead= */ false);

        assertThat(result)
                .containsExactly(vaccineDatasource1, vaccineDatasource2, allergyDatasource1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readByRequest_isNotEnforceSelfRead_createsAccessLog() {
        ReadMedicalResourcesInitialRequest readRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                PhrPageTokenWrapper.from(readRequest.toParcel()),
                readRequest.getPageSize(),
                DATA_SOURCE_PACKAGE_NAME,
                /* enforceSelfRead= */ false);

        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_READ,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readByRequest_isEnforceSelfRead_doesNotCreateAccessLog() {
        ReadMedicalResourcesInitialRequest readRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                PhrPageTokenWrapper.from(readRequest.toParcel()),
                readRequest.getPageSize(),
                DATA_SOURCE_PACKAGE_NAME,
                /* enforceSelfRead= */ true);

        List<AccessLog> accessLogs = mAccessLogsHelper.queryAccessLogs(mUserHandle);

        assertThat(accessLogs).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readByRequest_isNotEnforceSelfRead_vaccineFilter_canReadAllVaccines() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        // In total inserts 8 resources, among which 6 are vaccines to be read in 3 pages.
        List<MedicalResource> vaccinesDataSource1 =
                mUtil.upsertResources(
                        PhrDataFactory::createVaccineMedicalResources,
                        /* numOfResources= */ 4,
                        dataSource1);
        List<MedicalResource> vaccinesDataSource2 =
                mUtil.upsertResources(
                        PhrDataFactory::createVaccineMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource2);
        List<MedicalResource> allergyDatasource1 =
                mUtil.upsertResources(
                        PhrDataFactory::createAllergyMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource1);
        List<MedicalResource> resources =
                joinLists(vaccinesDataSource1, vaccinesDataSource2, allergyDatasource1);

        ReadMedicalResourcesInitialRequest initialRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(2)
                        .build();
        PhrPageTokenWrapper initialPageTokenWrapper =
                PhrPageTokenWrapper.from(initialRequest.toParcel());
        ReadMedicalResourcesInternalResponse initialResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        initialPageTokenWrapper,
                        initialRequest.getPageSize(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* enforceSelfRead= */ false);
        String pageToken1 = initialResult.getPageToken();
        assertThat(initialResult.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken1).isNotEmpty();
        assertThat(pageToken1)
                .isEqualTo(
                        initialPageTokenWrapper.cloneWithNewLastRowId(/* lastRowId= */ 2).encode());
        assertThat(initialResult.getRemainingCount()).isEqualTo(4);

        ReadMedicalResourcesPageRequest pageRequest1 =
                new ReadMedicalResourcesPageRequest.Builder(pageToken1).setPageSize(2).build();
        PhrPageTokenWrapper pageTokenWrapper1 = PhrPageTokenWrapper.from(pageRequest1.toParcel());
        ReadMedicalResourcesInternalResponse pageResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        pageTokenWrapper1,
                        pageRequest1.getPageSize(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* enforceSelfRead= */ false);
        String pageToken2 = pageResult.getPageToken();
        assertThat(pageResult.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken2).isNotEmpty();
        assertThat(pageToken2)
                .isEqualTo(pageTokenWrapper1.cloneWithNewLastRowId(/* lastRowId= */ 4).encode());
        assertThat(pageResult.getRemainingCount()).isEqualTo(2);

        ReadMedicalResourcesPageRequest pageRequest2 =
                new ReadMedicalResourcesPageRequest.Builder(pageToken2).setPageSize(2).build();
        PhrPageTokenWrapper pageTokenWrapper2 = PhrPageTokenWrapper.from(pageRequest2.toParcel());
        ReadMedicalResourcesInternalResponse pageResult2 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        pageTokenWrapper2,
                        pageRequest2.getPageSize(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* enforceSelfRead= */ false);
        String pageToken3 = pageResult2.getPageToken();
        assertThat(pageResult2.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(4), resources.get(5)));
        assertThat(pageToken3).isNull();
        assertThat(pageResult2.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void readByRequest_enforceSelfRead_vaccineFilter_canReadOnlySelfVaccines() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        // In total inserts 8 resources, among which 4 are vaccines from data source 1 to be
        // read in 2 pages.
        List<MedicalResource> vaccinesDataSource1 =
                mUtil.upsertResources(
                        PhrDataFactory::createVaccineMedicalResources,
                        /* numOfResources= */ 4,
                        dataSource1);
        List<MedicalResource> vaccinesDataSource2 =
                mUtil.upsertResources(
                        PhrDataFactory::createVaccineMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource2);
        List<MedicalResource> allergyDatasource1 =
                mUtil.upsertResources(
                        PhrDataFactory::createAllergyMedicalResources,
                        /* numOfResources= */ 2,
                        dataSource1);
        List<MedicalResource> resources =
                joinLists(vaccinesDataSource1, vaccinesDataSource2, allergyDatasource1);

        ReadMedicalResourcesInitialRequest initialRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(2)
                        .build();
        PhrPageTokenWrapper initialPageTokenWrapper =
                PhrPageTokenWrapper.from(initialRequest.toParcel());
        ReadMedicalResourcesInternalResponse initialResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        initialPageTokenWrapper,
                        initialRequest.getPageSize(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* enforceSelfRead= */ true);
        String pageToken1 = initialResult.getPageToken();
        assertThat(initialResult.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken1).isNotEmpty();
        assertThat(pageToken1)
                .isEqualTo(
                        initialPageTokenWrapper.cloneWithNewLastRowId(/* lastRowId= */ 2).encode());
        assertThat(initialResult.getRemainingCount()).isEqualTo(2);

        ReadMedicalResourcesPageRequest pageRequest =
                new ReadMedicalResourcesPageRequest.Builder(pageToken1).setPageSize(2).build();
        PhrPageTokenWrapper pageTokenWrapper = PhrPageTokenWrapper.from(pageRequest.toParcel());
        ReadMedicalResourcesInternalResponse pageResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithPermissionChecks(
                        pageTokenWrapper,
                        pageRequest.getPageSize(),
                        DATA_SOURCE_PACKAGE_NAME,
                        /* enforceSelfRead= */ true);
        String pageToken2 = pageResult.getPageToken();
        assertThat(pageResult.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken2).isNull();
        assertThat(pageResult.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMultipleMedicalResourcesWithSameDataSource_readMultipleResources() {
        // TODO(b/351992434): Create test utilities to make these large repeated code blocks
        // clearer.
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource1 = createVaccineMedicalResource(dataSource.getId());
        MedicalResource resource2 = createAllergyMedicalResource(dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(resource1), makeUpsertRequest(resource2)));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(resource1.getId(), resource2.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(resource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMedicalResourcesOfSameType_createsAccessLog_success() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResources(
                PhrDataFactory::createVaccineMedicalResources, /* numOfResources= */ 6, dataSource);

        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_UPSERT,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMedicalResourcesOfDifferentTypes_createsAccessLog_success() {
        String dataSource = mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource vaccine = createVaccineMedicalResource(dataSource);
        MedicalResource allergy = createAllergyMedicalResource(dataSource);
        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME,
                createUpsertMedicalResourceRequests(List.of(vaccine, allergy), dataSource));

        AccessLog expected =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_UPSERT,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .contains(expected);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertAndUpdateMedicalResources_createsAccessLog_success() throws JSONException {
        String dataSource = mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME).getId();
        MedicalResource vaccine = createVaccineMedicalResource(dataSource);
        MedicalResource allergy = createAllergyMedicalResource(dataSource);
        MedicalResource updatedVaccine = createUpdatedVaccineMedicalResource(dataSource);
        // initial insert
        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME,
                createUpsertMedicalResourceRequests(List.of(vaccine, allergy), dataSource));
        // update the vaccine resource
        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME,
                createUpsertMedicalResourceRequests(List.of(updatedVaccine), dataSource));

        AccessLog insertAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_UPSERT,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);
        AccessLog updateAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_UPSERT,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsAtLeast(insertAccessLog, updateAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void insertMultipleMedicalResourcesWithDifferentDataSources_readMultipleResources() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource resource2 = createDifferentVaccineMedicalResource(dataSource2.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(resource1), makeUpsertRequest(resource2)));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(resource1.getId(), resource2.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(resource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(indicesResult)
                .containsExactly(MEDICAL_RESOURCE_TYPE_VACCINES, MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void updateSingleMedicalResource_success() throws JSONException {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource originalResource = createVaccineMedicalResource(dataSource.getId());
        MedicalResource updatedResource = createUpdatedVaccineMedicalResource(dataSource.getId());

        mMedicalResourceHelper.upsertMedicalResources(
                DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(originalResource)));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(updatedResource)));
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(originalResource.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).containsExactly(updatedResource);
        assertThat(result).isEqualTo(updatedMedicalResource);
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_updateSingleMedicalResource_success()
            throws JSONException {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource originalResource1 = createVaccineMedicalResource(dataSource.getId());
        MedicalResource updatedResource1 = createUpdatedVaccineMedicalResource(dataSource.getId());
        MedicalResource resource2 = createAllergyMedicalResource(dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(
                                makeUpsertRequest(originalResource1),
                                makeUpsertRequest(resource2)));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, List.of(makeUpsertRequest(updatedResource1)));
        List<MedicalResource> readResults =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(updatedResource1.getId(), resource2.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(readResults).containsExactly(updatedResource1, resource2);
        assertThat(upsertedMedicalResources).containsExactly(originalResource1, resource2);
        assertThat(updatedMedicalResource).containsExactly(updatedResource1);
        assertThat(indicesResult)
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_updateMultipleMedicalResources_success()
            throws JSONException {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource1 = createVaccineMedicalResource(dataSource.getId());
        MedicalResource updatedResource1 = createUpdatedVaccineMedicalResource(dataSource.getId());
        MedicalResource resource2 = createAllergyMedicalResource(dataSource.getId());
        MedicalResource updatedResource2 = createUpdatedAllergyMedicalResource(dataSource.getId());

        List<MedicalResource> insertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(makeUpsertRequest(resource1), makeUpsertRequest(resource2)));
        List<MedicalResource> updatedMedicalResource =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME,
                        List.of(
                                makeUpsertRequest(updatedResource1),
                                makeUpsertRequest(updatedResource2)));
        List<MedicalResourceId> medicalIdFilters = List.of(resource1.getId(), resource2.getId());
        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        medicalIdFilters);
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(insertedMedicalResources).containsExactly(resource1, resource2);
        assertThat(updatedMedicalResource)
                .containsExactly(updatedResource1, updatedResource2)
                .inOrder();
        assertThat(result).containsExactly(updatedResource1, updatedResource2);
        assertThat(indicesResult)
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResources_readByRequest_success() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        List<MedicalResource> resources =
                createVaccineMedicalResources(/* numOfResources= */ 6, dataSource.getId());
        List<UpsertMedicalResourceInternalRequest> upsertRequests =
                createUpsertMedicalResourceRequests(resources, dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(
                        DATA_SOURCE_PACKAGE_NAME, upsertRequests);
        assertThat(upsertedMedicalResources).containsExactlyElementsIn(resources);

        ReadMedicalResourcesInitialRequest initialRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(2)
                        .build();
        PhrPageTokenWrapper pageTokenWrapper = PhrPageTokenWrapper.from(initialRequest.toParcel());
        ReadMedicalResourcesInternalResponse initialResult =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        pageTokenWrapper, initialRequest.getPageSize());
        String pageToken1 = initialResult.getPageToken();
        assertThat(initialResult.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(0), resources.get(1)));
        assertThat(pageToken1).isNotEmpty();
        assertThat(pageToken1)
                .isEqualTo(pageTokenWrapper.cloneWithNewLastRowId(/* lastRowId= */ 2).encode());
        assertThat(initialResult.getRemainingCount()).isEqualTo(4);

        ReadMedicalResourcesPageRequest pageRequest1 =
                new ReadMedicalResourcesPageRequest.Builder(pageToken1).setPageSize(2).build();
        PhrPageTokenWrapper pageTokenWrapper1 = PhrPageTokenWrapper.from(pageRequest1.toParcel());
        ReadMedicalResourcesInternalResponse pageResult1 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        pageTokenWrapper1, pageRequest1.getPageSize());
        String pageToken2 = pageResult1.getPageToken();
        assertThat(pageResult1.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(2), resources.get(3)));
        assertThat(pageToken2).isNotEmpty();
        assertThat(pageToken2)
                .isEqualTo(pageTokenWrapper1.cloneWithNewLastRowId(/* lastRowId= */ 4).encode());
        assertThat(pageResult1.getRemainingCount()).isEqualTo(2);

        ReadMedicalResourcesPageRequest pageRequest2 =
                new ReadMedicalResourcesPageRequest.Builder(pageToken2).setPageSize(2).build();
        PhrPageTokenWrapper pageTokenWrapper2 = PhrPageTokenWrapper.from(pageRequest2.toParcel());
        ReadMedicalResourcesInternalResponse result2 =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        pageTokenWrapper2, pageRequest2.getPageSize());
        String pageToken3 = result2.getPageToken();
        assertThat(result2.getMedicalResources())
                .containsExactlyElementsIn(List.of(resources.get(4), resources.get(5)));
        assertThat(pageToken3).isNull();
        assertThat(result2.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void insertMultipleMedicalResourcesReadByRequest_pageSizeLargerThanResources_success() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        List<MedicalResource> resources =
                createVaccineMedicalResources(/* numOfResources= */ 6, dataSource.getId());
        List<UpsertMedicalResourceInternalRequest> requests =
                createUpsertMedicalResourceRequests(resources, dataSource.getId());

        List<MedicalResource> upsertedMedicalResources =
                mMedicalResourceHelper.upsertMedicalResources(DATA_SOURCE_PACKAGE_NAME, requests);
        ReadMedicalResourcesInitialRequest readRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(10)
                        .build();
        ReadMedicalResourcesInternalResponse result =
                mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                        PhrPageTokenWrapper.from(readRequest.toParcel()),
                        readRequest.getPageSize());

        assertThat(upsertedMedicalResources).containsExactlyElementsIn(resources);
        assertThat(result.getMedicalResources()).containsExactlyElementsIn(resources);
        assertThat(result.getPageToken()).isEqualTo(null);
        assertThat(result.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void readMedicalResourcedByRequest_invalidPageToken_throws() {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(INVALID_PAGE_TOKEN).build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalResourceHelper.readMedicalResourcesByRequestWithoutPermissionChecks(
                                PhrPageTokenWrapper.from(request.toParcel()),
                                request.getPageSize()));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_appIdDoesNotExist_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                                List.of(getMedicalResourceId()), "fake.package.com"));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_withPackageName_noDataDeleted_noDeleteAccessLogs() {
        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                List.of(getMedicalResourceId()), DATA_SOURCE_PACKAGE_NAME);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIdsWithoutPermissionChecks_noDeleteAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource resource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(resource1.getId()));

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIdsWithPermissionChecks_resourcesWithDifferentPackages_correctAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccinePackage1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource unknownResourcePackage2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccinePackage1.getId(), unknownResourcePackage2.getId()),
                /* packageName= */ DATA_SOURCE_PACKAGE_NAME);

        // In this test, we have inserted two different resource types from different packages.
        // When the calling app, calls the delete API, we expect access log to be created only
        // for the deleted resource type. In this case it would be: vaccinePackage1
        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByIds_withPackageName_resourcesWithSamePackages_correctAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccinePackage1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource unknownResourcePackage1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                List.of(vaccinePackage1.getId(), unknownResourcePackage1.getId()),
                /* packageName= */ DATA_SOURCE_PACKAGE_NAME);

        // In this test, we have inserted two different resource types from the same package.
        // When the calling app, calls the delete API, we expect access log to be created
        // for the deleted resource types. In this case it would be: vaccinePackage1,
        // allergyResourcePackage1
        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_noId_fails() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of()));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneIdNotPresent_succeeds() {
        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(
                        new MedicalResourceId(
                                DATA_SOURCE_ID,
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                FHIR_RESOURCE_ID_IMMUNIZATION)));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneIdPresent_succeedsDeleting() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource medicalResource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(medicalResource1.getId()));

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(medicalResource1.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        assertThat(result).isEmpty();
        assertThat(indicesResult).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneOfTwoSpecified_onlySpecifiedDeleted() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource medicalResource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        MedicalResource medicalResource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(medicalResource1.getId()));
        List<Integer> indicesResult = readEntriesInMedicalResourceIndicesTable();

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(medicalResource1.getId(), medicalResource2.getId()));
        assertThat(result).containsExactly(medicalResource2);
        assertThat(indicesResult).containsExactly(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneOfTwoSpecifiedWrongPackage_nothingDeleted() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource medicalResource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        MedicalResource medicalResource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                List.of(medicalResource1.getId()), DIFFERENT_DATA_SOURCE_PACKAGE_NAME);

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(medicalResource1.getId(), medicalResource2.getId()));
        assertThat(result).containsExactly(medicalResource1, medicalResource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_oneOfTwoSpecifiedRightPackage_oneOfTwo() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource medicalResource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        MedicalResource medicalResource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithPermissionChecks(
                List.of(medicalResource1.getId()), DATA_SOURCE_PACKAGE_NAME);

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(medicalResource1.getId(), medicalResource2.getId()));
        assertThat(result).containsExactly(medicalResource2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByIds_multipleIdsFromDifferentPackages_succeeds() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource expectedResource1Source1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource expectedResource1Source2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource expectedResource2Source1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource expectedResource2Source2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        mMedicalResourceHelper.deleteMedicalResourcesByIdsWithoutPermissionChecks(
                List.of(expectedResource1Source1.getId(), expectedResource2Source2.getId()));

        List<MedicalResource> result =
                mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                        List.of(
                                expectedResource1Source1.getId(),
                                expectedResource1Source2.getId(),
                                expectedResource2Source1.getId(),
                                expectedResource2Source2.getId()));
        assertThat(result).containsExactly(expectedResource1Source2, expectedResource2Source1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_appHasNotInsertedData_throws() {
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                                request, "fake.package.com"));
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_singleDataSource_succeeds() {
        // Create two datasources, with one resource each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource dataSource1Resource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource dataSource2Resource1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource dataSource1Resource2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);

        // Delete all of the data for just the first datasource
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that the data for data source 1 is gone, but 2 is still present
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(
                                        dataSource1Resource1.getId(),
                                        dataSource1Resource2.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(dataSource2Resource1.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_multipleDataSources_succeeds() {
        // Create three datasources, with one resource each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource3 =
                mUtil.insertR4MedicalDataSource("ds3", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource vaccineDS3 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource3);

        // Delete all of the data for the first and second datasource
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that the data for data source 1 and 2 is gone, but 3 is still present
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS1.getId(), vaccineDS2.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS3.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_singleType_succeeds() {
        // Create two data sources, with one vaccine each and one extra allergy.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccine2 =
                mUtil.upsertResource(
                        PhrDataFactory::createDifferentVaccineMedicalResource, dataSource2);
        MedicalResource allergy =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);

        // Delete all vaccines.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that only the allergy remains
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccine1.getId(), vaccine2.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(allergy.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_multipleTypes_succeeds() {
        // Create two data sources, with one allergy and one vaccine each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(
                        PhrDataFactory::createDifferentVaccineMedicalResource, dataSource2);
        MedicalResource allergyDS1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource allergyDS2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        // Delete all vaccines and allergies.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that nothing remains
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(
                                        vaccineDS1.getId(),
                                        vaccineDS2.getId(),
                                        allergyDS1.getId(),
                                        allergyDS2.getId())))
                .isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_byTypeAndDataSource_succeeds() {
        // Create two data sources, with one allergy and one vaccine each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(
                        PhrDataFactory::createDifferentVaccineMedicalResource, dataSource2);
        MedicalResource allergyDS1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        MedicalResource allergyDS2 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);

        // Delete data by vaccine and data source 1.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that only one vaccine was deleted.
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(
                                        vaccineDS2.getId(),
                                        allergyDS1.getId(),
                                        allergyDS2.getId())))
                .hasSize(3);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS1.getId())))
                .hasSize(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_byDataSourceNoData_succeeds() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithPermChecks_byTypeNoData_succeeds() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            deleteMedicalResourcesByRequestWithPermChecks_byTypeDataBelongsToOtherApp_noDelete() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that nothing was deleted
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccine.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            deleteMedicalResourcesByRequestWithPermChecks_bySourceDataBelongsToOtherApp_noDelete() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource.getId())
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that nothing was deleted
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccine.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            deleteMedicalResourcesByRequestWithPermChecks_bySourceAndTypeOtherPackage_noDelete() {
        MedicalDataSource dataSource =
                mUtil.insertR4MedicalDataSource("ds1", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccine =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                DATA_SOURCE_PACKAGE_NAME);

        // Test that nothing was deleted
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccine.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_bothSourcesFromCallingPackage_expectAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        // Both created dataSources are from the same calling app.
        // So when the calling app deletes medicalResources from both those dataSources,
        // resourceTypes for both should be included in the accessLogs.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_dataSourcesFromDifferentPackages_expectAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        // The created dataSources are from different calling apps.
        // When the first calling app tries to delete resources given both dataSources,
        // only the resources belonging to the dataSource of the calling app will
        // be deleted. So accessLogs are added only for the deleted resourceTypes which would
        // be resourceTypes belonging to dataSource1.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            deleteByRequestWithPermChecks_resourceTypesFromDifferentPackages_expectAccessLogs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        // The created resources are in data sources from different calling apps.
        // When the first calling app tries to delete resources given both resource types,
        // only the resources belonging to the calling app will be deleted. So accessLogs are added
        // only for the deleted resourceType.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_resourcesTypesFromSamePackage_logsForAccessedTypes() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_expectAccessLogsForAccessedTypesAndSources_noMatch() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource2);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource2.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_expectAccessLogsForAccessedTypesAndSources_logs() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        AccessLog deleteAccessLog =
                new AccessLog(
                        DATA_SOURCE_PACKAGE_NAME,
                        INSTANT_NOW.toEpochMilli(),
                        OPERATION_TYPE_DELETE,
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* isMedicalDataSourceAccessed= */ false);
        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle))
                .comparingElementsUsing(ACCESS_LOG_EQUIVALENCE)
                .containsExactly(deleteAccessLog);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithPermChecks_noMatchingDataSources_noAccessLogsCreated() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .build(),
                /* callingPackageName= */ DATA_SOURCE_PACKAGE_NAME);

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithoutPermChecks_withoutPackageRestriction_noAccessLogsCreated() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        // Clear access logs table, so that only the access logs from delete will be present
        mAccessLogsHelper.clearData(mTransactionManager);

        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithoutPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .build());

        assertThat(mAccessLogsHelper.queryAccessLogs(mUserHandle)).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithoutPermsCheck_singleResourceType_succeeds() {
        // Create two datasources, with one resource each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyResource =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);

        // Delete all of the data for just the first datasource
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithoutPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build());

        // Test that the data for the vaccines are gone, but the allergy is still present
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS1.getId(), vaccineDS2.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(allergyResource.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteMedicalResourcesByRequestWithoutPermsCheck_multipleSources_succeeds() {
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource3 =
                mUtil.insertR4MedicalDataSource("ds3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource vaccineDS3 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource3);
        MedicalResource allergyDS1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);

        // Delete data for data sources 1 and 2.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithoutPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build());

        // Test that only data for data source 3 remains.
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(
                                        vaccineDS1.getId(),
                                        vaccineDS2.getId(),
                                        allergyDS1.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS3.getId())))
                .hasSize(1);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void deleteByRequestWithoutPermChecks_oneResourceTypeAndDataSource_succeeds() {
        // Create two data sources, with one resource each.
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalResource vaccineDS1 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        MedicalResource vaccineDS2 =
                mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        MedicalResource allergyDS1 =
                mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);

        // Delete all of the data for just data source 1.
        mMedicalResourceHelper.deleteMedicalResourcesByRequestWithoutPermissionChecks(
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build());

        // Test that the data for the vaccines are gone, but the allergy is still present.
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(vaccineDS1.getId())))
                .hasSize(0);
        assertThat(
                        mMedicalResourceHelper.readMedicalResourcesByIdsWithoutPermissionChecks(
                                List.of(allergyDS1.getId(), vaccineDS2.getId())))
                .hasSize(2);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void testGetMedicalResourceTypeToContributingDataSourcesMap_success() {
        // Create some data sources with data: ds1 contains [vaccine, differentVaccine,
        // allergy], ds2 contains [vaccine], and ds3 contains [allergy].
        MedicalDataSource dataSource1 =
                mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource2 =
                mUtil.insertR4MedicalDataSource("ds2", DATA_SOURCE_PACKAGE_NAME);
        MedicalDataSource dataSource3 =
                mUtil.insertR4MedicalDataSource("ds3", DIFFERENT_DATA_SOURCE_PACKAGE_NAME);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createDifferentVaccineMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource2);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource1);
        mUtil.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource3);
        Instant lastDataUpdateTime =
                Instant.ofEpochMilli(mFakeTimeSource.getInstantNow().toEpochMilli());
        MedicalDataSource expectedDataSource1 =
                new MedicalDataSource.Builder(dataSource1)
                        .setLastDataUpdateTime(lastDataUpdateTime)
                        .build();
        MedicalDataSource expectedDataSource2 =
                new MedicalDataSource.Builder(dataSource2)
                        .setLastDataUpdateTime(lastDataUpdateTime)
                        .build();
        MedicalDataSource expectedDataSource3 =
                new MedicalDataSource.Builder(dataSource3)
                        .setLastDataUpdateTime(lastDataUpdateTime)
                        .build();

        Map<Integer, Set<MedicalDataSource>> response =
                mMedicalResourceHelper.getMedicalResourceTypeToContributingDataSourcesMap();

        assertThat(response).hasSize(2);
        assertThat(response.keySet())
                .containsExactly(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        assertThat(response.get(MEDICAL_RESOURCE_TYPE_VACCINES))
                .containsExactly(expectedDataSource1, expectedDataSource2);
        assertThat(response.get(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES))
                .containsExactly(expectedDataSource1, expectedDataSource3);
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void testGetMedicalResourceTypeToContributingDataSourcesMap_noDataSources_success() {
        Map<Integer, Set<MedicalDataSource>> response =
                mMedicalResourceHelper.getMedicalResourceTypeToContributingDataSourcesMap();

        assertThat(response).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE, Flags.FLAG_PERSONAL_HEALTH_RECORD})
    public void
            testGetMedicalResourceTypeToContributingDataSourcesMap_noMedicalResources_success() {
        mUtil.insertR4MedicalDataSource("ds1", DATA_SOURCE_PACKAGE_NAME);

        Map<Integer, Set<MedicalDataSource>> response =
                mMedicalResourceHelper.getMedicalResourceTypeToContributingDataSourcesMap();

        assertThat(response).isEmpty();
    }

    /**
     * Returns a UUID for the given triple {@code resourceId}, {@code resourceType} and {@code
     * dataSourceId}.
     */
    private static String makeMedicalResourceHexString(MedicalResourceId medicalResourceId) {
        return getHexString(
                generateMedicalResourceUUID(
                        medicalResourceId.getFhirResourceId(),
                        medicalResourceId.getFhirResourceType(),
                        medicalResourceId.getDataSourceId()));
    }

    private List<Integer> readEntriesInMedicalResourceIndicesTable() {
        List<Integer> medicalResourceTypes = new ArrayList<>();
        ReadTableRequest readTableRequest = new ReadTableRequest(getTableName());
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            if (cursor.moveToFirst()) {
                do {
                    medicalResourceTypes.add(
                            getCursorInt(cursor, getMedicalResourceTypeColumnName()));
                } while (cursor.moveToNext());
            }
            return medicalResourceTypes;
        }
    }

    private int getMedicalResourcesTableRowCount() {
        ReadTableRequest readTableRequest =
                new ReadTableRequest(MedicalResourceHelper.getMainTableName());
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            return cursor.getCount();
        }
    }

    /**
     * Creates a list of {@link UpsertMedicalResourceInternalRequest}s for the given list of {@link
     * MedicalResource}s and {@code dataSourceId}.
     */
    private static List<UpsertMedicalResourceInternalRequest> createUpsertMedicalResourceRequests(
            List<MedicalResource> medicalResources, String dataSourceId) {
        List<UpsertMedicalResourceInternalRequest> requests = new ArrayList<>();
        for (MedicalResource medicalResource : medicalResources) {
            FhirResource fhirResource = medicalResource.getFhirResource();
            UpsertMedicalResourceInternalRequest request =
                    new UpsertMedicalResourceInternalRequest()
                            .setMedicalResourceType(medicalResource.getType())
                            .setFhirResourceId(fhirResource.getId())
                            .setFhirResourceType(fhirResource.getType())
                            .setFhirVersion(medicalResource.getFhirVersion())
                            .setData(fhirResource.getData())
                            .setDataSourceId(dataSourceId);
            requests.add(request);
        }
        return requests;
    }

    /**
     * Returns the list of {@link AccessLog}s sorted based on the {@link AccessLog#getAccessTime()}
     * in an ascending order.
     */
    private static List<AccessLog> sortByAccessTime(List<AccessLog> accessLogs) {
        return accessLogs.stream()
                .sorted(Comparator.comparing(AccessLog::getAccessTime))
                .collect(Collectors.toList());
    }

    private static <T> List<T> joinLists(List<T>... lists) {
        return Stream.of(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
