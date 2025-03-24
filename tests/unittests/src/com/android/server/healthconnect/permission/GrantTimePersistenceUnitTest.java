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

import static com.android.server.healthconnect.permission.FirstGrantTimeDatastore.DATA_TYPE_CURRENT;
import static com.android.server.healthconnect.permission.FirstGrantTimeDatastore.DATA_TYPE_STAGED;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final UserHandle mUser = UserHandle.of(UserHandle.myUserId());
    private FirstGrantTimeDatastore mDatastore;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(temporaryFolder.getRoot())
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .build();
        mDatastore = healthConnectInjector.getFirstGrantTimeDatastore();
    }

    @Test
    public void testWriteReadData_packageAndSharedUserState_restoredCorrectly() {
        mDatastore.writeForUser(DEFAULT_STATE, mUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, DEFAULT_STATE);
    }

    @Test
    public void testWriteReadData_multipleSharedUserState_restoredCorrectly() {
        mDatastore.writeForUser(SHARED_USERS_STATE, mUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, SHARED_USERS_STATE);
    }

    @Test
    public void testWriteReadData_multiplePackagesState_restoredCorrectly() {
        mDatastore.writeForUser(PACKAGES_STATE, mUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, PACKAGES_STATE);
    }

    @Test
    public void testWriteReadData_emptyState_restoredCorrectly() {
        mDatastore.writeForUser(EMPTY_STATE, mUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, EMPTY_STATE);
    }

    @Test
    public void testWriteReadData_overwroteState_restoredCorrectly() {
        mDatastore.writeForUser(PACKAGES_STATE, mUser, DATA_TYPE_CURRENT);
        mDatastore.writeForUser(DEFAULT_STATE, mUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, DEFAULT_STATE);
    }

    @Test
    public void testWriteReadData_writeAllStateTypes_restoredCorrectly() {
        mDatastore.writeForUser(PACKAGES_STATE, mUser, DATA_TYPE_CURRENT);
        mDatastore.writeForUser(EMPTY_STATE, mUser, DATA_TYPE_STAGED);
        assertRestoredStateIsCorrect(
                mDatastore.readForUser(mUser, DATA_TYPE_CURRENT), PACKAGES_STATE);
        assertRestoredStateIsCorrect(mDatastore.readForUser(mUser, DATA_TYPE_STAGED), EMPTY_STATE);
    }

    @Test
    public void testWriteReadData_statesForTwoUsersWritten_restoredCorrectly() {
        UserHandle secondUser = UserHandle.of(mUser.getIdentifier() + 10);
        mDatastore.writeForUser(PACKAGES_STATE, mUser, DATA_TYPE_CURRENT);
        mDatastore.writeForUser(SHARED_USERS_STATE, secondUser, DATA_TYPE_CURRENT);
        UserGrantTimeState restoredState = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState, PACKAGES_STATE);
        UserGrantTimeState restoredState2 = mDatastore.readForUser(secondUser, DATA_TYPE_CURRENT);
        assertRestoredStateIsCorrect(restoredState2, SHARED_USERS_STATE);
    }

    @Test
    public void testReadData_stateIsNotWritten_nullIsReturned() {
        UserGrantTimeState state = mDatastore.readForUser(mUser, DATA_TYPE_CURRENT);
        assertThat(state).isNull();
    }

    @Test
    public void testParseData_stateIsNotWritten_nullIsReturned() {
        UserGrantTimeState state =
                new GrantTimeXmlHelper()
                        .parseGrantTime(new File(temporaryFolder.getRoot(), "test_file.xml"));
        assertThat(state).isNull();
    }

    @Test
    public void testWriteData_writeAndReadState_restoredEqualToWritten() {
        File testFile = new File(temporaryFolder.getRoot(), "test_file.xml");
        GrantTimeXmlHelper grantTimeXmlHelper = new GrantTimeXmlHelper();
        grantTimeXmlHelper.serializeGrantTimes(testFile, DEFAULT_STATE);
        UserGrantTimeState state = grantTimeXmlHelper.parseGrantTime(testFile);
        assertRestoredStateIsCorrect(state, DEFAULT_STATE);
    }

    @Test
    public void testGetFile_getAllTypes_allFilesNonNullAndDifferent() {
        File current = mDatastore.getFile(mUser, DATA_TYPE_CURRENT);
        File staged = mDatastore.getFile(mUser, DATA_TYPE_STAGED);
        assertThat(current).isNotNull();
        assertThat(staged).isNotNull();
        assertThat(current).isNotEqualTo(staged);
    }

    private static void assertRestoredStateIsCorrect(
            UserGrantTimeState actual, UserGrantTimeState expected) {
        assertThat(actual.getVersion()).isEqualTo(expected.getVersion());
        assertThat(actual.getPackageGrantTimes()).isEqualTo(expected.getPackageGrantTimes());
        assertThat(actual.getSharedUserGrantTimes()).isEqualTo(expected.getSharedUserGrantTimes());
    }
}
