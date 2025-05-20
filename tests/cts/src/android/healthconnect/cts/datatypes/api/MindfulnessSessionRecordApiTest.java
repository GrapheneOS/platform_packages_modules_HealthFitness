/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.datatypes.api;

import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.healthconnect.testing.shared.recordfactory.MindfulnessSessionRecordFactory;
import android.platform.test.annotations.AppModeFull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class MindfulnessSessionRecordApiTest extends BaseApiTest<MindfulnessSessionRecord> {
    public MindfulnessSessionRecordApiTest() {
        super(
                () -> MindfulnessSessionRecord.class,
                HealthPermissions.READ_MINDFULNESS,
                HealthPermissions.WRITE_MINDFULNESS,
                new MindfulnessSessionRecordFactory());
    }
}
