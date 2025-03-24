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

package android.healthconnect.tests.exportimport;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.app.UiAutomation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.exportimport.ExportImportDocumentProvider;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.tests.documentprovider.utils.DocumentProviderIntent;
import android.healthconnect.tests.documentprovider.utils.DocumentProviderRoot;
import android.net.Uri;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.provider.DocumentsContract;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// When running these tests locally make sure to disable/uninstall any apps on the device that
// implement the document provider API. The tests will fail otherwise as they're only expecting the
// document providers configured by the tests.
@RunWith(AndroidJUnit4.class)
public final class QueryDocumentProvidersTest {
    private static final String TEST_DOCUMENT_PROVIDER_APP_PACKAGE_NAME =
            "android.healthconnect.tests.documentproviderapp";
    private static final String TEST_DOCUMENT_PROVIDER_APP_2_PACKAGE_NAME =
            "android.healthconnect.tests.documentproviderapp2";

    private static final String TEST_ROOT_ID = "TestRoot";
    private static final String TEST_TITLE = "Document Provider";
    private static final String TEST_ACCOUNT_1_SUMMARY = "Account1";
    private static final String TEST_ROOT_URI =
            "content://android.healthconnect.tests.documentproviderapp.documents/root/"
                    + TEST_ROOT_ID;

    private static final String TEST_ROOT_2_ID = "TestRoot2";
    private static final String TEST_ACCOUNT_2_SUMMARY = "Account2";
    private static final String TEST_ROOT_2_URI =
            "content://android.healthconnect.tests.documentproviderapp.documents/root/"
                    + TEST_ROOT_2_ID;

    private static final String TEST_DOCUMENT_PROVIDER_2_ROOT_ID = "TestRootDocumentProvider2";
    private static final String TEST_DOCUMENT_PROVIDER_2_TITLE = "Document Provider 2";
    private static final String TEST_DOCUMENT_PROVIDER_2_ACCOUNT_SUMMARY =
            "AccountDocumentProvider2";
    private static final String TEST_DOCUMENT_PROVIDER_2_ROOT_URI =
            "content://android.healthconnect.tests.documentproviderapp2.documents/root/"
                    + TEST_DOCUMENT_PROVIDER_2_ROOT_ID;

    private static final String ZIP_MIME_TYPE = "application/zip";
    private static final String PDF_AND_ZIP_MIME_TYPES = "application/pdf\napplication/zip";

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws Exception {
        launchTestDocumentProviderApp(clearDocumentProviders());
        launchTestDocumentProviderApp2(clearDocumentProviders());
    }

    @Test
    public void queryDocumentProviders_noSupportedDocumentProviders_returnsEmpty()
            throws InterruptedException {
        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).isEmpty();
    }

    @Test
    public void queryDocumentProviders_throwsException_returnsEmpty() throws Exception {
        launchTestDocumentProviderApp(setDocumentProviderThrowsException());

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).isEmpty();
    }

    @Test
    public void queryDocumentProviders_singleRoot_returnsRoot() throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(1);
        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));
    }

    @Test
    public void queryDocumentProviders_singleRootWithNullSummary_returnsRoot() throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(null)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(1);
        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEmpty();
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));
    }

    @Test
    public void queryDocumentProviders_singleRootLocalOnly_returnsEmpty() throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(
                                DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                                        | DocumentsContract.Root.FLAG_LOCAL_ONLY)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).isEmpty();
    }

    @Test
    public void queryDocumentProviders_singleRootDoesNotSupportCreate_returnsEmpty()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).isEmpty();
    }

    @Test
    public void queryDocumentProviders_singleRootDoesNotSupportZipMimeType_returnsEmpty()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes("");
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).isEmpty();
    }

    @Test
    public void queryDocumentProviders_singleRootSupportsMultipleMimeTypes_returnsRoot()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(PDF_AND_ZIP_MIME_TYPES);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(1);
        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));
    }

    @Test
    public void queryDocumentProviders_singleRootSupportsAllMimeTypes_returnsRoot()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(1);
        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));
    }

    @Test
    public void queryDocumentProviders_multipleRoots_returnsRoots() throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        DocumentProviderRoot root2 =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_2_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_2_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(1)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root2));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(2);

        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));

        assertThat(documentProviders.get(1).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(1).getSummary()).isEqualTo(TEST_ACCOUNT_2_SUMMARY);
        assertThat(documentProviders.get(1).getIconResource()).isEqualTo(1);
        assertThat(documentProviders.get(1).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_2_URI));
    }

    @Test
    public void queryDocumentProviders_secondDocumentProviderThrowsException_returnsRoot()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        launchTestDocumentProviderApp2(setDocumentProviderThrowsException());

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(1);
        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));
    }

    @Test
    public void queryDocumentProviders_singleRootFromMultipleDocumentProviders_returnsRoots()
            throws Exception {
        DocumentProviderRoot root =
                new DocumentProviderRoot()
                        .setRootId(TEST_ROOT_ID)
                        .setTitle(TEST_TITLE)
                        .setSummary(TEST_ACCOUNT_1_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(0)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp(addDocumentProviderRoot(root));

        DocumentProviderRoot root2 =
                new DocumentProviderRoot()
                        .setRootId(TEST_DOCUMENT_PROVIDER_2_ROOT_ID)
                        .setTitle(TEST_DOCUMENT_PROVIDER_2_TITLE)
                        .setSummary(TEST_DOCUMENT_PROVIDER_2_ACCOUNT_SUMMARY)
                        .setFlags(DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
                        .setIconResourceId(1)
                        .setMimeTypes(ZIP_MIME_TYPE);
        launchTestDocumentProviderApp2(addDocumentProviderRoot(root2));

        List<ExportImportDocumentProvider> documentProviders = queryDocumentProviders();

        assertThat(documentProviders).hasSize(2);

        assertThat(documentProviders.get(0).getTitle()).isEqualTo(TEST_TITLE);
        assertThat(documentProviders.get(0).getSummary()).isEqualTo(TEST_ACCOUNT_1_SUMMARY);
        assertThat(documentProviders.get(0).getIconResource()).isEqualTo(0);
        assertThat(documentProviders.get(0).getRootUri()).isEqualTo(Uri.parse(TEST_ROOT_URI));

        assertThat(documentProviders.get(1).getTitle()).isEqualTo(TEST_DOCUMENT_PROVIDER_2_TITLE);
        assertThat(documentProviders.get(1).getSummary())
                .isEqualTo(TEST_DOCUMENT_PROVIDER_2_ACCOUNT_SUMMARY);
        assertThat(documentProviders.get(1).getIconResource()).isEqualTo(1);
        assertThat(documentProviders.get(1).getRootUri())
                .isEqualTo(Uri.parse(TEST_DOCUMENT_PROVIDER_2_ROOT_URI));
    }

    private static List<ExportImportDocumentProvider> queryDocumentProviders()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager manager =
                requireNonNull(context.getSystemService(HealthConnectManager.class));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<ExportImportDocumentProvider>> documentProvidersAtomicReference =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();

        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);

        try {
            manager.queryDocumentProviders(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(List<ExportImportDocumentProvider> result) {
                            documentProvidersAtomicReference.set(result);
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            healthConnectExceptionAtomicReference.set(exception);
                            latch.countDown();
                        }
                    });

            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
            if (healthConnectExceptionAtomicReference.get() != null) {
                throw healthConnectExceptionAtomicReference.get();
            }

            return documentProvidersAtomicReference.get();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    private void launchTestDocumentProviderApp(Bundle bundleToCreateIntent) throws Exception {
        launchTestDocumentProviderApp(
                TEST_DOCUMENT_PROVIDER_APP_PACKAGE_NAME, bundleToCreateIntent);
    }

    private void launchTestDocumentProviderApp2(Bundle bundleToCreateIntent) throws Exception {
        launchTestDocumentProviderApp(
                TEST_DOCUMENT_PROVIDER_APP_2_PACKAGE_NAME, bundleToCreateIntent);
    }

    private void launchTestDocumentProviderApp(String packageName, Bundle bundleToCreateIntent)
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        final BroadcastReceiver broadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.hasExtra(DocumentProviderIntent.EXCEPTION)) {
                            exceptionAtomicReference.set(
                                    (Exception)
                                            (intent.getSerializableExtra(
                                                    DocumentProviderIntent.EXCEPTION)));
                        }
                        latch.countDown();
                    }
                };

        final Context context = ApplicationProvider.getApplicationContext();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DocumentProviderIntent.RESPONSE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.putExtras(bundleToCreateIntent);

            context.startActivity(intent);

            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

            if (exceptionAtomicReference.get() != null) {
                throw exceptionAtomicReference.get();
            }
        } finally {
            context.unregisterReceiver(broadcastReceiver);
        }
    }

    private static Bundle clearDocumentProviders() {
        Bundle bundle = new Bundle();
        bundle.putString(
                DocumentProviderIntent.ACTION_TYPE,
                DocumentProviderIntent.CLEAR_DOCUMENT_PROVIDER_ROOTS);
        return bundle;
    }

    private static Bundle setDocumentProviderThrowsException() {
        Bundle bundle = new Bundle();
        bundle.putString(
                DocumentProviderIntent.ACTION_TYPE,
                DocumentProviderIntent.SET_DOCUMENT_PROVIDER_THROWS_EXCEPTION);
        return bundle;
    }

    private static Bundle addDocumentProviderRoot(DocumentProviderRoot root) {
        Bundle bundle = root.toBundle();
        bundle.putString(
                DocumentProviderIntent.ACTION_TYPE,
                DocumentProviderIntent.ADD_DOCUMENT_PROVIDER_ROOT);
        return bundle;
    }
}
