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

package android.healthconnect.testing.cts.testapphelpers;

import static android.Manifest.permission.FORCE_STOP_PACKAGES;
import static android.Manifest.permission.GET_RUNTIME_PERMISSIONS;
import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.FLAG_PERMISSION_ONE_TIME;
import static android.health.connect.datatypes.FhirVersion.parseFhirVersion;
import static android.healthconnect.testing.cts.BundleHelper.INTENT_EXCEPTION;
import static android.healthconnect.testing.cts.BundleHelper.QUERY_TYPE;

import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.MatchmakingRequest;
import android.health.connect.MatchmakingResponse;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.healthconnect.testing.cts.BundleHelper;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/** Performs API calls to HC on behalf of test apps. */
public final class TestAppProxy {
    private static final String TAG = "TestAppProxy";
    private static final long POLLING_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(55);

    private static final String TEST_APP_RECEIVER_CLASS_NAME =
            "android.healthconnect.testing.testapp.TestAppReceiver";

    public static final TestAppProxy APP_WRITE_PERMS_ONLY =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.writePermsOnly");

    private final Context mContext;
    private final String mPackageName;
    private final boolean mInBackground;

    private TestAppProxy(String packageName, boolean inBackground) {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageName = packageName;
        mInBackground = inBackground;
    }

    /** Create a new {@link TestAppProxy} for given package name. */
    public static TestAppProxy forPackageName(String packageName) {
        return new TestAppProxy(packageName, false);
    }

    /**
     * Create a new {@link TestAppProxy} for given package name which performs calls in the
     * background.
     */
    public static TestAppProxy forPackageNameInBackground(String packageName) {
        return new TestAppProxy(packageName, true);
    }

    /** Returns the package name of the app. */
    public String getPackageName() {
        return mPackageName;
    }

    /** Inserts a record to HC on behalf of the app. */
    public String insertRecord(Record record) throws Exception {
        return insertRecords(Collections.singletonList(record)).get(0);
    }

    /** Inserts records to HC on behalf of the app. */
    public List<String> insertRecords(Record... records) throws Exception {
        return insertRecords(Arrays.asList(records));
    }

    /** Inserts records to HC on behalf of the app. */
    public List<String> insertRecords(List<? extends Record> records) throws Exception {
        Bundle requestBundle = BundleHelper.fromInsertRecordsRequest(records);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toInsertRecordsResponse(responseBundle);
    }

    /** Deletes records from HC on behalf of the app. */
    public void deleteRecords(RecordIdFilter... recordIdFilters) throws Exception {
        deleteRecords(Arrays.asList(recordIdFilters));
    }

    /** Deletes records from HC on behalf of the app. */
    public void deleteRecords(List<RecordIdFilter> recordIdFilters) throws Exception {
        Bundle requestBundle = BundleHelper.fromDeleteRecordsByIdsRequest(recordIdFilters);
        getFromTestApp(requestBundle);
    }

    /** Updates records in HC on behalf of the app. */
    public void updateRecords(Record... records) throws Exception {
        updateRecords(Arrays.asList(records));
    }

    /** Updates records in HC on behalf of the app. */
    public void updateRecords(List<? extends Record> records) throws Exception {
        Bundle requestBundle = BundleHelper.fromUpdateRecordsRequest(records);
        getFromTestApp(requestBundle);
    }

    /** Read records from HC on behalf of the app. */
    public <T extends Record> List<T> readRecords(ReadRecordsRequestUsingFilters<T> request)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromReadRecordsRequestUsingFilters(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toReadRecordsResponse(responseBundle);
    }

    /** Read records from HC on behalf of the app. */
    public <T extends Record> List<T> readRecords(ReadRecordsRequestUsingIds<T> request)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromReadRecordsRequestUsingIds(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toReadRecordsResponse(responseBundle);
    }

    /** Aggregate steps records from HC on behalf of the app. */
    public Long aggregateStepsCountTotal(
            Instant startTime, Instant endTime, List<String> packageNames) throws Exception {
        Bundle requestBundle =
                BundleHelper.fromAggregateStepsCountTotalRequest(startTime, endTime, packageNames);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toAggregateStepsCountTotalResponse(responseBundle);
    }

    /** Gets changelogs from HC on behalf of the app. */
    public ChangeLogsResponse getChangeLogs(ChangeLogsRequest request) throws Exception {
        Bundle requestBundle = BundleHelper.fromChangeLogsRequest(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toChangeLogsResponse(responseBundle);
    }

    /** Gets a change log token from HC on behalf of the app. */
    public String getChangeLogToken(ChangeLogTokenRequest request) throws Exception {
        Bundle requestBundle = BundleHelper.fromChangeLogTokenRequest(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toChangeLogTokenResponse(responseBundle);
    }

    /**
     * Inserts a Medical Data Source to HC on behalf of the app.
     *
     * @return the inserted data source
     */
    public MedicalDataSource createMedicalDataSource(CreateMedicalDataSourceRequest request)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromCreateMedicalDataSourceRequest(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toMedicalDataSource(responseBundle);
    }

    /** Gets a list of {@link MedicalDataSource}s given a list of ids on behalf of the app. */
    public List<MedicalDataSource> getMedicalDataSources(List<String> ids) throws Exception {
        Bundle requestBundle = BundleHelper.fromMedicalDataSourceIds(ids);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toMedicalDataSources(responseBundle);
    }

    /** Gets a list of {@link MedicalDataSource}s given a {@link GetMedicalDataSourcesRequest}. */
    public List<MedicalDataSource> getMedicalDataSources(GetMedicalDataSourcesRequest request)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromMedicalDataSourceRequest(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toMedicalDataSources(responseBundle);
    }

    /**
     * Upserts a Medical Resource to HC on behalf of the app.
     *
     * @return the inserted resource
     */
    public MedicalResource upsertMedicalResource(String datasourceId, String data)
            throws Exception {
        String R4VersionString = "4.0.1";
        UpsertMedicalResourceRequest request =
                new UpsertMedicalResourceRequest.Builder(
                                datasourceId, parseFhirVersion(R4VersionString), data)
                        .build();
        Bundle requestBundle = BundleHelper.fromUpsertMedicalResourceRequests(List.of(request));
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toMedicalResources(responseBundle).get(0);
    }

    /**
     * Reads a list of {@link MedicalResource}s for the provided {@code request} on behalf of the
     * app.
     */
    public ReadMedicalResourcesResponse readMedicalResources(ReadMedicalResourcesRequest request)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromReadMedicalResourcesRequest(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toReadMedicalResourcesResponse(responseBundle);
    }

    /**
     * Reads a list of {@link MedicalResource}s for the provided {@code ids} on behalf of the app.
     */
    public List<MedicalResource> readMedicalResources(List<MedicalResourceId> ids)
            throws Exception {
        Bundle requestBundle = BundleHelper.fromMedicalResourceIdsForRead(ids);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toMedicalResources(responseBundle);
    }

    /** Deletes Medical Resources from HC on behalf of the app for the given {@code ids}. */
    public void deleteMedicalResources(List<MedicalResourceId> ids) throws Exception {
        Bundle requestBundle = BundleHelper.fromMedicalResourceIdsForDelete(ids);
        getFromTestApp(requestBundle);
    }

    /** Deletes Medical Resources from HC on behalf of the app for the given {@code request}. */
    public void deleteMedicalResources(DeleteMedicalResourcesRequest request) throws Exception {
        Bundle requestBundle = BundleHelper.fromDeleteMedicalResourcesRequest(request);
        getFromTestApp(requestBundle);
    }

    /** Deletes Medical Data Source with data for the provided {@code id} on behalf of the app. */
    public void deleteMedicalDataSourceWithData(String id) throws Exception {
        Bundle requestBundle = BundleHelper.fromMedicalDataSourceId(id);
        getFromTestApp(requestBundle);
    }

    /** Calls isMatchmakingPossible on behalf of the app. */
    public MatchmakingResponse isMatchmakingPossible(MatchmakingRequest request) throws Exception {
        Bundle requestBundle = BundleHelper.fromIsMatchmakingPossibleQuery(request);
        Bundle responseBundle = getFromTestApp(requestBundle);
        return BundleHelper.toIsMatchmakingPossibleResponse(responseBundle);
    }

    /** Instructs the app to self-revokes the specified permission. */
    public void selfRevokePermission(String permission) throws Exception {
        Bundle requestBundle = BundleHelper.forSelfRevokePermissionRequest(permission);
        getFromTestApp(requestBundle);

        // Self-revoke is async; wait for it to complete by checking for the one-time flag it sets.
        PackageManager packageManager = mContext.getPackageManager();
        runWithShellPermissionIdentity(
                () ->
                        eventually(
                                () -> {
                                    @SuppressLint("MissingPermission")
                                    int flags =
                                            packageManager.getPermissionFlags(
                                                    permission,
                                                    mPackageName,
                                                    Process.myUserHandle());
                                    assertThat(flags & FLAG_PERMISSION_ONE_TIME).isNotEqualTo(0);
                                }),
                GET_RUNTIME_PERMISSIONS);
    }

    /** Force-stops the app. */
    @SuppressLint("MissingPermission")
    public void forceStop() {
        ActivityManager activityManager =
                requireNonNull(mContext.getSystemService(ActivityManager.class));
        runWithShellPermissionIdentity(
                () -> activityManager.forceStopPackage(mPackageName), FORCE_STOP_PACKAGES);
    }

    /** Starts an activity on behalf of the app and returns the result. */
    public Instrumentation.ActivityResult startActivityForResult(Intent intent) throws Exception {
        return startActivityForResult(intent, null);
    }

    /**
     * Starts an activity on behalf of the app, executes the runnable and returns the result.
     *
     * <p>The corresponding test app must have the following activity declared in the Manifest.
     *
     * <pre>{@code
     * <activity android:name="android.healthconnect.cts.utils.ProxyActivity"
     *           android:exported="true">
     *   <intent-filter>
     *      <action android:name="android.healthconnect.cts.ACTION_START_ACTIVITY_FOR_RESULT"/>
     *      <category android:name="android.intent.category.DEFAULT"/>
     *   </intent-filter>
     * </activity>
     * }</pre>
     */
    public Instrumentation.ActivityResult startActivityForResult(Intent intent, Runnable runnable)
            throws Exception {
        Intent testAppIntent = new Intent(ProxyActivity.PROXY_ACTIVITY_ACTION);
        testAppIntent.setPackage(mPackageName);
        testAppIntent.putExtra(Intent.EXTRA_INTENT, intent);

        return ProxyActivity.launchActivityForResult(testAppIntent, runnable);
    }

    @Override
    public String toString() {
        return "TestAppProxy [userId="
                + mContext.getUser().getIdentifier()
                + ", packageName="
                + mPackageName
                + ", inBackground="
                + mInBackground
                + "]";
    }

    private Bundle getFromTestApp(Bundle bundleToCreateIntent) throws Exception {
        if (mInBackground) {
            return getFromTestAppReceiver(bundleToCreateIntent);
        } else {
            return getFromTestAppActivity(bundleToCreateIntent);
        }
    }

    private Bundle getFromTestAppReceiver(Bundle bundleToCreateIntent) throws Exception {
        ArrayBlockingQueue<Bundle> resultQueue = new ArrayBlockingQueue<>(1);
        BroadcastReceiver resultReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle resultExtras = getResultExtras(/* makeMap= */ true);
                        Log.d(
                                TAG,
                                "Got broadcast result code: "
                                        + getResultCode()
                                        + " with extras: "
                                        + TestAppProxy.toString(resultExtras));
                        resultQueue.add(resultExtras);
                    }
                };

        Intent intent = new Intent();
        intent.setClassName(mPackageName, TEST_APP_RECEIVER_CLASS_NAME);
        intent.putExtras(bundleToCreateIntent);

        Log.d(
                TAG,
                "Sending broadcast: "
                        + intent
                        + " with QUERY_TYPE="
                        + intent.getStringExtra(QUERY_TYPE)
                        + " and extras "
                        + toString(bundleToCreateIntent));
        mContext.sendOrderedBroadcast(
                intent,
                /* receiverPermission= */ null,
                resultReceiver,
                /* scheduler= */ null,
                /* initialResult= */ RESULT_OK,
                /* initialData= */ null,
                /* initialExtras= */ null);

        Bundle resultExtras = resultQueue.poll(POLLING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        if (resultExtras == null) {
            throw new TimeoutException("Timed out waiting to get broadcast result for " + intent);
        }
        throwExceptionIfPresent(resultExtras);
        return resultExtras;
    }

    private Bundle getFromTestAppActivity(Bundle bundleToCreateIntent) throws Exception {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(mPackageName);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtras(bundleToCreateIntent);

        Log.d(
                TAG,
                "Starting activity: "
                        + intent
                        + " with QUERY_TYPE="
                        + intent.getStringExtra(QUERY_TYPE)
                        + " and extras="
                        + toString(bundleToCreateIntent));
        Instrumentation.ActivityResult activityResult =
                ProxyActivity.launchActivityForResult(intent);
        Log.d(
                TAG,
                "Got activity result code: "
                        + activityResult.getResultCode()
                        + " with data: "
                        + toString(activityResult.getResultData()));

        Bundle resultExtras = requireNonNull(activityResult.getResultData().getExtras());
        throwExceptionIfPresent(resultExtras);
        return resultExtras;
    }

    private void throwExceptionIfPresent(Bundle resultExtras) throws Exception {
        Exception exception = (Exception) resultExtras.getSerializable(INTENT_EXCEPTION);
        if (exception != null) {
            throw exception;
        }
    }

    private static String toString(Intent intent) {
        if (intent == null) {
            return "(null)";
        }
        return "Intent[action="
                + intent.getAction()
                + ", extras="
                + toString(intent.getExtras())
                + "]";
    }

    @SuppressWarnings("deprecation")
    private static String toString(Bundle bundle) {
        return "["
                + bundle.keySet().stream()
                        .map(key -> key + "=" + bundle.get(key))
                        .collect(Collectors.joining(", "))
                + "]";
    }
}
