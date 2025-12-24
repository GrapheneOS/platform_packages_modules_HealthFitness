#!/bin/bash

# Configuration
APP_TARGET="HealthConnectToolbox1"
PACKAGE_NAME="com.android.healthconnect.testapps.toolboxcombined"
PERM_FILE="privapp-permissions-hc-toolbox.xml"

# Ensure we are at the root of the build environment
if [ -z "$ANDROID_BUILD_TOP" ]; then
    echo "Error: ANDROID_BUILD_TOP is not set. Please run 'source build/envsetup.sh' and 'lunch' first."
    exit 1
fi

echo "--- Building $APP_TARGET ---"
# Build the APK
m $APP_TARGET

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# The path to the built APK (standard output location for system apps)
APK_PATH="$OUT/system/app/$APP_TARGET/$APP_TARGET.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi

echo "--- Preparing Device ---"
adb root
adb wait-for-device

# Remount and check if reboot is required (common on fresh eng/userdebug builds with overlayfs)
REMOUNT_OUT=$(adb remount 2>&1)
echo "$REMOUNT_OUT"

if [[ "$REMOUNT_OUT" == *"reboot"* ]]; then
    echo "Remount requires reboot. Rebooting now..."
    adb reboot
    adb wait-for-device
    adb root
    adb wait-for-device
    adb remount
    adb wait-for-device
fi

echo "--- Uninstalling existing user app (if any) ---"
adb uninstall $PACKAGE_NAME
echo "--- Uninstalling existing system app (if any) ---"
adb shell rm /system/priv-app/$APP_TARGET/$APP_TARGET.apk

echo "--- Installing APK to /system/priv-app/ ---"
# Create the directory in priv-app
adb shell mkdir -p /system/priv-app/$APP_TARGET

# Push the APK
adb push "$APK_PATH" /system/priv-app/$APP_TARGET/

echo "--- Configuring Privileged Permissions ---"
# Generate the permissions XML file locally
cat > $PERM_FILE <<EOF
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="$PACKAGE_NAME">
        <permission name="android.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA"/>
        <permission name="android.permission.MANAGE_HEALTH_DATA"/>
    </privapp-permissions>
</permissions>
EOF

# Push the permissions file to the system configuration directory
adb push $PERM_FILE /system/etc/permissions/

# Clean up local file
rm $PERM_FILE

echo "--- Rebooting Device ---"
echo "Rebooting to apply changes..."
adb reboot

echo "Done! After reboot, $APP_TARGET should have the privileged permission."
