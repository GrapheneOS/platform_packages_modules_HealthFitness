/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.healthconnect.cts.phr.apis;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_READ_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MEDICAL_RESOURCE_TYPES_LIST;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeHealthPermission;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_READ_MEDICAL_RESOURCES_FIX_QUERY_LIMIT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesPageRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesByRequestCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private PhrCtsTestUtils mUtil;

    private HealthConnectManager mManager;

    @Before
    public void setUp() throws InterruptedException {
        // To make sure we don't leave any state behind after running each test.
        revokeAllHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(), "to test specific permissions");
        revokeAllHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(), "to test specific permissions");
        TestUtils.deleteAllStagedRemoteData();
        mUtil = new PhrCtsTestUtils(TestUtils.getHealthConnectManager());
        mUtil.deleteAllMedicalData();
        mManager = TestUtils.getHealthConnectManager();
        if (TestUtils.setLowerRateLimitsForTesting(true)) {
            mUtil.mLimitsAdjustmentForTesting = 10;
        }
    }

    @After
    public void after() throws InterruptedException {
        mUtil.deleteAllMedicalData();
        TestUtils.setLowerRateLimitsForTesting(false);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_migrationInProgress_apiBlocked()
            throws InterruptedException {
        startMigrationWithShellPermissionIdentity();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mManager.readMedicalResources(request, executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);

        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_readLimitExceeded_throws()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(PhrDataFactory.getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        // Make the maximum number of calls allowed by quota
        int maximumCalls = MAX_FOREGROUND_READ_CALL_15M / mUtil.mLimitsAdjustmentForTesting;
        float remainingQuota =
                mUtil.tryAcquireCallQuotaNTimesForRead(dataSource, List.of(resource), maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);
        }

        HealthConnectException exception = receiver.assertAndGetException();
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_unknownTypeInPageRequest_throws()
            throws InterruptedException {
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        // Encode a page token according to PhrPageTokenWrapper#encode(), with medicalResourceType
        // being unknown type 0.
        String pageTokenStringWithUnknownType = "2,0,";
        Base64.Encoder encoder = Base64.getEncoder();
        String pageToken = encoder.encodeToString(pageTokenStringWithUnknownType.getBytes());
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(pageToken).build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_emptyPageTokenInPageRequest_throws()
            throws InterruptedException {
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder("").build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_invalidDataSourceIdsByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        setFieldValueUsingReflection(request, "mDataSourceIds", Set.of("invalid id"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_invalidResourceTypeByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        setFieldValueUsingReflection(request, "mMedicalResourceType", 100);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_invalidPageSizeByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder("").build();

        setFieldValueUsingReflection(request, "mPageSize", MAXIMUM_PAGE_SIZE + 1);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_nullPageTokenInPageRequestByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder("").build();

        setFieldValueUsingReflection(request, "mPageToken", null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_noData_returnsEmptyList()
            throws InterruptedException {
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        ReadMedicalResourcesResponse response = receiver.getResponse();
        assertThat(response.getMedicalResources()).isEmpty();
        assertThat(response.getNextPageToken()).isNull();
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_noDataForResourceTypeAndDataSource_ReturnsEmpty()
            throws InterruptedException {
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        ReadMedicalResourcesResponse response = receiver.getResponse();
        assertThat(response.getMedicalResources()).isEmpty();
        assertThat(response.getNextPageToken()).isNull();
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_filtersByMedicalResourceType()
            throws InterruptedException {
        // Given we have three vaccines and one Allergy in two data sources
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine3 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);

        // When we read vaccines with a page size of 2 and use the nextPageToken to read
        // remaining vaccines
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allVaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(2)
                        .build();
        mManager.readMedicalResources(
                allVaccinesRequest, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build(),
                Executors.newSingleThreadExecutor(),
                receiver2);

        // Then the response gives two pages containing vaccines only, with page token 1
        // linking to page 2
        assertThat(receiver1.getResponse().getMedicalResources())
                .containsExactly(vaccine1, vaccine2);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(1);

        assertThat(receiver2.getResponse().getMedicalResources()).containsExactly(vaccine3);
        assertThat(receiver2.getResponse().getNextPageToken()).isNull();
        assertThat(receiver2.getResponse().getRemainingCount()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_filtersByMedicalResourceTypeAndOneDataSource()
            throws InterruptedException {
        // Given we have three vaccines in two data sources
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalResource vaccine1FromDataSource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2FromDataSource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        // When we read vaccines only from data source 1 in 2 pages.
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesFromDataSource1Request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource1.getId())
                        .setPageSize(1)
                        .build();
        mManager.readMedicalResources(
                vaccinesFromDataSource1Request, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build(),
                Executors.newSingleThreadExecutor(),
                receiver2);

        // Then the response gives two pages with results filtered by vaccine and data source 1
        assertThat(receiver1.getResponse().getMedicalResources())
                .containsExactly(vaccine1FromDataSource1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(1);

        assertThat(receiver2.getResponse().getMedicalResources())
                .containsExactly(vaccine2FromDataSource1);
        assertThat(receiver2.getResponse().getNextPageToken()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_filtersByMedicalResourceTypeAndBothDataSources()
            throws InterruptedException {
        // Given we have two vaccines and one Allergy in two data sources
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalResource vaccineFromDataSource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccineFromDataSource2 =
                mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);

        // When we read vaccines only from both data sources in 2 pages
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesFromBothDataSourcesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .setPageSize(1)
                        .build();
        mManager.readMedicalResources(
                vaccinesFromBothDataSourcesRequest, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build(),
                Executors.newSingleThreadExecutor(),
                receiver2);

        // Then we receive 2 pages containing vaccines from both data sources with page token
        // 1 linking to page 2
        assertThat(receiver1.getResponse().getMedicalResources())
                .containsExactly(vaccineFromDataSource1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(1);

        assertThat(receiver2.getResponse().getMedicalResources())
                .containsExactly(vaccineFromDataSource2);
        assertThat(receiver2.getResponse().getNextPageToken()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_hasManagementPermission_succeeds()
            throws InterruptedException {
        // Given we have two vaccines in one data source
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        // When we read vaccines with MANAGE_HEALTH_DATA permission in 2 pages
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(1)
                        .build();
        runWithShellPermissionIdentity(
                () ->
                        mManager.readMedicalResources(
                                vaccinesRequest, Executors.newSingleThreadExecutor(), receiver1),
                MANAGE_HEALTH_DATA_PERMISSION);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () ->
                        mManager.readMedicalResources(
                                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build(),
                                Executors.newSingleThreadExecutor(),
                                receiver2),
                MANAGE_HEALTH_DATA_PERMISSION);

        // The we receive two pages containing all vaccines, with page token 1 linking to page
        // 2
        assertThat(receiver2.getResponse().getMedicalResources()).containsExactly(vaccine2);
        assertThat(receiver2.getResponse().getNextPageToken()).isNull();

        assertThat(receiver1.getResponse().getMedicalResources()).containsExactly(vaccine1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(1);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_setPageSizeOnPageRequest_succeeds()
            throws InterruptedException {
        // Given we have six vaccines in three data source
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalDataSource dataSource3 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("3"));
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource3.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource3.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        // When we read all vaccines in two pages and specify a page size on the page request
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesRequestWithPageSize1 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(1)
                        .build();
        mManager.readMedicalResources(
                vaccinesRequestWithPageSize1, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        ReadMedicalResourcesPageRequest pageRequestWithPageSize2 =
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).setPageSize(2).build();
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                pageRequestWithPageSize2, Executors.newSingleThreadExecutor(), receiver2);

        // Then we receive two pages, with the correct page size
        assertThat(receiver1.getResponse().getMedicalResources()).hasSize(1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(5);

        assertThat(receiver2.getResponse().getMedicalResources()).hasSize(2);
        assertThat(receiver2.getResponse().getNextPageToken()).isNotEmpty();
        assertThat(receiver2.getResponse().getRemainingCount()).isEqualTo(3);
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PHR_READ_MEDICAL_RESOURCES_FIX_QUERY_LIMIT
    })
    public void
            testReadMedicalResourcesByRequest_moreTotalResourcesThanPageSize_returnsAllRequested()
                    throws InterruptedException {
        // Given we insert 2 Allergies followed by 2 Vaccines
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);
        mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_ALLERGY);
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        // When we read all vaccines with page size 2
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesRequestWithPageSize2 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(2)
                        .build();
        mManager.readMedicalResources(
                vaccinesRequestWithPageSize2, Executors.newSingleThreadExecutor(), receiver);

        // Then we receive all vaccines in one page
        assertThat(receiver.getResponse().getMedicalResources()).hasSize(2);
        assertThat(receiver.getResponse().getNextPageToken()).isNull();
        assertThat(receiver.getResponse().getRemainingCount()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_dataDeletedAfterInitialRequest_correctRemaining()
            throws InterruptedException {
        // Given we have three vaccines in two data source
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalResource resource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource resource2 =
                mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        MedicalResource resource3 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);

        // When we read the first vaccine and then delete all vaccines
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesRequestWithPageSize1 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(1)
                        .build();
        mManager.readMedicalResources(
                vaccinesRequestWithPageSize1, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        mUtil.deleteResources(List.of(resource1.getId(), resource2.getId(), resource3.getId()));

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build(),
                Executors.newSingleThreadExecutor(),
                receiver2);

        // Then the second page read will return no vaccines with 0 remaining
        assertThat(receiver1.getResponse().getMedicalResources()).hasSize(1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(2);

        assertThat(receiver2.getResponse().getMedicalResources()).hasSize(0);
        assertThat(receiver2.getResponse().getNextPageToken()).isNull();
        assertThat(receiver2.getResponse().getRemainingCount()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByRequest_dataAddedAfterInitialRequest_correctRemaining()
            throws InterruptedException {
        // Given we have two vaccines in two data source
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        // When we read the first vaccine and then insert two more vaccines.
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver1 =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest vaccinesRequestWithPageSize1 =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(1)
                        .build();
        mManager.readMedicalResources(
                vaccinesRequestWithPageSize1, Executors.newSingleThreadExecutor(), receiver1);

        String nextPageToken = receiver1.getResponse().getNextPageToken();
        assertThat(nextPageToken).isNotEmpty();

        mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver2 =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).setPageSize(1).build(),
                Executors.newSingleThreadExecutor(),
                receiver2);

        // Then the second page read will reflect the updated number of vaccines
        assertThat(receiver1.getResponse().getMedicalResources()).hasSize(1);
        assertThat(receiver1.getResponse().getRemainingCount()).isEqualTo(1);

        assertThat(receiver2.getResponse().getMedicalResources()).hasSize(1);
        assertThat(receiver2.getResponse().getNextPageToken()).isNotNull();
        assertThat(receiver2.getResponse().getRemainingCount()).isEqualTo(2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundWithNoPerms_throws() {
        // App has not been granted any permissions.
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                                + " android.permission.health.READ_MEDICAL_DATA_VACCINES to read"
                                + " MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundWithReadPermNoWritePerm_canReadResourcesWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app reads all vaccine resources from the foreground
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_FOREGROUND_APP.readMedicalResources(request);

        // Then it receives all vaccine resources
        assertThat(response.getMedicalResources())
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundWithReadPermNoWritePerm_throwsForResourcesWithoutReadPerms() {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                            + " android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES"
                            + " to read MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundHasWritePermNoReadPerms_canReadDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has WRITE_MEDICAL_DATA permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(
                backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // When the app reads all vaccine resources from the foreground
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_FOREGROUND_APP.readMedicalResources(request);

        // Then it only receives the vaccine resources written by itself
        assertThat(response.getMedicalResources()).containsExactly(foregroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundHasWriteAndReadPerms_canReadSelfDataAndOtherDataWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app only has WRITE_MEDICAL_DATA and READ_MEDICAL_DATA_VACCINES
        // permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        PHR_BACKGROUND_APP.upsertMedicalResource(
                backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        // When the app reads vaccine resources and allergy resources from the foreground
        ReadMedicalResourcesInitialRequest readVaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesInitialRequest readAllergiesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesResponse readVaccinesResponse =
                PHR_FOREGROUND_APP.readMedicalResources(readVaccinesRequest);
        ReadMedicalResourcesResponse readAllergiesResponse =
                PHR_FOREGROUND_APP.readMedicalResources(readAllergiesRequest);

        // Then it receives all vaccine resources, but only the allergy resources written by
        // itself
        assertThat(readVaccinesResponse.getMedicalResources())
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine);
        assertThat(readAllergiesResponse.getMedicalResources())
                .containsExactly(foregroundAppAllergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadNoOtherPerms_throws() {
        // App has background read permissions, but no other permissions.
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), READ_HEALTH_DATA_IN_BACKGROUND);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                                + " android.permission.health.READ_MEDICAL_DATA_VACCINES to read"
                                + " MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadHasReadPermNoWritePerm_canReadResourcesWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app has READ_HEALTH_DATA_IN_BACKGROUND and READ_MEDICAL_DATA_VACCINES
        // permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app reads all vaccine resources from the background
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_BACKGROUND_APP.readMedicalResources(request);

        // Then it receives all vaccine resources
        assertThat(response.getMedicalResources())
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadHasReadPermNoWritePerm_throwsForResourceWithoutReadPerms() {
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(READ_HEALTH_DATA_IN_BACKGROUND, READ_MEDICAL_DATA_VACCINES));
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                            + " android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES"
                            + " to read MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadHasWritePermNoReadPerms_canReadDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has READ_HEALTH_DATA_IN_BACKGROUND and WRITE_MEDICAL_DATA permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_HEALTH_DATA_IN_BACKGROUND));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // When the app reads all vaccine resources from the background
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_BACKGROUND_APP.readMedicalResources(request);

        // Then it only receives the vaccine resources written by itself
        assertThat(response.getMedicalResources()).containsExactly(backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void
            testRead_inBgWithBgReadHasWriteAndReadPerms_canReadSelfDataAndOtherDataWithReadPerms()
                    throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app has READ_HEALTH_DATA_IN_BACKGROUND, WRITE_MEDICAL_DATA and
        // READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        PHR_FOREGROUND_APP.upsertMedicalResource(
                foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        // When the app reads vaccine resources and allergy resources from the background
        ReadMedicalResourcesInitialRequest readVaccinesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesInitialRequest readAllergiesRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        ReadMedicalResourcesResponse readVaccinesResponse =
                PHR_BACKGROUND_APP.readMedicalResources(readVaccinesRequest);
        ReadMedicalResourcesResponse readAllergiesResponse =
                PHR_BACKGROUND_APP.readMedicalResources(readAllergiesRequest);

        // Then it receives all vaccine resources, but only the allergy resources written by
        // itself
        assertThat(readVaccinesResponse.getMedicalResources())
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine);
        assertThat(readAllergiesResponse.getMedicalResources())
                .containsExactly(backgroundAppAllergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBackgoundWithNoPerms_throws() {
        // App has not been granted any permissions.
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                                + " android.permission.health.READ_MEDICAL_DATA_VACCINES to read"
                                + " MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithoutBgReadOnlyWritePerm_canReadDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has WRITE_MEDICAL_DATA permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // When the app reads all vaccine resources from the background
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_BACKGROUND_APP.readMedicalResources(request);

        // Then it only receives the vaccine resources written by itself
        assertThat(response.getMedicalResources()).containsExactly(backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithoutBgReadOnlyReadPerm_canReadOwnDataWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // and the calling app only has READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app reads vaccine resources from the background
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        ReadMedicalResourcesResponse response = PHR_BACKGROUND_APP.readMedicalResources(request);

        // Then it receives only receives its own vaccine resources
        assertThat(response.getMedicalResources()).containsExactly(backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithoutBgReadOnlyReadPerm_throwsForResourcesWithoutReadPerms() {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.readMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                            + " android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES"
                            + " to read MedicalResource");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_readPermRemovedBeforePageRequest_throws() throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // and the calling app has READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(
                backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app reads the first vaccine, but loses read permissions before the second
        // page read
        ReadMedicalResourcesInitialRequest initialRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .setPageSize(1)
                        .build();
        ReadMedicalResourcesResponse initialResponse =
                PHR_FOREGROUND_APP.readMedicalResources(initialRequest);
        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);
        String nextPageToken = initialResponse.getNextPageToken();
        assertThat(nextPageToken).isNotNull();
        ReadMedicalResourcesPageRequest pageRequest =
                new ReadMedicalResourcesPageRequest.Builder(nextPageToken).build();

        // Then an exception is thrown on the second page read
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.readMedicalResources(pageRequest));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "Caller doesn't have"
                                + " android.permission.health.READ_MEDICAL_DATA_VACCINES"
                                + " to read MedicalResource");
    }

    // We are only testing permission mapping for one type here, because testing all permissions
    // in one test leads to presubmit test timeout. The full list of read permission mappings is
    // tested in the ReadMedicalResourcesByIdsCtsTest.
    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadPermissionMapping_permission_onlyGivesAccessToSpecificData()
            throws Exception {
        mUtil.insertSourceAndOneResourcePerPermissionCategory(PHR_BACKGROUND_APP);

        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_CONDITIONS);
        Set<Integer> notPermittedTypes = new HashSet<>(MEDICAL_RESOURCE_TYPES_LIST);
        notPermittedTypes.remove(Integer.valueOf(MEDICAL_RESOURCE_TYPE_CONDITIONS));

        assertThat(
                        PHR_FOREGROUND_APP
                                .readMedicalResources(
                                        new ReadMedicalResourcesInitialRequest.Builder(
                                                        MEDICAL_RESOURCE_TYPE_CONDITIONS)
                                                .build())
                                .getMedicalResources()
                                .get(0)
                                .getType())
                .isEqualTo(MEDICAL_RESOURCE_TYPE_CONDITIONS);
        for (int medicalResourceType : notPermittedTypes) {
            ReadMedicalResourcesInitialRequest request =
                    new ReadMedicalResourcesInitialRequest.Builder(medicalResourceType).build();
            assertThrows(
                    "Reading medicalResourceType: " + medicalResourceType,
                    HealthConnectException.class,
                    () -> PHR_FOREGROUND_APP.readMedicalResources(request));
        }
    }
}
