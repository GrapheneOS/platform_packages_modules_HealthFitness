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

package com.android.server.healthconnect.phr;

import static android.healthconnect.testing.shared.phr.PhrDataFactory.getMedicalResource;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.MedicalResource;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesInternalResponseTest {

    @Test
    public void testReadMedicalResourcesInternalResponse_nonNullPageToken_success() {
        String pageToken = "aw==";
        List<MedicalResource> medicalResources = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, 1);

        assertThat(response.getMedicalResources()).isEqualTo(medicalResources);
        assertThat(response.getPageToken()).isEqualTo(pageToken);
        assertThat(response.getRemainingCount()).isEqualTo(1);
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_nullPageToken_success() {
        List<MedicalResource> medicalResources = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response =
                new ReadMedicalResourcesInternalResponse(medicalResources, null, 0);

        assertThat(response.getMedicalResources()).isEqualTo(medicalResources);
        assertThat(response.getPageToken()).isEqualTo(null);
        assertThat(response.getRemainingCount()).isEqualTo(0);
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_equals_sameResponse_success() {
        String pageToken = "aw==";
        List<MedicalResource> medicalResources = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, 1);

        assertThat(response.equals(response)).isTrue();
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_equals_differentPageToken_success() {
        String pageToken1 = "aw==";
        String pageToken2 = "ba==";
        List<MedicalResource> medicalResources = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken1, 2);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken2, 2);

        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void
            testReadMedicalResourcesInternalResponse_equals_differentMedicalResources_success() {
        String pageToken = "aw==";
        List<MedicalResource> medicalResources1 = List.of(getMedicalResource());
        List<MedicalResource> medicalResources2 = List.of();

        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources1, pageToken, 2);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources2, pageToken, 2);

        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_equals_differentTotalSize() {
        String pageToken = "aw==";
        List<MedicalResource> medicalResources = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, 2);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, 3);

        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_equals_twoFieldsDifferent_success() {
        String pageToken1 = "aw==";
        String pageToken2 = "ba==";
        List<MedicalResource> medicalResources1 = List.of(getMedicalResource());
        List<MedicalResource> medicalResources2 = List.of();

        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources1, pageToken1, 10);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources2, pageToken2, 10);

        assertThat(response1.equals(response2)).isFalse();
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_equals_allFieldsTheSame_success() {
        String pageToken1 = "aw==";
        String pageToken2 = "aw==";
        List<MedicalResource> medicalResources1 = List.of(getMedicalResource());
        List<MedicalResource> medicalResources2 = List.of(getMedicalResource());

        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources1, pageToken1, 10);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources2, pageToken2, 10);

        assertThat(response1.equals(response2)).isTrue();
    }

    @Test
    public void testReadMedicalResourcesInternalResponse_hashCode_allFieldsTheSame_success() {
        String pageToken = "aw==";
        List<MedicalResource> medicalResources = List.of(getMedicalResource());
        int totalSize = 10;
        ReadMedicalResourcesInternalResponse response1 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, totalSize);
        ReadMedicalResourcesInternalResponse response2 =
                new ReadMedicalResourcesInternalResponse(medicalResources, pageToken, totalSize);

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
}
