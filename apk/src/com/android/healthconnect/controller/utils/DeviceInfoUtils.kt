package com.android.healthconnect.controller.utils

import android.app.compat.gms.GmsCompat
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.shared.HelpAndFeedbackFragment.Companion.FEEDBACK_INTENT_RESULT_CODE
import com.android.healthconnect.controller.permissions.shared.HelpAndFeedbackFragment.Companion.USER_INITIATED_FEEDBACK_BUCKET_ID
import com.android.settingslib.HelpUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

interface DeviceInfoUtils {
    fun isSendFeedbackAvailable(context: Context): Boolean

    fun isPlayStoreAvailable(context: Context): Boolean

    fun openHCGetStartedLink(activity: FragmentActivity)

    fun openSendFeedbackActivity(activity: FragmentActivity)

    fun isIntentHandlerAvailable(context: Context, intent: Intent): Boolean
}

class DeviceInfoUtilsImpl @Inject constructor() : DeviceInfoUtils {

    companion object {
        private val TAG = "DeviceInfoUtils"
    }

    override fun isSendFeedbackAvailable(context: Context): Boolean {
        return isIntentHandlerAvailable(context, Intent(Intent.ACTION_BUG_REPORT))
    }

    override fun isPlayStoreAvailable(context: Context): Boolean {
        val playStorePackageName = context.resources?.getString(R.string.playstore_collection_url)
        val vendingPackageName = context.resources?.getString(R.string.playstore_package_name)
        if (TextUtils.isEmpty(playStorePackageName) || playStorePackageName == null) {
            // Package name not configured. Return.
            return false
        }
        if (!isIntentHandlerAvailable(
            context,
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(playStorePackageName)
                setPackage(vendingPackageName)
            })) {
            return false
        }

        val pm = context.packageManager
        return try {
            val ai = pm.getApplicationInfo(playStorePackageName, 0)
            GmsCompat.isGmsApp(ai)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun openHCGetStartedLink(activity: FragmentActivity) {
        val helpUrlString = activity.getString(R.string.hc_get_started_link)
        val fullUri = HelpUtils.uriWithAddedParameters(activity, Uri.parse(helpUrlString))
        val intent =
            Intent(Intent.ACTION_VIEW, fullUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Unable to open help center URL.", e)
        }
    }

    override fun openSendFeedbackActivity(activity: FragmentActivity) {
        val intent = Intent(Intent.ACTION_BUG_REPORT)
        intent.putExtra("category_tag", USER_INITIATED_FEEDBACK_BUCKET_ID)
        activity.startActivityForResult(intent, FEEDBACK_INTENT_RESULT_CODE)
    }

    override fun isIntentHandlerAvailable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            return true
        }
        return false
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceInfoUtilsEntryPoint {
    fun deviceInfoUtils(): DeviceInfoUtils
}

@Module
@InstallIn(SingletonComponent::class)
class DeviceInfoUtilsModule {
    @Provides
    fun providesDeviceInfoUtils(): DeviceInfoUtils {
        return DeviceInfoUtilsImpl()
    }
}
