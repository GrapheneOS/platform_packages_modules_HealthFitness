package com.android.healthconnect.controller.dataentries

import com.android.healthconnect.controller.shared.DataType

/** OnDeleteListener for Data entries. */
interface OnDeleteEntryListener {
    fun onDeleteEntry(id: String, dataType: DataType, index: Int)
}
