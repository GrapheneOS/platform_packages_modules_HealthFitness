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

package com.android.server.healthconnect.storage;

import static com.google.common.truth.Truth.assertWithMessage;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.migration.MigrationEntityHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private final Set<Class<?>> mNonSingletonClasses = Set.of(MigrationEntityHelper.class);

    @Test
    public void nonSingletons_doNotHaveCentralState() {
        for (Class<?> nonSingletonClass : mNonSingletonClasses) {
            Field[] declaredFields = nonSingletonClass.getDeclaredFields();
            String className = nonSingletonClass.getName();
            for (Field declaredField : declaredFields) {
                String fieldName = declaredField.getName();
                if (declaredField.isSynthetic()) {
                    continue;
                }

                if (!declaredField.getType().equals(String.class)) {
                    assertWithMessage(
                                    className
                                            + " has non-primitive (except String) member variable "
                                            + fieldName)
                            .that(declaredField.getType().isPrimitive())
                            .isTrue();
                }
                assertWithMessage(className + " has non-static member variable " + fieldName)
                        .that(Modifier.isStatic(declaredField.getModifiers()))
                        .isTrue();
                assertWithMessage(className + " has non-final member variable " + fieldName)
                        .that(Modifier.isFinal(declaredField.getModifiers()))
                        .isTrue();
            }
        }
    }
}
