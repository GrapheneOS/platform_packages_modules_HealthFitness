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

package android.healthconnect.testing.cts;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermission;
import static android.healthconnect.testing.cts.PermissionUtils.revokeHealthPermission;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_CONDITION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ENCOUNTER;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_MEDICATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_OBSERVATION_LABS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_OBSERVATION_PREGNANCY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_OBSERVATION_SOCIAL_HISTORY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_OBSERVATION_VITAL_SIGNS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_PRACTITIONER;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_PROCEDURE;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_Patient;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createVaccineMedicalResources;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.google.common.base.Preconditions.checkState;

import static java.util.stream.Collectors.toSet;

import android.app.UiAutomation;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectManager;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.Iterables;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhrCtsTestUtils {
    private static final String TAG = "PhrCtsTestUtils";
    public static final int MAX_FOREGROUND_READ_CALL_15M = 2000;
    public static final int MAX_FOREGROUND_WRITE_CALL_15M = 1000;
    public static final int RECORD_SIZE_LIMIT_IN_BYTES = 1000000;
    public static final int CHUNK_SIZE_LIMIT_IN_BYTES = 5000000;
    private static final int MAX_NUMBER_OF_MEDICAL_RESOURCES_PER_INSERT_REQUEST = 20;
    private static final int MAXIMUM_PAGE_SIZE = 5000;
    public static final String PHR_BACKGROUND_APP_PKG =
            "android.healthconnect.cts.phr.testhelper.app1";
    public static final String PHR_FOREGROUND_APP_PKG =
            "android.healthconnect.cts.phr.testhelper.app2";
    public static final TestAppProxy PHR_BACKGROUND_APP =
            TestAppProxy.forPackageNameInBackground(PHR_BACKGROUND_APP_PKG);
    public static final TestAppProxy PHR_FOREGROUND_APP =
            TestAppProxy.forPackageName(PHR_FOREGROUND_APP_PKG);

    public static final Set<Integer> MEDICAL_RESOURCE_TYPES_LIST =
            Set.of(
                    MEDICAL_RESOURCE_TYPE_VACCINES,
                    MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                    MEDICAL_RESOURCE_TYPE_CONDITIONS,
                    MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                    MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS,
                    MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS,
                    MEDICAL_RESOURCE_TYPE_VISITS,
                    MEDICAL_RESOURCE_TYPE_PROCEDURES,
                    MEDICAL_RESOURCE_TYPE_PREGNANCY,
                    MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY,
                    MEDICAL_RESOURCE_TYPE_VITAL_SIGNS,
                    MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS);

    public int mLimitsAdjustmentForTesting = 1;
    private final HealthConnectManager mManager;

    public PhrCtsTestUtils(HealthConnectManager manager) {
        mManager = manager;
    }

    /**
     * Makes a call to {@link HealthConnectManager#createMedicalDataSource} and returns the created
     * data source.
     */
    public MedicalDataSource createDataSource(CreateMedicalDataSourceRequest createRequest)
            throws InterruptedException {
        Log.d(TAG, TAG + "#createDataSource(CreateMedicalDataSourceRequest createRequest)");
        HealthConnectReceiver<MedicalDataSource> createReceiver =
                new HealthConnectReceiver<>(
                        TAG + "#createDataSource(CreateMedicalDataSourceRequest createRequest)");
        mManager.createMedicalDataSource(
                createRequest, Executors.newSingleThreadExecutor(), createReceiver);
        return createReceiver.getResponse();
    }

    /**
     * Makes a call to {@link HealthConnectManager#getMedicalDataSources(List, Executor,
     * OutcomeReceiver)}.
     */
    public List<MedicalDataSource> getMedicalDataSourcesByIds(List<String> ids)
            throws InterruptedException {
        Log.d(TAG, TAG + "#getMedicalDataSourcesByIds(List<String> ids)");
        HealthConnectReceiver<List<MedicalDataSource>> createReceiver =
                new HealthConnectReceiver<>(TAG + "#getMedicalDataSourcesByIds(List<String> ids)");
        mManager.getMedicalDataSources(ids, Executors.newSingleThreadExecutor(), createReceiver);
        return createReceiver.getResponse();
    }

    /**
     * Makes a call to {@link
     * HealthConnectManager#getMedicalDataSources(GetMedicalDataSourcesRequest, Executor,
     * OutcomeReceiver)}.
     */
    public List<MedicalDataSource> getMedicalDataSourcesByRequest(
            GetMedicalDataSourcesRequest request) throws InterruptedException {
        Log.d(TAG, TAG + "#getMedicalDataSourcesByRequest(GetMedicalDataSourcesRequest request)");
        HealthConnectReceiver<List<MedicalDataSource>> createReceiver =
                new HealthConnectReceiver<>(
                        TAG
                                + "#getMedicalDataSourcesByRequest(GetMedicalDataSourcesRequest"
                                + " request)");
        mManager.getMedicalDataSources(
                request, Executors.newSingleThreadExecutor(), createReceiver);
        return createReceiver.getResponse();
    }

    /**
     * Given a {@code dataSourceId} and {@code numOfResources}, it inserts as many vaccine medical
     * resources as specified.
     */
    public List<MedicalResource> upsertVaccineMedicalResources(
            String dataSourceId, int numOfResources) throws InterruptedException {
        Log.d(TAG, TAG + "#upsertVaccineMedicalResources(String dataSourceId, int numOfResources)");
        List<MedicalResource> medicalResources =
                createVaccineMedicalResources(numOfResources, dataSourceId);
        return upsertMedicalResources(medicalResources);
    }

    /** Upsert the given medical resources. The data sources must already been inserted. */
    public List<MedicalResource> upsertMedicalResources(List<MedicalResource> medicalResources)
            throws InterruptedException {
        int numOfResources = medicalResources.size();
        // To avoid hitting transaction limit:
        List<MedicalResource> result = new ArrayList<>();
        for (int chunk = 0;
                chunk <= numOfResources / MAX_NUMBER_OF_MEDICAL_RESOURCES_PER_INSERT_REQUEST;
                chunk++) {
            List<UpsertMedicalResourceRequest> requests = new ArrayList<>();
            HealthConnectReceiver<List<MedicalResource>> dataReceiver =
                    new HealthConnectReceiver<>(
                            TAG + "#upsertMedicalData(List<MedicalResource> medicalResources)");
            for (int indexWithinChunk = 0;
                    indexWithinChunk < MAX_NUMBER_OF_MEDICAL_RESOURCES_PER_INSERT_REQUEST;
                    indexWithinChunk++) {
                int index =
                        chunk * MAX_NUMBER_OF_MEDICAL_RESOURCES_PER_INSERT_REQUEST
                                + indexWithinChunk;
                if (index >= numOfResources) {
                    break;
                }
                MedicalResource medicalResource = medicalResources.get(index);
                UpsertMedicalResourceRequest request =
                        new UpsertMedicalResourceRequest.Builder(
                                        medicalResource.getDataSourceId(),
                                        medicalResource.getFhirVersion(),
                                        medicalResource.getFhirResource().getData())
                                .build();
                requests.add(request);
            }
            mManager.upsertMedicalResources(
                    requests, Executors.newSingleThreadExecutor(), dataReceiver);
            result.addAll(dataReceiver.getResponse());
        }
        return result;
    }

    /**
     * Makes a call to {@link HealthConnectManager#upsertMedicalResources} and returns the upserted
     * medical resource.
     */
    public MedicalResource upsertMedicalData(String dataSourceId, String data)
            throws InterruptedException {
        Log.d(TAG, TAG + "#upsertMedicalData(String dataSourceId, String data)");
        HealthConnectReceiver<List<MedicalResource>> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG + "#upsertMedicalData(String dataSourceId, String data)");
        UpsertMedicalResourceRequest request =
                new UpsertMedicalResourceRequest.Builder(dataSourceId, FHIR_VERSION_R4, data)
                        .build();
        mManager.upsertMedicalResources(
                List.of(request), Executors.newSingleThreadExecutor(), dataReceiver);
        // Make sure something got inserted.
        return Iterables.getOnlyElement(dataReceiver.getResponse());
    }

    /** Makes a call to {@link HealthConnectManager#deleteMedicalResources}. */
    public void deleteResources(List<MedicalResourceId> resourceIds) throws InterruptedException {
        Log.d(TAG, TAG + "#deleteResources(List<MedicalResourceId> resourceIds)");
        HealthConnectReceiver<Void> deleteReceiver =
                new HealthConnectReceiver<>(
                        TAG + "#deleteResources(List<MedicalResourceId> resourceIds)");
        mManager.deleteMedicalResources(
                resourceIds, Executors.newSingleThreadExecutor(), deleteReceiver);
        deleteReceiver.verifyNoExceptionOrThrow();
    }

    /**
     * A utility method to call {@link HealthConnectManager#readMedicalResources(List, Executor,
     * OutcomeReceiver)}.
     */
    public List<MedicalResource> readMedicalResourcesByIds(List<MedicalResourceId> ids)
            throws InterruptedException {
        Log.d(TAG, TAG + "#readMedicalResourcesByIds(List<MedicalResourceId> ids)");
        HealthConnectReceiver<List<MedicalResource>> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG + "#readMedicalResourcesByIds(List<MedicalResourceId> ids)");
        mManager.readMedicalResources(ids, Executors.newSingleThreadExecutor(), dataReceiver);
        return dataReceiver.getResponse();
    }

    /**
     * A utility method to call {@link
     * HealthConnectManager#readMedicalResources(ReadMedicalResourcesRequest, Executor,
     * OutcomeReceiver)}.
     */
    public ReadMedicalResourcesResponse readMedicalResourcesByRequest(
            ReadMedicalResourcesRequest request) throws InterruptedException {
        Log.d(TAG, TAG + "#readMedicalResourcesByRequest(ReadMedicalResourcesRequest request)");
        HealthConnectReceiver<ReadMedicalResourcesResponse> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG
                                + "#readMedicalResourcesByRequest(ReadMedicalResourcesRequest"
                                + " request)");
        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), dataReceiver);
        return dataReceiver.getResponse();
    }

    /**
     * A utility method to call {@link HealthConnectManager#deleteMedicalResources(List, Executor,
     * OutcomeReceiver)}.
     */
    public void deleteMedicalResourcesByIds(List<MedicalResourceId> ids)
            throws InterruptedException {
        Log.d(TAG, TAG + "#deleteMedicalResourcesByIds(List<MedicalResourceId> ids)");
        HealthConnectReceiver<Void> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG + "#deleteMedicalResourcesByIds(List<MedicalResourceId> ids)");
        mManager.deleteMedicalResources(ids, Executors.newSingleThreadExecutor(), dataReceiver);
        dataReceiver.getResponse();
    }

    /**
     * A utility method to call {@link
     * HealthConnectManager#deleteMedicalResources(DeleteMedicalResourcesRequest, Executor,
     * OutcomeReceiver)}.
     */
    public void deleteMedicalResourcesByRequest(DeleteMedicalResourcesRequest request)
            throws InterruptedException {
        Log.d(TAG, TAG + "#deleteMedicalResourcesByRequest(DeleteMedicalResourcesRequest request)");
        HealthConnectReceiver<Void> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG
                                + "#deleteMedicalResourcesByRequest(DeleteMedicalResourcesRequest"
                                + " request)");
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), dataReceiver);
        dataReceiver.getResponse();
    }

    /**
     * A utility method to call {@link
     * HealthConnectManager#deleteMedicalResources(DeleteMedicalResourcesRequest, Executor,
     * OutcomeReceiver)}.
     */
    public void readMedicalResourcesByRequest(DeleteMedicalResourcesRequest request)
            throws InterruptedException {
        Log.d(TAG, TAG + "#deleteMedicalResourcesByRequest(DeleteMedicalResourcesRequest request)");
        HealthConnectReceiver<Void> dataReceiver =
                new HealthConnectReceiver<>(
                        TAG
                                + "#deleteMedicalResourcesByRequest(DeleteMedicalResourcesRequest"
                                + " request)");
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), dataReceiver);
        dataReceiver.getResponse();
    }

    /**
     * Delete all health records (data sources, resources etc) stored in the Health Connect
     * database.
     */
    public void deleteAllMedicalData() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<List<MedicalDataSource>> receiver =
                    new HealthConnectReceiver<>(TAG + "#getMedicalDataSources");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            mManager.getMedicalDataSources(
                    new GetMedicalDataSourcesRequest.Builder().build(), executor, receiver);
            List<MedicalDataSource> dataSources = receiver.getResponse();
            for (MedicalDataSource dataSource : dataSources) {
                HealthConnectReceiver<Void> callback =
                        new HealthConnectReceiver<>(TAG + "#deleteMedicalDataSourceWithData");
                mManager.deleteMedicalDataSourceWithData(dataSource.getId(), executor, callback);
                callback.verifyNoExceptionOrThrow();
            }
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    /**
     * Given a list of {@link MedicalResource}s, reads out the resources using the {@link
     * MedicalResourceId}s. It splits the resources to fit the maximum page size limit.
     */
    public List<MedicalResource> readMedicalResources(List<MedicalResource> medicalResources)
            throws InterruptedException {
        Log.d(TAG, TAG + "#readMedicalResources(List<MedicalResource> medicalResources)");
        List<MedicalResourceId> ids =
                medicalResources.stream()
                        .map(
                                medicalResource ->
                                        new MedicalResourceId(
                                                medicalResource.getDataSourceId(),
                                                medicalResource.getFhirResource().getType(),
                                                medicalResource.getFhirResource().getId()))
                        .toList();

        List<MedicalResource> result = new ArrayList<>();
        for (int chunk = 0; chunk <= ids.size() / MAXIMUM_PAGE_SIZE; chunk++) {
            List<MedicalResourceId> resourceIds = new ArrayList<>();
            HealthConnectReceiver<List<MedicalResource>> dataReceiver =
                    new HealthConnectReceiver<>(
                            TAG
                                    + "#readMedicalResources(List<MedicalResource>"
                                    + " medicalResources) - chunk #"
                                    + chunk);
            for (int indexWithinChunk = 0;
                    indexWithinChunk < MAXIMUM_PAGE_SIZE;
                    indexWithinChunk++) {
                int index = chunk * MAXIMUM_PAGE_SIZE + indexWithinChunk;
                if (index >= ids.size()) {
                    break;
                }

                resourceIds.add(ids.get(index));
            }
            mManager.readMedicalResources(
                    resourceIds, Executors.newSingleThreadExecutor(), dataReceiver);
            result.addAll(dataReceiver.getResponse());
        }
        return result;
    }

    /**
     * Given a {@code dataSourceId} deletes the {@link MedicalDataSource} and all its associated
     * {@link MedicalResource}s.
     */
    public void deleteMedicalDataSourceWithData(String dataSourceId) throws InterruptedException {
        Log.d(TAG, TAG + "#deleteMedicalDataSourceWithData(String dataSourceId)");
        HealthConnectReceiver<Void> callback =
                new HealthConnectReceiver<>(TAG + "#deleteMedicalDataSourceWithData");
        mManager.deleteMedicalDataSourceWithData(
                dataSourceId, Executors.newSingleThreadExecutor(), callback);
        callback.verifyNoExceptionOrThrow();
    }

    /**
     * Inserts a data source with one resource for each permission category and returns the ids of
     * inserted resources.
     */
    public List<MedicalResourceId> insertSourceAndOneResourcePerPermissionCategory(
            TestAppProxy appProxy) throws Exception {
        Log.d(TAG, TAG + "#insertSourceAndOneResourcePerPermissionCategory(TestAppProxy appProxy)");
        grantHealthPermission(appProxy.getPackageName(), WRITE_MEDICAL_DATA);
        String dataSourceId =
                appProxy.createMedicalDataSource(getCreateMedicalDataSourceRequest()).getId();
        List<MedicalResource> insertedMedicalResources =
                List.of(
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_IMMUNIZATION),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_ALLERGY),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_CONDITION),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_MEDICATION),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_Patient),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_PRACTITIONER),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_ENCOUNTER),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_PROCEDURE),
                        appProxy.upsertMedicalResource(
                                dataSourceId, FHIR_DATA_OBSERVATION_PREGNANCY),
                        appProxy.upsertMedicalResource(
                                dataSourceId, FHIR_DATA_OBSERVATION_SOCIAL_HISTORY),
                        appProxy.upsertMedicalResource(
                                dataSourceId, FHIR_DATA_OBSERVATION_VITAL_SIGNS),
                        appProxy.upsertMedicalResource(dataSourceId, FHIR_DATA_OBSERVATION_LABS));
        revokeHealthPermission(appProxy.getPackageName(), WRITE_MEDICAL_DATA);

        checkState(
                insertedMedicalResources.stream()
                        .map(MedicalResource::getType)
                        .collect(toSet())
                        .equals(MEDICAL_RESOURCE_TYPES_LIST));

        return insertedMedicalResources.stream().map(MedicalResource::getId).toList();
    }

    /**
     * This method tries to use the specified quota by calling readMedicalResources and
     * getMedicalResources APIs.
     *
     * @return the available quota that may have accumulated during the read.
     */
    public float tryAcquireCallQuotaNTimesForRead(
            MedicalDataSource insertedMedicalDataSource,
            List<MedicalResource> insertedMedicalResources,
            int nTimes)
            throws InterruptedException {
        int readMedicalDataSourceCalls = nTimes / 2;
        int readMedicalResourceCalls = nTimes - readMedicalDataSourceCalls;
        List<MedicalResourceId> medicalResourceIds =
                insertedMedicalResources.stream().map(MedicalResource::getId).toList();

        Instant readStartTime = Instant.now();
        for (int i = 0; i < readMedicalDataSourceCalls; i++) {
            getMedicalDataSourcesByIds(List.of(insertedMedicalDataSource.getId()));
        }
        for (int i = 0; i < readMedicalResourceCalls; i++) {
            readMedicalResourcesByIds(medicalResourceIds);
        }
        Instant readEndTime = Instant.now();

        return getAvailableQuotaAccumulated(
                readStartTime, readEndTime, Duration.ofMinutes(15), MAX_FOREGROUND_READ_CALL_15M);
    }

    /**
     * This method tries to use the specified quota by calling the upsertMedicalResources API.
     *
     * @return the available quota that may have accumulated during the write.
     */
    public float tryAcquireCallQuotaNTimesForWrite(
            MedicalDataSource insertedMedicalDataSource, int nTimes) throws InterruptedException {
        String dataSourceId = insertedMedicalDataSource.getId();

        Instant readStartTime = Instant.now();
        for (int i = 0; i < nTimes; i++) {
            upsertMedicalData(dataSourceId, FHIR_DATA_IMMUNIZATION);
        }
        Instant readEndTime = Instant.now();

        return getAvailableQuotaAccumulated(
                readStartTime, readEndTime, Duration.ofMinutes(15), MAX_FOREGROUND_WRITE_CALL_15M);
    }

    /**
     * Returns the quota that would have accumulated between start and end time for the specified
     * window and max quota. The calculation matches the calculation in
     * RateLimiter#getAvailableQuota.
     */
    private float getAvailableQuotaAccumulated(
            Instant startTime, Instant endTime, Duration window, int maxQuota) {
        Duration timeSpent = Duration.between(startTime, endTime);
        return timeSpent.toMillis() * ((float) maxQuota / (float) window.toMillis());
    }
}
