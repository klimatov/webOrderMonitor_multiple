package bot

import bot.models.BotInstanceParameters
import botCore
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ChatIdentifier
import domain.repository.BotProcessingRepository
import restTS.models.WebOrder

class BotProcessingRepositoryImpl : BotProcessingRepository {

    override var shop: String = ""
    override var shopOpenTime: Int
        get() = botCore.botInstancesParameters[shop]?.shopOpenTime ?:0
        set(value) {botCore.botInstancesParameters[shop]?.shopOpenTime = value}
    override var shopCloseTime: Int
        get() = botCore.botInstancesParameters[shop]?.shopCloseTime ?:0
        set(value) {botCore.botInstancesParameters[shop]?.shopCloseTime = value}
    override var gmt: String
        get() = botCore.botInstancesParameters[shop]?.gmt ?:"+0300"
        set(value) {botCore.botInstancesParameters[shop]?.gmt = value}
    override var targetChatId: ChatIdentifier
        get() = botCore.botInstancesParameters[shop]?.targetChatId ?: ChatId(0)
        set(value) {
            botCore.botInstancesParameters[shop]?.targetChatId = value
        }
    override var currentInfoMsgId: Long?
        get() = botCore.botInstancesParameters[shop]?.currentInfoMsgId
        set(value) {
            botCore.botInstancesParameters[shop]?.currentInfoMsgId = value
        }

//    override var newInfoMsgId: Long?
//        get() = botCore.botInstancesParameters[shop]?.newInfoMsgId
//        set(value) {
//            botCore.botInstancesParameters[shop]?.newInfoMsgId = value
//        }
    override var dayOrderRecievedCount: Int
        get() = botCore.botInstancesParameters[shop]?.dayOrderRecievedCount ?: 0
        set(value) {
            botCore.botInstancesParameters[shop]?.dayOrderRecievedCount = value
        }
    override var dayOrderConfirmedCount: Int
        get() = botCore.botInstancesParameters[shop]?.dayOrderConfirmedCount ?: 0
        set(value) {
            botCore.botInstancesParameters[shop]?.dayOrderConfirmedCount = value
        }
    override var dayOrderConfirmedByEmployee: MutableMap<String, Int>
        get() = botCore.botInstancesParameters[shop]?.dayOrderConfirmedByEmployee ?: mutableMapOf()
        set(value) {
            botCore.botInstancesParameters[shop]?.dayOrderConfirmedByEmployee = value
        }
    override var msgNotification: Boolean
        get() = botCore.botInstancesParameters[shop]?.msgNotification ?: true
        set(value) {
            botCore.botInstancesParameters[shop]?.msgNotification = value
        }

    override var notConfirmedOrders: Int
        get() = botCore.botInstancesParameters[shop]?.notConfirmedOrders ?: 0
        set(value) {
            botCore.botInstancesParameters[shop]?.notConfirmedOrders = value
        }
    
    override suspend fun build(
        shopNew: String,
        targetChatId: ChatIdentifier,
        shopOpenTime: Int,
        shopCloseTime: Int,
        gmt: String
    ) {
        if (botCore.botInstancesParameters[shopNew] == null) {
            botCore.botInstancesParameters[shopNew] = BotInstanceParameters(
                shopOpenTime = shopOpenTime,
                shopCloseTime = shopCloseTime,
                targetChatId = targetChatId,
                msgNotification = botCore.botMessage.shopInWork(shopOpenTime, shopCloseTime, gmt)
            ) //инициализируем параметры
        }

        this.shop = shopNew
        this.targetChatId = targetChatId
        this.shopOpenTime = shopOpenTime
        this.shopCloseTime = shopCloseTime
        this.gmt = gmt

//        botCore.botInstancesParameters[shopNew]?.targetChatId = targetChatId
//        botCore.botInstancesParameters[shopNew]?.shopOpenTime = shopOpenTime
//        botCore.botInstancesParameters[shopNew]?.shopCloseTime = shopCloseTime
//        botCore.botInstancesParameters[shopNew]?.gmt = gmt

    }

    override suspend fun botSendInfoMessage() {
        botCore.botSendInfoMessage(shop)
    }

    override suspend fun updateErrorInfoMsg(errorCode: Int) {
        botCore.updateErrorInfoMsg(shop, errorCode)
    }

    override suspend fun updateInfoMsg() {
        botCore.updateInfoMsg(shop)
    }

    override suspend fun botSendMessage(webOrder: WebOrder?): Long? {
        return botCore.botSendMessage(webOrder, shop)
    }

//    override suspend fun botConfirmMessage(webOrder: WebOrder?) {
//        botCore.botConfirmMessage(webOrder, shop)
//    }

    override suspend fun botUpdateMessage(webOrder: WebOrder?) {
        botCore.botUpdateMessage(webOrder, shop)
    }
}