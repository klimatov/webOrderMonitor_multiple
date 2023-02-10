package domain.repository

import data.restTS.models.WebOrder
import dev.inmo.tgbotapi.types.ChatIdentifier

interface BotProcessingRepository {
    var shop: String
    var targetChatId: ChatIdentifier
    var currentInfoMsgId: Long?
    var newInfoMsgId: Long?
    var dayConfirmedCount: Int
    var msgNotification: Boolean
    var notConfirmedOrders: Int
    var shopOpenTime: Int
    var shopCloseTime: Int

    suspend fun build(
        shop: String,
        targetChatId: ChatIdentifier,
        shopOpenTime: Int,
        shopCloseTime: Int
    )
    suspend fun botSendInfoMessage()
    suspend fun updateInfoMsg()
    suspend fun botSendMessage(webOrder: WebOrder?): Long?
    suspend fun botConfirmMessage(webOrder: WebOrder?)
    suspend fun botTimerUpdate(webOrder: WebOrder?)
}