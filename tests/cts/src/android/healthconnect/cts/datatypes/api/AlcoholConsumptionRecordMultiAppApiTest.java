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
package android.healthconnect.cts.datatypes.api;

import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.AlcoholConsumptionRecord;
import android.healthconnect.testing.shared.recordfactory.AlcoholConsumptionRecordFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.runner.RunWith;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_ALCOHOL_CONSUMPTION,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB,
    Flags.FLAG_SMOKING_DB
})
public class AlcoholConsumptionRecordMultiAppApiTest
        extends BaseMultiAppApiTest<AlcoholConsumptionRecord> {
    public AlcoholConsumptionRecordMultiAppApiTest() {
        super(
                () -> AlcoholConsumptionRecord.class,
                HealthPermissions.READ_ALCOHOL_CONSUMPTION,
                HealthPermissions.WRITE_ALCOHOL_CONSUMPTION,
                new AlcoholConsumptionRecordFactory());
    }
}
