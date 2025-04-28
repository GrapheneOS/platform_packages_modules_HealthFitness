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

import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.datatypes.MedicalDataSource;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.os.OutcomeReceiver;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock IHealthConnectService mService;

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

    private static class TestOutcomeReceiver<T>
            implements OutcomeReceiver<T, HealthConnectException> {
        private static final int DEFAULT_TIMEOUT_SECONDS = 5;
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private final AtomicReference<T> mResponse = new AtomicReference<>();
        private final AtomicReference<HealthConnectException> mException = new AtomicReference<>();

        /**
         * Returns the resppnse received. Fails if no response received within the default timeout.
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
