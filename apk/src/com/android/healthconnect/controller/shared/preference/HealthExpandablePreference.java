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

package com.android.healthconnect.controller.shared.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.healthconnect.controller.R;

/**
 * A {@link PreferenceGroup} that can be expanded and collapsed.
 *
 * <p>This preference will display an arrow that can be clicked to expand or collapse the
 * preferences contained within it.
 */
public class HealthExpandablePreference extends PreferenceGroup {

    private boolean mIsExpanded = false;
    private OnExpandChangeListener mOnExpandChangeListener;

    /**
     * Interface definition for a callback to be invoked when the expansion state of this preference
     * changes.
     */
    public interface OnExpandChangeListener {
        /**
         * Called when the expansion state of this preference has changed.
         *
         * @param isExpanded The new expansion state.
         */
        void onExpandChanged(boolean isExpanded);
    }

    public HealthExpandablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.expandable_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedBelow(false);
        holder.setDividerAllowedAbove(false);

        ImageView arrow = (ImageView) holder.findViewById(R.id.expand_arrow);
        arrow.setRotation(mIsExpanded ? 180f : 0f);
        holder.itemView.setOnClickListener(
                v -> {
                    setExpanded(!mIsExpanded);
                    if (mOnExpandChangeListener != null) {
                        mOnExpandChangeListener.onExpandChanged(mIsExpanded);
                    }
                });
        updatePreferences();
    }

    @Override
    public boolean addPreference(Preference preference) {
        final boolean result = super.addPreference(preference);
        updatePreferences();
        return result;
    }

    /** Sets the expansion state of this preference. */
    public void setExpanded(boolean isExpanded) {
        if (mIsExpanded != isExpanded) {
            mIsExpanded = isExpanded;
            updatePreferences();
            notifyChanged();
        }
    }

    /** Sets a callback to be invoked when the expansion state of this preference changes. */
    public void setOnExpandChangeListener(OnExpandChangeListener listener) {
        mOnExpandChangeListener = listener;
    }

    private void updatePreferences() {
        for (int i = 0; i < getPreferenceCount(); i++) {
            getPreference(i).setVisible(mIsExpanded);
        }
    }
}
