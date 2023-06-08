package domain.models

import java.time.LocalDateTime
import java.util.*

data class ShopWorkersParam(
    val workerId: UUID,
    val login: String,
    val password: String,
    val shop: String,
    val ownerTgId: Long,
    val isActive: Boolean,
    val shopOpen: Int,
    val shopClose: Int,
    val telegramChatId: Long,
    var workerState: WorkerState,
    val gmt: String,
    var loginTime: LocalDateTime = LocalDateTime.now(),
    val deviceType: String,
    val deviceVersion: String
)

enum class WorkerState{
    CREATE,
    UPDATE,
    DELETE,
    WORK,
    DELETED
}