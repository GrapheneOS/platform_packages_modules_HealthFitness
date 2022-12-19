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

package android.healthconnect.migration;

/**
 * Constants for data migration fields.
 *
 * @hide
 */
public final class DataMigrationFields {

    public static final String DM_PERMISSION_PACKAGE_NAME = "packageName";
    public static final String DM_PERMISSION_PERMISSION_NAMES = "permissionNames";
    public static final String DM_PERMISSION_FIRST_GRANT_TIME = "firstGrantTime";

    public static final String DM_RECORD_TYPE = "type";
    public static final String DM_RECORD_LAST_MODIFIED_TIME = "lastModifiedTime";
    public static final String DM_RECORD_CLIENT_RECORD_ID = "clientRecordId";
    public static final String DM_RECORD_APP_INFO = "appInfo";
    public static final String DM_RECORD_DEVICE_INFO = "deviceInfo";
    public static final String DM_RECORD_TIME = "time";
    public static final String DM_RECORD_ZONE_OFFSET = "zoneOffset";
    public static final String DM_RECORD_START_TIME = "startTime";
    public static final String DM_RECORD_START_ZONE_OFFSET = "startZoneOffset";
    public static final String DM_RECORD_END_TIME = "endTime";
    public static final String DM_RECORD_END_ZONE_OFFSET = "endZoneOffset";
    public static final String DM_RECORD_SAMPLES = "samples";
    public static final String DM_RECORD_SAMPLE_TIME = "time";
    public static final String DM_RECORD_HEIGHT_HEIGHT = "height";
    public static final String DM_RECORD_STEPS_COUNT = "count";
    public static final String DM_RECORD_POWER_WATTS = "watts";

    public static final String DM_APP_INFO_PACKAGE_NAME = "packageName";
    public static final String DM_APP_INFO_APP_NAME = "appName";

    public static final String DM_DEVICE_INFO_MANUFACTURER = "manufacturer";
    public static final String DM_DEVICE_INFO_MODEL = "model";
    public static final String DM_DEVICE_INFO_TYPE = "type";

    private DataMigrationFields() {}
}
