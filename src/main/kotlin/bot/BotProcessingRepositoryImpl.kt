package bot

import botCore
import domain.repository.BotProcessingRepository
import data.restTS.data.WebOrder

class BotProcessingRepositoryImpl: BotProcessingRepository {
    override var currentInfoMsgId: Long?
        get() = botCore.currentInfoMsgId
        set(value) {botCore.currentInfoMsgId = value}

    override var newInfoMsgId: Long?
        get() = botCore.newInfoMsgId
        set(value) {botCore.newInfoMsgId = value}
    override var dayConfirmedCount: Int
        get() = botCore.dayConfirmedCount
        set(value) {botCore.dayConfirmedCount = value}
    override var msgNotification: Boolean
        get() = botCore.msgNotification
        set(value) {botCore.msgNotification = value}

    override var notConfirmedOrders: Int
        get() = botCore.notConfirmedOrders
        set(value) {botCore.notConfirmedOrders = value}

    override suspend fun botSendInfoMessage() {
        botCore.botSendInfoMessage()
    }

    override suspend fun updateInfoMsg() {
        botCore.updateInfoMsg()
    }

    override suspend fun botSendMessage(webOrder: WebOrder?): Long? {
        return botCore.botSendMessage(webOrder)
    }

    override suspend fun botConfirmMessage(webOrder: WebOrder?) {
        botCore.botConfirmMessage(webOrder)
    }

    override suspend fun botTimerUpdate(webOrder: WebOrder?) {
        botCore.botTimerUpdate(webOrder)
    }
}