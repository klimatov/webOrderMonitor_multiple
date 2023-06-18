package domain.orderProcessing


import com.google.gson.Gson
import domain.models.ShopParameters
import domain.repository.BotProcessingRepository
import domain.repository.ServerTSRepository
import domain.repository.ShopParametersDBRepository
import restTS.models.WebOrder
import restTS.models.WebOrderSimply
import utils.Logging


class Processing(private val serverTSRepository: ServerTSRepository, val gmt: String, val shop: String) {
    private val tag = this::class.java.simpleName
    var activeOrders: MutableMap<String?, WebOrder?> = mutableMapOf() //иниц. список активных вебок

    suspend fun processInworkOrders(
        inworkOrderList: List<WebOrderSimply>,
        botProcessingRepository: BotProcessingRepository,
        shopParametersDBRepository: ShopParametersDBRepository
    ) {
        var newFlag = false
        // проверка, появились ли новые вебки, отсутствующие в списке активных?
        inworkOrderList.forEach { webOrder ->
            if (!activeOrders.containsKey(webOrder.webNum)) {
                val newOrder = serverTSRepository.getNewOrderList(webOrder.webNum)
                if (newOrder.isNotEmpty()) {
                    activeOrders[webOrder.webNum] =
                        newOrder[0]// добавляем новую вебку в список активных
                    botProcessingRepository.dayOrderRecievedCount++ // увеличиваем на 1 счетчик поступивших за день

                    itemsUpdate(webOrder.webNum) // обновляем items и остатки по каждому, если все ок, то статус true

                    activeOrders[webOrder.webNum]?.activeTime =
                        BotMessage().timeDiff(webOrder.docDate, gmt = gmt) // время активности
                    newOrder(webOrder.webNum, botProcessingRepository)
                    newFlag = true
                }
            }
        }
        var msg = ""
        activeOrders.forEach { msg += " ${it.key}, ${it.value?.docDate} |" }
        Logging.i(tag, "$shop Active orders: (${activeOrders.count()} pc.) $msg")

        // проверка, исчезли (подтверждены) ли ранее сохраненные вебки?
        // +обновляем таймера в сообщениях активных вебок
        // +обновляем товары и остатки в непрогрузившихся
        val delOrderList: MutableList<String> = mutableListOf()
        activeOrders.forEach { activeOrder ->
            if (activeOrder.value?.itemsUpdateStatus == false) itemsUpdate(activeOrder.key)


            if (activeOrder.value?.activeTime != BotMessage().timeDiff( // если время подтверждения изменилось
                    activeOrder.value?.docDate,
                    gmt = gmt
                )
            ) {
                updateOrderTimer(activeOrder.key, botProcessingRepository) //обновляем таймер в сообщении
            }


            var deleteFlag = true
            inworkOrderList.forEach {
                if (activeOrder.key == it.webNum) deleteFlag = false
            }
            if (deleteFlag) delOrderList.add(activeOrder.key.toString())//delOrder(activeOrder.key)
        }
        // удаляем подтвержденные вебки
        delOrderList.forEach {
            delOrder(it, botProcessingRepository)
        }

        // обновляем инфокнопку
        botProcessingRepository.notConfirmedOrders = activeOrders.count()
        botProcessingRepository.updateInfoMsg()

        // записываем активные в БД> если были изменения +-
        if (newFlag || delOrderList.count() > 0) {
            val serializedActiveOrders = Gson().toJson(activeOrders)
            shopParametersDBRepository.updateShopParameters(ShopParameters(
                shop = botProcessingRepository.shop,
                serializedActiveOrders = serializedActiveOrders,
                currentInfoMsgId = botProcessingRepository.currentInfoMsgId?:0,
                dayConfirmedCount = botProcessingRepository.dayOrderRecievedCount
            ))

            Logging.i(tag, "$shop activeOrders SAVE: $serializedActiveOrders")
            Logging.i(
                tag,
                "$shop currentInfoMsgId SAVE: ${botProcessingRepository.currentInfoMsgId}"
            )
            Logging.i(
                tag,
                "$shop dayConfirmedCount SAVE: ${botProcessingRepository.dayOrderRecievedCount}"
            )
        }
    }

    // обновляем товары и остатки
    private suspend fun itemsUpdate(webNum: String?) {
        var result = true

        // обновляем список товара (items) если он пуст
        if (activeOrders[webNum]?.items?.isEmpty() == true) {
            activeOrders[webNum]?.items =
                serverTSRepository.getItems(activeOrders[webNum]?.orderId)
            if (serverTSRepository.errorCode != 200) result =
                false // фиксируем что в запросе все ок
        }

        activeOrders[webNum]?.items?.forEach { items ->  // обновляем остатки по каждому товару если он пуст
            if (items.remains.isEmpty()) {
                items.remains = serverTSRepository.getRemains(items.goodCode)
                if (serverTSRepository.errorCode != 200) result =
                    false // фиксируем что в запросе все ок
            }
        }
        activeOrders[webNum]?.itemsUpdateStatus =
            result // обновляем items и остатки по каждому, если все ок, то статус true
        Logging.d(tag, "$shop $webNum Items and remains update result: $result")
    }

    private suspend fun newOrder(webNum: String?, botProcessingRepository: BotProcessingRepository) {
        val messageId: Long? = botProcessingRepository.botSendMessage(activeOrders[webNum])
        activeOrders[webNum]?.messageId = messageId
    }

    private suspend fun delOrder(webNum: String?, botProcessingRepository: BotProcessingRepository) {
        val updateOrders = serverTSRepository.getNewOrderList(webNum)
        if (updateOrders.isNotEmpty()) {
            val updateOrder = updateOrders[0]
            activeOrders[webNum]?.docStatus = updateOrder.docStatus
            activeOrders[webNum]?.paid = updateOrder.paid
            activeOrders[webNum]?.collector = updateOrder.collector

            Logging.d(tag, "!!!!!! Для счетчика подтвержденных ${updateOrder.docStatus} ${updateOrder.collector}")
            Logging.d(tag, "webOrder from TS server: $updateOrder")
            Logging.d(tag, "webOrder from memory DB: ${activeOrders[webNum]}")
        }
//        botProcessingRepository.dayConfirmedCount++ // увеличиваем на 1 счетчик собранных за день
        // TODO: переделать счетчик собранных

        if (activeOrders[webNum]?.docStatus == "WRQST_CRTD") activeOrders[webNum]?.docStatus = ""

        botProcessingRepository.botUpdateMessage(activeOrders[webNum])
        activeOrders.remove(webNum)
    }

    private suspend fun updateOrderTimer(webNum: String?, botProcessingRepository: BotProcessingRepository) {
        botProcessingRepository.botUpdateMessage(activeOrders[webNum])
        activeOrders[webNum]?.activeTime =
            BotMessage().timeDiff(activeOrders[webNum]?.docDate, gmt = gmt) // время активности
    }


}