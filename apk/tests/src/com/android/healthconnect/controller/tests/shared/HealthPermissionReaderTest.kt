package com.android.healthconnect.controller.tests.shared

import android.content.Context
import android.health.connect.HealthPermissions
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.UNSUPPORTED_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeFeatureUtils
import com.android.healthconnect.controller.utils.FeatureUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class HealthPermissionReaderTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var permissionReader: HealthPermissionReader
    @Inject lateinit var fakeFeatureUtils: FeatureUtils
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun getDeclaredPermissions_hidesSessionTypesIfDisabled() = runTest {
        (fakeFeatureUtils as FakeFeatureUtils).setIsSessionTypesEnabled(false)

        assertThat(permissionReader.getDeclaredPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission())
    }

    @Test
    fun getDeclaredPermissions_hidesExerciseRouteIfDisabled() = runTest {
        (fakeFeatureUtils as FakeFeatureUtils).setIsExerciseRoutesEnabled(false)

        assertThat(permissionReader.getDeclaredPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission())
    }

    @Test
    fun getDeclaredPermissions_filtersOutBackgroundReadPermission() = runTest {
        (fakeFeatureUtils as FakeFeatureUtils).setIsBackgroundReadEnabled(true)

        assertThat(permissionReader.getDeclaredPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission())
    }

    @Test
    fun getDeclaredPermissions_returnsAllPermissions_exceptReadAllRoutes() = runTest {
        assertThat(permissionReader.getDeclaredPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                HealthPermissions.WRITE_EXERCISE_ROUTE.toHealthPermission(),
                HealthPermissions.READ_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_EXERCISE.toHealthPermission(),
                HealthPermissions.WRITE_SLEEP.toHealthPermission(),
                HealthPermissions.READ_SLEEP.toHealthPermission(),
                HealthPermissions.READ_ACTIVE_CALORIES_BURNED.toHealthPermission(),
                HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED.toHealthPermission())
    }

    @Test
    fun isRationalIntentDeclared_withIntent_returnsTrue() {
        assertThat(permissionReader.isRationalIntentDeclared(TEST_APP_PACKAGE_NAME)).isTrue()
    }

    @Test
    fun isRationalIntentDeclared_noIntent_returnsTrue() {
        assertThat(permissionReader.isRationalIntentDeclared(UNSUPPORTED_TEST_APP_PACKAGE_NAME))
            .isFalse()
    }

    @Test
    fun getAppsWithHealthPermissions_returnsSupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithHealthPermissions())
            .containsAtLeast(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
    }

    @Test
    fun getAppsWithHealthPermissions_returnsDistinctApps() = runTest {
        val apps = permissionReader.getAppsWithHealthPermissions()
        assertThat(apps).isEqualTo(apps.distinct())
    }


    @Test
    fun getAppsWithHealthPermissions_doesNotReturnUnsupportedApps() = runTest {
        assertThat(permissionReader.getAppsWithHealthPermissions())
            .doesNotContain(UNSUPPORTED_TEST_APP_PACKAGE_NAME)
    }

    private fun String.toHealthPermission(): HealthPermission {
        return HealthPermission.fromPermissionString(this)
    }
}
