/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.tests.migration;

import android.healthconnect.migration.DataMigrationFields;

import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class DataMigrationFieldsTest {

    private static final Map<String, String> sFields = new HashMap<>();

    static {
        sFields.put("DM_PERMISSION_PACKAGE_NAME", "packageName");
        sFields.put("DM_PERMISSION_PERMISSION_NAMES", "permissionNames");
        sFields.put("DM_PERMISSION_FIRST_GRANT_TIME", "firstGrantTime");

        sFields.put("DM_RECORD_TYPE", "type");
        sFields.put("DM_RECORD_LAST_MODIFIED_TIME", "lastModifiedTime");
        sFields.put("DM_RECORD_CLIENT_RECORD_ID", "clientRecordId");
        sFields.put("DM_RECORD_APP_INFO", "appInfo");
        sFields.put("DM_RECORD_DEVICE_INFO", "deviceInfo");
        sFields.put("DM_RECORD_TIME", "time");
        sFields.put("DM_RECORD_ZONE_OFFSET", "zoneOffset");
        sFields.put("DM_RECORD_START_TIME", "startTime");
        sFields.put("DM_RECORD_START_ZONE_OFFSET", "startZoneOffset");
        sFields.put("DM_RECORD_END_TIME", "endTime");
        sFields.put("DM_RECORD_END_ZONE_OFFSET", "endZoneOffset");
        sFields.put("DM_RECORD_SAMPLES", "samples");
        sFields.put("DM_RECORD_SAMPLE_TIME", "time");
        sFields.put("DM_RECORD_HEIGHT_HEIGHT", "height");
        sFields.put("DM_RECORD_STEPS_COUNT", "count");
        sFields.put("DM_RECORD_POWER_WATTS", "watts");

        sFields.put("DM_APP_INFO_PACKAGE_NAME", "packageName");
        sFields.put("DM_APP_INFO_APP_NAME", "appName");

        sFields.put("DM_DEVICE_INFO_MANUFACTURER", "manufacturer");
        sFields.put("DM_DEVICE_INFO_MODEL", "model");
        sFields.put("DM_DEVICE_INFO_TYPE", "type");
    }

    @Rule public final Expect mExpect = Expect.create();

    private static String getFieldValue(String name) {
        try {
            return (String) DataMigrationFields.class.getField(name).get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    @Test
    public void fieldValuesNotChanged() {
        sFields.forEach((name, value) -> mExpect.that(getFieldValue(name)).isEqualTo(value));
    }

    @Test
    public void allFieldsAreTested() {
        for (Field field : DataMigrationFields.class.getFields()) {
            mExpect.that(sFields.keySet()).contains(field.getName());
        }
    }
}
