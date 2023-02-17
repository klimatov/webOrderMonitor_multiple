package domain.orderProcessing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import data.restTS.models.WebOrder
import domain.models.ShopParameters
import domain.repository.BotProcessingRepository
import domain.repository.ServerTSRepository
import domain.repository.ShopParametersDBRepository
import kotlinx.coroutines.delay
import utils.Logging
import java.time.LocalDateTime

class OrderDaemon(
    private val login: String,
    private val password: String,
    private val werk: String,
    private val serverTSRepository: ServerTSRepository,
) {
    private val tag = this::class.java.simpleName

    private val processing = Processing(serverTSRepository)

    private val botMessage = BotMessage()

    //    val appStartTime: LocalDateTime = LocalDateTime.now()
    var loginTime: LocalDateTime = LocalDateTime.now()

    suspend fun start(
        botProcessingRepository: BotProcessingRepository,
        shopParametersDBRepository: ShopParametersDBRepository,
    ) {

        Logging.i(tag, "$werk Запускаем...")

        var serializedActiveOrders: String? = null
        var currentInfoMsgId: Long? = null
        var dayConfirmedCount: Int? = null

        // считываем данные из БД
        val shopParameters = shopParametersDBRepository.getShopParametersByShop(werk)

        // если запись в БД отсутствует - создаем
        if (shopParameters == null) shopParametersDBRepository.updateShopParameters(
            ShopParameters(
                shop = werk,
                serializedActiveOrders = "{}",
                currentInfoMsgId = 0,
                dayConfirmedCount = 0
            )
        ) else {
            serializedActiveOrders = shopParameters.serializedActiveOrders
            currentInfoMsgId = shopParameters.currentInfoMsgId
            dayConfirmedCount = shopParameters.dayConfirmedCount
        }

        if (serializedActiveOrders != null) {
            val type = object : TypeToken<MutableMap<String?, WebOrder?>>() {}.type
            processing.activeOrders =
                Gson().fromJson(serializedActiveOrders, type)
            Logging.i(tag, "$werk activeOrders READ: $serializedActiveOrders")
        }

        if (currentInfoMsgId != null) {
            botProcessingRepository.currentInfoMsgId = currentInfoMsgId.toLong()
            botProcessingRepository.newInfoMsgId = botProcessingRepository.currentInfoMsgId
            Logging.i(tag, "$werk currentInfoMsgId READ: $currentInfoMsgId")
        }

        if (dayConfirmedCount != null) {
            botProcessingRepository.dayConfirmedCount = dayConfirmedCount.toInt()
            Logging.i(tag, "$werk dayConfirmedCount READ: $dayConfirmedCount")
        }


        serverTSRepository.login(login, password, werk)

        while (true) {  // основной цикл проверки
            Logging.i(tag, "$werk Обновляем данные...")

            // проверяем открыт ли магазин, триггерим звук уведомлений
            if (botMessage.shopInWork(
                    shopOpenTime = botProcessingRepository.shopOpenTime,
                    shopCloseTime = botProcessingRepository.shopCloseTime
                ) != botProcessingRepository.msgNotification
            ) {
                botProcessingRepository.msgNotification = !botProcessingRepository.msgNotification
                botProcessingRepository.botSendInfoMessage()
                // если магазин закрылся, то сбрасываем счетчик собранных за день и записываем в настройки
                if (!botProcessingRepository.msgNotification) {
                    botProcessingRepository.dayConfirmedCount = 0

                    shopParametersDBRepository.updateDayConfirmedCount(
                        shop = werk,
                        dayConfirmedCount = botProcessingRepository.dayConfirmedCount
                    )

                    Logging.i(
                        tag,
                        "$werk dayConfirmedCount SAVE: ${botProcessingRepository.dayConfirmedCount}"
                    )
                }
            }
//            Logging.d(
//                tag, "$werk Shop open: ${
//                    botMessage.shopInWork(
//                        shopOpenTime = botProcessingRepository.shopOpenTime,
//                        shopCloseTime = botProcessingRepository.shopCloseTime
//                    )
//                }"
//            )

            val orderListSimple = serverTSRepository.getOrderListSimple()

            when (orderListSimple?.errorCode) {
                200 -> processing.processInworkOrders(
                    orderListSimple.listWebOrdersSimply,
                    botProcessingRepository,
                    shopParametersDBRepository
                )

                401 -> serverTSRepository.login(login, password, werk)
                else -> Logging.e(tag, "$werk ErrorCode: ${orderListSimple?.errorCode} Error: ${orderListSimple?.error}")
            }


            Logging.i(tag, "$werk Wait next iteration 30 second Код ответа сервера: ${orderListSimple?.errorCode.toString()}")
//            Logging.i(tag, "$werk Версия базы на сервере: ${netClient.remoteDbVersion}")

            delay(30000L)
        }

    }

}