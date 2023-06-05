package domain.orderProcessing

import dev.inmo.tgbotapi.extensions.utils.formatting.makeDeepLink
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

    fun orderMessage(webOrder: WebOrder?, botName: Username): TextSourcesList {
        val resultMessage = buildEntities {
            regular("#️⃣${webOrder?.webNum}/${webOrder?.orderId} ")
            linkln("[i]", makeDeepLink(botName, Base64.getUrlEncoder().encodeToString("t=info&web=${webOrder?.webNum}&order=${webOrder?.orderId}".toByteArray())))
            regular("${webOrder?.ordType} ")
            if (webOrder?.isLegalEntity == "Y") bold("СЧЁТ КОНТРАГЕНТА")
            underlineln("\n\uD83D\uDCC6${replaceDateTime(webOrder?.docDate ?: "")}")
            regularln("${if (webOrder?.paid == "Y") "\uD83D\uDCB0Онлайн оплата" else "\uD83E\uDDFEНе оплачен"} \uD83D\uDCB5${webOrder?.docSum} руб.")
            regular("\uD83D\uDC68${webOrder?.fioCustomer} ")
            phone("+${webOrder?.phone}")
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
            codeln("⭕\uD83D\uDEE0Собираем ${minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))}!")
        }
            .plus(orderMessage(webOrder, botName))
        return resultMessage
    }

    fun completeMessage(webOrder: WebOrder?, gmt: String, botName: Username): TextSourcesList {
        val resultMessage = buildEntities {
            boldln("✅Подтверждена за ${minutesEnding(timeDiff(webOrder?.docDate, gmt = gmt))}!")
        }
            .plus(italic(orderMessage(webOrder, botName)))
        return resultMessage
    }

    fun confirmButton(webOrder: WebOrder?, botName: Username): InlineKeyboardMarkup {
        return inlineKeyboard {
                row {
                    urlButton("Подтвердить",
                        makeDeepLink(botName, Base64.getUrlEncoder().encodeToString("t=confirm&web=${webOrder?.webNum}&order=${webOrder?.orderId}".toByteArray()))
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

    private fun orderStarting(order: Int): String {
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

    fun notificationMessage(notification: Boolean, dayConfirmedCount: Int): String {
        if (notification) {
            return "\uD83D\uDD08 Магазин открыт, включаем уведомления!"
        } else {
            return "\uD83D\uDD07 Магазин закрыт, отключаем уведомления!\n" +
                    "\uD83D\uDE2B Сегодня за день ${orderStarting(dayConfirmedCount)} ${orderEnding(dayConfirmedCount)}"
        }
    }

    fun popupMessage(dayConfirmedCount: Int): String {
        return "Сегодня ${orderStarting(dayConfirmedCount)} ${orderEnding(dayConfirmedCount)}"
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
                "Ограничения:\n" +
                "•\tБот пока не показывает веб-заявки, упавшие в пикап (перемещение с магазина на магазин), т.к. сервер компании пока не умеет отдавать такую информацию. Как только исправят, бот тут же начнет их показывать :)\n" +
                "В разработке (пока не реализовано):\n" +
                "•\tВывод информации о веб-заявке по ее номеру или номеру заказа покупателя\n" +
                "•\tПодтверждение веб-заявок\n" +
                "\n\nДанный бот не официальный и создан сотрудником компании для облегчения работы с веб-заявками."
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