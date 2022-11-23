package com.android.healthconnect.controller.exportdata

import androidx.annotation.IdRes
import com.android.healthconnect.controller.categories.HealthDataCategory

data class ExportDataSelectionItem(
    val category: HealthDataCategory,
    var selected: Boolean = false,
    @IdRes val checkboxId: Int
)
