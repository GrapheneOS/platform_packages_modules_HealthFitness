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

package android.healthconnect.testing.unittest.mocks;

import static com.android.server.healthconnect.device.DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import org.mockito.Mockito;

/**
 * A helper class to simulate the default "android" system package.
 *
 * <p>For records coming from the device itself, they are labeled with coming from the "android"
 * package. This exists on real devices, but does not exist on Robolectric. So to keep consistency
 * across tests, this class helps provide mock versions for the "android" package information.
 */
public class AndroidPackageMocker {

    /**
     * Add a mock version of the "android" package information to the given mock or spy Context.
     *
     * @param spyContext the context to modify
     */
    public static void addToContext(Context spyContext)
            throws PackageManager.NameNotFoundException {
        // This is required as AppInfoHelper derives its context via this method.
        doReturn(spyContext).when(spyContext).createContextAsUser(any(), anyInt());
        Drawable mockDrawable = Mockito.mock(Drawable.class);
        PackageManager mockPackageManager = Mockito.mock(PackageManager.class);

        when(spyContext.getPackageManager()).thenReturn(mockPackageManager);
        // The "android" package is always present on real devices, however Robolectric does not
        // attempt to simulate this, so we need to mock it here.
        ApplicationInfo fakeSystemPackage = new ApplicationInfo();
        doReturn(fakeSystemPackage)
                .when(mockPackageManager)
                .getApplicationInfo(eq(DEVICE_DATA_PROVIDER_PACKAGE), any());
        doReturn("Android System").when(mockPackageManager).getApplicationLabel(fakeSystemPackage);
        when(mockDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mockDrawable.getIntrinsicWidth()).thenReturn(200);
        doReturn(mockDrawable).when(mockPackageManager).getApplicationIcon(fakeSystemPackage);
    }
}
