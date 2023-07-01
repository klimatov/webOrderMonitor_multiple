package domain.orderProcessing

import dev.inmo.tgbotapi.extensions.utils.formatting.makeInternalTgDeepLink
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.Username
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.message.textsources.italic
import dev.inmo.tgbotapi.utils.*
import restTS.models.WebOrder
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class BotMessage : DateTimeProcess() {

    fun orderMessage(
        webOrder: WebOrder?,
        botName: Username,
        fullCustomerInfo: Boolean = false
    ): TextSourcesList {
        val resultMessage = buildEntities {
            regular("#️⃣${webOrder?.webNum}/${webOrder?.orderId} ")
            linkln("[i]", makeInternalTgDeepLink(botName, Base64.getUrlEncoder().encodeToString("t=info&web=${webOrder?.webNum}&order=${webOrder?.orderId}".toByteArray())))
            regular("${webOrder?.ordType} ")
            if (webOrder?.isLegalEntity == "Y") bold("СЧЁТ КОНТРАГЕНТА")
            underlineln("\n\uD83D\uDCC6${replaceDateTime(webOrder?.docDate ?: "")}")
            regularln("${if (webOrder?.paid == "Y") "\uD83D\uDCB0Онлайн оплата" else "\uD83E\uDDFEНе оплачен"} \uD83D\uDCB5${webOrder?.docSum} руб.")
            if (fullCustomerInfo) {
                regular("\uD83D\uDC68${webOrder?.fioCustomer?.trim()} ")
                phone("+${webOrder?.phone}")
            } else {
                regular("\uD83D\uDC68${webOrder?.fioCustomer?.trim()?.substringBefore(" ")} ")
                phone("+${webOrder?.phone?.replaceRange(4..6,"***")}")
            }
            webOrder?.items?.forEach {
                linkln("\n\n\uD83D\uDD35${it.goodCode} ${it.name}", "https://${it.eshopUrl}\n")
                regular("\uD83D\uDCB0Цена: ${it.amount} ")
                if ((it.quantity ?: 0) > 1) regular("\uD83E\uDDF3Кол-во: ${it.quantity}")
                if (it.remains.isNotEmpty()) {
                    bold("\n\uD83D\uDED2Остатки:")
                    it.remains.forEach { remains ->
                        bold(" ${remains.storageKind.toString()} - ${remains.quantity.toString()} шт.")
                    }
                }
            }
        }
        return resultMessage
    }

    fun inworkMessage(webOrder: WebOrder?, gmt: String, botName: Username): TextSourcesList {
        val resultMessage = buildEntities {
//            codeln("⭕\uD83D\uDEE0Собираем ${minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))}!")
            if (webOrder?.sapFioList?.isEmpty() == true) {
                codeln("⭕Собираем ${minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))}!")
            } else {
                codeln("⭕\uD83D\uDEE0${getFirstCollectorNameFromWebOrder(webOrder)} собирает ${minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))}!")
            }
        }
            .plus(orderMessage(webOrder, botName))
        return resultMessage
    }

    fun completeMessage(webOrder: WebOrder?, gmt: String, botName: Username): TextSourcesList {
        val assemblyTime = minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))
        val docStatus = webOrder?.docStatus
        val resultMessage = buildEntities {
            boldln(
                when{
                    (docStatus == "DOC_STORN") -> "✅Отменена клиентом через $assemblyTime!"
                    ((webOrder?.collector?.hrCode != null) && (webOrder?.collector?.hrCode != "0")) -> "✅Подтвердил(-а) ${getFirstCollectorNameFromWebOrder(webOrder)} за $assemblyTime!" // если не пусто, то собрана в боте
                    ((docStatus == "WRQST_ACPT")||(docStatus == "PWRQT_DLVD")) -> "✅Подтверждена за $assemblyTime!"
                    ((docStatus == "WRQST_WAIT")||(docStatus == "PWRQT_SHRT")) -> "✅Заказ изменен за $assemblyTime, требуется подтверждение клиента!"
                    ((docStatus == "WRQST_BNLY")||(docStatus == "WRQST_BNLN")) -> "✅Выставлен безнал. Собрана за $assemblyTime!"
                    (docStatus == "PWRQT_PMNT") -> "✅Ожидается предоплата. Собрана за $assemblyTime!"
                    else -> "✅Отменена через $assemblyTime!"
                }
            )
        }
            .plus(italic(orderMessage(webOrder, botName)))
        return resultMessage
    }

    private fun getFirstCollectorNameFromWebOrder(webOrder: WebOrder?): String {
        return webOrder?.sapFioList?.firstOrNull()?.split(" ")?.take(2)?.joinToString(" ")?:""
    }

    private fun getFirstCollectorName(sapFio: String?): String {
        return sapFio?.split(" ")?.take(2)?.joinToString(" ")?:""
    }

    fun confirmButton(webOrder: WebOrder?, botName: Username): InlineKeyboardMarkup {
        return inlineKeyboard {
                row {
                    urlButton(if (webOrder?.sapFioList?.isEmpty() == true) {
                        "Подтвердить"
                    } else {
                           "Собирает ${getFirstCollectorNameFromWebOrder(webOrder)}"
                           },
                        makeInternalTgDeepLink(botName, Base64.getUrlEncoder().encodeToString("t=c&w=${webOrder?.webNum}&o=${webOrder?.orderId}&m=${webOrder?.messageId}".toByteArray()))
                    )
                }
            }
    }

    fun timeDiff(docDate: String?, gmt: String): Long {
        val lateDate: LocalDateTime = LocalDateTime.now(ZoneId.of(gmt))
        if (docDate == null) return 0
        val docDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        val docDateFormatting = LocalDateTime.parse(docDate, docDateFormat)
        return docDateFormatting.until(lateDate, ChronoUnit.MINUTES)
    }

    private fun timeNow(gmt: String): String {
        val time: LocalTime = LocalTime.now(ZoneId.of(gmt))
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        return formatter.format(time)
    }

    fun shopInWork(shopOpenTime: Int, shopCloseTime: Int, gmt: String): Boolean {
        val time: LocalTime = LocalTime.now(ZoneId.of(gmt))
        return time.hour in shopOpenTime until shopCloseTime
    }

    private fun minutesEnding(minute: Long): String {
        return "$minute " + minute.let {
            if (it % 100 in 11..14) {
                "минут"
            } else {
                when ((it % 10).toInt()) {
                    1 -> "минуту"
                    2, 3, 4 -> "минуты"
                    else -> "минут"//0, 5, 6, 7, 8, 9
                }
            }
        }
    }

    private fun orderEnding(order: Int): String {
        return "$order " + order.let {
            if (it % 100 in 11..14) {
                "заявок"
            } else {
                when ((it % 10)) {
                    1 -> "заявка"
                    2, 3, 4 -> "заявки"
                    else -> "заявок"//0, 5, 6, 7, 8, 9
                }
            }
        }
    }

    private fun orderRecievedStarting(order: Int): String {
        return order.let {
            if (it % 100 in 11..14) {
                "упало"
            } else {
                when ((it % 10)) {
                    1 -> "упала"
                    else -> "упало"
                }
            }
        }
    }

    private fun orderConfirmedStarting(order: Int): String {
        return order.let {
            if (it % 100 in 11..14) {
                "Подтверждено"
            } else {
                when ((it % 10)) {
                    1 -> "Подтверждена"
                    else -> "Подтверждено"
                }
            }
        }
    }

    fun infoMessage(notConfirmedOrders: Int, gmt: String): String {
        val resTxt: String
        if (notConfirmedOrders == 0) {
            resTxt = "✅ Все заявки подтверждены ${timeNow(gmt = gmt)}"
        } else {
            resTxt =
                "⭕\uD83D\uDEE0 В подборе ${orderEnding(notConfirmedOrders)} ${timeNow(gmt = gmt)}"
        }
        return resTxt
    }

    fun infoErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            403, 401 -> "\uD83C\uDD98Изменился пароль TS администратора!\uD83C\uDD98"
            in 500..510 -> "\uD83D\uDD34Ошибка на сервере. Код: $errorCode\uD83D\uDD34"
            else -> "\uD83D\uDD34Неизвестная ошибка. Код: $errorCode\uD83D\uDD34"
        }
    }

    fun notificationMessage(
        notification: Boolean,
        dayOrderRecievedCount: Int,
        dayOrderConfirmedCount: Int,
        dayOrderConfirmedByEmployee: MutableMap<String, Int>
    ): String {
        if (notification) {
            return "\uD83D\uDD08 Магазин открыт, включаем звуковые уведомления!"
        } else {
            return "\uD83D\uDD07 Магазин закрыт, отключаем звуковые уведомления!\n" +
                    "\uD83D\uDE2B Сегодня за день ${orderRecievedStarting(dayOrderRecievedCount)}: ${orderEnding(dayOrderRecievedCount)}, \n" +
                    "✅${orderConfirmedStarting(dayOrderConfirmedCount)}: ${orderEnding(dayOrderConfirmedCount)}, \n" +
                    "\uD83E\uDD16Из них через бота: ${
                        if (dayOrderConfirmedByEmployee.isEmpty()) {
                            orderEnding(0)
                        } else {
                            dayOrderConfirmedByEmployee.entries.sortedByDescending{ (_, value) -> value }.joinToString { (sapFio, count) ->
                                "\n${getFirstCollectorName(sapFio)}: ${orderEnding(count)}"
                            }
                        }
                    }"
        }
    }

    fun popupMessage(
        dayOrderRecievedCount: Int,
        dayOrderConfirmedCount: Int,
        dayOrderConfirmedByEmployee: MutableMap<String, Int>
    ): String {
        return "Сегодня ${orderRecievedStarting(dayOrderRecievedCount)}: ${orderEnding(dayOrderRecievedCount)}, \n" +
                "${orderConfirmedStarting(dayOrderConfirmedCount)}: ${orderEnding(dayOrderConfirmedCount)}, \n" +
                "Из них через бота: ${
                    if (dayOrderConfirmedByEmployee.isEmpty()) {
                        orderEnding(0)
                    } else {
                        dayOrderConfirmedByEmployee.entries.sortedByDescending{ (_, value) -> value }.joinToString { (sapFio, count) -> 
                            "\n${getFirstCollectorName(sapFio)}: ${orderEnding(count)}"
                        }
                    }
                }"
    }

    fun descriptionMessage(): String {
        return "Бот облегчающий работу с веб-заявками.\n" +
                "Что умеет этот бот:\n" +
                "•\tМониторит появление новых веб-заявок и выводит в чат информацию о поступившей веб-заявке\n" +
                "•\tМониторит подтверждение веб-заявок и выводит информацию о том, что заявка подтверждена\n" +
                "•\tОтдельное информационное окно (инфо-кнопка) о количестве требующих подбора и не подтвержденных веб-заявок\n" +
                "•\tВыводит время подбора каждой заявки\n" +
                "•\tВ сообщении с заявкой выводится время подбора, номер веб-заявки, номер заказа покупателя, тип заявки, время заказа, информация об оплате, стоимость всего заказа , телефон и данные покупателя, номенклатурный номер и наименование товара с активной ссылкой на сайт для просмотра информации о товаре, стоимость товара, остатки товара на складах\n" +
                "•\tВ часы, когда магазин закрыт автоматически отключаются звуковые уведомления (только текст)\n" +
                "•\tВыводит количество поступивших за день веб-заявок (счетчик обнуляется ежедневно в момент закрытия магазина) при нажатии на инфо-кнопку, а также в чате в конце дня.\n" +
                "•\tВывод информации о веб-заявке по ее номеру (просто отправьте номер веб-заявки в личку боту)\n" +
                "•\tПодтверждение веб-заявок (выбор полок, типа подтверждения, комментарий) с отправкой листа подбора на принтер\n" +
                "Ограничения:\n" +
                "•\tБот пока не показывает веб-заявки, упавшие в пикап (перемещение с магазина на магазин), т.к. сервер компании пока не умеет отдавать такую информацию. Как только исправят, бот тут же начнет их показывать :)\n" +
                "\n\nДанный бот НЕ ОФИЦИАЛЬНЫЙ и создан сотрудником компании для облегчения работы с веб-заявками." +
                "\nПо всем вопросам и предложениям пишите в личку - @klimatov"
    }

    fun statusCodeResolve(docStatus: String?): String {
        if (docStatus == null) return ""
        return when (docStatus) {
            "DOC_STORN" -> "Заказ отменен клиентом"
            "PWRQT_SHRT" -> "Заказ изменен, требуется подтверждение клиента (УС)"
            "WRQST_WAIT" -> "Заказ изменен, требуется подтверждение клиента (ЛС)"
            "PWRQT_DLVR" -> "Заказ доставляется (УС)"
            "PWRQT_PRCH" -> "Заказ выдан клиенту (УС)"
            "WRQST_RCVD" -> "Заказ выдан клиенту (ЛС)"
            "DWRQT_PRCH" -> "Заказ выдан клиенту (Д)"
            "PWRQT_RCVD" -> "Заказ ППО получен в магазине по документу отпуска"
            "WRQST_BNLY" -> "Выставлен безнал (ЛС)-Оплачен"
            "WRQST_BNLN" -> "Выставлен безнал (ЛС)"
            "WRQST_SHPD" -> "Товар по веб-заявке отгружен"
            "PWRQT_PMNT" -> "Ожидается предоплата"
            "PWRQT_DLVD" -> "Заказ подготовлен к выдаче (УС)"
            "WRQST_ACPT" -> "Заказ подготовлен к выдаче (ЛС)"
            "PWRQT_CRTD" -> "Заказ передан на исполнение (УС)"
            "WRQST_CRTD" -> "Заказ передан на исполнение (ЛС)"
            "DWRQT_CRTD" -> "Заказ передан на исполнение (Д)"
            "ZEXPIRED" -> "Заказ отменен. Истек срок хранения товара"
            else -> docStatus
        }
    }
}