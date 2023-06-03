package bot.operations

import bot.models.*
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import restTS.models.Collector
import restTS.models.SaveIncomplet
import restTS.models.SaveItems
import utils.Logging
import kotlin.math.abs

class OrderConfirmation(
    val bot: TelegramBot,
    private val botTSOperations: BotTSOperations,
    private val stateUser: MutableMap<Identifier, BotState>,
    private val allBotUsers: MutableMap<Identifier, BotUser>
) {
    private val tag = this::class.java.simpleName

    private fun templateBaseMessageText(webNum: String, orderId: String, ordType: String) =
        "Заявка  №$webNum/$orderId\n" +
                "$ordType\n"

    private fun templatePrinterText(printerName: String?) =
        if (printerName == null) "\n❕Лист подбора не печатаем" else "\uD83D\uDDA8\uFE0FПечатаем на $printerName"

    private fun templateItemMessageText(
        goodCode: String,
        name: String,
        reasonName: String,
        comment: String?,
        shelf: String?
    ) =
        "\n" +
                "$goodCode $name\n" +
                "✅$reasonName\n" +
                if (comment != null) {
                    "\uD83D\uDCDD$comment\n"
                } else {
                    ""
                } +
                if (shelf == null) "⭕Полка не выбрана\n" else "✅$shelf\n"


    private fun templateLoadData(webNum: String) =
        "Запрашиваем данные для подтверждения веб-заявки №$webNum с сервера TS"

    private fun templateAlredyConfirmed(webNum: String) = "Веб-заявка №$webNum уже подтверждена"
    private fun templateConfirmedStorn(webNum: String) = "Веб-заявка №$webNum была отменена"
    private fun templateReasonRequest(goodCode: String, name: String) = "Выберите тип подтверждения для $goodCode $name"
    private fun templateCommentRequest(goodCode: String, name: String, reason: String) =
        "Так как для $goodCode $name Вы выбрали \'$reason\', нужно ввести обязательный комментарий:"

    private fun templateShelfRequest(goodCode: String, name: String) = "Выберите полку для $goodCode $name"
    private fun templatePrinterRequest(webNum: String) =
        "Выберите принтер для печати листа подтверждения веб-заявки №$webNum"

    private fun templateIsTheDataCorrect(webNum: String) = "Для веб-заявки №$webNum вы выбрали:"
    private fun templateCancelOrder(webNum: String) = "Подтверждение веб-заявки №$webNum отменено"
    private fun templateConfirmedOk(webNum: String) = "Веб-заявка №$webNum успешно подтверждена"
    private fun templateConfirmedFalse(webNum: String) =
        "Подтверить веб-заявку №$webNum не удалось по техническим причинам"


    suspend fun confirmWebOrder(
        chatId: IdChatIdentifier,
        orderId: String,
        webNum: String,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        Logging.d(tag, "confirm order #$orderId")

        defaultBehaviourContextWithFSM.startChain(ConfirmationStartState(chatId, webNum, orderId))


//        val printersList = botTSOperations.getPrintersList(chatId.chatId)


//        if ((webOrder.result.success) && (reasonsList.isNotEmpty()) && (shelfsList.shelfsList.isNotEmpty())) {
////            Logging.d(tag, webOrder.toString() + "\n\n" + reasonsList.toString() + "\n\n" + shelfsList.toString() + "\n\n" + printersList.toString())
//
//            defaultBehaviourContextWithFSM.startChain(ConfirmationStart(chatId, webNum, orderId))
//        } else bot.sendMessage(
//            chatId,
//            "Ошибка получения данных из TS",
//            disableWebPagePreview = true
//        )

        with(defaultBehaviourContextWithFSM) {

            /**
             *  ConfirmationStartState
             */
            strictlyOn<ConfirmationStartState> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationStartState")

                var messageId: MessageId? = null
                try {
                    messageId = send(
                        it.context,
                        templateLoadData(it.webNum),
                        replyMarkup = null
                    ).messageId
                } catch (e: Exception) {
                    Logging.e(tag, "Exception: ${e.stackTraceToString()}}")
                }

                val orderSaveParam = OrderSaveParam(
                    orderId = it.orderId,
                    webNum = it.webNum,
                    messageId = messageId,
                    collector = Collector(
                        username = allBotUsers[it.context.chatId]?.tsLogin,
                        hrCode = allBotUsers[it.context.chatId]?.sapId
                    )
                )

                if (messageId == null) {
                    orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                    ConfirmationStopState(it.context, orderSaveParam)
                } else {
                    orderSaveParam.saveStatus = OrderDataSaveStatus.PROCESS
                    val webOrder = botTSOperations.getWebOrderDetail(chatId.chatId, it.orderId)

                    if (webOrder.result.success) {
                        orderSaveParam.webNum = webOrder.webOrder.webNum
                        orderSaveParam.orderType = webOrder.webOrder.orderType
                        orderSaveParam.company = webOrder.webOrder.company
                        orderSaveParam.ordType = webOrder.webOrder.ordType
                        webOrder.webOrder.items.forEach { item ->
                            orderSaveParam.items.add(
                                ItemsSaveParam(
                                    goodCode = item.goodCode,
                                    name = item.name,
                                    itemNo = "${item.itemNo}",
                                    incomplet = SaveIncompletParam(
                                        reasonCode = "OK",
                                        reasonName = "Товар найден",
                                        comment = null
                                    ),
                                    shelf = null,
                                    quantity = "${item.quantity}"
                                )
                            )
                        }
                        ConfirmationMainState(it.context, orderSaveParam)
                    } else {
                        orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, orderSaveParam)
                    }

                }
            }

            /**
             * ConfirmationMainState
             */
            strictlyOn<ConfirmationMainState> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationMainState")
                Logging.d(tag, "MainState и параметры на старте такие: ${it.orderSaveParam.toString()}")

                val mainStateButtons = inlineKeyboard {
                    it.orderSaveParam.items.forEach { item ->
                        row {
                            dataButton(
                                (if (item.shelf == null) "\uD83D\uDD34" else "\uD83D\uDFE2") + item.goodCode + " " + item.name,
                                "item=${item.itemNo}"
                            )
                        }
                    }
                    row {
                        dataButton(
                            (if (it.orderSaveParam.printerName == null) "Выбрать принтер" else "Изменить принтер"),
                            "printer=${it.orderSaveParam.webNum}"
                        )
                    }
                    row {
                        dataButton("Отмена", "cancel=${it.orderSaveParam.webNum}")
                        dataButton("Подтвердить", "confirm=${it.orderSaveParam.webNum}")
                    }
                }

                changeConfirmationMainMessage(it.context, it.orderSaveParam, mainStateButtons)

                val mainStateChoiceCode = waitDataCallbackQuery()
                    .filter { message ->
                        message.message?.sameChat(it.context) ?: false
                    }
                    .first()
                    .data
                    .split("=")

                it.orderSaveParam.infoMessage = null

                when (mainStateChoiceCode.first()) {
                    "cancel" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.CANCEL
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }

                    "item" -> {
                        it.orderSaveParam.activeItem = mainStateChoiceCode.last()
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FINISH
                        ConfirmationItemState(it.context, it.orderSaveParam)
                    }

                    "printer" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FINISH
                        ConfirmationChoosingPrinter(it.context, it.orderSaveParam)
                    }

                    "confirm" -> {
                        if (it.orderSaveParam.items.all { item -> item.shelf != null }) {

                            val orderDetail =
                                botTSOperations.getWebOrderDetail(it.context.chatId, it.orderSaveParam.orderId ?: "")
                            if (orderDetail.result.success) {
//!!!!!!!!!!
                                when (orderDetail.webOrder.docStatus) {
                                    "DOC_STORN" -> {
                                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.STORN
                                        Logging.e(
                                            tag,
                                            "Заявка №${it.orderSaveParam.webNum} отменена"
                                        )
                                    }

                                    "WRQST_CRTD" -> {
                                        val saveItemsList: MutableList<SaveItems> = mutableListOf()

                                        it.orderSaveParam.items.forEach { item ->
                                            saveItemsList.add(
                                                SaveItems(
                                                    quantity = item.quantity ?: "",
                                                    itemNo = item.itemNo ?: "",
                                                    goodCode = item.goodCode,
                                                    incomplet = SaveIncomplet(
                                                        reasonCode = item.incomplet?.reasonCode,
                                                        comment = item.incomplet?.comment ?: item.incomplet?.reasonName
                                                    ),
                                                    shelf = item.shelf?.shelfId.toString()
                                                )
                                            )
                                        }

                                        val saveResult = botTSOperations.saveWebOrder(
                                            userId = it.context.chatId,
                                            orderType = it.orderSaveParam.orderType ?: "",
                                            orderId = it.orderSaveParam.orderId ?: "",
                                            company = it.orderSaveParam.company ?: "",
                                            items = saveItemsList,
                                            collector = it.orderSaveParam.collector ?: Collector(),
                                            ordType = it.orderSaveParam.ordType ?: ""
                                        )
                                        if (saveResult.result.success) {
                                            if (it.orderSaveParam.printerName != null) botTSOperations.printWebOrder(
                                                userId = it.context.chatId,
                                                pcName = it.orderSaveParam.printerName,
                                                printOrders = saveResult.saveWebOrder.printOrders
                                            )
                                            it.orderSaveParam.saveStatus = OrderDataSaveStatus.FINISH
                                        } else {
                                            Logging.e(
                                                tag,
                                                "Ошибка подтверждения заявки №${it.orderSaveParam.webNum} ${saveResult.result.errorMessage}"
                                            )
                                            it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                                        }
                                        Logging.d(tag, saveResult.saveWebOrder.message.toString())
                                    }

                                    else -> {
                                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.EXIST
                                        Logging.e(
                                            tag,
                                            "Заявка №${it.orderSaveParam.webNum} уже подтверждена ${orderDetail.webOrder.collector?.username}"
                                        )
                                    }
                                }

//!!!!!!!!!!
                            } else {
                                Logging.e(
                                    tag,
                                    "Ошибка подтверждения заявки №${it.orderSaveParam.webNum} ${orderDetail.result.errorMessage}"
                                )
                                it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                            }

                            ConfirmationStopState(it.context, it.orderSaveParam)


                        } else {
                            it.orderSaveParam.infoMessage = "❗ Не все параметры выбраны ❗"
                            it
                        }
                    }

                    else -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }
                }
            }

            /**
             * ConfirmationItemState
             */
            strictlyOn<ConfirmationItemState> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationItemState")
                Logging.d(tag, "ConfirmationItemState и параметры на старте такие: ${it.orderSaveParam.toString()}")

                val currentItem = it.orderSaveParam.items.find { item -> item.itemNo == it.orderSaveParam.activeItem }

                val itemStateButtons = inlineKeyboard {
                    row {
                        dataButton(
                            "${currentItem?.incomplet?.reasonName} (изменить)",
                            "chooseReason=${it.orderSaveParam.webNum}"
                        )
                    }
                    row {
                        dataButton(
                            (if (currentItem?.shelf == null) "Выбрать полку" else ("${currentItem.shelf?.description ?: "???"} (изменить)")),
                            "chooseShelf=${it.orderSaveParam.webNum}"
                        )
                    }
                    row {
                        dataButton(
                            "${
                                if (currentItem?.incomplet?.comment == null) "Внести" else "Изменить"
                            } комментарий", "enterComment=${it.orderSaveParam.webNum}"
                        )
                    }
                    row {
                        dataButton("Отмена", "cancel=${it.orderSaveParam.webNum}")
                        dataButton("Назад", "back=${it.orderSaveParam.webNum}")
                    }
                }

//                it.orderSaveParam.infoMessage = "\uD83D\uDEE0Подбираем ${currentItem?.goodCode} ${currentItem?.name}:"

                changeConfirmationItemMessage(it.context, it.orderSaveParam, itemStateButtons)

                val itemStateChoiceCode = waitDataCallbackQuery()
                    .filter { message ->
                        message.message?.sameChat(it.context) ?: false
                    }
                    .first()
                    .data
                    .split("=")

//                it.orderSaveParam.infoMessage = null

                when (itemStateChoiceCode.first()) {
                    "cancel" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.CANCEL
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }

                    "back" -> {
                        ConfirmationMainState(it.context, it.orderSaveParam)
                    }

                    "chooseShelf" -> {
                        ConfirmationChoosingShelf(it.context, it.orderSaveParam)
                    }

                    "chooseReason" -> {
                        ConfirmationChoosingReasonState(it.context, it.orderSaveParam)
                    }

                    "enterComment" -> {
                        ConfirmationEnterReasonCommentState(it.context, it.orderSaveParam)
                    }

                    else -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }
                }
            }

            /**
             * ConfirmationChoosingReasonState
             */
            strictlyOn<ConfirmationChoosingReasonState> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationChoosingReasonState")
                Logging.d(
                    tag,
                    "ConfirmationChoosingReasonState и параметры на старте такие: ${it.orderSaveParam.toString()}"
                )

                val currentItem = it.orderSaveParam.items.find { item -> item.itemNo == it.orderSaveParam.activeItem }
// TODO: вставить в инфо строку ожидания данных
                val doReasonsList =
                    botTSOperations.getReasonForIncompliteness(
                        chatId.chatId,
                        orderId,
                        currentItem?.goodCode.toString()
                    ).reasonsList

//                val doReasonsList =
//                    reasonsList["590018193"] ?: emptyList()

                val reasonStateButtons = inlineKeyboard {

                    row {
                        doReasonsList.forEach {
                            row {
                                dataButton(it.name.toString(), "reasonCode=${it.reasonCode.toString()}")
                            }
                        }
                    }
                    row {
                        dataButton("Отмена", "cancel=${it.orderSaveParam.webNum}")
                        dataButton("Назад", "back=${it.orderSaveParam.webNum}")
                    }
                }

//                it.orderSaveParam.infoMessage = "\uD83D\uDEE0Подбираем ${currentItem?.goodCode} ${currentItem?.name}:"

                changeConfirmationItemMessage(it.context, it.orderSaveParam, reasonStateButtons)

                val reasonStateChoiceCode = waitDataCallbackQuery()
                    .filter { message ->
                        message.message?.sameChat(it.context) ?: false
                    }
                    .first()
                    .data
                    .split("=")

//                it.orderSaveParam.infoMessage = null

                when (reasonStateChoiceCode.first()) {
                    "cancel" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.CANCEL
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }

                    "back" -> {
                        ConfirmationItemState(it.context, it.orderSaveParam)
                    }

                    "reasonCode" -> {
                        val reason =
                            doReasonsList.find { selectedReason -> selectedReason.reasonCode == reasonStateChoiceCode.last() }
                        currentItem?.incomplet = SaveIncompletParam(
                            reasonCode = reason?.reasonCode,
                            reasonName = reason?.name,
                            needComm = reason?.needComm
                        )
                        if (reason?.needComm == "Y") {
                            ConfirmationEnterReasonCommentState(it.context, it.orderSaveParam)
                        } else {
                            ConfirmationItemState(it.context, it.orderSaveParam)
                        }
                    }

                    else -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }
                }
            }

            /**
             * ConfirmationChoosingShelf
             */
            strictlyOn<ConfirmationChoosingShelf> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationChoosingShelf")
                Logging.d(
                    tag,
                    "ConfirmationChoosingShelf и параметры на старте такие: ${it.orderSaveParam.toString()}"
                )

                val currentItem = it.orderSaveParam.items.find { item -> item.itemNo == it.orderSaveParam.activeItem }

                val shelfsList = botTSOperations.getShelfs(chatId.chatId)

                val sortedShelfs = shelfsList.shelfsList.sortedBy { shelfItem -> shelfItem.shelfId }
                val size = sortedShelfs.size - 3
                val x = 5
                val y = abs(size / x)

                var count = 1

                // TODO: отфильтровать секции больше 3

                val shelfStateButtons = inlineKeyboard {

                    for (yy in 1..(y + 1)) {
                        row {
                            for (xx in 1..x) {
                                if (count >= size) break
                                val shelf = sortedShelfs.get(count)
                                dataButton(
                                    "${shelf.sectionNumber}/${shelf.rackNumber}/${shelf.shelfNumber}",
                                    "shelfId=${shelf.shelfId.toString()}"
                                )
                                count++
                            }
                        }
                    }
                    for (xx in count..(sortedShelfs.size - 1)) {
                        row {
                            val shelf = sortedShelfs.get(xx)
                            dataButton(
                                shelf.description.toString(),
                                "shelfId=${shelf.shelfId.toString()}"
                            )
                        }
                    }

                    row {
                        dataButton("Отмена", "cancel=${it.orderSaveParam.webNum}")
                        dataButton("Назад", "back=${it.orderSaveParam.webNum}")
                    }
                }

//                it.orderSaveParam.infoMessage = "\uD83D\uDEE0Подбираем ${currentItem?.goodCode} ${currentItem?.name}:"

                changeConfirmationItemMessage(it.context, it.orderSaveParam, shelfStateButtons)

                val shelfChoiceId = waitDataCallbackQuery()
                    .filter { message ->
                        message.message?.sameChat(it.context) ?: false
                    }
                    .first()
                    .data
                    .split("=")

//                it.orderSaveParam.infoMessage = null

                when (shelfChoiceId.first()) {
                    "cancel" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.CANCEL
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }

                    "back" -> {
                        ConfirmationItemState(it.context, it.orderSaveParam)
                    }

                    "shelfId" -> {
                        val shelfChoice =
                            shelfsList.shelfsList.find { shelf -> shelf.shelfId.toString() == shelfChoiceId.last() }
                        currentItem?.shelf = shelfChoice
                        ConfirmationItemState(it.context, it.orderSaveParam)

                    }

                    else -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }
                }
            }

            /**
             * ConfirmationChoosingPrinter
             */
            strictlyOn<ConfirmationChoosingPrinter> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationChoosingPrinter")
                Logging.d(
                    tag,
                    "ConfirmationChoosingPrinter и параметры на старте такие: ${it.orderSaveParam.toString()}"
                )

                val printersList = botTSOperations.getPrintersList(chatId.chatId)

                val printerStateButtons = inlineKeyboard {

                    row {
                        printersList.printersList.forEach {
                            row {
                                dataButton(it.pcName.toString(), "pcName=${it.pcName.toString()}")
                            }
                        }
                    }
                    row {
                        dataButton("Не печатать", "notPrint=${it.orderSaveParam.webNum}")
                    }
                    row {
                        dataButton("Отмена", "cancel=${it.orderSaveParam.webNum}")
                        dataButton("Назад", "back=${it.orderSaveParam.webNum}")
                    }
                }

                it.orderSaveParam.infoMessage = "\uD83D\uDEE0Выбираем принтер для печати листа подбора"

                changeConfirmationMainMessage(it.context, it.orderSaveParam, printerStateButtons)

                val printerStateChoiceName = waitDataCallbackQuery()
                    .filter { message ->
                        message.message?.sameChat(it.context) ?: false
                    }
                    .first()
                    .data
                    .split("=")

                it.orderSaveParam.infoMessage = null

                when (printerStateChoiceName.first()) {
                    "cancel" -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.CANCEL
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }

                    "back" -> {
                        ConfirmationMainState(it.context, it.orderSaveParam)
                    }

                    "pcName" -> {
                        val pcName = printerStateChoiceName.last()
                        it.orderSaveParam.printerName = pcName

                        ConfirmationMainState(it.context, it.orderSaveParam)
                    }

                    "notPrint" -> {
                        it.orderSaveParam.printerName = null
                        ConfirmationMainState(it.context, it.orderSaveParam)
                    }

                    else -> {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                    }
                }
            }

            /**
             * ConfirmationEnterReasonCommentState
             */
            strictlyOn<ConfirmationEnterReasonCommentState> {
                stateUser[it.context.chatId] = it
                Logging.i(tag, "ConfirmationEnterReasonCommentState")
                Logging.d(
                    tag,
                    "ConfirmationEnterReasonCommentState и параметры на старте такие: ${it.orderSaveParam.toString()}"
                )

                val currentItem = it.orderSaveParam.items.find { item -> item.itemNo == it.orderSaveParam.activeItem }

                it.orderSaveParam.infoMessage =
                    "⬇\uFE0FВведите комментарий⬇\uFE0F"

                changeConfirmationItemMessage(it.context, it.orderSaveParam, null)

                val reasonComment = waitTextMessage().filter { message ->
                    message.sameChat(it.context)
                }.first()
                delete(reasonComment.chat.id, reasonComment.messageId)

                it.orderSaveParam.infoMessage = null
                currentItem?.incomplet?.comment = reasonComment.text

                ConfirmationItemState(it.context, it.orderSaveParam)
            }

            /**
             * ConfirmationStopState
             */
            strictlyOn<ConfirmationStopState>
            {
                stateUser.remove(it.context.chatId)

                Logging.i(tag, "ConfirmationStopState")
                Logging.d(tag, "ConfirmationStopState и параметры на старте такие: ${it.orderSaveParam.toString()}")

                val textMessage = when (it.orderSaveParam.saveStatus) {
                    OrderDataSaveStatus.FALSE -> templateConfirmedFalse(it.orderSaveParam.webNum ?: "")
                    OrderDataSaveStatus.EXIST -> templateAlredyConfirmed(it.orderSaveParam.webNum ?: "")
                    OrderDataSaveStatus.CANCEL -> templateCancelOrder(it.orderSaveParam.webNum ?: "")
                    OrderDataSaveStatus.FINISH -> templateConfirmedOk(it.orderSaveParam.webNum ?: "")
                    OrderDataSaveStatus.STORN -> templateConfirmedStorn(it.orderSaveParam.webNum ?: "")
                    else -> ""
                }
                doMessage(
                    chatId = it.context,
                    textMessage = textMessage,
                    messageId = it.orderSaveParam.messageId
                )
                null
            }
        }
    }

    private suspend fun changeConfirmationMainMessage(
        chatId: ChatIdentifier,
        orderSaveParam: OrderSaveParam,
        mainStateButtons: InlineKeyboardMarkup? = null
    ) {
        var messageText = templateBaseMessageText(
            orderSaveParam.webNum ?: "",
            orderSaveParam.orderId ?: "",
            orderSaveParam.ordType ?: ""
        )
        orderSaveParam.items.forEach { item ->
            messageText += templateItemMessageText(
                item.goodCode ?: "",
                item.name ?: "",
                item.incomplet?.reasonName ?: "",
                item.incomplet?.comment,
                item.shelf?.description
            )
        }
        messageText += templatePrinterText(orderSaveParam.printerName)

        if (orderSaveParam.infoMessage != null) messageText += "\n\n${orderSaveParam.infoMessage}"

        doMessage(
            chatId,
            messageText,
            orderSaveParam.messageId,
            mainStateButtons
        )

    }

    private suspend fun changeConfirmationItemMessage(
        chatId: ChatIdentifier,
        orderSaveParam: OrderSaveParam,
        mainStateButtons: InlineKeyboardMarkup? = null
    ) {
        val currentItem = orderSaveParam.items.find { item -> item.itemNo == orderSaveParam.activeItem }
        var messageText: String = templateItemMessageText(
            currentItem?.goodCode ?: "",
            currentItem?.name ?: "",
            currentItem?.incomplet?.reasonName ?: "",
            currentItem?.incomplet?.comment,
            currentItem?.shelf?.description
        )

        if (orderSaveParam.infoMessage != null) messageText += "\n\n${orderSaveParam.infoMessage}"

        doMessage(
            chatId,
            messageText,
            orderSaveParam.messageId,
            mainStateButtons
        )

    }

    private suspend fun doMessage(
        chatId: ChatIdentifier,
        textMessage: String? = null,
        messageId: MessageId? = null,
        replyMarkup: InlineKeyboardMarkup? = null
    ): Boolean {
        try {
            if (messageId != null) {
                bot.edit(
                    chatId = chatId,
                    messageId = messageId,
                    text = textMessage ?: "",
                    replyMarkup = replyMarkup,
                    disableWebPagePreview = true
                )
            } else {
                bot.send(
                    chatId = chatId,
                    text = textMessage ?: "",
                    replyMarkup = replyMarkup,
                    disableWebPagePreview = true
                )
            }
            return true
        } catch (e: Exception) {
            Logging.e(tag, "Exception: ${e.stackTraceToString()}}")
            return false
        }
    }
}