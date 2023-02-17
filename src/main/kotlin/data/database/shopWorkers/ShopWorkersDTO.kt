package data.database.shopWorkers

import bot.NewWorker
import domain.models.ShopWorkersParam
import domain.models.WorkerState
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShopWorkersDTO(
    val workerId: UUID,
    val login: String,
    val password: String,
    val shop: String,
    val ownerTgId: Long,
    val isActive: Boolean,
    val shopOpen: Int,
    val shopClose: Int,
    val telegramChatId: Long,
    val gmt: String
)

fun ShopWorkersParam.mapToShopWorkersDTO(): ShopWorkersDTO =
    ShopWorkersDTO(
        workerId = workerId,
        login = login,
        password = password,
        shop = shop,
        ownerTgId = ownerTgId,
        isActive = isActive,
        shopOpen = shopOpen,
        shopClose = shopClose,
        telegramChatId = telegramChatId,
        gmt = gmt
    )

fun ShopWorkersDTO.mapToShopWorkersParam(): ShopWorkersParam =
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
        workerState = WorkerState.CREATE,
        gmt = gmt
    )

fun NewWorker.mapToShopWorkersDTO(): ShopWorkersDTO =
    ShopWorkersDTO(
        workerId = workerId,
        login = login,
        password = password,
        shop = shop,
        ownerTgId = ownerTgId,
        isActive = isActive,
        shopOpen = shopOpen,
        shopClose = shopClose,
        telegramChatId = telegramChatId,
        gmt = gmt
    )

fun ShopWorkersDTO.mapToNewWorker(): NewWorker =
    NewWorker(
        workerId = workerId,
        login = login,
        password = password,
        shop = shop,
        ownerTgId = ownerTgId,
        isActive = isActive,
        shopOpen = shopOpen,
        shopClose = shopClose,
        telegramChatId = telegramChatId,
        workerState = WorkerState.WORK,
        gmt = gmt
    )