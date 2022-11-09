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

package com.android.server.healthconnect;

import android.content.Context;
import android.content.pm.PackageManager;
import android.healthconnect.HealthConnectManager;

import com.android.server.SystemService;
import com.android.server.healthconnect.storage.TransactionManager;

/**
 * HealthConnect system service scaffold.
 *
 * @hide
 */
public class HealthConnectManagerService extends SystemService {
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final TransactionManager mTransactionManager;
    private final Context mContext;

    public HealthConnectManagerService(Context context) {
        super(context);
        PackageManager packageManager = context.getPackageManager();
        mPermissionHelper =
                new HealthConnectPermissionHelper(
                        context,
                        packageManager,
                        HealthConnectManager.getHealthPermissions(context));
        mTransactionManager = TransactionManager.getInstance(getContext());
        mContext = context;
    }

    @Override
    public void onStart() {
        publishBinderService(
                Context.HEALTHCONNECT_SERVICE,
                new HealthConnectServiceImpl(mTransactionManager, mPermissionHelper, mContext));
    }
}
