package bot

import data.database.shopWorkers.ShopWorkersDTO
import dev.inmo.tgbotapi.types.Identifier
import domain.models.ShopWorkersParam
import domain.models.WorkerState
import java.util.*


data class NewWorker(
    var workerId: UUID,
    var login: String,
    var password: String,
    var shop: String,
    var ownerTgId: Identifier,
    var isActive: Boolean,
    var shopOpen: Int,
    var shopClose: Int,
    var telegramChatId: Long,
    var workerState: WorkerState
)


data class BotUser(
    var tsLogin: String,
    var tsPassword: String,
    var tsShop: String,
    var tgUserId: Identifier,
    var userRole: String
)

enum class UserRole {
    GUEST,
    USER,
    ADMIN,
    ROOT
}

fun NewWorker.mapToShopWorkersParam(): ShopWorkersParam =
    ShopWorkersParam(
        workerId = workerId,
        login = login,
        password = password,
        shop = shop,
        ownerTgId = ownerTgId,
        isActive = isActive,
        shopOpen = shopOpen,
        shopClose = shopClose,
        telegramChatId = telegramChatId,
        workerState = workerState
    )
