package com.android.healthconnect.controller.deletion

/** Constants used for deletion operations. */
object DeletionConstants {

    /** Used for attaching the DeletionFragment. */
    const val FRAGMENT_TAG_DELETION = "FRAGMENT_TAG_DELETION"

    /** The key of a fragment result representing that the delete data button has been pressed. */
    const val START_DELETION_EVENT = "START_DELETION_EVENT"

    /** The key of a fragment result representing that a time range has been selected. */
    const val TIME_RANGE_SELECTION_EVENT = "TIME_RANGE_SELECTION_EVENT"

    /** The key of a fragment result representing that the go back button has been pressed. */
    const val GO_BACK_EVENT = "GO_BACK_EVENT"

    /**
     * The key of a fragment result representing that the deletion parameters have been confirmed.
     */
    const val CONFIRMATION_EVENT = "CONFIRMATION_EVENT"

    /** The key of a fragment result representing a successful deletion. */
    const val DELETION_COMPLETED_SUCCESS_EVENT = "DELETION_COMPLETED_SUCCESS_EVENT"

    /** The key of a fragment result representing a failed deletion. */
    const val DELETION_FAILURE_EVENT = "DELETION_FAILURE_EVENT"

    /** The key of a fragment result representing that the try again button has been pressed. */
    const val TRY_AGAIN_EVENT = "TRY_AGAIN_EVENT"

    /** The bundle key for the deletion type requested by a fragment. */
    const val DELETION_TYPE = "DELETION_TYPE"

    /**
     * The bundle key for showing the time range picker dialog from a fragment, used when the value
     * should be false.
     */
    const val SHOW_PICKER = "SHOW_PICKER"
}
