package android.healthconnect.cts.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.android.compatibility.common.util.SystemUtil.runShellCommandOrThrow
import org.junit.Assume
import org.junit.Before

open class HealthConnectBaseTest {

    protected val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUpClass() {
        Assume.assumeTrue(isHardwareSupported())
        // Collapse notifications
        runShellCommandOrThrow("cmd statusbar collapse")

        unlockDevice()
    }

    private fun unlockDevice() {
        runShellCommandOrThrow("input keyevent KEYCODE_WAKEUP")
        if ("false".equals(runShellCommandOrThrow("cmd lock_settings get-disabled"))) {
            // Unlock screen only when it's lock settings enabled to prevent showing "wallpaper
            // picker" which may cover another UI elements on freeform window configuration.
            runShellCommandOrThrow("input keyevent 82")
        }
        runShellCommandOrThrow("wm dismiss-keyguard")
    }

    private fun isHardwareSupported(): Boolean {
        // These UI tests are not optimised for Watches, TVs, Auto;
        // IoT devices do not have a UI to run these UI tests
        val pm: PackageManager = context.packageManager
        return (!pm.hasSystemFeature(PackageManager.FEATURE_EMBEDDED) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
    }
}
