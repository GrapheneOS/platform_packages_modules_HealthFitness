package com.android.healthconnect.controller.data.entries.api

import android.health.connect.datatypes.Record
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadSleepDataUseCase
@Inject
constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val loadEntriesHelper: LoadEntriesHelper
) : BaseUseCase<LoadDataEntriesInput, List<Record>>(dispatcher), ILoadSleepDataUseCase {

    /**
     * Used to load the sleep session records. For aggregating sleep we need to know both the start
     * and end times of each session.
     */
    override suspend fun execute(input: LoadDataEntriesInput): List<Record> {
        val timeFilterRange =
            loadEntriesHelper.getTimeFilter(
                input.displayedStartTime, input.period, endTimeExclusive = true)
        val dataTypes = HealthPermissionToDatatypeMapper.getDataTypes(input.permissionType)

        return dataTypes
            .map { dataType ->
                loadEntriesHelper.readDataType(dataType, timeFilterRange, input.packageName)
            }
            .flatten()
    }
}

interface ILoadSleepDataUseCase {
    suspend fun invoke(input: LoadDataEntriesInput): UseCaseResults<List<Record>>

    suspend fun execute(input: LoadDataEntriesInput): List<Record>
}
