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
package android.health.connect.device;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Locale;

@RunWith(JUnit4.class)
public class SyntheticPackageNameMatcherArabicLocaleTest {
    // Regression test for b/474456880

    @Test
    public void withArabicLocale_init_doesNotThrow() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("ar", "EG"));
            // Attempt to use the class to ensure it initializes without error
            // This test might pass if the class was already loaded, but it serves as a
            // regression test if run in isolation or if the class is reloaded.
            SyntheticPackageNameMatcher.matches(
                    "com.android.healthconnect.watch.jc45bd741a7123764b514e2c27df9fe41");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
