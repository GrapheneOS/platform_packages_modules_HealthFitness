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

package com.android.server.healthconnect.device;

/**
 * A {@link DeviceDataSourceHelper} that overrides the serial number of the device to a fake value.
 *
 * <p>This is designed to be used in testing, so that {@link android.os.Build#getSerial} doesn't
 * need to be called in a context where the permission is not present.
 */
public class FakeSerialDeviceDataSourceHelper extends DeviceDataSourceHelper {

    /** The string that will be returned as the serial number. */
    public static final String TEST_SERIAL_NUMBER = "TEST_SERIAL_NUMBER";

    @Override
    String getSerial() {
        return TEST_SERIAL_NUMBER;
    }
}
