package com.android.healthconnect.controller.tests.managedata

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.autodelete.AutoDeleteRange
import com.android.healthconnect.controller.autodelete.AutoDeleteViewModel
import com.android.healthconnect.controller.managedata.ManageDataFragment
import com.android.healthconnect.controller.tests.utils.di.FakeFeatureUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.whenever
import com.android.healthconnect.controller.utils.FeatureUtils
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class ManageDataFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue
    val autoDeleteViewModel: AutoDeleteViewModel = Mockito.mock(AutoDeleteViewModel::class.java)
    @Inject lateinit var fakeFeatureUtils: FeatureUtils

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(autoDeleteViewModel.storedAutoDeleteRange).then {
            MutableLiveData(
                AutoDeleteViewModel.AutoDeleteState.WithData(
                    AutoDeleteRange.AUTO_DELETE_RANGE_NEVER))
        }
    }

    @Test
    fun manageDataFragment_isDisplayed_newAppPriorityFlagOn() {
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewAppPriorityEnabled(true)
        launchFragment<ManageDataFragment>(Bundle())

        onView(withText("Auto-delete")).check(matches(isDisplayed()))
        onView(withText("Data sources and priority")).check(matches(isDisplayed()))
        onView(withText("Set units")).check(matches(isDisplayed()))
    }

    @Test
    fun manageDataFragment_isDisplayed_newAppPriorityFlagOff() {
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewAppPriorityEnabled(false)
        launchFragment<ManageDataFragment>(Bundle())

        onView(withText("Auto-delete")).check(matches(isDisplayed()))
        onView(withText("Data sources and priority")).check(doesNotExist())
        onView(withText("Set units")).check(matches(isDisplayed()))
    }
}
