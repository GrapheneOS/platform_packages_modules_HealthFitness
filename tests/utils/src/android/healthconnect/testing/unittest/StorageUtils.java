/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.healthconnect.testing.unittest;

import android.database.DatabaseUtils;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;

public class StorageUtils {

    private final TransactionManager mTransactionManager;

    public StorageUtils(HealthConnectInjector healthConnectInjector) {
        mTransactionManager = healthConnectInjector.getTransactionManager();
    }

    /** Returns the number of rows in the specified table. */
    public static long queryNumEntries(HealthConnectDatabase database, String tableName) {
        return DatabaseUtils.queryNumEntries(database.getReadableDatabase(), tableName);
    }

    /** Returns the number of rows in the specified table. */
    public long queryNumEntries(String tableName) {
        return mTransactionManager.queryNumEntries(tableName);
    }
}
