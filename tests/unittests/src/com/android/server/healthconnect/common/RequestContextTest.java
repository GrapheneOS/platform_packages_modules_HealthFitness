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

package com.android.server.healthconnect.common;

import static com.google.common.truth.Truth.assertThat;

import android.os.Binder;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RequestContextTest {

    @Test
    public void getCallingApplicationUid_equalToBinderCallingUid() {
        RequestContext requestContext = RequestContext.create();

        // Note: since we're not executing this in a binder thread, it just returns the current
        // (test app) application's UID. This is not therefore a realistic test, but at least
        // verifies that a value has been set. CTS tests will exercise this realistically.
        // The same applies to subsequent tests that rely on binder state.
        assertThat(requestContext.getCallingApplicationUid()).isEqualTo(Binder.getCallingUid());
    }

    @Test
    public void getCallingUser_equalToBinderCallingUser() {
        RequestContext requestContext = RequestContext.create();

        assertThat(requestContext.getCallingUser()).isEqualTo(Binder.getCallingUserHandle());
    }

    @Test
    public void getCallingProcessId_equalToBinderCallingPid() {
        RequestContext requestContext = RequestContext.create();

        assertThat(requestContext.getCallingProcessId()).isEqualTo(Binder.getCallingPid());
    }
}
