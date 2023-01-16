package com.android.healthconnect.controller.deniedapps

import android.content.Context
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R

class DeniedAppsBannerPreference
@JvmOverloads
constructor(context: Context, private val onButtonClickListener: OnClickListener? = null) :
    Preference(context) {

    private lateinit var bannerTitle: TextView
    private lateinit var bannerMessage: TextView
    private lateinit var bannerButton: Button

    init {
        layoutResource = R.layout.widget_banner_preference
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bannerTitle = holder.findViewById(R.id.banner_title) as TextView
        bannerMessage = holder.findViewById(R.id.banner_message) as TextView
        bannerButton = holder.findViewById(R.id.banner_button) as Button

        bannerTitle.text = title
        bannerMessage.text = summary
        bannerButton.setOnClickListener(onButtonClickListener)
    }
}
