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
 * A base class for migration payloads. There is no need extend or instantiate this class, use
 * existing subclasses instead. <br>
 * <br>
 *
 * <p>Steps when adding a new type:
 *
 * <ul>
 *   <li>Create a new class, make sure it extends {@code MigrationPayload} and implements {@link
 *       android.os.Parcelable}.
 *   <li>Handle the new class in {@link MigrationEntity#writeToParcel(android.os.Parcel, int)} and
 *       in {@link MigrationEntity}'s constructor.
 *   <li>Handle the new class in {@link
 *       com.android.server.healthconnect.migration.DataMigrationManager}
 * </ul>
 *
 * Refer to existing subclasses for details.
 *
 * @hide
 */
public class MigrationPayload {

    /** Package-private constructor - instances and custom subclasses are not allowed. */
    MigrationPayload() {}
}
