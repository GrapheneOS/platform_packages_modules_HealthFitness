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

package com.android.server.healthconnect.phr;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DIFFERENT_DATA_SOURCE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.ReadMedicalResourcesInitialRequest;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

import java.util.Base64;

@RunWith(AndroidJUnit4.class)
public class PhrPageTokenWrapperTest {
    private static final int LAST_ROW_ID = 20;
    private static final String INVALID_PAGE_TOKEN_NON_BASE_64 = "2|3aw";
    private static final String INVALID_PAGE_TOKEN_WRONG_NUMBER_OF_TOKENS = "1,2,3,4";
    private static final String INVALID_PAGE_TOKEN_WITH_NON_INT_TOKENS = "a,1,2";
    private static final String INVALID_PAGE_TOKEN_WITH_NEGATIVE_LAST_ROW_ID =
            "-1,1," + DATA_SOURCE_ID;
    private static final String INVALID_PAGE_TOKEN_WITH_UNSUPPORTED_MEDICAL_RESOURCE_TYPE =
            "1,100," + DATA_SOURCE_ID;
    private static final String INVALID_PAGE_TOKEN_WITH_INVALID_DATA_SOURCE_ID = "1,2,dataSource1";

    @Test
    public void phrPageTokenWrapper_createUsingInitialRequest_success() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();

        PhrPageTokenWrapper pageTokenWrapper = PhrPageTokenWrapper.from(request.toParcel());

        assertThat(pageTokenWrapper.getRequest()).isEqualTo(request);
        assertThat(pageTokenWrapper.getLastRowId()).isEqualTo(DEFAULT_LONG);
    }

    @Test
    public void phrPageTokenWrapper_encodeAndDecodeWithoutDataSources_success() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        PhrPageTokenWrapper expected =
                PhrPageTokenWrapper.from(request.toParcel()).cloneWithNewLastRowId(LAST_ROW_ID);

        String pageToken = expected.encode();
        PhrPageTokenWrapper result = PhrPageTokenWrapper.from(pageToken);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void phrPageTokenWrapper_encodeAndDecodeWithAllFilters_success() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataSourceId(DATA_SOURCE_ID)
                        .addDataSourceId(DIFFERENT_DATA_SOURCE_ID)
                        .build();
        PhrPageTokenWrapper expected =
                PhrPageTokenWrapper.from(request.toParcel()).cloneWithNewLastRowId(LAST_ROW_ID);

        String pageToken = expected.encode();
        PhrPageTokenWrapper result = PhrPageTokenWrapper.from(pageToken);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void phrPageTokenWrapper_encodeAndDecodeWithoutFilters_success() {
        PhrPageTokenWrapper expected = PhrPageTokenWrapper.from(LAST_ROW_ID);

        String pageToken = expected.encode();
        PhrPageTokenWrapper result = PhrPageTokenWrapper.from(pageToken);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void phrPageTokenWrapper_encodeWithNegativeLastRowId_throws() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        assertThrows(
                IllegalStateException.class,
                () -> PhrPageTokenWrapper.from(request.toParcel()).encode());
    }

    @Test
    public void phrPageTokenWrapper_fromNonBase64PageToken_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PhrPageTokenWrapper.from(INVALID_PAGE_TOKEN_NON_BASE_64));
    }

    @Test
    public void phrPageTokenWrapper_pageTokenWithWrongNumberOfTokens_throws() {
        IllegalArgumentException exp =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PhrPageTokenWrapper.from(
                                        encodePageToken(
                                                INVALID_PAGE_TOKEN_WRONG_NUMBER_OF_TOKENS)));
        assertThat(exp.getMessage()).isEqualTo("Invalid pageToken");
    }

    @Test
    public void phrPageTokenWrapper_pageTokenWithNonIntTokens_throws() {
        assertThrows(
                NumberFormatException.class,
                () ->
                        PhrPageTokenWrapper.from(
                                encodePageToken(INVALID_PAGE_TOKEN_WITH_NON_INT_TOKENS)));
    }

    @Test
    public void phrPageTokenWrapper_pageTokenWithNegativeLastRowId_throws() {
        IllegalArgumentException exp =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PhrPageTokenWrapper.from(
                                        encodePageToken(
                                                INVALID_PAGE_TOKEN_WITH_NEGATIVE_LAST_ROW_ID)));
        assertThat(exp.getMessage()).isEqualTo("Invalid pageToken");
    }

    @Test
    public void phrPageTokenWrapper_pageTokenWithUnsupportedMedicalResourceType_throws() {
        ThrowingRunnable runnable =
                () ->
                        PhrPageTokenWrapper.from(
                                encodePageToken(
                                        INVALID_PAGE_TOKEN_WITH_UNSUPPORTED_MEDICAL_RESOURCE_TYPE));

        IllegalArgumentException exp = assertThrows(IllegalArgumentException.class, runnable);
        assertThat(exp.getMessage()).isEqualTo("Invalid pageToken");
    }

    @Test
    public void phrPageTokenWrapper_pageTokenWithInvalidDataSourceId_throws() {
        IllegalArgumentException exp =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                PhrPageTokenWrapper.from(
                                        encodePageToken(
                                                INVALID_PAGE_TOKEN_WITH_INVALID_DATA_SOURCE_ID)));
        assertThat(exp.getMessage()).isEqualTo("Invalid pageToken");
    }

    @Test
    public void phrPageTokenWrapper_pageTokenNull_returnsEmptyPageToken() {
        String pageTokenNull = null;

        assertThat(PhrPageTokenWrapper.fromPageTokenAllowingNull(pageTokenNull))
                .isEqualTo(PhrPageTokenWrapper.EMPTY_PAGE_TOKEN);
    }

    @Test
    public void phrPageTokenWrapper_pageTokenEmpty_returnsEmptyPageToken() {
        assertThat(PhrPageTokenWrapper.fromPageTokenAllowingNull(""))
                .isEqualTo(PhrPageTokenWrapper.EMPTY_PAGE_TOKEN);
    }

    @Test
    public void phrPageTokenWrapper_fromInvalidRowId_throws() {
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        assertThrows(
                IllegalStateException.class,
                () ->
                        PhrPageTokenWrapper.from(request.toParcel())
                                .cloneWithNewLastRowId(/* lastRowId= */ -1));
    }

    private String encodePageToken(String nonEncodedPageToken) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(nonEncodedPageToken.getBytes());
    }
}
