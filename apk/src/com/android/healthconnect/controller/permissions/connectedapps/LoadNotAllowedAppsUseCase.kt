package com.android.healthconnect.controller.permissions.connectedapps

import com.android.healthconnect.controller.shared.APP_3
import com.android.healthconnect.controller.shared.APP_4
import com.android.healthconnect.controller.shared.AppMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadNotAllowedAppsUseCase @Inject constructor() {
    /** Returns a list of apps. */
    suspend operator fun invoke(): List<AppMetadata> {
        return listOf(APP_3, APP_4)
    }
}