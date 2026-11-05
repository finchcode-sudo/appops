package dev.izumi.appopsnext.history.model

data class AppOpHistoryEvent(
    val uid: Int,
    val packageName: String,
    val operationName: String,
    val attributionTag: String?,
    val accessTimeMillis: Long,
    val durationMillis: Long?,
    val uidState: String,
    val flags: String,
    val accessCount: Int = 1,
    val isAggregated: Boolean = false,
    val rejectCount: Int = 0,
    /** Aggregate counts cover this interval; accessTimeMillis is its end, not an attempt time. */
    val intervalStartTimeMillis: Long? = null,
)

data class HistoryPermission(
    val shellOperationName: String,
) {
    init {
        require(shellOperationName.isNotBlank()) {
            "History permission operation name must not be blank"
        }
    }
}

object HistoryPermissionDefaults {
    val permissions = listOf(
        HistoryPermission("FINE_LOCATION"),
        HistoryPermission("COARSE_LOCATION"),
        HistoryPermission("CAMERA"),
        HistoryPermission("RECORD_AUDIO"),
        HistoryPermission("READ_CONTACTS"),
        HistoryPermission("READ_PHONE_STATE"),
        HistoryPermission("READ_CALENDAR"),
        HistoryPermission("READ_SMS"),
        HistoryPermission("READ_CLIPBOARD"),
        HistoryPermission("READ_DEVICE_IDENTIFIERS"),
    )
}

enum class AppOpHistoryFailureReason {
    BACKEND_UNAVAILABLE,
    COMMAND_FAILED,
    COMMAND_TIMED_OUT,
}

sealed interface AppOpHistoryLoadResult {
    data class Success(
        val events: List<AppOpHistoryEvent>,
    ) : AppOpHistoryLoadResult

    data class Failure(
        val reason: AppOpHistoryFailureReason,
    ) : AppOpHistoryLoadResult
}
