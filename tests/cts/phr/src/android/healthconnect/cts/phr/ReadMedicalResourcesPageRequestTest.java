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

package android.healthconnect.cts.phr;

import static android.healthconnect.cts.phr.utils.PhrDataFactory.PAGE_TOKEN;
import static android.healthconnect.cts.utils.DataFactory.DEFAULT_PAGE_SIZE;
import static android.healthconnect.cts.utils.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.utils.DataFactory.MINIMUM_PAGE_SIZE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.ReadMedicalResourcesPageRequest;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesPageRequestTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testRequestBuilder_requiredFieldsOnly() {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).build();

        assertThat(request.getPageToken()).isEqualTo(PAGE_TOKEN);
        assertThat(request.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testRequestBuilder_setAllFields() {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN)
                        .setPageToken("different_" + PAGE_TOKEN)
                        .setPageSize(100)
                        .build();

        assertThat(request.getPageToken()).isEqualTo("different_" + PAGE_TOKEN);
        assertThat(request.getPageSize()).isEqualTo(100);
    }

    @Test
    public void testRequestBuilder_lowerThanMinPageSize_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN)
                                .setPageSize(MAXIMUM_PAGE_SIZE + 1));
    }

    @Test
    public void testRequestBuilder_exceedsMaxPageSize_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN)
                                .setPageSize(MINIMUM_PAGE_SIZE - 1));
    }

    @Test
    public void testRequestBuilder_fromExistingBuilder() {
        ReadMedicalResourcesPageRequest.Builder original =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100);
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(original).build();

        assertThat(request).isEqualTo(original.build());
    }

    @Test
    public void testRequestBuilder_fromExistingInstance() {
        ReadMedicalResourcesPageRequest original =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100).build();
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(original).build();

        assertThat(request).isEqualTo(original);
    }

    @Test
    public void testToString() {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100).build();
        String pageTokenString = "pageToken=" + PAGE_TOKEN;
        String pageSizeString = "pageSize=100";

        assertThat(request.toString()).contains(pageTokenString);
        assertThat(request.toString()).contains(pageSizeString);
    }

    @Test
    public void testEquals_sameRequests() {
        ReadMedicalResourcesPageRequest request1 =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100).build();
        ReadMedicalResourcesPageRequest request2 =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).setPageSize(100).build();

        assertThat(request1.equals(request2)).isTrue();
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void testEquals_comparesAllValues() {
        ReadMedicalResourcesPageRequest request =
                new ReadMedicalResourcesPageRequest.Builder(PAGE_TOKEN).build();
        ReadMedicalResourcesPageRequest requestDifferentPageToken =
                new ReadMedicalResourcesPageRequest.Builder("different" + PAGE_TOKEN).build();
        ReadMedicalResourcesPageRequest requestDifferentPageSize =
                new ReadMedicalResourcesPageRequest.Builder(request).setPageSize(100).build();

        assertThat(requestDifferentPageToken.equals(request)).isFalse();
        assertThat(requestDifferentPageSize.equals(request)).isFalse();
        assertThat(requestDifferentPageToken.hashCode()).isNotEqualTo(request.hashCode());
        assertThat(requestDifferentPageSize.hashCode()).isNotEqualTo(request.hashCode());
    }
}
