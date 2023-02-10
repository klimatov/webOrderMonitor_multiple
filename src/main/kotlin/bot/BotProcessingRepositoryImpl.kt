package bot

import botCore
import domain.repository.BotProcessingRepository
import data.restTS.models.WebOrder
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import domain.orderProcessing.BotMessage

class BotProcessingRepositoryImpl : BotProcessingRepository {

    override var shop: String = ""
        get() {
            return field
        }
        set(value) {
            field = value
            if (botCore.botInstancesParameters[field] == null) {
                botCore.botInstancesParameters[field] = BotInstanceParameters()
            }
        }
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

    override var newInfoMsgId: Long?
        get() = botCore.botInstancesParameters[shop]?.newInfoMsgId
        set(value) {
            botCore.botInstancesParameters[shop]?.newInfoMsgId = value
        }
    override var dayConfirmedCount: Int
        get() = botCore.botInstancesParameters[shop]?.dayConfirmedCount ?: 0
        set(value) {
            botCore.botInstancesParameters[shop]?.dayConfirmedCount = value
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

    override suspend fun botSendInfoMessage() {
        botCore.botSendInfoMessage(shop)
    }

    override suspend fun updateInfoMsg() {
        botCore.updateInfoMsg(shop)
    }

    override suspend fun botSendMessage(webOrder: WebOrder?): Long? {
        return botCore.botSendMessage(webOrder, shop)
    }

    override suspend fun botConfirmMessage(webOrder: WebOrder?) {
        botCore.botConfirmMessage(webOrder, shop)
    }

    override suspend fun botTimerUpdate(webOrder: WebOrder?) {
        botCore.botTimerUpdate(webOrder, shop)
    }
}