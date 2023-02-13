/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.permission;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Environment;
import android.os.UserHandle;
import android.util.ArrayMap;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.server.LocalManagerRegistry;
import com.android.server.appop.AppOpsManagerLocal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.io.File;
import java.time.Instant;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class GrantTimePersistenceUnitTest {
    private static final UserGrantTimeState DEFAULT_STATE =
            new UserGrantTimeState(
                    Map.of("package1", Instant.ofEpochSecond((long) 1e8)),
                    Map.of("shared_user1", Instant.ofEpochSecond((long) 1e7)),
                    1);

    private static final UserGrantTimeState SHARED_USERS_STATE =
            new UserGrantTimeState(
                    new ArrayMap<>(),
                    Map.of(
                            "shared_user1",
                            Instant.ofEpochSecond((long) 1e7),
                            "shared_user2",
                            Instant.ofEpochSecond((long) 1e5)),
                    2);

    private static final UserGrantTimeState PACKAGES_STATE =
            new UserGrantTimeState(
                    Map.of(
                            "package1",
                            Instant.ofEpochSecond((long) 1e7),
                            "package2",
                            Instant.ofEpochSecond((long) 1e5)),
                    new ArrayMap<>(),
                    2);

    private static final UserGrantTimeState EMPTY_STATE =
            new UserGrantTimeState(new ArrayMap<>(), new ArrayMap<>(), 3);

    private MockitoSession mStaticMockSession;
    private final UserHandle mUser = UserHandle.of(UserHandle.myUserId());
    private File mMockDataDirectory;
    @Mock private AppOpsManagerLocal mAppOpsManagerLocal;

    @Before
    public void mockApexEnvironment() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(Environment.class)
                        .mockStatic(LocalManagerRegistry.class)
                        .strictness(Strictness.LENIENT)
                        .startMocking();

        Context context = InstrumentationRegistry.getContext();
        mMockDataDirectory = context.getDir("mock_data", Context.MODE_PRIVATE);
        Mockito.when(Environment.getDataDirectory()).thenReturn(mMockDataDirectory);
        when(LocalManagerRegistry.getManager(AppOpsManagerLocal.class))
                .thenReturn(mAppOpsManagerLocal);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
        deleteFile(mMockDataDirectory);
    }

    @Test
    public void testWriteReadData_packageAndSharedUserState_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(DEFAULT_STATE, mUser);
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, DEFAULT_STATE);
    }

    @Test
    public void testWriteReadData_multipleSharedUserState_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(SHARED_USERS_STATE, mUser);
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, SHARED_USERS_STATE);
    }

    @Test
    public void testWriteReadData_multiplePackagesState_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(PACKAGES_STATE, mUser);
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, PACKAGES_STATE);
    }

    @Test
    public void testWriteReadData_emptyState_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(EMPTY_STATE, mUser);
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, EMPTY_STATE);
    }

    @Test
    public void testWriteReadData_overwroteState_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(PACKAGES_STATE, mUser);
        datastore.writeForUser(DEFAULT_STATE, mUser);
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, DEFAULT_STATE);
    }

    @Test
    public void testWriteReadData_statesForTwoUsersWritten_restoredCorrectly() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        datastore.writeForUser(PACKAGES_STATE, mUser);
        datastore.writeForUser(SHARED_USERS_STATE, UserHandle.of(10));
        UserGrantTimeState restoredState = datastore.readForUser(mUser);
        assertRestoredStateIsCorrect(restoredState, PACKAGES_STATE);
        UserGrantTimeState restoredState2 = datastore.readForUser(UserHandle.of(10));
        assertRestoredStateIsCorrect(restoredState2, SHARED_USERS_STATE);
    }

    @Test
    public void testReadData_stateIsNotWritten_nullIsReturned() {
        FirstGrantTimeDatastore datastore = FirstGrantTimeDatastore.createInstance();
        UserGrantTimeState state = datastore.readForUser(mUser);
        assertThat(state).isNull();
    }

    private static void deleteFile(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteFile(f);
            }
        }
        assertThat(file.delete()).isTrue();
    }

    private static void assertRestoredStateIsCorrect(
            UserGrantTimeState restoredState, UserGrantTimeState initialState) {
        assertThat(initialState.getVersion()).isEqualTo(restoredState.getVersion());
        assertThat(initialState.getPackageGrantTimes())
                .isEqualTo(restoredState.getPackageGrantTimes());
        assertThat(initialState.getSharedUserGrantTimes())
                .isEqualTo(restoredState.getSharedUserGrantTimes());
    }
}
