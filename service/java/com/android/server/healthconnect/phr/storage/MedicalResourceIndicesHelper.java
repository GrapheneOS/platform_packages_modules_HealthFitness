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

package com.android.server.healthconnect.phr.storage;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;

import android.content.ContentValues;
import android.util.Pair;

import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for medical_resource_indices table. This is a child table of medical_resource_table
 * and will store extracted information from the FHIR JSONs (e.g. display name) and the category
 * each MedicalResource belongs to. For more context: http://shortn/_E5yENKUACX
 *
 * @hide
 */
public class MedicalResourceIndicesHelper {
    private static final String MEDICAL_RESOURCE_INDICES_TABLE_NAME =
            "medical_resource_indices_table";
    private static final String MEDICAL_RESOURCE_TYPE = "medical_resource_type";
    private static final String MEDICAL_RESOURCE_ID = "medical_resource_id";

    public static String getTableName() {
        return MEDICAL_RESOURCE_INDICES_TABLE_NAME;
    }

    public static String getParentColumnReference() {
        return MEDICAL_RESOURCE_ID;
    }

    public static String getMedicalResourceTypeColumnName() {
        return MEDICAL_RESOURCE_TYPE;
    }

    public static CreateTableRequest getCreateMedicalResourceIndicesTableRequest() {
        return new CreateTableRequest(
                        MEDICAL_RESOURCE_INDICES_TABLE_NAME,
                        getMedicalResourceIndicesTableColumnInfo())
                .addForeignKey(
                        MedicalResourceHelper.getMainTableName(),
                        Collections.singletonList(MEDICAL_RESOURCE_ID),
                        Collections.singletonList(MedicalResourceHelper.getPrimaryColumn()));
    }

    /** Creates {@link UpsertTableRequest} for medical_resource_indices table. */
    public static ContentValues getContentValues(long parentRowId, int medicalResourceType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MEDICAL_RESOURCE_TYPE, medicalResourceType);
        contentValues.put(MEDICAL_RESOURCE_ID, parentRowId);
        return contentValues;
    }

    private static List<Pair<String, String>> getMedicalResourceIndicesTableColumnInfo() {
        return List.of(
                Pair.create(MEDICAL_RESOURCE_ID, INTEGER_NOT_NULL),
                Pair.create(MEDICAL_RESOURCE_TYPE, INTEGER_NOT_NULL));
    }
}
