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
import restTS.models.*
import utils.Logging
import kotlin.math.abs

class OrderConfirmation(
    val bot: TelegramBot,
    private val botTSOperations: BotTSOperations,
    private val stateUser: MutableMap<Identifier, BotState>,
    private val allBotUsers: MutableMap<Identifier, BotUser>
) {
    private val tag = this::class.java.simpleName


    private val webOrder: WebOrderResult = WebOrderResult(
        result = Result(success = true, errorMessage = "", errorCode = 200),
        webOrder = WebOrder(
            orderId = "6736709",
            ordType = "Самовывоз ЛС",
            isLegalEntity = "N",
            fioCustomer = "Иванов Иван",
            paid = "Y",
            phone = "71233540000",
            isRepeated = "N",
            docVers = "4",
            isPassportNeeded = "N",
            isBonusCardUsed = "Y",
            orderType = "WRQST",
            reasonCode = null,
            creationPlace = "BITRIX",
            collector = Collector(username = "dmn", hrCode = "0"),
            docDate = "27.05.2023 00:05:29",
            docSum = 399,
            docStatus = "WRQST_RCVD",
            webNum = "299157381",
            messageId = null,
            activeTime = null,
            items = listOf(
                Items(
                    goodCode = "590018193",
                    name = "Батарея Duracell Optimum АА 4 шт",
                    amount = 399,
                    quantity = 1,
                    itemNo = 10,
                    barCode = "5000394158696",
                    kit = null,
                    routeIsNeeded = "Y",
                    eshopUrl = "www.eldorado.ru/cat/detail/590018193/?utm_a=A181",
                    itemId = 15441505,
                    parentiTemNo = null,
                    shelf = "101",
                    params = listOf(),
                    incomplet = null,
                    remains = listOf()
                )/*,
                Items(
                    goodCode = "590018193",
                    name = "Батарея Duracell Optimum АА 4 штz",
                    amount = 399,
                    quantity = 1,
                    itemNo = 20,
                    barCode = "5000394158696",
                    kit = null,
                    routeIsNeeded = "Y",
                    eshopUrl = "www.eldorado.ru/cat/detail/590018193/?utm_a=A181",
                    itemId = 15441505,
                    parentiTemNo = null,
                    shelf = "101",
                    params = listOf(),
                    incomplet = null,
                    remains = listOf()
                )*/
            ),
            status = Status(title = null, description = null, rgb = null),
            itemsUpdateStatus = false,
            company = ""
        )
    )

    private val reasonsList: MutableMap<String, List<ShortageReasonDto>> =
        mutableMapOf(
            "590018193" to listOf(
                ShortageReasonDto(
                    needComm = "N",
                    shipped = "N",
                    priority = 0,
                    name = "Товар найден",
                    reasonCode = "OK"
                ),
                ShortageReasonDto(
                    needComm = "N",
                    shipped = "Y",
                    priority = 2,
                    name = "Отсутствие товара",
                    reasonCode = "MISSING"
                ),
                ShortageReasonDto(
                    needComm = "Y",
                    shipped = "N",
                    priority = 3,
                    name = "Мех. повреждения (незначительные)*",
                    reasonCode = "FLAW"
                ),
                ShortageReasonDto(
                    needComm = "N",
                    shipped = "N",
                    priority = 4,
                    name = "Витринный образец",
                    reasonCode = "SHOWCASE"
                ),
                ShortageReasonDto(
                    needComm = "Y",
                    shipped = "N",
                    priority = 5,
                    name = "Некомплект*",
                    reasonCode = "SHORTAGE"
                ),
                ShortageReasonDto(
                    needComm = "Y",
                    shipped = "Y",
                    priority = 6,
                    name = "Мех. повреждения (значительные) - 0",
                    reasonCode = "DEFECT"
                ),
                ShortageReasonDto(
                    needComm = "Y",
                    shipped = "Y",
                    priority = 7,
                    name = "БРАК - 0*",
                    reasonCode = "REJECT"
                )
            )
        )

    private val shelfsList: ShelfsResult = ShelfsResult(
        result = Result(success = true, errorMessage = "", errorCode = 200),
        shelfsList = listOf(
            ShelfItem(
                shelfId = 195,
                description = "Секция 4 / Стеллаж 9 / Полка 5",
                sectionNumber = 4,
                rackNumber = 9,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 201,
                description = "Секция 5 / Стеллаж 1 / Полка 1",
                sectionNumber = 5,
                rackNumber = 1,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 202,
                description = "Секция 5 / Стеллаж 1 / Полка 2",
                sectionNumber = 5,
                rackNumber = 1,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 206,
                description = "Секция 5 / Стеллаж 2 / Полка 1",
                sectionNumber = 5,
                rackNumber = 2,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 209,
                description = "Секция 5 / Стеллаж 2 / Полка 4",
                sectionNumber = 5,
                rackNumber = 2,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 211,
                description = "Секция 5 / Стеллаж 3 / Полка 1",
                sectionNumber = 5,
                rackNumber = 3,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 216,
                description = "Секция 5 / Стеллаж 4 / Полка 1",
                sectionNumber = 5,
                rackNumber = 4,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 220,
                description = "Секция 5 / Стеллаж 4 / Полка 5",
                sectionNumber = 5,
                rackNumber = 4,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 221,
                description = "Секция 5 / Стеллаж 5 / Полка 1",
                sectionNumber = 5,
                rackNumber = 5,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 226,
                description = "Секция 5 / Стеллаж 6 / Полка 1",
                sectionNumber = 5,
                rackNumber = 6,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 227,
                description = "Секция 5 / Стеллаж 6 / Полка 2",
                sectionNumber = 5,
                rackNumber = 6,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 231,
                description = "Секция 5 / Стеллаж 7 / Полка 1",
                sectionNumber = 5,
                rackNumber = 7,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 239,
                description = "Секция 5 / Стеллаж 8 / Полка 4",
                sectionNumber = 5,
                rackNumber = 8,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 240,
                description = "Секция 5 / Стеллаж 8 / Полка 5",
                sectionNumber = 5,
                rackNumber = 8,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 241,
                description = "Секция 5 / Стеллаж 9 / Полка 1",
                sectionNumber = 5,
                rackNumber = 9,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 242,
                description = "Секция 5 / Стеллаж 9 / Полка 2",
                sectionNumber = 5,
                rackNumber = 9,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 243,
                description = "Секция 5 / Стеллаж 9 / Полка 3",
                sectionNumber = 5,
                rackNumber = 9,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 244,
                description = "Секция 5 / Стеллаж 9 / Полка 4",
                sectionNumber = 5,
                rackNumber = 9,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 245,
                description = "Секция 5 / Стеллаж 9 / Полка 5",
                sectionNumber = 5,
                rackNumber = 9,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 248,
                description = "Секция 5 / Стеллаж 10 / Полка 3",
                sectionNumber = 5,
                rackNumber = 10,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 250,
                description = "Секция 5 / Стеллаж 10 / Полка 5",
                sectionNumber = 5,
                rackNumber = 10,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 101,
                description = "Секция 3 / Стеллаж 1 / Полка 1",
                sectionNumber = 3,
                rackNumber = 1,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 102,
                description = "Секция 3 / Стеллаж 1 / Полка 2",
                sectionNumber = 3,
                rackNumber = 1,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 103,
                description = "Секция 3 / Стеллаж 1 / Полка 3",
                sectionNumber = 3,
                rackNumber = 1,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 104,
                description = "Секция 3 / Стеллаж 1 / Полка 4",
                sectionNumber = 3,
                rackNumber = 1,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 105,
                description = "Секция 3 / Стеллаж 1 / Полка 5",
                sectionNumber = 3,
                rackNumber = 1,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 106,
                description = "Секция 3 / Стеллаж 2 / Полка 1",
                sectionNumber = 3,
                rackNumber = 2,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 107,
                description = "Секция 3 / Стеллаж 2 / Полка 2",
                sectionNumber = 3,
                rackNumber = 2,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 108,
                description = "Секция 3 / Стеллаж 2 / Полка 3",
                sectionNumber = 3,
                rackNumber = 2,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 109,
                description = "Секция 3 / Стеллаж 2 / Полка 4",
                sectionNumber = 3,
                rackNumber = 2,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 110,
                description = "Секция 3 / Стеллаж 2 / Полка 5",
                sectionNumber = 3,
                rackNumber = 2,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 111,
                description = "Секция 3 / Стеллаж 3 / Полка 1",
                sectionNumber = 3,
                rackNumber = 3,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 112,
                description = "Секция 3 / Стеллаж 3 / Полка 2",
                sectionNumber = 3,
                rackNumber = 3,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 113,
                description = "Секция 3 / Стеллаж 3 / Полка 3",
                sectionNumber = 3,
                rackNumber = 3,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 114,
                description = "Секция 3 / Стеллаж 3 / Полка 4",
                sectionNumber = 3,
                rackNumber = 3,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 115,
                description = "Секция 3 / Стеллаж 3 / Полка 5",
                sectionNumber = 3,
                rackNumber = 3,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 116,
                description = "Секция 3 / Стеллаж 4 / Полка 1",
                sectionNumber = 3,
                rackNumber = 4,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 117,
                description = "Секция 3 / Стеллаж 4 / Полка 2",
                sectionNumber = 3,
                rackNumber = 4,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 118,
                description = "Секция 3 / Стеллаж 4 / Полка 3",
                sectionNumber = 3,
                rackNumber = 4,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 119,
                description = "Секция 3 / Стеллаж 4 / Полка 4",
                sectionNumber = 3,
                rackNumber = 4,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 120,
                description = "Секция 3 / Стеллаж 4 / Полка 5",
                sectionNumber = 3,
                rackNumber = 4,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 121,
                description = "Секция 3 / Стеллаж 5 / Полка 1",
                sectionNumber = 3,
                rackNumber = 5,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 122,
                description = "Секция 3 / Стеллаж 5 / Полка 2",
                sectionNumber = 3,
                rackNumber = 5,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 123,
                description = "Секция 3 / Стеллаж 5 / Полка 3",
                sectionNumber = 3,
                rackNumber = 5,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 124,
                description = "Секция 3 / Стеллаж 5 / Полка 4",
                sectionNumber = 3,
                rackNumber = 5,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 125,
                description = "Секция 3 / Стеллаж 5 / Полка 5",
                sectionNumber = 3,
                rackNumber = 5,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 126,
                description = "Секция 3 / Стеллаж 6 / Полка 1",
                sectionNumber = 3,
                rackNumber = 6,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 127,
                description = "Секция 3 / Стеллаж 6 / Полка 2",
                sectionNumber = 3,
                rackNumber = 6,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 128,
                description = "Секция 3 / Стеллаж 6 / Полка 3",
                sectionNumber = 3,
                rackNumber = 6,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 129,
                description = "Секция 3 / Стеллаж 6 / Полка 4",
                sectionNumber = 3,
                rackNumber = 6,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 130,
                description = "Секция 3 / Стеллаж 6 / Полка 5",
                sectionNumber = 3,
                rackNumber = 6,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 131,
                description = "Секция 3 / Стеллаж 7 / Полка 1",
                sectionNumber = 3,
                rackNumber = 7,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 132,
                description = "Секция 3 / Стеллаж 7 / Полка 2",
                sectionNumber = 3,
                rackNumber = 7,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 133,
                description = "Секция 3 / Стеллаж 7 / Полка 3",
                sectionNumber = 3,
                rackNumber = 7,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 134,
                description = "Секция 3 / Стеллаж 7 / Полка 4",
                sectionNumber = 3,
                rackNumber = 7,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 135,
                description = "Секция 3 / Стеллаж 7 / Полка 5",
                sectionNumber = 3,
                rackNumber = 7,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 136,
                description = "Секция 3 / Стеллаж 8 / Полка 1",
                sectionNumber = 3,
                rackNumber = 8,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 137,
                description = "Секция 3 / Стеллаж 8 / Полка 2",
                sectionNumber = 3,
                rackNumber = 8,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 138,
                description = "Секция 3 / Стеллаж 8 / Полка 3",
                sectionNumber = 3,
                rackNumber = 8,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 139,
                description = "Секция 3 / Стеллаж 8 / Полка 4",
                sectionNumber = 3,
                rackNumber = 8,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 140,
                description = "Секция 3 / Стеллаж 8 / Полка 5",
                sectionNumber = 3,
                rackNumber = 8,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 141,
                description = "Секция 3 / Стеллаж 9 / Полка 1",
                sectionNumber = 3,
                rackNumber = 9,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 142,
                description = "Секция 3 / Стеллаж 9 / Полка 2",
                sectionNumber = 3,
                rackNumber = 9,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 143,
                description = "Секция 3 / Стеллаж 9 / Полка 3",
                sectionNumber = 3,
                rackNumber = 9,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 144,
                description = "Секция 3 / Стеллаж 9 / Полка 4",
                sectionNumber = 3,
                rackNumber = 9,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 145,
                description = "Секция 3 / Стеллаж 9 / Полка 5",
                sectionNumber = 3,
                rackNumber = 9,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 146,
                description = "Секция 3 / Стеллаж 10 / Полка 1",
                sectionNumber = 3,
                rackNumber = 10,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 147,
                description = "Секция 3 / Стеллаж 10 / Полка 2",
                sectionNumber = 3,
                rackNumber = 10,
                shelfNumber = 2
            ),
            ShelfItem(
                shelfId = 148,
                description = "Секция 3 / Стеллаж 10 / Полка 3",
                sectionNumber = 3,
                rackNumber = 10,
                shelfNumber = 3
            ),
            ShelfItem(
                shelfId = 149,
                description = "Секция 3 / Стеллаж 10 / Полка 4",
                sectionNumber = 3,
                rackNumber = 10,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 150,
                description = "Секция 3 / Стеллаж 10 / Полка 5",
                sectionNumber = 3,
                rackNumber = 10,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 151,
                description = "Секция 4 / Стеллаж 1 / Полка 1",
                sectionNumber = 4,
                rackNumber = 1,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 155,
                description = "Секция 4 / Стеллаж 1 / Полка 5",
                sectionNumber = 4,
                rackNumber = 1,
                shelfNumber = 5
            ),
            ShelfItem(
                shelfId = 156,
                description = "Секция 4 / Стеллаж 2 / Полка 1",
                sectionNumber = 4,
                rackNumber = 2,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 159,
                description = "Секция 4 / Стеллаж 2 / Полка 4",
                sectionNumber = 4,
                rackNumber = 2,
                shelfNumber = 4
            ),
            ShelfItem(
                shelfId = 171,
                description = "Секция 4 / Стеллаж 5 / Полка 1",
                sectionNumber = 4,
                rackNumber = 5,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 176,
                description = "Секция 4 / Стеллаж 6 / Полка 1",
                sectionNumber = 4,
                rackNumber = 6,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 181,
                description = "Секция 4 / Стеллаж 7 / Полка 1",
                sectionNumber = 4,
                rackNumber = 7,
                shelfNumber = 1
            ),
            ShelfItem(
                shelfId = 1451,
                description = "Товар, находящийся в Торговом зале.",
                sectionNumber = 0,
                rackNumber = 0,
                shelfNumber = 0
            ),
            ShelfItem(
                shelfId = 1452,
                description = "Место хранения особо ценных товаров.",
                sectionNumber = 1,
                rackNumber = 0,
                shelfNumber = 0
            ),
            ShelfItem(
                shelfId = 1453,
                description = "Место хранения КГТ товаров. Склад.",
                sectionNumber = 2,
                rackNumber = 0,
                shelfNumber = 0
            )
        )
    )

    private val printersList: PrintersListResult = PrintersListResult(
        result = Result(success = true, errorMessage = "", errorCode = 200),
        printersList =
        listOf(
            PcNameList(pcName = "A181_TERM2"),
            PcNameList(pcName = "A181_TERM08"),
            PcNameList(pcName = "A181_OPERDIR"),
            PcNameList(pcName = "A181-SERVICE3"),
            PcNameList(pcName = "A181_STKASSA01"),
            PcNameList(pcName = "A181_TERM06"),
            PcNameList(pcName = "A181_SERVICE5"),
            PcNameList(pcName = "A181_TERM1"),
            PcNameList(pcName = "A181_SCLAD"),
            PcNameList(pcName = "A181_TERM05"),
            PcNameList(pcName = "A181_SERVICE01"),
            PcNameList(pcName = "A181_TERM07"),
            PcNameList(pcName = "A181_DIR"),
            PcNameList(pcName = "A181_SERVICE02"),
            PcNameList(pcName = "A181_TERM03")
        )
    )

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
//                    val webOrder = botTSOperations.getWebOrderDetail(chatId.chatId, orderId)

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

                                if (orderDetail.webOrder.docStatus == "WRQST_CRTD") {

                                    val saveItemsList: MutableList<SaveItems> = mutableListOf()

                                    it.orderSaveParam.items.forEach { item ->
                                        saveItemsList.add(
                                            SaveItems(
                                                quantity = item.quantity?:"",
                                                itemNo = item.itemNo?:"",
                                                goodCode = item.goodCode,
                                                incomplet = SaveIncomplet(
                                                    reasonCode = item.incomplet?.reasonCode,
                                                    comment = item.incomplet?.comment?:item.incomplet?.reasonName
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
                                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FINISH
                                    } else {
                                        Logging.e(
                                            tag,
                                            "Ошибка подтверждения заявки №${it.orderSaveParam.webNum} ${saveResult.result.errorMessage}"
                                        )
                                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                                    }
                                    Logging.d(tag, saveResult.saveWebOrder.message.toString())
                                } else {

                                    when(orderDetail.webOrder.docStatus) {
                                        "DOC_STORN" -> it.orderSaveParam.saveStatus = OrderDataSaveStatus.STORN
                                        else -> it.orderSaveParam.saveStatus = OrderDataSaveStatus.EXIST
                                    }
                                    Logging.e(
                                        tag,
                                        "Заявка №${it.orderSaveParam.webNum} уже подтверждена/отменена ${orderDetail.webOrder.collector?.username}"
                                    )

                                }

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
                val doReasonsList =
                    reasonsList["590018193"] ?: emptyList()

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

                val sortedShelfs = shelfsList.shelfsList.sortedBy { it.shelfId }
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
             * ConfirmationChoosingSomeState
             */
            strictlyOn<ConfirmationChoosingSomeState>
            {
                stateUser[it.context.chatId] = it

                Logging.d(tag, "Параметры тут такие: ${it.orderSaveParam.toString()}")

//                val reasonsList: MutableMap<String, List<ShortageReasonDto>> =
//                    emptyMap<String, List<ShortageReasonDto>>().toMutableMap()
//                webOrder.webOrder.items.forEach { item ->
//                    reasonsList[item.goodCode.toString()] =
//                        botTSOperations.getReasonForIncompliteness(
//                            chatId.chatId,
//                            orderId,
//                            item.goodCode.toString()
//                        ).reasonsList
//                }


//                val shelfsList = botTSOperations.getShelfs(chatId.chatId)
                it.orderSaveParam.items.forEach { item ->
                    val doReasonsList =
                        reasonsList["590018193"] ?: emptyList()
//                        botTSOperations.getReasonForIncompliteness(chatId.chatId, orderId, item.goodCode.toString()).reasonsList

                    if ((doReasonsList.isNotEmpty()) && (shelfsList.result.success)) {

                        // блок выбора типа подтверждения


                        val reasonButtons = inlineKeyboard {
                            doReasonsList.forEach {
                                row {
                                    dataButton(it.name.toString(), it.reasonCode.toString())
                                }
                            }
                        }
                        doMessage(
                            it.context,
                            templateReasonRequest(item.goodCode ?: "", item.name ?: ""),
                            it.orderSaveParam.messageId,
                            reasonButtons
                        )
                        val reasonChoiceCode = waitDataCallbackQuery().filter { message ->
                            message.message?.sameChat(it.context) ?: false
                        }.first()

                        val reasonChoice = doReasonsList.find { it.reasonCode == reasonChoiceCode.data }

                        if (reasonChoice?.needComm == "Y") {
                            doMessage(
                                it.context,
                                templateCommentRequest(item.goodCode ?: "", item.name ?: "", reasonChoice.name ?: ""),
                                it.orderSaveParam.messageId
                            )
                            val reasonComment = waitTextMessage().filter { message ->
                                message.sameChat(it.context)
                            }.first()
                            delete(it.context, reasonComment.messageId)


                        }
                        println("вы выбрали тип ${reasonChoiceCode.data}")

                        // блок выбора полки

//                        var count = 0

                        val sortedShelfs = shelfsList.shelfsList.sortedBy { it.shelfId }
                        val size = sortedShelfs.size - 3
                        val x = 5
                        val y = abs(size / x)

                        var count = 1

                        // TODO: отфильтровать секции больше 3


                        val shelfButtons = inlineKeyboard {

                            for (yy in 1..(y + 1)) {
                                row {
                                    for (xx in 1..x) {
                                        if (count >= size) break
                                        val shelf = sortedShelfs.get(count)
                                        dataButton(
                                            "${shelf.sectionNumber}/${shelf.rackNumber}/${shelf.shelfNumber}",
                                            shelf.shelfId.toString()
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
                                        shelf.shelfId.toString()
                                    )
                                }
                            }

//                            shelfsList.shelfsList.sortedBy { it.shelfId }.forEach {
//
//                                if (count++ < (shelfsList.shelfsList.size - 3)) {
//                                    row {
//                                        dataButton(
//                                            "${it.sectionNumber}/${it.rackNumber}/${it.shelfNumber}",
//                                            it.shelfId.toString()
//                                        )
//                                    }
//
//                                } else {
//                                    row {
//                                        dataButton(
//                                            it.description.toString(),
//                                            it.shelfId.toString()
//                                        )
//                                    }
//                                }
//
//
//                            }
                        }

                        doMessage(
                            it.context,
                            templateShelfRequest(item.goodCode ?: "", item.name ?: ""),
                            it.orderSaveParam.messageId,
                            shelfButtons
                        )
                        val shelfChoiceId = waitDataCallbackQuery().filter { message ->
                            message.message?.sameChat(it.context) ?: false
                        }.first()

                        val shelfChoice = shelfsList.shelfsList.find { it.shelfId.toString() == shelfChoiceId.data }

                        println("вы выбрали полку ${shelfChoice}")


                    } else {
                        it.orderSaveParam.saveStatus = OrderDataSaveStatus.FALSE
                        ConfirmationStopState(it.context, it.orderSaveParam)
                        return@forEach
                        null
                    }

                }


                it.orderSaveParam.saveStatus = OrderDataSaveStatus.FINISH
                ConfirmationStopState(it.context, it.orderSaveParam)

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