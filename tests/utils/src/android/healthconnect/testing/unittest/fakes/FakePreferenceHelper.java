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

package android.healthconnect.testing.unittest.fakes;

import android.util.Pair;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import com.google.common.base.Objects;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Fake impl of Preference Helper for use in testing.
 *
 * <p>This is an in-memory impl, and doesn't persist changes to the database.
 */
public class FakePreferenceHelper extends PreferenceHelper {

    private final Map<Pair<String, String>, CountDownLatch> mAwaitedKeyValuePairs = new HashMap<>();

    public FakePreferenceHelper() {
        super(null, new DatabaseHelpers());
        mPreferences = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void insertOrReplacePreference(String key, String value) {
        getPreferences().put(key, value);
        // Alert any threads waiting for this condition
        Pair<String, String> keyValue = new Pair<>(key, value);
        CountDownLatch latch = mAwaitedKeyValuePairs.remove(keyValue);
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public synchronized void removeKey(String id) {
        getPreferences().remove(id);
    }

    @Override
    public synchronized void clearCache() {
        mPreferences.clear();
    }

    /**
     * Wait for the preference for the given key to be set to the given value.
     *
     * @return false if the thread timed out waiting, or true otherwise
     */
    public boolean await(String key, String value, long time, TimeUnit timeUnit)
            throws InterruptedException {
        CountDownLatch latch;
        // Synchronize access to this class state variables. However, don't synchronize on
        // latch.await(), otherwise we get deadlock.
        synchronized (this) {
            // Don't await anything if the value is already set to this.
            String currentValue = getPreferences().get(key);
            if (Objects.equal(currentValue, value)) {
                return true;
            }
            Pair<String, String> keyValue = new Pair<>(key, value);
            latch = mAwaitedKeyValuePairs.get(keyValue);
            if (latch == null) {
                latch = new CountDownLatch(1);
                mAwaitedKeyValuePairs.put(keyValue, latch);
            }
        }
        return latch.await(time, timeUnit);
    }
}
