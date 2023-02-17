package domain.orderProcessing

import data.restTS.models.WebOrder
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.message.textsources.italic
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class BotMessage {

    fun orderMessage(webOrder: WebOrder?): TextSourcesList {
        val resultMessage = buildEntities {
            regularln("#️⃣${webOrder?.webNum}/${webOrder?.orderId}")
            regular("${webOrder?.ordType} ")
            if (webOrder?.isLegalEntity == "Y") bold("СЧЁТ КОНТРАГЕНТА")
            underlineln("\n\uD83D\uDCC6${DateTimeProcess().replaceDateTime(webOrder?.docDate ?: "")}")
            regularln("${if (webOrder?.paid == "Y") "\uD83D\uDCB0Онлайн оплата" else "\uD83E\uDDFEНе оплачен"} \uD83D\uDCB5${webOrder?.docSum} руб.")
            regular("\uD83D\uDC68${webOrder?.fioCustomer} ")
            phone("+${webOrder?.phone}")
            webOrder?.items?.forEach {
                linkln("\n\n\uD83D\uDD35${it.goodCode} ${it.name}", "https://${it.eshopUrl}\n")
                regular("\uD83D\uDCB0Цена: ${it.amount} ")
                if ((it.quantity ?: 0) > 1) regular("\uD83E\uDDF3Кол-во: ${it.quantity}")
                bold("\n\uD83D\uDED2Остатки:")
                it.remains.forEach { remains ->
                    bold(" ${remains.storageKind.toString()} - ${remains.quantity.toString()} шт.")
                }
            }
        }
        return resultMessage
    }

    fun inworkMessage(webOrder: WebOrder?): TextSourcesList {
        val resultMessage = buildEntities {
            codeln("⭕\uD83D\uDEE0Собираем ${minutesEnding(timeDiff(webOrder?.docDate))}!")
        }.plus(orderMessage(webOrder))
        return resultMessage
    }

    fun completeMessage(webOrder: WebOrder?): TextSourcesList {
        val resultMessage = buildEntities {
            boldln("✅Подтверждена за ${minutesEnding(timeDiff(webOrder?.docDate))}!")
        }.plus(italic(orderMessage(webOrder)))
        return resultMessage
    }

    fun timeDiff(docDate: String?, lateDate: LocalDateTime = LocalDateTime.now()): Long {
        if (docDate == null) return 0
        val docDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        val docDateFormatting = LocalDateTime.parse(docDate, docDateFormat)
        return docDateFormatting.until(lateDate, ChronoUnit.MINUTES)
    }

    fun timeNow(time: LocalTime = LocalTime.now()): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        return formatter.format(time)
    }

    fun shopInWork(shopOpenTime: Int, shopCloseTime: Int, time: LocalTime = LocalTime.now()): Boolean {
        return time.hour in shopOpenTime until shopCloseTime
    }

    fun minutesEnding(minute: Long): String {
        return "${minute} " + minute.let {
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

    fun orderEnding(order: Int): String {
        return "${order} " + order.let {
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

    fun orderStarting(order: Int): String {
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

    fun infoMessage(notConfirmedOrders: Int): String {
        val resTxt: String
        if (notConfirmedOrders == 0) {
            resTxt = "✅ Все заявки подтверждены ${timeNow()}"
        } else {
            resTxt =
                "⭕\uD83D\uDEE0 В подборе ${orderEnding(notConfirmedOrders)} ${timeNow()}"
        }
        return resTxt
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