package com.android.healthconnect.controller.tests.shared

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED
import android.health.connect.HealthPermissions
import android.health.connect.HealthPermissions.MANAGE_HEALTH_PERMISSIONS
import android.health.connect.HealthPermissions.READ_PLANNED_EXERCISE
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.HealthPermissions.WRITE_PLANNED_EXERCISE
import android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE
import android.os.Build
import android.os.Process
import android.permission.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType.COMBINED_PERMISSIONS
import com.android.healthconnect.controller.shared.app.AppPermissionsType.FITNESS_PERMISSIONS_ONLY
import com.android.healthconnect.controller.shared.app.AppPermissionsType.MEDICAL_PERMISSIONS_ONLY
import com.android.healthconnect.controller.tests.utils.BODY_SENSORS_AND_HEALTH_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.BODY_SENSORS_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.OLD_PERMISSIONS_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.UNSUPPORTED_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.WEAR_TEST_APP_PACKAGE_NAME
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HealthPermissionReaderTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Inject lateinit var permissionReader: HealthPermissionReader
    private lateinit var context: Context

    @Before
    fun setup() {
        runWithShellPermissionIdentity({ hiltRule.inject() }, MANAGE_HEALTH_PERMISSIONS)
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getValidHealthPermissions_phrFlagOff_returnsFitnessAndAdditionalPermissions() = runTest {
        assertThat(permissionReader.getValidHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_EXERCISE_ROUTES,
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.WRITE_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.READ_PLANNED_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_PLANNED_EXERCISE.toHealthPermission(),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getValidHealthPermissions_phrFlagOn_returnsAllHealthAndAdditionalPermissions() = runTest {
        assertThat(permissionReader.getValidHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_EXERCISE_ROUTES,
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_PREGNANCY.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_PROCEDURES.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_VACCINES.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_VISITS.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS.toHealthPermission(),
                HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                HealthPermissions.READ_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.WRITE_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.READ_PLANNED_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_PLANNED_EXERCISE.toHealthPermission(),
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getValidHealthPermissions_returnsAllPermissions_exceptHiddenPermissions() = runTest {
        assertThat(permissionReader.getValidHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_EXERCISE_ROUTES,
                HealthPermissions.READ_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.WRITE_SKIN_TEMPERATURE.toHealthPermission(),
                HealthPermissions.READ_PLANNED_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_PLANNED_EXERCISE.toHealthPermission(),
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND.toHealthPermission(),
                HealthPermissions.READ_HEALTH_DATA_HISTORY.toHealthPermission(),
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getDeclaredHealthPermissions_phrFlagOff_returnsAllFitnessAndAdditionalPermissions() {
        assertThat(permissionReader.getDeclaredHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED,
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED,
                HealthPermissions.READ_EXERCISE,
                HealthPermissions.WRITE_EXERCISE,
                HealthPermissions.READ_SLEEP,
                HealthPermissions.WRITE_SLEEP,
                HealthPermissions.READ_EXERCISE_ROUTES,
                HealthPermissions.WRITE_EXERCISE_ROUTE,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_SKIN_TEMPERATURE,
                HealthPermissions.WRITE_SKIN_TEMPERATURE,
                HealthPermissions.READ_PLANNED_EXERCISE,
                HealthPermissions.WRITE_PLANNED_EXERCISE,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getDeclaredHealthPermissions_medicalFlagOn_returnsAllHealthAndAdditionalPermissions() {
        assertThat(permissionReader.getDeclaredHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED,
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED,
                HealthPermissions.READ_EXERCISE,
                HealthPermissions.WRITE_EXERCISE,
                HealthPermissions.READ_SLEEP,
                HealthPermissions.WRITE_SLEEP,
                HealthPermissions.READ_EXERCISE_ROUTES,
                HealthPermissions.WRITE_EXERCISE_ROUTE,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS,
                HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS,
                HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PREGNANCY,
                HealthPermissions.READ_MEDICAL_DATA_PROCEDURES,
                HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY,
                HealthPermissions.READ_MEDICAL_DATA_VACCINES,
                HealthPermissions.READ_MEDICAL_DATA_VISITS,
                HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS,
                HealthPermissions.WRITE_MEDICAL_DATA,
                HealthPermissions.READ_SKIN_TEMPERATURE,
                HealthPermissions.WRITE_SKIN_TEMPERATURE,
                HealthPermissions.READ_PLANNED_EXERCISE,
                HealthPermissions.WRITE_PLANNED_EXERCISE,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
            )
    }

    @RequiresFlagsEnabled(
        Flags.FLAG_ACTIVITY_INTENSITY,
        Flags.FLAG_ACTIVITY_INTENSITY_DB,
        Flags.FLAG_HEALTH_CONNECT_MAPPINGS,
    )
    @Test
    fun getHealthPermissions_activityIntensityFlagsEnabled_returnsPermissions() {
        assertThat(permissionReader.getHealthPermissions())
            .containsAtLeast(
                HealthPermissions.READ_ACTIVITY_INTENSITY,
                HealthPermissions.WRITE_ACTIVITY_INTENSITY,
            )
    }

    @RequiresFlagsEnabled(Flags.FLAG_ACTIVITY_INTENSITY_DB, Flags.FLAG_HEALTH_CONNECT_MAPPINGS)
    @RequiresFlagsDisabled(Flags.FLAG_ACTIVITY_INTENSITY)
    @Test
    fun getHealthPermissions_activityIntensityFlagDisabled_doesNotReturnPermissions() {
        assertThat(permissionReader.getHealthPermissions())
            .containsNoneOf(
                HealthPermissions.READ_ACTIVITY_INTENSITY,
                HealthPermissions.WRITE_ACTIVITY_INTENSITY,
            )
    }

    @RequiresFlagsEnabled(Flags.FLAG_ACTIVITY_INTENSITY, Flags.FLAG_HEALTH_CONNECT_MAPPINGS)
    @RequiresFlagsDisabled(Flags.FLAG_ACTIVITY_INTENSITY_DB)
    @Test
    fun getHealthPermissions_activityIntensityDbFlagDisabled_doesNotReturnPermissions() {
        assertThat(permissionReader.getHealthPermissions())
            .containsNoneOf(
                HealthPermissions.READ_ACTIVITY_INTENSITY,
                HealthPermissions.WRITE_ACTIVITY_INTENSITY,
            )
    }

    @RequiresFlagsEnabled(Flags.FLAG_ACTIVITY_INTENSITY, Flags.FLAG_ACTIVITY_INTENSITY_DB)
    @RequiresFlagsDisabled(Flags.FLAG_HEALTH_CONNECT_MAPPINGS)
    @Test
    fun getHealthPermissions_healthConnectMappingsFlagDisabled_doesNotReturnPermissions() {
        assertThat(permissionReader.getHealthPermissions())
            .containsNoneOf(
                HealthPermissions.READ_ACTIVITY_INTENSITY,
                HealthPermissions.WRITE_ACTIVITY_INTENSITY,
            )
    }

    @Test
    fun isRationalIntentDeclared_withIntent_returnsTrue() {
        assertThat(permissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME)).isTrue()
    }

    @Test
    fun isRationalIntentDeclared_noIntent_returnsTrue() {
        assertThat(permissionReader.isRationaleIntentDeclared(UNSUPPORTED_TEST_APP_PACKAGE_NAME))
            .isFalse()
    }

    @Test
    fun getAppsWithHealthPermissions_handHeldDevices_returnsSupportedApps() = runTest {
        assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
        assertThat(permissionReader.getAppsWithHealthPermissions().keys)
            .containsAtLeast(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
    }

    // Still need the SDK version check here because @RequiresFlagsEnabled
    // won't work on older platforms where the flags aren't defined (b/383440585).
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun getAppsWithHealthPermissions_handHeldDevices_replaceBodySensors_returnsSplitPermissionApps() =
        runTest {
            assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
            runWithShellPermissionIdentity(
                {
                    assertThat(permissionReader.getAppsWithHealthPermissions().keys)
                        .containsAtLeast(
                            TEST_APP_PACKAGE_NAME,
                            TEST_APP_PACKAGE_NAME_2,
                            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
                        )
                },
                MANAGE_HEALTH_PERMISSIONS,
            )
        }

    // Still need the SDK version check here because @RequiresFlagsEnabled
    // won't work on older platforms where the flags aren't defined (b/383440585).
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun getAppsWithHealthPermissions_handHeldDevices_returnWithIsSystemApp() = runTest {
        assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
        clearFlag(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEART_RATE,
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED,
        )
        clearFlag(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEART_RATE,
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED,
        )
        clearFlag(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED,
        )
        clearFlag(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED,
        )

        runWithShellPermissionIdentity(
            {
                val appsWithHealthPermissions = permissionReader.getAppsWithHealthPermissions()
                assertThat(appsWithHealthPermissions)
                    .containsEntry(BODY_SENSORS_TEST_APP_PACKAGE_NAME, true)
                assertThat(appsWithHealthPermissions).containsEntry(TEST_APP_PACKAGE_NAME, false)
                assertThat(appsWithHealthPermissions).containsEntry(TEST_APP_PACKAGE_NAME_2, false)
            },
            MANAGE_HEALTH_PERMISSIONS,
        )
    }

    @Test
    fun getAppsWithHealthPermissions_returnsDistinctApps() = runTest {
        val apps = permissionReader.getAppsWithHealthPermissions().keys
        assertThat(ArrayList(apps)).isEqualTo(apps.distinct())
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun getAppsWithHealthPermissions_handHeldDevices_doesNotReturnUnsupportedOrSplitPermissionApps() =
        runTest {
            assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))

            val healthApps: MutableList<String> = mutableListOf()
            runWithShellPermissionIdentity(
                { healthApps.addAll(permissionReader.getAppsWithHealthPermissions().keys) },
                MANAGE_HEALTH_PERMISSIONS,
            )
            assertThat(healthApps).doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
            assertThat(healthApps).doesNotContain(BODY_SENSORS_TEST_APP_PACKAGE_NAME)
        }

    // Still need the SDK version check here because @RequiresFlagsEnabled
    // won't work on older platforms where the flags aren't defined (b/383440585).
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun getAppsWithHealthPermissions_handHeldDevices_doesNotReturnUnsupportedApps() = runTest {
        assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
        assertThat(permissionReader.getAppsWithHealthPermissions().keys)
            .doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun getAppsWithHealthPermissions_returnAppsRequestingHealthPermissions_wearDevices() = runTest {
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))

        val wearAppsWithHealthPermissions = permissionReader.getAppsWithHealthPermissions().keys
        assertThat(wearAppsWithHealthPermissions)
            .containsAtLeast(
                TEST_APP_PACKAGE_NAME,
                TEST_APP_PACKAGE_NAME_2,
                BODY_SENSORS_TEST_APP_PACKAGE_NAME, // Test split permissiom from BODY_SENSORS
                WEAR_TEST_APP_PACKAGE_NAME,
            )
        // An app is not considered a wear app with health permissions if not requesting a system
        // health permission, regardless of declaring an intent filter
        assertThat(wearAppsWithHealthPermissions)
            .doesNotContain(MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun isBodySensorSplitPermissionApp_flagDisabled_notSplitPermissionApp() = runTest {
        runWithShellPermissionIdentity(
            {
                assertThat(
                        permissionReader.isBodySensorSplitPermissionApp(
                            BODY_SENSORS_TEST_APP_PACKAGE_NAME
                        )
                    )
                    .isFalse()
            },
            MANAGE_HEALTH_PERMISSIONS,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun isBodySensorSplitPermissionApp_flagEnabled_readHrApp_notSplitPermissionApp() = runTest {
        runWithShellPermissionIdentity(
            {
                assertThat(
                        permissionReader.isBodySensorSplitPermissionApp(WEAR_TEST_APP_PACKAGE_NAME)
                    )
                    .isFalse()
            },
            MANAGE_HEALTH_PERMISSIONS,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun isBodySensorSplitPermissionApp_flagEnabled_bodySensorAndBackgroundApp_returnsSplitPermissionApp() =
        runTest {
            runWithShellPermissionIdentity(
                {
                    assertThat(
                            permissionReader.isBodySensorSplitPermissionApp(
                                BODY_SENSORS_TEST_APP_PACKAGE_NAME
                            )
                        )
                        .isTrue()
                },
                MANAGE_HEALTH_PERMISSIONS,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun isBodySensorSplitPermissionApp_flagEnabled_returnsNotSplitPermissionApp() = runTest {
        runWithShellPermissionIdentity(
            {
                assertThat(permissionReader.isBodySensorSplitPermissionApp(TEST_APP_PACKAGE_NAME))
                    .isFalse()
            },
            MANAGE_HEALTH_PERMISSIONS,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun isBodySensorSplitPermissionApp_flagEnabled_bodySensorAndHealthApp_notSplitPermissionApp() =
        runTest {
            runWithShellPermissionIdentity(
                {
                    assertThat(
                            permissionReader.isBodySensorSplitPermissionApp(
                                BODY_SENSORS_AND_HEALTH_TEST_APP_PACKAGE_NAME
                            )
                        )
                        .isFalse()
                },
                MANAGE_HEALTH_PERMISSIONS,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    fun isBodySensorSplitPermissionApp_returnsSplitPermissionApp() = runTest {
        assumeFalse(context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
        runWithShellPermissionIdentity(
            {
                assertThat(permissionReader.getAppsWithHealthPermissions().keys)
                    .containsAtLeast(
                        TEST_APP_PACKAGE_NAME,
                        TEST_APP_PACKAGE_NAME_2,
                        BODY_SENSORS_TEST_APP_PACKAGE_NAME,
                    )
            },
            MANAGE_HEALTH_PERMISSIONS,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun getSystemHealthPermissions_returnSystemHealthPermissions() = runTest {
        assertThat(permissionReader.getSystemHealthPermissions())
            .containsExactly(
                HealthPermissions.READ_HEART_RATE,
                HealthPermissions.READ_OXYGEN_SATURATION,
                HealthPermissions.READ_SKIN_TEMPERATURE,
            )
    }

    @Test
    fun getAppsWithFitnessPermissions_returnsSupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithFitnessPermissions())
            .containsAtLeast(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
    }

    @Test
    fun getAppsWithFitnessPermissions_returnsDistinctApps() = runTest {
        val apps = permissionReader.getAppsWithFitnessPermissions()
        assertThat(apps).isEqualTo(apps.distinct())
    }

    @Test
    fun getAppsWithFitnessPermissions_doesNotReturnUnsupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithFitnessPermissions())
            .doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun getAppsWithFitnessPermissions_doesNotReturnMedicalPermissionApps() = runTest {
        assertThat(permissionReader.getAppsWithFitnessPermissions())
            .doesNotContain(MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getAppsWithMedicalPermissions_phrFlagOff_returnsEmptyList() = runTest {
        assertThat(permissionReader.getAppsWithMedicalPermissions()).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getAppsWithMedicalPermissions_returnsSupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithMedicalPermissions())
            .containsAtLeast(TEST_APP_PACKAGE_NAME, MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getAppsWithMedicalPermissions_returnsDistinctApps() = runTest {
        val apps = permissionReader.getAppsWithMedicalPermissions()
        assertThat(apps).isEqualTo(apps.distinct())
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getAppsWithMedicalPermissions_doesNotReturnUnsupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithMedicalPermissions())
            .doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun getAppsWithOldHealthPermissions_returnsOldSupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithOldHealthPermissions())
            .contains(OLD_PERMISSIONS_TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun getAppsWithOldHealthPermissions_returnsDistinctApps() = runTest {
        val apps = permissionReader.getAppsWithOldHealthPermissions()
        assertThat(apps).isEqualTo(apps.distinct())
    }

    @Test
    fun getAppsWithOldHealthPermissions_doesNotReturnAppsWithNewPermissions() = runTest {
        assertThat(permissionReader.getAppsWithOldHealthPermissions())
            .containsNoneOf(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
    }

    @Test
    fun getAppsWithOldHealthPermissions_doesNotReturnUnsupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithOldHealthPermissions())
            .doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun shouldHidePermission_whenFeatureEnabled_returnsFalse() = runTest {
        assertThat(permissionReader.shouldHidePermission(READ_SKIN_TEMPERATURE)).isFalse()
        assertThat(permissionReader.shouldHidePermission(WRITE_SKIN_TEMPERATURE)).isFalse()

        assertThat(permissionReader.shouldHidePermission(READ_PLANNED_EXERCISE)).isFalse()
        assertThat(permissionReader.shouldHidePermission(WRITE_PLANNED_EXERCISE)).isFalse()
    }

    @Test
    fun maybeFilterOutAdditionalIfNotValid_fitnessReadDeclared_doesNotChangeList() {
        val declaredPermissions =
            listOf(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_VACCINES.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_EXERCISE_ROUTES,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
            )
        val result = permissionReader.maybeFilterOutAdditionalIfNotValid(declaredPermissions)
        assertThat(result).containsExactlyElementsIn(declaredPermissions)
    }

    @Test
    fun maybeFilterOutAdditionalIfNotValid_noReadDeclared_filtersOutHistoryAndBackground() {
        val declaredPermissions =
            listOf(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
            )
        val result = permissionReader.maybeFilterOutAdditionalIfNotValid(declaredPermissions)
        assertThat(result)
            .containsExactlyElementsIn(
                listOf(
                    HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                    HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                    HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                    HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                    HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                )
            )
    }

    @Test
    fun maybeFilterOutAdditionalIfNotValid_noFitnessReadDeclared_filtersOutHistory() {
        val declaredPermissions =
            listOf(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                HealthPermissions.READ_MEDICAL_DATA_VACCINES.toHealthPermission(),
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
            )
        val result = permissionReader.maybeFilterOutAdditionalIfNotValid(declaredPermissions)
        assertThat(result)
            .containsExactlyElementsIn(
                listOf(
                    HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                    HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                    HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                    HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                    HealthPermissions.WRITE_MEDICAL_DATA.toHealthPermission(),
                    HealthPermissions.READ_MEDICAL_DATA_VACCINES.toHealthPermission(),
                    HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                )
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getAppPermissionsType_phrFlagOff_returnsFitnessOnly() = runTest {
        assertThat(permissionReader.getAppPermissionsType(TEST_APP_PACKAGE_NAME))
            .isEqualTo(FITNESS_PERMISSIONS_ONLY)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getAppPermissionsType_phrFlagOn_returnsCombinedPermissions() = runTest {
        assertThat(permissionReader.getAppPermissionsType(TEST_APP_PACKAGE_NAME))
            .isEqualTo(COMBINED_PERMISSIONS)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun getAppPermissionsType_medicalPermissionsOnlyApp_returnsMedicalPermissions() = runTest {
        assertThat(
                permissionReader.getAppPermissionsType(MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME)
            )
            .isEqualTo(MEDICAL_PERMISSIONS_ONLY)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun getAppPermissionsType_noPermissions_returnsFitnessPermissions() = runTest {
        assertThat(
                permissionReader.getAppPermissionsType(MEDICAL_PERMISSIONS_TEST_APP_PACKAGE_NAME)
            )
            .isEqualTo(FITNESS_PERMISSIONS_ONLY)
    }

    private fun String.toHealthPermission(): HealthPermission {
        return HealthPermission.fromPermissionString(this)
    }

    private fun clearFlag(app: String, permission: String, flag: Int) {
        runWithShellPermissionIdentity {
            context.packageManager.updatePermissionFlags(
                permission,
                app,
                flag,
                0,
                Process.myUserHandle(),
            )
        }
    }
}
