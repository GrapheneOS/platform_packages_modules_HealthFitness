package com.android.healthconnect.controller.migration.api

import android.health.connect.HealthConnectManager
import com.android.healthconnect.controller.service.IoDispatcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class LoadMigrationTimeoutUseCase
@Inject
constructor(
    private val manager: HealthConnectManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): Long {
        return withContext(dispatcher) {
            // TODO (b/275685600) fetch migration timeout

            Instant.parse("2023-03-31T07:06:05.432Z").toEpochMilli()
        }
    }
}
