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

package com.android.server.healthconnect.backuprestore;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataRequest;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.ArrayMap;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.utils.FilesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/** Unit test for class {@link BackupRestore} */
@RunWith(AndroidJUnit4.class)
public class BackupRestoreTest {
    private static final String DATABASE_NAME = "healthconnect.db";
    private static final String GRANT_TIME_FILE_NAME = "health-permissions-first-grant-times.xml";
    @Mock private TransactionManager mTransactionManager;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private MigrationStateManager mMigrationStateManager;
    @Mock private Context mContext;
    @Mock private Context mServiceContext;
    private BackupRestore mBackupRestore;
    private MockitoSession mStaticMockSession;
    private UserHandle mUserHandle = UserHandle.of(UserHandle.myUserId());
    private File mMockDataDirectory;
    private File mMockBackedDataDirectory;

    @Before
    public void setUp() throws Exception {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(Environment.class)
                        .mockStatic(PreferenceHelper.class)
                        .mockStatic(TransactionManager.class)
                        .strictness(Strictness.LENIENT)
                        .startMocking();
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mMockDataDirectory = mContext.getDir("mock_data", Context.MODE_PRIVATE);
        mMockBackedDataDirectory = mContext.getDir("mock_backed_data", Context.MODE_PRIVATE);
        when(Environment.getDataDirectory()).thenReturn(mMockDataDirectory);
        when(Environment.getDataDirectory()).thenReturn(mMockBackedDataDirectory);
        when(PreferenceHelper.getInstance()).thenReturn(mPreferenceHelper);
        when(TransactionManager.getInitialisedInstance()).thenReturn(mTransactionManager);

        mBackupRestore =
                new BackupRestore(mFirstGrantTimeManager, mMigrationStateManager, mServiceContext);
    }

    @After
    public void tearDown() {
        FilesUtil.deleteDir(mMockDataDirectory);
        FilesUtil.deleteDir(mMockBackedDataDirectory);
        clearInvocations(mPreferenceHelper);
        clearInvocations(mTransactionManager);
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testGetAllBackupFileNames_forDeviceToDevice_returnsAllFileNames() throws Exception {
        File dbFile = createAndGetNonEmptyFile(mMockDataDirectory, DATABASE_NAME);
        File grantTimeFile = createAndGetNonEmptyFile(mMockDataDirectory, GRANT_TIME_FILE_NAME);
        when(mTransactionManager.getDatabasePath()).thenReturn(dbFile);
        when(mFirstGrantTimeManager.getFile(mUserHandle)).thenReturn(grantTimeFile);

        BackupFileNamesSet backupFileNamesSet =
                mBackupRestore.getAllBackupFileNames(mUserHandle, true);

        assertThat(backupFileNamesSet).isNotNull();
        assertThat(backupFileNamesSet.getFileNames()).hasSize(2);
        assertThat(backupFileNamesSet.getFileNames()).contains(DATABASE_NAME);
        assertThat(backupFileNamesSet.getFileNames()).contains(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testGetAllBackupFileNames_forNonDeviceToDevice_returnsSmallFileNames()
            throws Exception {
        File dbFile = createAndGetNonEmptyFile(mMockDataDirectory, DATABASE_NAME);
        File grantTimeFile = createAndGetNonEmptyFile(mMockDataDirectory, GRANT_TIME_FILE_NAME);
        when(mTransactionManager.getDatabasePath()).thenReturn(dbFile);
        when(mFirstGrantTimeManager.getFile(mUserHandle)).thenReturn(grantTimeFile);

        BackupFileNamesSet backupFileNamesSet =
                mBackupRestore.getAllBackupFileNames(mUserHandle, false);

        assertThat(backupFileNamesSet).isNotNull();
        assertThat(backupFileNamesSet.getFileNames()).hasSize(1);
        assertThat(backupFileNamesSet.getFileNames()).contains(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testGetAllBackupData_forDeviceToDevice_copiesAllData() throws Exception {
        File dbFileToBackup = createAndGetEmptyFile(mMockDataDirectory, DATABASE_NAME);
        File grantTimeFileToBackup =
                createAndGetEmptyFile(mMockDataDirectory, GRANT_TIME_FILE_NAME);
        File dbFileBacked = createAndGetEmptyFile(mMockBackedDataDirectory, DATABASE_NAME);
        File grantTimeFileBacked =
                createAndGetEmptyFile(mMockBackedDataDirectory, GRANT_TIME_FILE_NAME);

        when(mTransactionManager.getDatabasePath()).thenReturn(dbFileToBackup);
        when(mFirstGrantTimeManager.getFile(mUserHandle)).thenReturn(grantTimeFileToBackup);

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                dbFileBacked.getName(),
                ParcelFileDescriptor.open(dbFileBacked, ParcelFileDescriptor.MODE_READ_ONLY));
        pfdsByFileName.put(
                grantTimeFileBacked.getName(),
                ParcelFileDescriptor.open(
                        grantTimeFileBacked, ParcelFileDescriptor.MODE_READ_ONLY));

        mBackupRestore.getAllDataForBackup(new StageRemoteDataRequest(pfdsByFileName), mUserHandle);

        assertThat(dbFileBacked.length()).isEqualTo(dbFileToBackup.length());
        assertThat(grantTimeFileBacked.length()).isEqualTo(dbFileToBackup.length());
    }

    private static File createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    private static File createAndGetEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        file.createNewFile();
        return file;
    }
}
