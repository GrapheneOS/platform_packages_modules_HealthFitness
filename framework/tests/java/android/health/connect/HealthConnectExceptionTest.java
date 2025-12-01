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

package android.health.connect;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class HealthConnectExceptionTest {
    private final String mCanonicalSpn =
            "com.android.healthconnect.watch.dae97b731dde83745b62b9111deee3456";

    private final String mMaskedSpn =
            "com.android.healthconnect.watch.jc45bd741a7123764b514e2c27df9fe41";

    private final Throwable mSensitiveCause =
            new RuntimeException("Could not find " + mCanonicalSpn);

    @Test
    public void redaction_withSensitiveCause_copiesStackTrace() {
        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, "Error", mSensitiveCause);

        assertThat(exception.getCause().getStackTrace()).isEqualTo(mSensitiveCause.getStackTrace());
    }

    @Test
    public void redaction_withNestedSensitiveCauses_redactsCauseRecursively() {
        Throwable nestedCause =
                new IllegalArgumentException(
                        "Some error " + mCanonicalSpn,
                        new IllegalArgumentException(
                                "Nested error " + mCanonicalSpn,
                                new IllegalArgumentException(
                                        "Deep nested error " + mCanonicalSpn)));

        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, "Error", nestedCause);

        String actualCauseMessage = exception.getCause().toString();
        assertThat(actualCauseMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualCauseMessage).contains("com.android.healthconnect.watch");

        String actualNestedCauseMessage = exception.getCause().getCause().toString();
        assertThat(actualNestedCauseMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualNestedCauseMessage).contains("com.android.healthconnect.watch");

        String actualDeepNestedCauseMessage = exception.getCause().getCause().getCause().toString();
        assertThat(actualDeepNestedCauseMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualDeepNestedCauseMessage).contains("com.android.healthconnect.watch");
    }

    @Test
    public void redaction_withDeepNestedSensitiveCauseOnly_redactsCauseRecursively() {
        Throwable nestedCause =
                new IllegalArgumentException(
                        "Some error " + mMaskedSpn,
                        new IllegalArgumentException(
                                "Nested error " + mMaskedSpn,
                                new IllegalArgumentException(
                                        "Deep nested error " + mCanonicalSpn)));

        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, "Error", nestedCause);

        String actualCauseMessage = exception.getCause().toString();
        assertThat(actualCauseMessage)
                .isEqualTo("java.lang.IllegalArgumentException: Some error " + mMaskedSpn);

        String actualNestedCauseMessage = exception.getCause().getCause().toString();
        assertThat(actualNestedCauseMessage)
                .isEqualTo("java.lang.IllegalArgumentException: Nested error " + mMaskedSpn);

        String actualDeepNestedCauseMessage = exception.getCause().getCause().getCause().toString();
        assertThat(actualDeepNestedCauseMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualDeepNestedCauseMessage).contains("com.android.healthconnect.watch");
    }

    @Test
    public void output_withNullMessage_handledGracefully() {
        HealthConnectException exception =
                new HealthConnectException(HealthConnectException.ERROR_UNKNOWN);

        assertThat(exception.getMessage()).isNull();
        assertThat(exception.toString()).isEqualTo("android.health.connect.HealthConnectException");
    }

    @Test
    public void output_canonicalSpn_isRedacted() {
        String message = "Error with " + mCanonicalSpn;

        createExceptionAndAssertRedaction(message, "com.android.healthconnect.watch");
    }

    @Test
    public void output_multipleSpacedCanonicalSpns_isRedacted() {
        String message = "Error with " + mCanonicalSpn + " " + mCanonicalSpn;

        createExceptionAndAssertRedaction(
                message, "com.android.healthconnect.watch com.android.healthconnect.watch");
    }

    @Test
    public void output_multipleCanonicalSpns_isRedacted() {
        String message = "Error with " + mCanonicalSpn + mCanonicalSpn;

        createExceptionAndAssertRedaction(
                message, "com.android.healthconnect.watchcom.android.healthconnect.watch");
    }

    @Test
    public void output_multipleDifferentSpns_canonicalIsRedacted() {
        String message = "Error with " + mCanonicalSpn + mMaskedSpn;

        createExceptionAndAssertRedaction(message, "com.android.healthconnect.watch" + mMaskedSpn);
    }

    @Test
    public void outputMessage_maskedSpn_isNotRedacted() {
        String message = "Error with " + mMaskedSpn;
        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, message, mSensitiveCause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause().toString()).doesNotContain(mCanonicalSpn);
    }

    @Test
    public void getMessage_normalPackage_isNotRedacted() {
        String packageName = "com.example.app";
        String message = "Error with " + packageName;
        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, message, mSensitiveCause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause().toString()).doesNotContain(mCanonicalSpn);
    }

    private void createExceptionAndAssertRedaction(
            String errorMessage, String redactedExceptionReplacement) {
        HealthConnectException exception =
                new HealthConnectException(
                        HealthConnectException.ERROR_UNKNOWN, errorMessage, mSensitiveCause);

        String actualMessage = exception.getMessage();
        assertThat(actualMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualMessage).contains(redactedExceptionReplacement);

        String actualToString = exception.toString();
        assertThat(actualToString).doesNotContain(mCanonicalSpn);
        assertThat(actualToString).contains(redactedExceptionReplacement);

        String actualCauseMessage = exception.getCause().toString();
        assertThat(actualCauseMessage).doesNotContain(mCanonicalSpn);
        assertThat(actualCauseMessage).contains("com.android.healthconnect.watch");
    }
}
