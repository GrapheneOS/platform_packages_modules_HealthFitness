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

package com.android.healthconnect.controller.matchmaking

import android.content.Context
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R

class MatchmakingPrivacyFooterPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var appName: String? = null
    private var onRationaleLinkClicked: () -> Unit = {}

    init {
        layoutResource = R.layout.matchmaking_privacy_footer
        isSelectable = false
    }

    fun setAppName(appName: String, onRationaleLinkClicked: () -> Unit) {
        this.appName = appName
        this.onRationaleLinkClicked = onRationaleLinkClicked
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summaryView = holder.findViewById(R.id.summary) as TextView

        appName?.let {
            val policyString = context.getString(R.string.request_permissions_privacy_policy)
            val rationaleText =
                context.resources.getString(R.string.app_privacy_policy_footer, it, policyString)
            summaryView.text =
                createSpannableString(rationaleText, policyString, onRationaleLinkClicked)
            summaryView.movementMethod = LinkMovementMethod.getInstance()
        }
        summaryView.isClickable = false
        summaryView.isFocusable = false
    }

    private fun createSpannableString(
        fullText: String,
        linkText: String,
        onClickListener: () -> Unit,
    ): SpannableString {
        val spannableString = SpannableString(fullText)
        val linkStartIndex = fullText.indexOf(linkText)
        val linkEndIndex = linkStartIndex + linkText.length

        val clickableSpan =
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClickListener()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                }
            }
        spannableString.setSpan(
            clickableSpan,
            linkStartIndex,
            linkEndIndex,
            SpannableString.SPAN_INCLUSIVE_INCLUSIVE,
        )
        return spannableString
    }
}
