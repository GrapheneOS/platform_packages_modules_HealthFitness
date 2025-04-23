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

package android.healthconnect.cts.lib;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static com.android.compatibility.common.util.SystemUtil.eventually;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.ExternalResource;

import java.util.List;

/** Test rule that manages the state of a test app. */
public class TestAppRule extends ExternalResource {

    private final Context mContext;
    private final String mPackageName;
    private final boolean mInBackground;
    private List<String> mRequestedPermissions;

    private TestAppRule(Builder builder) {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageName = builder.mPackageName;
        mInBackground = builder.mInBackground;
    }

    /** Returns a proxy for performing operations via the test app. */
    public TestAppProxy getProxy() {
        return mInBackground
                ? TestAppProxy.forPackageNameInBackground(mPackageName)
                : TestAppProxy.forPackageName(mPackageName);
    }

    @Override
    protected void before() throws Throwable {
        PackageManager packageManager = mContext.getPackageManager();
        PackageInfo packageInfo =
                packageManager.getPackageInfo(mPackageName, PackageManager.GET_PERMISSIONS);

        mRequestedPermissions =
                packageInfo.requestedPermissions == null
                        ? List.of()
                        : List.of(packageInfo.requestedPermissions);

        if (mInBackground) {
            // Ensure that App Ops considers the test app to be in the background. This may take a
            // few seconds if another test has recently launched it in the foreground. We rely on
            // the app being granted ACCESS_FINE_LOCATION to check this.
            assertThat(mRequestedPermissions).contains(ACCESS_FINE_LOCATION);
            AppOpsManager appOpsManager =
                    requireNonNull(mContext.getSystemService(AppOpsManager.class));
            int uid = packageManager.getPackageUid(mPackageName, /* flags= */ 0);
            eventually(
                    () ->
                            assertThat(
                                            appOpsManager.unsafeCheckOp(
                                                    AppOpsManager.OPSTR_FINE_LOCATION,
                                                    uid,
                                                    mPackageName))
                                    .isEqualTo(AppOpsManager.MODE_IGNORED));
        }
    }

    /** Builder for the rule. */
    public static final class Builder {
        private final String mPackageName;
        private boolean mInBackground;

        /** Constructs a builder for the test app with the given package name. */
        public Builder(String packageName) {
            mPackageName = packageName;
        }

        /**
         * Sets whether the test app should perform operations in the background.
         *
         * <p>By default operations will be performed in the foreground.
         */
        public Builder setInBackground(boolean inBackground) {
            mInBackground = inBackground;
            return this;
        }

        /** Builds the rule. */
        public TestAppRule build() {
            return new TestAppRule(this);
        }
    }
}
