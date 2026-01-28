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

package android.health.connect;

import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResourceId;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IDeviceDataSourceCapabilitiesCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetMatchingDataSourcesCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IIsMatchmakingPossibleCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.healthconnect.testing.shared.phr.PhrDataFactory;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {

    public static final String PACKAGE_TO_MATCH = "package.to.match";
    public static final String MATCHING_PACKAGE = "matching.package";
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock IHealthConnectService mService;

    @Test
    public void testDeviceDataSourceCapabilitiesSets() {
        Set<Class<? extends Record>> sensitiveCapabilities =
                HealthConnectManager.getPermissionSensitiveDeviceDataSourceCapabilities();

        assertThat(sensitiveCapabilities)
                .containsExactly(
                        BasalBodyTemperatureRecord.class,
                        OvulationTestRecord.class,
                        BloodGlucoseRecord.class);
    }

    @Test
    public void testHealthConnectManager_getNoGrantedHealthPermissions_succeeds() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);

        List<String> grantedHealthPermissions =
                healthConnectManager.getGrantedHealthPermissions("com.foo.bar");

        assertThat(grantedHealthPermissions).isEmpty();
    }

    @Test
    public void testHealthConnectManager_getSomeGrantedHealthPermissions_succeeds()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.getGrantedHealthPermissions(any(), any()))
                .thenReturn(
                        ImmutableList.of(
                                "android.permission.health.READ_HEART_RATE",
                                "android.permission.health.WRITE_HEART_RATE"));

        List<String> grantedHealthPermissions =
                healthConnectManager.getGrantedHealthPermissions("com.foo.bar");

        assertThat(grantedHealthPermissions)
                .containsExactly(
                        "android.permission.health.READ_HEART_RATE",
                        "android.permission.health.WRITE_HEART_RATE");
    }

    @Test
    public void testHealthConnectManager_getGrantedHealthPermissionsException_rethrows()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.getGrantedHealthPermissions(any(), any()))
                .thenThrow(new RemoteException("message"));

        assertThrows(
                RuntimeException.class,
                () -> healthConnectManager.getGrantedHealthPermissions("com.foo.bar"));
    }

    @Test
    public void testHealthConnectManager_getDatasourcesByIds_usesExceptionFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<List<MedicalDataSource>> receiver = new TestOutcomeReceiver<>();
        String id = "id";
        HealthConnectExceptionParcel error =
                new HealthConnectExceptionParcel(
                        new HealthConnectException(
                                HealthConnectException.ERROR_UNSUPPORTED_OPERATION));
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IMedicalDataSourcesResponseCallback callback =
                                            invocation.getArgument(2);
                                    callback.onError(error);
                                    return null;
                                })
                .when(mService)
                .getMedicalDataSourcesByIds(any(), any(), any());

        healthConnectManager.getMedicalDataSources(
                ImmutableList.of(id), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testHealthConnectManager_getDatasourcesByIds_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<List<MedicalDataSource>> receiver = new TestOutcomeReceiver<>();
        String id = "id";
        List<MedicalDataSource> response =
                List.of(PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly());
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IMedicalDataSourcesResponseCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(response);
                                    return null;
                                })
                .when(mService)
                .getMedicalDataSourcesByIds(any(), any(), any());

        healthConnectManager.getMedicalDataSources(
                ImmutableList.of(id), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).containsExactlyElementsIn(response);
    }

    @Test
    public void testHealthConnectManager_getDataSourcesByRequest_usesExceptionFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<List<MedicalDataSource>> receiver = new TestOutcomeReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        HealthConnectExceptionParcel error =
                new HealthConnectExceptionParcel(
                        new HealthConnectException(
                                HealthConnectException.ERROR_UNSUPPORTED_OPERATION));
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IMedicalDataSourcesResponseCallback callback =
                                            invocation.getArgument(2);
                                    callback.onError(error);
                                    return null;
                                })
                .when(mService)
                .getMedicalDataSourcesByRequest(any(), any(), any());

        healthConnectManager.getMedicalDataSources(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testHealthConnectManager_getDataSourcesByRequest_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<List<MedicalDataSource>> receiver = new TestOutcomeReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> response =
                List.of(PhrDataFactory.getMedicalDataSourceRequiredFieldsOnly());
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IMedicalDataSourcesResponseCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(response);
                                    return null;
                                })
                .when(mService)
                .getMedicalDataSourcesByRequest(any(), any(), any());

        healthConnectManager.getMedicalDataSources(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isEqualTo(response);
    }

    @Test
    public void testHealthConnectManager_deleteResources_usesExceptionFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .deleteMedicalResourcesByIds(any(), any(), any());

        healthConnectManager.deleteMedicalResources(
                ImmutableList.of(getMedicalResourceId()),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testHealthConnectManager_deleteResourcesByIds_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onResult();
                                    return null;
                                })
                .when(mService)
                .deleteMedicalResourcesByIds(any(), any(), any());

        healthConnectManager.deleteMedicalResources(
                ImmutableList.of(getMedicalResourceId()),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse()).isNull();
    }

    @Test
    public void testHealthConnectManager_deleteResourcesByIds_shortcutsEmptyRequest()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();

        healthConnectManager.deleteMedicalResources(
                ImmutableList.of(), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isNull();
    }

    @Test
    public void testHealthConnectManager_deleteResourcesByRequest_usesExceptionFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .deleteMedicalResourcesByRequest(any(), any(), any());
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        healthConnectManager.deleteMedicalResources(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testHealthConnectManager_deleteResourcesByRequest_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onResult();
                                    return null;
                                })
                .when(mService)
                .deleteMedicalResourcesByRequest(any(), any(), any());
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        healthConnectManager.deleteMedicalResources(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testIsMatchmakingPossible_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<MatchmakingResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IIsMatchmakingPossibleCallback callback =
                                            invocation.getArgument(2);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .isMatchmakingPossible(any(), any(), any());

        healthConnectManager.isMatchmakingPossible(
                new MatchmakingRequest.Builder().addRecordTypes(ImmutableSet.of()).build(),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testIsMatchmakingPossible_matchingApps_true() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<MatchmakingResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IIsMatchmakingPossibleCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(
                                            new MatchmakingResponse.Builder(true).build());
                                    return null;
                                })
                .when(mService)
                .isMatchmakingPossible(any(), any(), any());

        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordTypes(
                                ImmutableSet.of(StepsRecord.class, SleepSessionRecord.class))
                        .build();
        healthConnectManager.isMatchmakingPossible(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().isMatchmakingPossible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testIsMatchmakingPossible_noMatchingApps_false() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<MatchmakingResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IIsMatchmakingPossibleCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(
                                            new MatchmakingResponse.Builder(false).build());
                                    return null;
                                })
                .when(mService)
                .isMatchmakingPossible(any(), any(), any());

        MatchmakingRequest request =
                new MatchmakingRequest.Builder()
                        .addRecordTypes(
                                ImmutableSet.of(StepsRecord.class, SleepSessionRecord.class))
                        .build();
        healthConnectManager.isMatchmakingPossible(
                request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().isMatchmakingPossible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testGetMatchingDataSources_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<GetMatchingDataSourcesResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IGetMatchingDataSourcesCallback callback =
                                            invocation.getArgument(2);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .getMatchingDataSources(any(), any(), any());

        healthConnectManager.getMatchingDataSources(
                getMatchmakingRequest(ImmutableSet.of(), PACKAGE_TO_MATCH),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testGetMatchingDataSources_noMatchingDataSources_emptyMap() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<GetMatchingDataSourcesResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IGetMatchingDataSourcesCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(emptyResponse());
                                    return null;
                                })
                .when(mService)
                .getMatchingDataSources(any(), any(), any());

        healthConnectManager.getMatchingDataSources(
                getMatchmakingRequest(
                        ImmutableSet.of(StepsRecord.class, SleepSessionRecord.class),
                        PACKAGE_TO_MATCH),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse().getMatchingApps()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testGetMatchingDataSources_matchingDataSources_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<GetMatchingDataSourcesResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IGetMatchingDataSourcesCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(getMatchingDataSourcesResponse());
                                    return null;
                                })
                .when(mService)
                .getMatchingDataSources(any(), any(), any());

        healthConnectManager.getMatchingDataSources(
                getMatchmakingRequest(
                        ImmutableSet.of(StepsRecord.class, SleepSessionRecord.class),
                        PACKAGE_TO_MATCH),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse().getMatchingApps())
                .containsExactlyEntriesIn(getMatchingDataSourcesResponse().getMatchingApps());
    }

    @Test
    @EnableFlags({
        Flags.FLAG_MATCHMAKING,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void testGetMatchingDataSources_matchingDataSources_withDevices_usesResultFromService()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<GetMatchingDataSourcesResponse> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IGetMatchingDataSourcesCallback callback =
                                            invocation.getArgument(2);
                                    callback.onResult(getMatchingDataSourcesWithDevicesResponse());
                                    return null;
                                })
                .when(mService)
                .getMatchingDataSources(any(), any(), any());

        healthConnectManager.getMatchingDataSources(
                getMatchmakingRequest(
                        ImmutableSet.of(StepsRecord.class, SleepSessionRecord.class),
                        PACKAGE_TO_MATCH),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse().getMatchingApps())
                .containsExactlyEntriesIn(
                        getMatchingDataSourcesWithDevicesResponse().getMatchingApps());
        assertThat(receiver.getResponse().getMatchingDevices())
                .containsExactlyEntriesIn(
                        getMatchingDataSourcesWithDevicesResponse().getMatchingDevices());
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testRecordMatchmakingDenial_usesResultFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(3);
                                    callback.onResult();
                                    return null;
                                })
                .when(mService)
                .recordMatchmakingDenial(any(), any(), any(), any());

        healthConnectManager.recordMatchmakingDenial(
                PACKAGE_TO_MATCH,
                Map.of(MATCHING_PACKAGE, List.of(WRITE_EXERCISE)),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testRecordMatchmakingDenial_emptyPermissionsList_callsService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<Map<String, List<String>>> deniedAppsCaptor =
                ArgumentCaptor.forClass(Map.class);

        healthConnectManager.recordMatchmakingDenial(
                PACKAGE_TO_MATCH,
                Map.of(MATCHING_PACKAGE, Collections.emptyList()),
                Executors.newSingleThreadExecutor(),
                receiver);

        verify(mService)
                .recordMatchmakingDenial(
                        any(), eq(PACKAGE_TO_MATCH), deniedAppsCaptor.capture(), any());
        assertThat(deniedAppsCaptor.getValue().get(MATCHING_PACKAGE)).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testRecordMatchmakingDenial_withPermissions_callsServiceWithPermissions()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<Map<String, List<String>>> deniedAppsCaptor =
                ArgumentCaptor.forClass(Map.class);
        List<String> permissions = List.of(WRITE_STEPS, WRITE_SLEEP);

        healthConnectManager.recordMatchmakingDenial(
                PACKAGE_TO_MATCH,
                Map.of(MATCHING_PACKAGE, permissions),
                Executors.newSingleThreadExecutor(),
                receiver);

        verify(mService)
                .recordMatchmakingDenial(
                        any(), eq(PACKAGE_TO_MATCH), deniedAppsCaptor.capture(), any());
        assertThat(deniedAppsCaptor.getValue().get(MATCHING_PACKAGE))
                .containsExactlyElementsIn(permissions);
    }

    @Test
    @EnableFlags(Flags.FLAG_MATCHMAKING)
    public void testRecordMatchmakingDenial_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(3);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .recordMatchmakingDenial(any(), any(), any(), any());

        healthConnectManager.recordMatchmakingDenial(
                PACKAGE_TO_MATCH,
                Map.of(MATCHING_PACKAGE, List.of(WRITE_EXERCISE)),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void testGetDeviceDataSourceCapabilities_usesResultFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<DeviceDataSourceCapabilities> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<IDeviceDataSourceCapabilitiesCallback> callbackCaptor =
                ArgumentCaptor.forClass(IDeviceDataSourceCapabilitiesCallback.class);
        doNothing().when(mService).getDeviceDataSourceCapabilities(any(), callbackCaptor.capture());
        int stepsRecordType = RecordTypeIdentifier.RECORD_TYPE_STEPS;
        int distanceRecordType = RecordTypeIdentifier.RECORD_TYPE_DISTANCE;

        healthConnectManager.getDeviceDataSourceCapabilities(
                Executors.newSingleThreadExecutor(), receiver);
        android.health.connect.aidl.DeviceDataSourceCapabilities result =
                new android.health.connect.aidl.DeviceDataSourceCapabilities();
        result.recordTypeIds = new int[] {stepsRecordType, distanceRecordType};
        callbackCaptor.getValue().onResult(result);

        assertThat(receiver.getResponse().getRecordTypes())
                .containsExactly(StepsRecord.class, DistanceRecord.class);
    }

    @Test
    public void testGetDeviceDataSourceCapabilities_emptyCapabilities_returnsEmptySet()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<DeviceDataSourceCapabilities> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<IDeviceDataSourceCapabilitiesCallback> callbackCaptor =
                ArgumentCaptor.forClass(IDeviceDataSourceCapabilitiesCallback.class);
        doNothing().when(mService).getDeviceDataSourceCapabilities(any(), callbackCaptor.capture());

        healthConnectManager.getDeviceDataSourceCapabilities(
                Executors.newSingleThreadExecutor(), receiver);
        android.health.connect.aidl.DeviceDataSourceCapabilities result =
                new android.health.connect.aidl.DeviceDataSourceCapabilities();
        result.recordTypeIds = new int[0];
        callbackCaptor.getValue().onResult(result);

        assertThat(receiver.getResponse().getRecordTypes()).isEmpty();
    }

    @Test
    public void testGetDeviceDataSourceCapabilities_invalidRecordType_omitsValue()
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<DeviceDataSourceCapabilities> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<IDeviceDataSourceCapabilitiesCallback> callbackCaptor =
                ArgumentCaptor.forClass(IDeviceDataSourceCapabilitiesCallback.class);
        doNothing().when(mService).getDeviceDataSourceCapabilities(any(), callbackCaptor.capture());
        int recordType = RecordTypeIdentifier.RECORD_TYPE_STEPS;

        healthConnectManager.getDeviceDataSourceCapabilities(
                Executors.newSingleThreadExecutor(), receiver);
        android.health.connect.aidl.DeviceDataSourceCapabilities result =
                new android.health.connect.aidl.DeviceDataSourceCapabilities();
        result.recordTypeIds = new int[] {recordType, -1};
        callbackCaptor.getValue().onResult(result);

        assertThat(receiver.getResponse().getRecordTypes()).containsExactly(StepsRecord.class);
    }

    @Test
    public void testGetDeviceDataSourceCapabilities_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<DeviceDataSourceCapabilities> receiver = new TestOutcomeReceiver<>();
        ArgumentCaptor<IDeviceDataSourceCapabilitiesCallback> callbackCaptor =
                ArgumentCaptor.forClass(IDeviceDataSourceCapabilitiesCallback.class);
        doNothing().when(mService).getDeviceDataSourceCapabilities(any(), callbackCaptor.capture());
        HealthConnectException exception =
                new HealthConnectException(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);

        healthConnectManager.getDeviceDataSourceCapabilities(
                Executors.newSingleThreadExecutor(), receiver);
        callbackCaptor.getValue().onError(new HealthConnectExceptionParcel(exception));

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(exception.getErrorCode());
    }

    @Test
    public void setTrackingEnabledFails_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onError(
                                            new HealthConnectExceptionParcel(
                                                    new HealthConnectException(
                                                            HealthConnectException
                                                                    .ERROR_UNSUPPORTED_OPERATION)));
                                    return null;
                                })
                .when(mService)
                .setTrackingEnabled(any(), anyBoolean(), any());

        healthConnectManager.setTrackingEnabled(
                StepsRecord.class, true, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_UNSUPPORTED_OPERATION);
    }

    @Test
    public void setTrackingEnabled_generatesDataTypeKeys() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();

        healthConnectManager.setTrackingEnabled(
                StepsRecord.class, false, Executors.newSingleThreadExecutor(), receiver);

        verify(mService).setTrackingEnabled(eq("TRACKING_PREF_1"), eq(false), any());
    }

    @Test
    public void setTrackingEnabled_success() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        TestOutcomeReceiver<Void> receiver = new TestOutcomeReceiver<>();
        doAnswer(
                        (Answer<Void>)
                                invocation -> {
                                    IEmptyResponseCallback callback = invocation.getArgument(2);
                                    callback.onResult();
                                    return null;
                                })
                .when(mService)
                .setTrackingEnabled(any(), anyBoolean(), any());

        healthConnectManager.setTrackingEnabled(
                StepsRecord.class, true, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isNull();
    }

    @Test
    public void isTrackingEnabledFails_usesExceptionFromService() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.isTrackingEnabled(any())).thenThrow(new RemoteException("message"));

        assertThrows(
                RuntimeException.class,
                () -> healthConnectManager.isTrackingEnabled(List.of(StepsRecord.class)));
    }

    @Test
    public void isTrackingEnabled_generatesDataTypeKeys() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        healthConnectManager.isTrackingEnabled(List.of(StepsRecord.class, DistanceRecord.class));

        verify(mService).isTrackingEnabled(captor.capture());
        assertThat(captor.getValue()).containsExactly("TRACKING_PREF_1", "TRACKING_PREF_7");
    }

    @Test
    public void isTrackingEnabled_success() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.isTrackingEnabled(any()))
                .thenReturn(Map.of("TRACKING_PREF_1", true, "TRACKING_PREF_7", false));

        Map<Class<? extends Record>, Boolean> result =
                healthConnectManager.isTrackingEnabled(
                        List.of(StepsRecord.class, DistanceRecord.class));

        assertThat(result)
                .containsExactly(
                        StepsRecord.class, true,
                        DistanceRecord.class, false);
    }

    @Test
    public void hasUserEnabledTracking_withException_throwsException() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.hasUserEnabledTracking(any(), any()))
                .thenThrow(new RemoteException("message"));

        assertThrows(
                RuntimeException.class,
                () -> healthConnectManager.hasUserEnabledTracking(StepsRecord.class));
    }

    @Test
    public void hasUserEnabledTracking_withRecordTypes_generatesDataTypeKeys() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        for (Class<? extends Record> recordClass :
                HealthConnectMappings.getInstance()
                        .getRecordIdToExternalRecordClassMap()
                        .values()) {
            int recordIdentifier = HealthConnectMappings.getInstance().getRecordType(recordClass);
            healthConnectManager.hasUserEnabledTracking(recordClass);
            verify(mService).hasUserEnabledTracking(any(), captor.capture());
            assertThat(captor.getValue()).isEqualTo("TRACKING_PREF_" + recordIdentifier);
            clearInvocations(mService);
        }
    }

    @Test
    public void hasUserEnabledTracking_withHavingDisabled_success() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager healthConnectManager = newHealthConnectManager(context, mService);
        when(mService.hasUserEnabledTracking(any(), eq("TRACKING_PREF_1"))).thenReturn(false);

        boolean result = healthConnectManager.hasUserEnabledTracking(StepsRecord.class);

        assertThat(result).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void testDeviceDataProvidersIntentActionsAndExtrasExist() {
        assertThat(HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING)
                .isEqualTo("android.health.connect.action.SHOW_DEVICE_ONBOARDING");
        assertThat(HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT)
                .isEqualTo("android.health.connect.action.SHOW_DEVICE_MANAGEMENT");

        assertThat(HealthConnectManager.EXTRA_DEVICE_ID)
                .isEqualTo("android.health.connect.extra.DEVICE_ID");
        assertThat(HealthConnectManager.EXTRA_DEVICE_RECORD_TYPES)
                .isEqualTo("android.health.connect.extra.DEVICE_RECORD_TYPES");
        assertThat(HealthConnectManager.EXTRA_DEVICE_SYMPTOM_TYPES)
                .isEqualTo("android.health.connect.extra.DEVICE_SYMPTOM_TYPES");

        assertThat(HealthConnectManager.RESULT_DEVICE_ONBOARDING_ALLOWED)
                .isEqualTo(android.app.Activity.RESULT_FIRST_USER);
        assertThat(HealthConnectManager.RESULT_DEVICE_ONBOARDING_DENIED)
                .isEqualTo(android.app.Activity.RESULT_FIRST_USER + 1);
        assertThat(HealthConnectManager.RESULT_DEVICE_ONBOARDING_ABORTED)
                .isEqualTo(android.app.Activity.RESULT_FIRST_USER + 2);
    }

    /**
     * Constructs a {@link HealthConnectManager} using reflection to access the constructor.
     *
     * <p>Unfortunately the {@link HealthConnectManager} loads from a different classloader to the
     * unit test, so even though they are in the same package name, technically the packages are
     * different. This leads to calling the constructor giving an {@link IllegalAccessError}. By
     * using reflection we can avoid this error in test cases, but this code should not be used
     * outside of testing.
     */
    private static HealthConnectManager newHealthConnectManager(
            Context context, IHealthConnectService service)
            throws InstantiationException,
                    IllegalAccessException,
                    InvocationTargetException,
                    NoSuchMethodException {
        return HealthConnectManager.class
                .getDeclaredConstructor(Context.class, IHealthConnectService.class)
                .newInstance(context, service);
    }

    private GetMatchingDataSourcesResponse getMatchingDataSourcesResponse() {
        return new GetMatchingDataSourcesResponse(
                Map.of("package.name", Set.of(WRITE_STEPS, WRITE_SLEEP)));
    }

    private GetMatchingDataSourcesResponse getMatchingDataSourcesWithDevicesResponse() {
        return new GetMatchingDataSourcesResponse(
                Map.of("package.name", Set.of(WRITE_STEPS, WRITE_SLEEP)),
                Map.of("device.package.name", Set.of(WRITE_STEPS, WRITE_SLEEP)));
    }

    private GetMatchingDataSourcesResponse emptyResponse() {
        return new GetMatchingDataSourcesResponse(Map.of());
    }

    private MatchmakingRequest getMatchmakingRequest(
            Set<Class<? extends Record>> recordTypes, String packageToMatch) {
        return new MatchmakingRequest.Builder()
                .addRecordTypes(recordTypes)
                .setCallingPackageName(PACKAGE_TO_MATCH)
                .build();
    }

    private static class TestOutcomeReceiver<T>
            implements OutcomeReceiver<T, HealthConnectException> {
        private static final int DEFAULT_TIMEOUT_SECONDS = 5;
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private final AtomicReference<T> mResponse = new AtomicReference<>();
        private final AtomicReference<HealthConnectException> mException = new AtomicReference<>();

        /**
         * Returns the response received. Fails if no response received within the default timeout.
         *
         * @throws InterruptedException if this is interrupted before any response received
         */
        public T getResponse() throws InterruptedException {
            verifyNoExceptionOrThrow();
            return mResponse.get();
        }

        /**
         * Asserts that no exception is received within the default timeout. If an exception is
         * received it is rethrown by this method.
         */
        public void verifyNoExceptionOrThrow() throws InterruptedException {
            verifyNoExceptionOrThrow(DEFAULT_TIMEOUT_SECONDS);
        }

        /**
         * Asserts that no exception is received within the given timeout. If an exception is
         * received it is rethrown by this method.
         */
        public void verifyNoExceptionOrThrow(int timeoutSeconds) throws InterruptedException {
            assertThat(mLatch.await(timeoutSeconds, TimeUnit.SECONDS)).isTrue();
            if (mException.get() != null) {
                throw mException.get();
            }
        }

        /**
         * Returns the exception received. Fails if no response received within the default timeout.
         *
         * @throws InterruptedException if this is interrupted before any response received
         */
        public HealthConnectException assertAndGetException() throws InterruptedException {
            assertThat(mLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(mResponse.get()).isNull();
            return mException.get();
        }

        @Override
        public void onResult(T result) {
            mResponse.set(result);
            mLatch.countDown();
        }

        @Override
        public void onError(@NonNull HealthConnectException error) {
            mException.set(error);
            mLatch.countDown();
        }
    }
}
