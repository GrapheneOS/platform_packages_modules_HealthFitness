package com.android.healthconnect.controller.tests.datasources

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.AggregationCardsState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.DataSourcesAndAggregationsInfo
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PriorityListState
import com.android.healthconnect.controller.matchmaking.api.GetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.datasources.api.FakeLoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.tests.datasources.api.FakeLoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.tests.datasources.api.FakeLoadPriorityListUseCase
import com.android.healthconnect.controller.tests.datasources.api.FakeUpdatePriorityListUseCase
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_APP
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getDeviceDataSourcesInfo
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DataSourcesViewModelTest {

    companion object {
        private fun formattedAggregation(aggregation: String) =
            FormattedEntry.FormattedAggregation(
                aggregation = aggregation,
                aggregationA11y = aggregation,
                contributingApps = "Test App",
            )
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue lateinit var appInfoReader: AppInfoReader

    private lateinit var viewModel: DataSourcesViewModel
    private lateinit var getDeviceDataSourcesInfoUseCase: GetDeviceDataSourcesInfoUseCase

    private val loadMostRecentAggregationsUseCase = FakeLoadMostRecentAggregationsUseCase()
    private val healthConnectManager: HealthConnectManager = mock()
    private val loadPotentialAppSourcesUseCase = FakeLoadPotentialPriorityListUseCase()
    private val loadPriorityListUseCase = FakeLoadPriorityListUseCase()
    private val updatePriorityListUseCase = FakeUpdatePriorityListUseCase()

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        doAnswer { invocation ->
                val callback =
                    invocation.arguments[1] as OutcomeReceiver<List<DeviceDataSourceInfo>, *>
                callback.onResult(getDeviceDataSourcesInfo().toList())
                null
            }
            .whenever(healthConnectManager)
            .getDeviceDataSourceInfos(any(), any())
        getDeviceDataSourcesInfoUseCase =
            GetDeviceDataSourcesInfoUseCase(healthConnectManager, Dispatchers.Main)
        viewModel =
            DataSourcesViewModel(
                loadMostRecentAggregationsUseCase,
                loadPotentialAppSourcesUseCase,
                loadPriorityListUseCase,
                updatePriorityListUseCase,
                getDeviceDataSourcesInfoUseCase,
                appInfoReader,
            )
    }

    @After
    fun tearDown() {
        loadMostRecentAggregationsUseCase.reset()
        loadPotentialAppSourcesUseCase.reset()
        loadPriorityListUseCase.reset()
        updatePriorityListUseCase.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun setCurrentSelection_setsCorrectCategory() = runTest {
        viewModel.setCurrentSelection(HealthDataCategory.ACTIVITY)
        assertThat(viewModel.getCurrentSelection()).isEqualTo(HealthDataCategory.ACTIVITY)
    }

    @Test
    fun loadData_withAllData_returnsDataSourcesAndAggregationsInfoWithData() = runTest {
        val mostRecentAggregations =
            listOf(
                AggregationCardInfo(
                    FitnessPermissionType.STEPS,
                    formattedAggregation("100 steps"),
                    Instant.now(),
                )
            )
        val priorityList = listOf(TEST_APP, TEST_APP_2)
        val potentialAppSources = listOf(TEST_APP_3)
        loadMostRecentAggregationsUseCase.setMostRecentAggregations(mostRecentAggregations)
        loadPriorityListUseCase.setPriorityList(priorityList)
        loadPotentialAppSourcesUseCase.setPotentialPriorityList(potentialAppSources)
        val testObserver = TestObserver<DataSourcesAndAggregationsInfo>()
        viewModel.dataSourcesAndAggregationsInfo.observeForever(testObserver)
        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        val expected =
            DataSourcesAndAggregationsInfo(
                priorityListState = PriorityListState.WithData(true, priorityList),
                potentialAppSourcesState =
                    PotentialAppSourcesState.WithData(true, potentialAppSources),
                aggregationCardsState = AggregationCardsState.WithData(true, mostRecentAggregations),
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun updatePriorityList_callsUpdatePriorityListUseCase_withCorrectListAndCategory() = runTest {
        val newPriorityList = listOf(TEST_APP_3.packageName, TEST_APP.packageName)
        val category = HealthDataCategory.SLEEP
        viewModel.updatePriorityList(newPriorityList, category)
        advanceUntilIdle()

        assertThat(updatePriorityListUseCase.category).isEqualTo(category)
        assertThat(updatePriorityListUseCase.priorityList).isEqualTo(newPriorityList)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadPotentialAppSources_ddpFlagEnabled_filtersLegacyDeviceDataProvider() = runTest {
        val potentialAppSources = listOf(TEST_APP, DEVICE_DATA_PROVIDER_APP)
        loadPotentialAppSourcesUseCase.setPotentialPriorityList(potentialAppSources)
        val testObserver = TestObserver<DataSourcesAndAggregationsInfo>()
        viewModel.dataSourcesAndAggregationsInfo.observeForever(testObserver)

        viewModel.loadPotentialAppSources(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val actual = testObserver.getLastValue()?.potentialAppSourcesState
        val expected = PotentialAppSourcesState.WithData(true, listOf(TEST_APP))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadPotentialAppSources_ddpFlagDisabled_includesLegacyDeviceDataProvider() = runTest {
        val potentialAppSources = listOf(TEST_APP, DEVICE_DATA_PROVIDER_APP)
        loadPotentialAppSourcesUseCase.setPotentialPriorityList(potentialAppSources)
        val testObserver = TestObserver<DataSourcesAndAggregationsInfo>()
        viewModel.dataSourcesAndAggregationsInfo.observeForever(testObserver)

        viewModel.loadPotentialAppSources(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val actual = testObserver.getLastValue()?.potentialAppSourcesState
        val expected = PotentialAppSourcesState.WithData(true, potentialAppSources)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadCurrentPriorityList_ddpFlagEnabled_filtersLegacyDeviceDataProvider() = runTest {
        val priorityList = listOf(TEST_APP, DEVICE_DATA_PROVIDER_APP)
        loadPriorityListUseCase.setPriorityList(priorityList)
        val testObserver = TestObserver<DataSourcesAndAggregationsInfo>()
        viewModel.dataSourcesAndAggregationsInfo.observeForever(testObserver)

        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val actual = testObserver.getLastValue()?.priorityListState
        val expected = PriorityListState.WithData(true, listOf(TEST_APP))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadCurrentPriorityList_ddpFlagDisabled_includesLegacyDeviceDataProvider() = runTest {
        val priorityList = listOf(TEST_APP, DEVICE_DATA_PROVIDER_APP)
        loadPriorityListUseCase.setPriorityList(priorityList)
        val testObserver = TestObserver<DataSourcesAndAggregationsInfo>()
        viewModel.dataSourcesAndAggregationsInfo.observeForever(testObserver)

        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val actual = testObserver.getLastValue()?.priorityListState
        val expected = PriorityListState.WithData(true, priorityList)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun updatePriorityList_ddpFlagDisabled_withCurrentDevice_noChange() = runTest {
        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val newPriorityList = listOf(TEST_APP_3.packageName, TEST_APP.packageName, TEST_PHONE_SPN)
        val category = HealthDataCategory.ACTIVITY

        viewModel.updatePriorityList(newPriorityList, category)
        advanceUntilIdle()

        assertThat(updatePriorityListUseCase.priorityList).isEqualTo(newPriorityList)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun updatePriorityList_ddpFlagEnabled_withCurrentDevice_addsLegacyDevice() = runTest {
        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val newPriorityList = listOf(TEST_APP_3.packageName, TEST_APP.packageName, TEST_PHONE_SPN)
        val category = HealthDataCategory.ACTIVITY

        viewModel.updatePriorityList(newPriorityList, category)
        advanceUntilIdle()

        val expectedList =
            listOf(
                TEST_APP_3.packageName,
                TEST_APP.packageName,
                DEVICE_DATA_PROVIDER_PACKAGE,
                TEST_PHONE_SPN,
            )
        assertThat(updatePriorityListUseCase.priorityList).isEqualTo(expectedList)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun updatePriorityList_ddpFlagEnabled_removesLegacyDevice_whenCurrentDeviceMissing() = runTest {
        viewModel.loadData(HealthDataCategory.ACTIVITY)
        advanceUntilIdle()

        val newPriorityList =
            listOf(TEST_APP_3.packageName, TEST_APP.packageName, DEVICE_DATA_PROVIDER_PACKAGE)
        val category = HealthDataCategory.ACTIVITY

        viewModel.updatePriorityList(newPriorityList, category)
        advanceUntilIdle()

        val expectedList = listOf(TEST_APP_3.packageName, TEST_APP.packageName)
        assertThat(updatePriorityListUseCase.priorityList).isEqualTo(expectedList)
    }
}
