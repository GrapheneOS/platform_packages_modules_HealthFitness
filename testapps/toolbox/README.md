## Setup

Build and run test the app using the following command

```
m <HealthConnectToolboxTarget> &&
adb install $OUT/system/app/<HealthConnectToolboxTarget>/<HealthConnectToolboxTarget>.apk
```

## Workflows

### Requesting Permissions

* Click on Request Permissions on Home Screen
* It will open the Request Permissions page on Health Connect showing toggles for all the
  permissions.
* Allow/Disallow the permissions as per the use case.

### Insert Data

* Click Insert Data on Home Screen
* Select the Data Category.
* Select the Data Type.
* There are two types of Records:
    * Interval Record - These records take place over an interval of time therefore they have a
      start time and an end time. They generate a single value or a range of values at the end of
      the whole interval. For example Elevation Gained, record contains a start time, an end time
      and the total elevation gained in that time period. To insert such a record, users can enter a
      start time, end time and the recorded values for the given record type.
    * Instantaneous Record - Contrary to the previous records, these records are recorded at a given
      instant and not over a range of time. They can be inserted similar to an interval record, just
      not with a start and end time, instead with a single time stamp.
* On confirming insert of data, users will be given the inserted record's UUID. Save this UUID to
  update this data later.

### Update Data

* Updating data will follow the same workflow as insert.
* Users can goto the page to insert data, enter the updated values and click Update Data.
* Users will be required to enter the UUID of the record they want to update.

[Demo video for the aforementioned workflows.](https://drive.google.com/file/d/1kbO2duqZ4NGJ9gpRJe3C-3MCjq6F7eFn/view?usp=sharing&resourcekey=0-A9DL0nlGNr56jfcKLHE7IQ)

## Modular Manifest Structure

The Health Connect Toolbox application uses a modular manifest system to create different build targets with varying sets of permissions and features. This is managed through the `Android.bp` file and several `AndroidManifest.xml` files.

### Manifest Files Overview

-   **`AndroidManifest.xml`**: The base manifest containing common application components like activities, providers, and receivers.
-   **`AndroidManifestFitnessPermissions.xml`**: Contains all `uses-permission` tags related to fitness data types.
-   **`AndroidManifestMedicalPermissions.xml`**: Contains all `uses-permission` tags related to medical data types.
-   **`AndroidManifestAdditionalPermissions.xml`**: Contains permissions for background and history data access.
-   **`AndroidManifestOnboardingActivity.xml`**: Adds a specific activity for the onboarding flow.
-   **`AndroidManifestToolbox*.xml`**: These are primary manifests for each build target. They mainly serve to set a unique `package` name and `android:label` for the application, allowing multiple versions of the Toolbox to be installed on a single device.

### Combining Manifests for a New Target

The `android_app` definitions in `Android.bp` show how these files are combined. The `manifest` property points to the primary manifest, and `additional_manifests` lists all other manifests to be merged.

To create a new build target (e.g., for a specific testing scenario):

1.  **Create a new primary manifest file**:
    Create a file like `AndroidManifestMyNewTarget.xml`. This file needs to define a unique package name and application label. You can use `AndroidManifestToolboxFitness.xml` as a template:

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
              xmlns:tools="http://schemas.android.com/tools"
              package="com.android.healthconnect.testapps.mynewtarget">
      <application android:label="HC Toolbox MyNewTarget" tools:replace="android:label"/>
    </manifest>
    ```

2.  **Add a new `android_app` module to `Android.bp`**:
    Copy an existing `android_app` definition and modify it for your new target.

    ```kotlin
    android_app {
        name: "HealthConnectToolboxMyNewTarget", // Unique name for the build target
        sdk_version: "module_current",
        min_sdk_version: "34",
        rename_resources_package: false,
        updatable: true,
        package_name: "com.android.healthconnect.testapps.mynewtarget", // Must match the package in your new manifest
        manifest: "AndroidManifestMyNewTarget.xml", // Your new primary manifest
        additional_manifests: [
            "AndroidManifest.xml", // Base manifest
            "AndroidManifestFitnessPermissions.xml", // Include desired permission sets
            // "AndroidManifestMedicalPermissions.xml",
            // "AndroidManifestAdditionalPermissions.xml",
        ],
        certificate: "platform",
        static_libs: [
            "HealthConnectToolboxLibrary",
        ],
    }
    ```

3.  **Build your new target**:
    You can now build your new APK using the `name` you defined:
    ```
    m HealthConnectToolboxMyNewTarget
    ```
This approach allows for flexible creation of different Toolbox variants without duplicating common manifest entries.