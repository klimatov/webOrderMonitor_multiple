package domain.models

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
    var workerState: WorkerState
)

enum class WorkerState{
    CREATE,
    UPDATE,
    DELETE,
    WORK,
    DELETED
}