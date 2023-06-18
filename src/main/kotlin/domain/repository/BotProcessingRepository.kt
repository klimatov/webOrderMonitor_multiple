package domain.repository

import dev.inmo.tgbotapi.types.ChatIdentifier
import restTS.models.WebOrder

interface BotProcessingRepository {
    var shop: String
    var targetChatId: ChatIdentifier
    var currentInfoMsgId: Long?
//    var newInfoMsgId: Long?
    var dayOrderRecievedCount: Int

    var dayOrderConfirmedCount: Int
    var dayOrderConfirmedByEmployee: MutableMap<String, Int>

    var msgNotification: Boolean
    var notConfirmedOrders: Int
    var shopOpenTime: Int
    var shopCloseTime: Int
    var gmt: String

    suspend fun build(
        shop: String,
        targetChatId: ChatIdentifier,
        shopOpenTime: Int,
        shopCloseTime: Int,
        gmt: String

    )
    suspend fun botSendInfoMessage()
    suspend fun updateInfoMsg()
    suspend fun botSendMessage(webOrder: WebOrder?): Long?
//    suspend fun botConfirmMessage(webOrder: WebOrder?)
    suspend fun botUpdateMessage(webOrder: WebOrder?)
    suspend fun updateErrorInfoMsg(errorCode: Int)
}