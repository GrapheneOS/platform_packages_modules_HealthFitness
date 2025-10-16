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

package com.android.health.connect.backuprestore;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.Context;
import android.health.connect.HealthConnectManager;
import android.health.connect.restore.StageRemoteDataException;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Unit test class for {@link HealthConnectBackupAgent} */
@RunWith(AndroidJUnit4.class)
public class HealthConnectBackupAgentTest {
    private TestableHealthConnectBackupAgent mHealthConnectBackupAgent;
    @Mock private HealthConnectManager mHealthConnectManager;

    @Captor ArgumentCaptor<Map<String, ParcelFileDescriptor>> mPfdsByFileNameCaptor;
    @Captor ArgumentCaptor<OutcomeReceiver<Void, StageRemoteDataException>> mStagingCallbackCaptor;
    @Mock private Context mContext;
    @Mock private FullBackupDataOutput mFullBackupDataOutput;
    @Mock private ParcelFileDescriptor mOldState;
    @Mock private ParcelFileDescriptor mNewState;
    @Mock private BackupDataInput mBackupDataInput;
    @Mock private BackupDataOutput mBackupDataOutput;
    private File mBackupDataDirectory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mBackupDataDirectory = mContext.getDir("mock_data", Context.MODE_PRIVATE);

        // Clean up directory before each test to ensure test isolation
        if (mBackupDataDirectory.exists()) {
            File[] files = mBackupDataDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        mHealthConnectBackupAgent = new TestableHealthConnectBackupAgent();
        mHealthConnectBackupAgent.onCreate();
    }

    @Test
    public void testOnRestoreFinished_stagingAll_sendsAllDataForStaging() throws IOException {
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile1");
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile2");
        Set<String> expectedStagedFileNames =
                Stream.of(mHealthConnectBackupAgent.getBackupDataDir().listFiles())
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet());
        assertThat(expectedStagedFileNames.size()).isEqualTo(2);

        Set<String> capturedStagedFileNames;

        mHealthConnectBackupAgent.onRestoreFinished();
        verify(mHealthConnectManager, times(1))
                .stageAllHealthConnectRemoteData(mPfdsByFileNameCaptor.capture(), any(), any());
        capturedStagedFileNames = mPfdsByFileNameCaptor.getValue().keySet();

        assertThat(capturedStagedFileNames.equals(expectedStagedFileNames)).isEqualTo(true);
    }

    @Test
    public void testOnRestoreFinished_afterStagingSuccess_deletesLocalData() throws IOException {
        HealthConnectBackupAgent spyForVerification = spy(mHealthConnectBackupAgent);
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile1");
        Set<String> expectedStagedFileNames =
                Stream.of(mHealthConnectBackupAgent.getBackupDataDir().listFiles())
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet());
        assertThat(expectedStagedFileNames.size()).isEqualTo(1);

        mHealthConnectBackupAgent.onRestoreFinished();
        verify(mHealthConnectManager, times(1))
                .stageAllHealthConnectRemoteData(any(), any(), mStagingCallbackCaptor.capture());
        mStagingCallbackCaptor.getValue().onResult(null);

        verify(spyForVerification, times(1)).deleteBackupFiles();
    }

    @Test
    public void testOnRestoreFinished_afterStagingFailure_deletesLocalData() throws IOException {
        HealthConnectBackupAgent spyForVerification = spy(mHealthConnectBackupAgent);
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile1");
        Set<String> expectedStagedFileNames =
                Stream.of(mHealthConnectBackupAgent.getBackupDataDir().listFiles())
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet());
        assertThat(expectedStagedFileNames.size()).isEqualTo(1);

        mHealthConnectBackupAgent.onRestoreFinished();
        verify(mHealthConnectManager, times(1))
                .stageAllHealthConnectRemoteData(any(), any(), mStagingCallbackCaptor.capture());
        mStagingCallbackCaptor.getValue().onError(new StageRemoteDataException(new ArrayMap<>()));

        verify(spyForVerification, times(1)).deleteBackupFiles();
    }

    @Test
    public void testOnFullBackup_backsUpEverything() throws Exception {
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile1");
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile2");

        mHealthConnectBackupAgent.onFullBackup(mFullBackupDataOutput);

        assertThat(mHealthConnectBackupAgent.mBackedUpFiles).hasSize(2);
    }

    @Test
    public void testOnFullBackup_deviceToDeviceFlag_callsBackupFileNamesWithTrue()
            throws Exception {
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile1");
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile2");

        when(mFullBackupDataOutput.getTransportFlags())
                .thenReturn(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER);
        mHealthConnectBackupAgent.onFullBackup(mFullBackupDataOutput);

        verify(mHealthConnectManager, times(1)).getAllBackupFileNames(true);
    }

    @Test
    public void testOnFullBackup_noDeviceToDeviceFlag_callsBackupFileNamesWithFalse()
            throws Exception {
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile1");
        createAndGetNonEmptyFile(mBackupDataDirectory, "testFile2");

        when(mFullBackupDataOutput.getTransportFlags()).thenReturn(0);
        mHealthConnectBackupAgent.onFullBackup(mFullBackupDataOutput);

        verify(mHealthConnectManager, times(1)).getAllBackupFileNames(false);
    }

    @Test
    public void onFullBackup_withExistingFile_truncatesFileBeforeBackup() throws IOException {
        // Purpose: Verifies that if a backup file already exists, it's truncated before writing
        // new backup data. This tests the flags used in openFileForWritingTruncateMode.
        String fileName = "testFile.txt";
        File testFile = new File(mBackupDataDirectory, fileName);
        String oldData = "OLD_DATA";
        String newData = "NEW_DATA";

        // Setup: Create a file with old data in the backup directory.
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(oldData);
        }
        assertThat(testFile.length()).isEqualTo(oldData.length());

        // Setup: Mock HealthConnectManager to return our test file name.
        when(mHealthConnectManager.getAllBackupFileNames(anyBoolean()))
                .thenReturn(Set.of(fileName));

        // Setup: When HealthConnectManager is asked to write data, write "NEW_DATA".
        doAnswer(
                        invocation -> {
                            Map<String, ParcelFileDescriptor> pfds = invocation.getArgument(0);
                            try (ParcelFileDescriptor pfd = pfds.get(fileName);
                                    FileWriter writer = new FileWriter(pfd.getFileDescriptor())) {
                                writer.write(newData);
                            }
                            return null;
                        })
                .when(mHealthConnectManager)
                .getAllDataForBackup(any());

        // Action: Run the full backup.
        mHealthConnectBackupAgent.onFullBackup(mFullBackupDataOutput);

        // Assertion: Check that the file content read during backupFile was the new data.
        assertThat(mHealthConnectBackupAgent.getBackedUpFileContent(fileName)).isEqualTo(newData);
    }

    @Test
    public void testOnRestore_doesNotStageAnything() throws IOException {
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile1");
        createAndGetNonEmptyFile(mHealthConnectBackupAgent.getBackupDataDir(), "testFile2");
        Set<String> expectedStagedFileNames =
                Stream.of(mHealthConnectBackupAgent.getBackupDataDir().listFiles())
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet());
        assertThat(expectedStagedFileNames.size()).isEqualTo(2);

        mHealthConnectBackupAgent.onRestore(mBackupDataInput, 1, mNewState);
        verify(mHealthConnectManager, never())
                .stageAllHealthConnectRemoteData(mPfdsByFileNameCaptor.capture(), any(), any());
    }

    private static void createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
    }

    /** A testable {@link HealthConnectBackupAgent} */
    public class TestableHealthConnectBackupAgent extends HealthConnectBackupAgent {
        List<File> mBackedUpFiles = new ArrayList<>();
        Map<String, String> mBackedUpFileContents = new HashMap<>();

        @Override
        public Context getBaseContext() {
            return mContext;
        }

        @Override
        File getBackupDataDir() {
            return mBackupDataDirectory;
        }

        @Override
        HealthConnectManager getHealthConnectService() {
            return mHealthConnectManager;
        }

        @Override
        void backupFile(File file, FullBackupDataOutput data) {
            mBackedUpFiles.add(file);
            // Read file content during backup for verification
            try {
                mBackedUpFileContents.put(
                        file.getName(), new String(Files.readAllBytes(file.toPath())));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read backup file for test verification", e);
            }
        }

        public String getBackedUpFileContent(String fileName) {
            return mBackedUpFileContents.get(fileName);
        }
    }
}
