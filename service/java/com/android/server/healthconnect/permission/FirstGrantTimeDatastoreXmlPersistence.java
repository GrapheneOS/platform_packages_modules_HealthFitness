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

import android.annotation.Nullable;
import android.health.connect.Constants;
import android.os.UserHandle;
import android.util.Log;

import com.android.server.healthconnect.utils.FilesUtil;

import java.io.File;

/**
 * XML implementation of {@link FirstGrantTimeDatastore}.
 *
 * @hide
 */
public class FirstGrantTimeDatastoreXmlPersistence implements FirstGrantTimeDatastore {
    private static final String TAG = "HealthConnectFirstGrantTimeDatastore";
    private static final String GRANT_TIME_FILE_NAME = "health-permissions-first-grant-times.xml";

    private static final String STAGED_GRANT_TIME_FILE_NAME =
            "staged-health-permissions-first-grant-times.xml";
    private final File mEnvironmentDataDirectory;
    private final GrantTimeXmlHelper mGrantTimeXmlHelper;

    public FirstGrantTimeDatastoreXmlPersistence(
            File environmentDataDirectory, GrantTimeXmlHelper grantTimeXmlHelper) {
        mEnvironmentDataDirectory = environmentDataDirectory;
        mGrantTimeXmlHelper = grantTimeXmlHelper;
    }

    /**
     * Read {@link UserGrantTimeState for given user}.
     *
     * @hide
     */
    @Nullable
    @Override
    public UserGrantTimeState readForUser(UserHandle user, @DataType int dataType) {
        File file = getFile(user, dataType);
        if (Constants.DEBUG) {
            Log.d(TAG, "Reading xml from " + file);
        }
        return mGrantTimeXmlHelper.parseGrantTime(file);
    }

    /**
     * Write {@link UserGrantTimeState for given user}.
     *
     * @hide
     */
    @Override
    public void writeForUser(
            UserGrantTimeState grantTimesState, UserHandle user, @DataType int dataType) {
        File file = getFile(user, dataType);
        if (Constants.DEBUG) {
            Log.d(TAG, "Writing xml to " + file);
        }
        mGrantTimeXmlHelper.serializeGrantTimes(file, grantTimesState);
    }

    @Override
    public File getFile(UserHandle user, @DataType int sourceType) {
        String fileName =
                sourceType == FirstGrantTimeDatastore.DATA_TYPE_CURRENT
                        ? GRANT_TIME_FILE_NAME
                        : STAGED_GRANT_TIME_FILE_NAME;
        return new File(
                FilesUtil.getDataSystemCeHCDirectoryForUser(
                        mEnvironmentDataDirectory, user.getIdentifier()),
                fileName);
    }
}
