package domain.orderProcessing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import domain.repository.BotProcessingRepository
import kotlinx.coroutines.*
import data.restTS.models.WebOrder
import domain.repository.ServerTSRepository
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

    //    val appStartTime: LocalDateTime = LocalDateTime.now()
    var loginTime: LocalDateTime = LocalDateTime.now()

//    val job = SupervisorJob()
//    val scope = CoroutineScope(Dispatchers.Default + job)

//    fun cancelCoroutine() {
//        scope.coroutineContext.cancelChildren()
//    }

    suspend fun start(botProcessingRepository: BotProcessingRepository) {

        Logging.i(tag, "Запускаем...")

        // считываем данные из SharedPerferences
//        val serializedActiveOrders = sharedPreferences.getString("ACTIVE_ORDERS", null)

        val serializedActiveOrders: String? = null
        if (serializedActiveOrders != null) {
            val type = object : TypeToken<MutableMap<String?, WebOrder?>>() {}.type
            processing.activeOrders =
                Gson().fromJson(serializedActiveOrders, type)
            Logging.i(tag, "sharedPreferences activeOrders READ: $serializedActiveOrders")
        }

//        val currentInfoMsgId = sharedPreferences.getString("CURRENT_INFO_MESSAGE_ID", null)
        val currentInfoMsgId: String? = null
        if (currentInfoMsgId != null) {
            botProcessingRepository.currentInfoMsgId = currentInfoMsgId.toLong()
            botProcessingRepository.newInfoMsgId = botProcessingRepository.currentInfoMsgId
            Logging.i(tag, "sharedPreferences currentInfoMsgId READ: $currentInfoMsgId")
        }

//        val dayConfirmedCount = sharedPreferences.getString("DAY_CONFIRMED_COUNT", null)
        val dayConfirmedCount: String? = null
        if (dayConfirmedCount != null) {
            botProcessingRepository.dayConfirmedCount = dayConfirmedCount.toInt()
            Logging.i(tag, "sharedPreferences dayConfirmedCount READ: $dayConfirmedCount")
        }


//        scope.launch {
        serverTSRepository.login(login, password, werk)
        while (true) {  // основной цикл проверки
            Logging.i(tag, "Обновляем данные...")

            // проверяем открыт ли магазин, триггерим звук уведомлений
            if (BotMessage().shopInWork() != botProcessingRepository.msgNotification) {
                botProcessingRepository.msgNotification = !botProcessingRepository.msgNotification
                botProcessingRepository.botSendInfoMessage()
                // если магазин закрылся, то сбрасываем счетчик собранных за день и записываем в настройки
                if (!botProcessingRepository.msgNotification) {
                    botProcessingRepository.dayConfirmedCount = 0
                    val serializedDayConfirmedCount = Gson().toJson(botProcessingRepository.dayConfirmedCount)
                    //TODO("Запись в БД")
//                        sharedPreferences.edit()
//                            .putString("DAY_CONFIRMED_COUNT", serializedDayConfirmedCount).apply()
                    Logging.i(
                        tag,
                        "sharedPreferences dayConfirmedCount SAVE: ${botProcessingRepository.dayConfirmedCount}"
                    )
                }
            }
            Logging.d(tag, "Shop open: ${BotMessage().shopInWork()}")

            val orderListSimple = serverTSRepository.getOrderListSimple()

            when (orderListSimple?.errorCode) {
                200 -> processing.processInworkOrders(orderListSimple.listWebOrdersSimply, botProcessingRepository)
                401 -> serverTSRepository.login(login, password, werk)
                else -> Logging.e(tag, "ErrorCode: ${orderListSimple?.errorCode} Error: ${orderListSimple?.error}")
            }


            Logging.i(tag, "Код ответа сервера: ${orderListSimple?.errorCode.toString()}")
            Logging.i(tag, "Ждем следующего обновления...")
//            Logging.i(tag, "Версия базы на сервере: ${netClient.remoteDbVersion}")


            Logging.i(tag, "Wait next iteration 30 second")
            delay(30000L)
        }
//        }.join()
    }

}