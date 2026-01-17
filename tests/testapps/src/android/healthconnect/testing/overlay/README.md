HealthConnectOverlayTestApp can be used to manually test the behavior of overlaying resources in
the Health Connect controller APK.

The `<overlayable>` definitions in the controller require that any overlays must be pre-installed.
To install HealthConnectOverlayTestApp to the system partition run the following:

```shell
m HealthConnectOverlayTestApp

adb root
adb disable-verity
adb reboot
adb wait-for-device

adb root
adb remount
adb shell mkdir -p /system/app/HealthConnectOverlayTestApp
adb push `find $OUT -name HealthConnectOverlayTestApp.apk` /system/app/HealthConnectOverlayTestApp
adb reboot
adb wait-for-device
```

Following the reboot, the overlay should be installed but not yet enabled. This can be confirmed
with:

```shell
adb shell cmd overlay dump android.healthconnect.testing.overlay
```

To enable the overlay, run:

```shell
adb shell cmd overlay enable android.healthconnect.testing.overlay
```

For more on overlays, see https://source.android.com/docs/core/runtime/rros and
https://source.android.com/docs/core/runtime/rro-troubleshoot
