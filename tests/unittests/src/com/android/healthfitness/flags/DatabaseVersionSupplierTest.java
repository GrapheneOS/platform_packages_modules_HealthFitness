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

package com.android.healthfitness.flags;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.TreeMap;
import java.util.function.BooleanSupplier;

@RunWith(AndroidJUnit4.class)
public class DatabaseVersionSupplierTest {
    @Test
    public void testGetDbVersion_empty() {
        DatabaseVersionSupplier supplier =
                new DatabaseVersionSupplier(/* lastRolledOutVersion= */ 42, new TreeMap<>());
        assertThat(supplier.get()).isEqualTo(42);
    }

    @Test
    public void testGetDbVersion_true_true_true() {
        TreeMap<Integer, BooleanSupplier> dbVersionToDbFlagMap = new TreeMap<>();
        dbVersionToDbFlagMap.put(1, () -> true);
        dbVersionToDbFlagMap.put(2, () -> true);
        dbVersionToDbFlagMap.put(3, () -> true);

        DatabaseVersionSupplier supplier =
                new DatabaseVersionSupplier(/* lastRolledOutVersion= */ 0, dbVersionToDbFlagMap);
        assertThat(supplier.get()).isEqualTo(3);
    }

    @Test
    public void testGetDbVersion_true_false_true() {
        TreeMap<Integer, BooleanSupplier> dbVersionToDbFlagMap = new TreeMap<>();
        dbVersionToDbFlagMap.put(1, () -> true);
        dbVersionToDbFlagMap.put(2, () -> false);
        dbVersionToDbFlagMap.put(3, () -> true);

        DatabaseVersionSupplier supplier =
                new DatabaseVersionSupplier(/* lastRolledOutVersion= */ 0, dbVersionToDbFlagMap);
        assertThat(supplier.get()).isEqualTo(1);
    }

    @Test
    public void testGetDbVersion_true_false_false() {
        TreeMap<Integer, BooleanSupplier> dbVersionToDbFlagMap = new TreeMap<>();
        dbVersionToDbFlagMap.put(1, () -> true);
        dbVersionToDbFlagMap.put(2, () -> false);
        dbVersionToDbFlagMap.put(3, () -> false);

        DatabaseVersionSupplier supplier =
                new DatabaseVersionSupplier(/* lastRolledOutVersion= */ 0, dbVersionToDbFlagMap);
        assertThat(supplier.get()).isEqualTo(1);
    }
}
