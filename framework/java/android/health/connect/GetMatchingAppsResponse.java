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

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a response the matching applications for a given set of record types and a package
 * name.
 *
 * @hide
 */
public final class GetMatchingAppsResponse implements Parcelable {
    private final Map<String, Set<String>> mMatchingApps;

    /**
     * Creates a response containing the map of matching apps. The {@link #hasMatchingApps()}
     * property is derived from whether the map is empty.
     *
     * @param matchingApps The map of matching apps to their matching permissions.
     */
    public GetMatchingAppsResponse(@NonNull Map<String, Set<String>> matchingApps) {
        mMatchingApps = Map.copyOf(requireNonNull(matchingApps));
    }

    /**
     * Private constructor to reconstruct a {@link GetMatchingAppsResponse} from a {@link Parcel}.
     *
     * @param in The Parcel from which to read the object data.
     */
    private GetMatchingAppsResponse(@NonNull Parcel in) {
        Bundle bundle = requireNonNull(in).readBundle(getClass().getClassLoader());
        if (bundle == null) {
            mMatchingApps = Map.of();
            return;
        }
        Map<String, Set<String>> map = new HashMap<>();
        for (String packageName : bundle.keySet()) {
            ArrayList<String> permissions = bundle.getStringArrayList(packageName);
            if (permissions != null) {
                map.put(packageName, new HashSet<>(permissions));
            }
        }
        mMatchingApps = Map.copyOf(map);
    }

    @NonNull
    public static final Creator<GetMatchingAppsResponse> CREATOR =
            new Creator<>() {
                @Override
                public GetMatchingAppsResponse createFromParcel(Parcel in) {
                    return new GetMatchingAppsResponse(in);
                }

                @Override
                public GetMatchingAppsResponse[] newArray(int size) {
                    return new GetMatchingAppsResponse[size];
                }
            };

    /** Returns whether there are matching apps in the response. */
    public boolean hasMatchingApps() {
        return !mMatchingApps.isEmpty();
    }

    @NonNull
    public Map<String, Set<String>> getMatchingApps() {
        return mMatchingApps;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Set<String>> entry : mMatchingApps.entrySet()) {
            bundle.putStringArrayList(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        dest.writeBundle(bundle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetMatchingAppsResponse that)) return false;
        return Objects.equals(mMatchingApps, that.mMatchingApps);
    }

    @Override
    public int hashCode() {
        return hash(mMatchingApps);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("hasMatchingApps=").append(hasMatchingApps());
        sb.append(",matchingApps=").append(getMatchingApps());
        sb.append("}");
        return sb.toString();
    }
}
