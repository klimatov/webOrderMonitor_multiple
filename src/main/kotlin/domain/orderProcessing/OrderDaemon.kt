package domain.orderProcessing

import bot.models.ConfirmationData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import domain.models.ShopParameters
import domain.repository.BotProcessingRepository
import domain.repository.BotWorkersRepository
import domain.repository.ServerTSRepository
import domain.repository.ShopParametersDBRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import restTS.models.WebOrder
import utils.Logging
import java.time.LocalDateTime
import kotlin.reflect.full.declaredMemberProperties

class OrderDaemon(
    private val login: String,
    private val password: String,
    private val werk: String,
    private val gmt: String,
    private val serverTSRepository: ServerTSRepository,
    private val botWorkersRepository: BotWorkersRepository,
    private val deviceType: String,
    private val deviceVersion: String,
    private val confirmationDataFlow: SharedFlow<ConfirmationData>
) {
    private val tag = this::class.java.simpleName

    private val processing = Processing(serverTSRepository, gmt = gmt, shop = werk)

    private val botMessage = BotMessage()

    //var loginTime: LocalDateTime = LocalDateTime.now()

    suspend fun start(
        botProcessingRepository: BotProcessingRepository,
        shopParametersDBRepository: ShopParametersDBRepository,
    ) {
        // проходим по всем полям класса и выводим их со значениями в лог
        Logging.i(
            tag, "$werk Запускаем... ${
                BotProcessingRepository::class.declaredMemberProperties.joinToString {
                    "${it.name} = ${it.get(botProcessingRepository)}"
                }
            }"
        )

        var serializedActiveOrders: String? = null
        var currentInfoMsgId: Long? = null
        var dayRecievedCount: Int? = null
        var dayConfirmedCount: Int? = null
        var serializedDayConfirmedByEmployee: String? = null

//        val listOfOrdersInTheConfirmation: MutableList<ConfirmationData> = mutableListOf()

        // считываем данные из БД
        val shopParameters = shopParametersDBRepository.getShopParametersByShop(werk)

        // если запись в БД отсутствует - создаем
        if (shopParameters == null) shopParametersDBRepository.updateShopParameters(
            ShopParameters(
                shop = werk,
                serializedActiveOrders = "{}",
                currentInfoMsgId = 0,
                dayRecievedCount = 0,
                dayConfirmedCount = 0,
                serializedDayConfirmedByEmployee = "{}"
            )
        ) else {
            serializedActiveOrders = shopParameters.serializedActiveOrders
            currentInfoMsgId = shopParameters.currentInfoMsgId
            dayRecievedCount = shopParameters.dayRecievedCount
            dayConfirmedCount = shopParameters.dayConfirmedCount
            serializedDayConfirmedByEmployee = shopParameters.serializedDayConfirmedByEmployee
        }

        if (serializedActiveOrders != null) {
            val type = object : TypeToken<MutableMap<String?, WebOrder?>>() {}.type
            processing.activeOrders =
                Gson().fromJson(serializedActiveOrders, type)
            Logging.i(tag, "$werk activeOrders READ: $serializedActiveOrders")
        }

        if (currentInfoMsgId != null) {
            botProcessingRepository.currentInfoMsgId = currentInfoMsgId.toLong()
            //botProcessingRepository.newInfoMsgId = botProcessingRepository.currentInfoMsgId
            Logging.i(tag, "$werk currentInfoMsgId READ: $currentInfoMsgId")
        }

        if (dayRecievedCount != null) {
            botProcessingRepository.dayOrderRecievedCount = dayRecievedCount.toInt()
            Logging.i(tag, "$werk dayRecievedCount READ: $dayRecievedCount")
        }

        if (dayConfirmedCount != null) {
            botProcessingRepository.dayOrderConfirmedCount = dayConfirmedCount.toInt()
            Logging.i(tag, "$werk dayConfirmedCount READ: $dayConfirmedCount")
        }

        if (serializedDayConfirmedByEmployee != null) {
            val type = object : TypeToken<MutableMap<String?, Int?>>() {}.type
            botProcessingRepository.dayOrderConfirmedByEmployee =
                Gson().fromJson(serializedDayConfirmedByEmployee, type)
            Logging.i(tag, "$werk serializedDayConfirmedByEmployee READ: $serializedDayConfirmedByEmployee")
        }


        serverTSRepository.login(login, password, werk, gmt, deviceType, deviceVersion)



        CoroutineScope(Dispatchers.Default).launch {// запуск отслеживания подтверждений
            while (true) {
                confirmationDataFlow.collect{ confirmationData ->
                    if (confirmationData.shop == werk) {
                        println("SharedFlow from $werk: $confirmationData")
//                        listOfOrdersInTheConfirmation.add(it)
                        if (processing.activeOrders.containsKey(confirmationData.webNum)) {
                            val activeOrder = processing.activeOrders[confirmationData.webNum]
                            if (confirmationData.inConfirmationProcess) {
                                if (activeOrder?.sapFioList?.contains(confirmationData.sapFio) == false) activeOrder?.sapFioList?.add(confirmationData.sapFio)
                            } else {
                                activeOrder?.sapFioList?.removeAll(listOf(confirmationData.sapFio))
                            }
                            if (confirmationData.collector.hrCode != null) { // если поле заполнено, то заявка подтверждена
                                activeOrder?.sapFioList?.retainAll(listOf(confirmationData.sapFio))
                                activeOrder?.docStatus = "WRQST_ACPT"
                                activeOrder?.collector = confirmationData.collector
                            }
                            botProcessingRepository.botUpdateMessage(activeOrder)
                        }
                    }
                }
            }
        }.start()

        mainLoop@
        while (true) {  // основной цикл проверки
            Logging.i(tag, "$werk Обновляем данные...")

            // проверяем открыт ли магазин, триггерим звук уведомлений
            if (botMessage.shopInWork(
                    shopOpenTime = botProcessingRepository.shopOpenTime,
                    shopCloseTime = botProcessingRepository.shopCloseTime,
                    gmt = gmt
                ) != botProcessingRepository.msgNotification
            ) {
                botProcessingRepository.msgNotification = !botProcessingRepository.msgNotification
                botProcessingRepository.botSendInfoMessage()
                // если магазин закрылся, то сбрасываем счетчик собранных/подтвержденных за день и записываем в настройки
                if (!botProcessingRepository.msgNotification) {
                    botProcessingRepository.dayOrderRecievedCount = 0
                    botProcessingRepository.dayOrderConfirmedCount = 0
                    botProcessingRepository.dayOrderConfirmedByEmployee = mutableMapOf()

                    shopParametersDBRepository.updateDayRecievedCount(
                        shop = werk,
                        dayRecievedCount = botProcessingRepository.dayOrderRecievedCount,
                        dayConfirmedCount = botProcessingRepository.dayOrderConfirmedCount,
                        serializedDayConfirmedByEmployee = Gson().toJson(botProcessingRepository.dayOrderConfirmedByEmployee)
                    )

                    Logging.i(tag, "$werk dayOrderRecievedCount SAVE: ${botProcessingRepository.dayOrderRecievedCount} " +
                            "dayOrderConfirmedCount SAVE: ${botProcessingRepository.dayOrderConfirmedCount} " +
                            "dayOrderConfirmedByEmployee SAVE: ${botProcessingRepository.dayOrderConfirmedByEmployee} ")

                }
            }

            val orderListSimple = serverTSRepository.getOrderListSimple()

            when (orderListSimple?.errorCode) {
                200 -> processing.processInworkOrders(
                    orderListSimple.listWebOrdersSimply,
                    botProcessingRepository,
                    shopParametersDBRepository
                )

                401 -> {
                    val loginResult = serverTSRepository.login(login, password, werk, gmt, deviceType, deviceVersion)
                    Logging.d(tag, "$werk Login result: Sucess:${loginResult.result.success} " +
                                "ErrorCode:${loginResult.result.errorCode} " +
                                "ErrorMessage:${loginResult.result.errorMessage}"
                    )
                    if (loginResult.result.success) { // если логин прошел успешно
                        //сохраняем дату логина для BotCore
                        botWorkersRepository.shopWorkersList.replaceAll {
                            if (it.shop == werk) {
                                it.loginTime = LocalDateTime.now()
                                it
                            } else it
                        }
                    } else { // если логин прошел НЕуспешно
                        // FIXME: отправить сообщение владельцу чата

                        // проброс инфы на инфокнопку
                        botProcessingRepository.updateErrorInfoMsg(loginResult.result.errorCode?:0)

                        Logging.e(tag, "$werk Ошибка входа. Ждем 6 минут. " +
                                "Код ошибки:${loginResult.result.errorCode} " +
                                "Сообщение: ${loginResult.result.errorMessage}")
                        delay(6*1000*60)
                    }

                    // продолжаем цикл со следующей итерации
                    continue@mainLoop
                }

                else -> {
                    // проброс инфы на инфокнопку
                    botProcessingRepository.updateErrorInfoMsg(orderListSimple?.errorCode?:0)

                    Logging.e(
                        tag, "$werk Ошибка! Ждем 30 секунд. " +
                                "Код ошибки:${orderListSimple?.errorCode} " +
                                "Сообщение: ${orderListSimple?.error}"
                    )
                    delay(30 * 1000)
                    // продолжаем цикл со следующей итерации
                    continue@mainLoop
                }
            }

            //сохраняем данные для BotCore
            botWorkersRepository.remoteDbVersion = serverTSRepository.remoteDbVersion
            botWorkersRepository.lastErrorCode = serverTSRepository.lastErrorCode
            botWorkersRepository.lastErrorMessage = serverTSRepository.lastErrorMessage

            Logging.i(
                tag,
                "$werk Wait next iteration 30 second Код ответа сервера: ${orderListSimple.errorCode}"
            )
//            Logging.i(tag, "$werk Версия базы на сервере: ${netClient.remoteDbVersion}")

            delay(30000L)
        }

    }

}