package com.android.healthconnect.controller.datasources.api

import android.health.connect.datatypes.Record
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.LoadPriorityListUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.utils.toInstantAtStartOfDay
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadPriorityEntriesUseCase
@Inject
constructor(
    private val loadEntriesHelper: LoadEntriesHelper,
    @LoadPriorityListUseCase
    private val loadPriorityListUseCase: BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<LoadPriorityEntriesInput, List<Record>>(dispatcher) {

    /**
     * Returns a list of records from the specified date originating from any of the apps on the
     * priority list for this healthPermissionType.
     */
    override suspend fun execute(input: LoadPriorityEntriesInput): List<Record> {
        val localDateInstant = input.localDate.toInstantAtStartOfDay()
        val records = mutableListOf<Record>()

        when (
            val priorityAppsResult =
                loadPriorityListUseCase.invoke(
                    HealthDataCategoryExtensions.fromFitnessPermissionType(
                        input.fitnessPermissionType
                    )
                )
        ) {
            is UseCaseResults.Success -> {
                val priorityApps = priorityAppsResult.data

                priorityApps.forEach { priorityApp ->
                    val input =
                        LoadDataEntriesInput(
                            FitnessPermissionType.SLEEP,
                            packageName = priorityApp.packageName,
                            displayedStartTime = localDateInstant,
                            period = DateNavigationPeriod.PERIOD_DAY,
                            showDataOrigin = false,
                        )
                    val entryRecords = loadEntriesHelper.readRecords(input)

                    records.addAll(entryRecords)
                }
            }
            is UseCaseResults.Failed -> {
                throw priorityAppsResult.exception
            }
        }

        // Sorted for testing
        return records.sortedByDescending { loadEntriesHelper.getStartTime(it) }
    }
}

data class LoadPriorityEntriesInput(
    val fitnessPermissionType: FitnessPermissionType,
    val localDate: LocalDate,
)
