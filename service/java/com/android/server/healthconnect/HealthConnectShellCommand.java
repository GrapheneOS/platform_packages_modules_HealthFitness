/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.server.healthconnect;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.content.AttributionSource;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetDeviceDataSourcesCallback;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.InsertRecordsResponseParcel;
import android.health.connect.aidl.ReadRecordsResponseParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.os.Process;
import android.util.Slog;

import com.android.modules.utils.BasicShellCommandHandler;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shell command implementation for HealthConnect to enable testing of multiple device data
 * providers in CTS. See {@link MultiProviderTest}.
 *
 * @hide
 */
public class HealthConnectShellCommand extends BasicShellCommandHandler {
    private static final String TAG = "HealthConnectShellCommand";
    public static final String SHELL_PACKAGE_NAME = "com.android.shell";
    private static final String DEVICE_ID = "ShellDeviceId";

    @Nullable private HealthConnectServiceImpl mService;
    private final AttributionSource mAttributionSource;

    private CountDownLatch mCountDownLatch;
    private int mRecordCount;
    private int mDataSourceCount;

    private final IEmptyResponseCallback mIEmptyResponseCallback;

    public HealthConnectShellCommand() {
        mCountDownLatch = new CountDownLatch(0);

        mAttributionSource =
                new AttributionSource.Builder(Process.SHELL_UID)
                        .setPackageName(SHELL_PACKAGE_NAME)
                        .build();

        mIEmptyResponseCallback =
                new IEmptyResponseCallback.Stub() {
                    @Override
                    public void onResult() {
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectExceptionParcel exception) {
                        mCountDownLatch.countDown();
                    }
                };
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }

        mService = (HealthConnectServiceImpl) getTarget();
        PrintWriter pw = getOutPrintWriter();
        try {
            return switch (cmd) {
                case "get-current-device-id" -> getCurrentDeviceId(pw);
                case "advertise-device" -> advertiseDevice();
                case "advertise-current-device" -> advertiseCurrentDevice();
                case "insert-current-device-records" -> insertCurrentDeviceRecords();
                case "insert-records" -> insertRecords();
                case "read-records" -> readRecords(pw);
                case "delete-records" -> deleteRecords();
                case "get-device-data-sources" -> getDeviceDataSources(pw);
                default -> handleDefaultCommands(cmd);
            };
        } catch (Exception e) {
            pw.println("Error: " + e.getMessage());
            Slog.e(TAG, "Error handling shell command: " + cmd, e);
        }
        return 0;
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("HealthConnect service commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  get-current-device-id");
        pw.println("    Get the current device ID.");
        pw.println("  advertise-device");
        pw.println("    Advertise a test device for the shell package.");
        pw.println("  advertise-current-device");
        pw.println("    Advertise the current device for the shell package.");
        pw.println("  insert-current-device-records");
        pw.println("    Insert a step record for the current device for the shell package.");
        pw.println("  insert-records");
        pw.println("    Insert a step record for a test device for the shell package.");
        pw.println("  read-records");
        pw.println("    Read device records of the test device for the shell package.");
        pw.println("  delete-records");
        pw.println("    Delete device records of the test device for the shell package.");
        pw.println("  get-device-data-sources");
        pw.println("    Get the number of available device data sources for the shell package.");
    }

    private int getCurrentDeviceId(PrintWriter pw) {
        requireNonNull(mService);
        pw.println(mService.getCurrentDeviceId(mAttributionSource));
        return 0;
    }

    private int advertiseDevice() throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        buildShellDevice(),
                        DEVICE_ID,
                        Set.of(
                                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                        .setAvailable(true)
                                        .build(),
                                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                        .setAvailable(true)
                                        .build()));

        mService.advertiseDeviceDataSources(
                mAttributionSource, List.of(advertisement), mIEmptyResponseCallback);

        awaitCountdown();

        return 0;
    }

    private int advertiseCurrentDevice() throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();

        String deviceId = mService.getCurrentDeviceId(mAttributionSource);
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        buildShellDevice(), deviceId, deviceDataTypeAdvertisements);

        mService.advertiseDeviceDataSources(
                mAttributionSource, List.of(advertisement), mIEmptyResponseCallback);

        awaitCountdown();

        return 0;
    }

    private int insertCurrentDeviceRecords() throws InterruptedException {
        requireNonNull(mService);
        String deviceId = mService.getCurrentDeviceId(mAttributionSource);
        return insertStepRecordForDevice(deviceId);
    }

    private int insertRecords() throws InterruptedException {
        return insertStepRecordForDevice(DEVICE_ID);
    }

    private int readRecords(PrintWriter pw) throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();
        mRecordCount = -1;

        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(DEVICE_ID)
                        .build();

        mService.readDeviceRecords(
                mAttributionSource,
                request.toReadRecordsRequestParcel(),
                new IReadRecordsResponseCallback.Stub() {
                    @Override
                    public void onResult(ReadRecordsResponseParcel response) {
                        mCountDownLatch.countDown();
                        mRecordCount = response.getRecordsParcel().getRecords().size();
                    }

                    @Override
                    public void onError(HealthConnectExceptionParcel exception) {
                        mCountDownLatch.countDown();
                    }
                });

        awaitCountdown();
        pw.println(mRecordCount);

        return 0;
    }

    private int deleteRecords() throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();

        mService.deleteDeviceRecords(
                mAttributionSource,
                DEVICE_ID,
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addRecordType(DistanceRecord.class)
                                .build()),
                mIEmptyResponseCallback);

        awaitCountdown();

        return 0;
    }

    private int getDeviceDataSources(PrintWriter pw) throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();
        mDataSourceCount = -1;

        mService.getDeviceDataSources(
                mAttributionSource,
                new IGetDeviceDataSourcesCallback.Stub() {
                    @Override
                    @RequiresNoPermission
                    public void onResult(GetDeviceDataSourcesResponse response) {
                        mCountDownLatch.countDown();
                        mDataSourceCount = response.getDeviceDataSources().size();
                    }

                    @Override
                    @RequiresNoPermission
                    public void onError(HealthConnectExceptionParcel exception) {
                        mCountDownLatch.countDown();
                    }
                });

        awaitCountdown();
        pw.println(mDataSourceCount);

        return 0;
    }

    private int insertStepRecordForDevice(String deviceId) throws InterruptedException {
        requireNonNull(mService);
        initializeCountdown();

        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = startTime.plusMillis(1000);
        StepsRecord record =
                new StepsRecord.Builder(new Metadata.Builder().build(), startTime, endTime, 500)
                        .build();

        RecordsParcel recordsParcel =
                new RecordsParcel(
                        List.of(record.toRecordInternal().setPackageName(SHELL_PACKAGE_NAME)));
        mService.insertDeviceRecords(
                mAttributionSource,
                deviceId,
                recordsParcel,
                new IInsertRecordsResponseCallback.Stub() {
                    @Override
                    public void onResult(InsertRecordsResponseParcel response) {
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectExceptionParcel exception) {
                        mCountDownLatch.countDown();
                    }
                });

        awaitCountdown();

        return 0;
    }

    private void initializeCountdown() {
        mCountDownLatch = new CountDownLatch(1);
    }

    private void awaitCountdown() throws InterruptedException {
        if (!mCountDownLatch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out while executing shell command.");
        }
    }

    private static Device buildShellDevice() {
        return new Device.Builder()
                .setManufacturer("ShellManufacturer")
                .setModel("ShellModel")
                .setType(Device.DEVICE_TYPE_PHONE)
                .setDisplayName("ShellDisplayName")
                .build();
    }
}
