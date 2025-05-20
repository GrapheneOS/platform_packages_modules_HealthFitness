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
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.android.compatibility.common.util.SystemUtil.eventually;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.healthconnect.testing.cts.PermissionUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test rule that manages the state of a test app. */
public class TestAppRule extends ExternalResource {

    private final Context mContext;
    private final String mPackageName;
    private final boolean mInBackground;
    private final Set<String> mPermissionsToRevoke;
    private final TestAppProxy mProxy;
    private String mTestName;

    private TestAppRule(Builder builder) {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageName = builder.mPackageName;
        mInBackground = builder.mInBackground;
        mPermissionsToRevoke = Set.copyOf(builder.mPermissionsToRevoke);
        mProxy =
                mInBackground
                        ? TestAppProxy.forPackageNameInBackground(mPackageName)
                        : TestAppProxy.forPackageName(mPackageName);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        mTestName = description.getMethodName();
        return super.apply(base, description);
    }

    /** Returns a proxy for performing operations via the test app. */
    public TestAppProxy getProxy() {
        return mProxy;
    }

    /** Grants the specified permission to the test app via {@link PackageManager}. */
    public void grantHealthPermission(String permission) {
        PermissionUtils.grantHealthPermission(mPackageName, permission);
    }

    /** Grants all the specified permissions to the test app via {@link PackageManager}. */
    public void grantHealthPermissions(Collection<String> permissions) {
        PermissionUtils.grantHealthPermissions(mPackageName, permissions);
    }

    /** Revokes the specified permission from the test app via {@link PackageManager}. */
    public void revokeHealthPermission(String permission) {
        PermissionUtils.revokeHealthPermission(mPackageName, permission, /* reason= */ mTestName);
    }

    /** Revokes all health permissions from the test app via {@link PackageManager}. */
    public void revokeAllHealthPermissions() {
        PermissionUtils.revokeAllHealthPermissions(mPackageName, /* reason= */ mTestName);
    }

    @Override
    protected void before() throws Throwable {
        // Force stop the app to clear any runtime state left over by a previous test.
        mProxy.forceStop();

        List<String> declaredPermissions =
                PermissionUtils.getDeclaredHealthPermissions(mPackageName);
        assertThat(declaredPermissions).containsAtLeastElementsIn(mPermissionsToRevoke);

        // Start from a consistent permissions state; a previous test may have left some revoked.
        List<String> permissionsToGrant =
                declaredPermissions.stream().filter(not(mPermissionsToRevoke::contains)).toList();
        PermissionUtils.grantHealthPermissions(mPackageName, permissionsToGrant);

        for (String permission : mPermissionsToRevoke) {
            PermissionUtils.revokeHealthPermission(
                    mPackageName, permission, /* reason= */ mTestName);
        }

        if (mInBackground) {
            // Ensure that App Ops considers the test app to be in the background. This may take a
            // few seconds if another test has recently launched it in the foreground. We rely on
            // the app being granted ACCESS_FINE_LOCATION to check this.
            PackageManager packageManager = mContext.getPackageManager();
            assertThat(packageManager.checkPermission(ACCESS_FINE_LOCATION, mPackageName))
                    .isEqualTo(PERMISSION_GRANTED);

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

    @Override
    protected void after() {
        // Leave permissions in a consistent state.
        PermissionUtils.grantAllHealthPermissions(mPackageName);
    }

    /** Builder for the rule. */
    public static final class Builder {
        private final String mPackageName;
        private boolean mInBackground;
        private final Set<String> mPermissionsToRevoke = new HashSet<>();

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

        /**
         * Adds a health permission that should be revoked before the tests. All other health
         * permissions will be granted.
         *
         * <p>After the test, any revoked permissions will be re-granted to the test app.
         */
        public Builder revokeHealthPermission(String permission) {
            mPermissionsToRevoke.add(permission);
            return this;
        }

        /** Builds the rule. */
        public TestAppRule build() {
            return new TestAppRule(this);
        }
    }
}
