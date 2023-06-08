package bot.models

import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.MessageId
import domain.models.ShopWorkersParam
import domain.models.WorkerState
import restTS.models.Collector
import restTS.models.ShelfItem
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
    var workerState: WorkerState,
    var gmt: String,
    val deviceType: String,
    val deviceVersion: String
)


data class BotUser(
    var tsLogin: String,
    var tsPassword: String,
    var tsShop: String,
    var tgUserId: Identifier,
    var userRole: String,
    var sapFio: String?,
    var sapPosition: String?,
    var sapId: String?,
    var lastPrinter: String? = null,
    val deviceType: String,
    val deviceVersion: String
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
        workerState = workerState,
        gmt = gmt,
        deviceType = deviceType,
        deviceVersion = deviceVersion
    )

data class OrderSaveParam(
    var webNum: String? = null,
    var orderId: String? = null,
    var orderType: String? = null,
    var company: String? = null,
    var items: MutableList<ItemsSaveParam> = mutableListOf(),
    var collector: Collector? = Collector(),
    var ordType: String? = null,
    var printerName: String? = null,
    var messageId: MessageId? = null,
    var saveStatus: OrderDataSaveStatus? = null,
    var infoMessage: String? = null,
    var activeItem: String? = null
)

data class ItemsSaveParam(
    var goodCode: String? = null,
    var name: String? = null,
    var itemNo: String? = null,
    var incomplet: SaveIncompletParam? = null,
    var shelf: ShelfItem? = null,
    var quantity: String? = null,
    var routeIsNeeded: String? = null
)

data class SaveIncompletParam(
    var reasonCode: String? = null,
    var reasonName: String? = null,
    var comment: String? = null,
    var needComm: String? = null
)

enum class OrderDataSaveStatus {
    CANCEL,
    EXIST,
    STORN,
    FALSE,
    PROCESS,
    FINISH
}