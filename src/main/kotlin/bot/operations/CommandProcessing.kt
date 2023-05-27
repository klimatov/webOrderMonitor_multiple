package bot.operations

import bot.models.BotState
import bot.models.ConfirmationExpectReason
import bot.models.ConfirmationStopState
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import domain.orderProcessing.BotMessage
import kotlinx.coroutines.flow.first
import restTS.models.*
import utils.Logging
import java.util.*

class CommandProcessing(
    val bot: TelegramBot,
    botRepositoryDB: BotRepositoryDB,
    botTSRepository: BotTSRepository,
    private val stateUser: MutableMap<Identifier, BotState>
) {

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
                )
            ),
            status = Status(title = null, description = null, rgb = null),
            itemsUpdateStatus = false,
            company = null
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


    private val tag = this::class.java.simpleName

    private val botTSOperations = BotTSOperations(botTSRepository, botRepositoryDB)
    private val botMessage = BotMessage()
    suspend fun incomingMessage(rawMessage: CommonMessage<MessageContent>) {
        val newMessage = rawMessage.text.toString()
        Logging.d(tag, newMessage)
        val chatId: IdChatIdentifier = rawMessage.chat.id

        when {
            (newMessage.toLongOrNull() != null) && (newMessage.length == 9) -> requestWebOrder(chatId, newMessage)
        }

    }

    suspend fun incomingDeepLink(
        deepLink: String,
        chatId: IdChatIdentifier,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        val rawValue = String(Base64.getUrlDecoder().decode(deepLink))
        val dataMap = rawValue
            .splitToSequence("&") // returns sequence of strings: [foo = 3, bar = 5, baz = 9000]
            .map { it.split("=") } // returns list of lists: [[foo, 3 ], [bar, 5 ], [baz, 9000]]
            .map { it[0] to it[1] } // return list of pairs: [(foo, 3), (bar, 5), (baz, 9000)]
            .toMap() // creates a map from your pairs

        Logging.d(tag, dataMap.toString())

        when (dataMap["t"]) {
            "info" -> {
                Logging.d(tag, "info")
                dataMap["web"]?.let { requestWebOrder(chatId, it) }
            }

            "confirm" -> {
                Logging.d(tag, "confirm")
                dataMap["order"]?.let { confirmWebOrder(chatId, it, defaultBehaviourContextWithFSM) }
            }
        }
    }

    private suspend fun confirmWebOrder(
        chatId: IdChatIdentifier,
        orderId: String,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        Logging.d(tag, "confirm order #$orderId")

        with(defaultBehaviourContextWithFSM) {
            strictlyOn<ConfirmationExpectReason> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Выберите тип подтверждения" }

                val infoMsg = inlineKeyboard {
                    row {
                        dataButton("Text button", "request=param")
                        dataButton("Text button2", "poom=purum")
                        dataButton("Text button3", "poom=purumvf")
                        dataButton("Text button4", "poom=purum33")
                        dataButton("Text button5", "poom=purum44")
                    }
                }

                try {
                    send(
                        it.context,
                        "Выберите тип подтверждения",
                        replyMarkup = infoMsg
                    ).reply_markup
                } catch (e: Exception) {
                    Logging.e(tag, "Exception: ${e.stackTraceToString()}}")
                }

                val contentMessage = waitDataCallbackQuery().first()
//                    .filter { message ->
//                    message.sameChat(it.context)
//                }.first()
                println(contentMessage.data)
//                println("вы выбрали тип ${contentMessage.content.text}")
                ConfirmationStopState(it.context)
            }

            strictlyOn<ConfirmationStopState> {
                stateUser.remove(it.context.chatId)
                sendMessage(
                    it.context,
                    buildEntities { +"Пробуем подтвердить веб-заявку" })

                null

            }
        }

        /*                        val webOrder = botTSOperations.getWebOrderDetail(chatId.chatId, orderId)
                                val reasonsList: MutableMap<String, List<ShortageReasonDto>> =
                                    emptyMap<String, List<ShortageReasonDto>>().toMutableMap()
                                webOrder.webOrder.items.forEach { item ->
                                    reasonsList[item.goodCode.toString()] =
                                        botTSOperations.getReasonForIncompliteness(chatId.chatId, orderId, item.goodCode.toString()).reasonsList
                                }
                                val shelfsList = botTSOperations.getShelfs(chatId.chatId)
                                val printersList = botTSOperations.getPrintersList(chatId.chatId)*/

        if ((webOrder.result.success) && (reasonsList.isNotEmpty()) && (shelfsList.shelfsList.isNotEmpty())) {
//            Logging.d(tag, webOrder.toString() + "\n\n" + reasonsList.toString() + "\n\n" + shelfsList.toString() + "\n\n" + printersList.toString())

            defaultBehaviourContextWithFSM.startChain(ConfirmationExpectReason(chatId))
        } else bot.sendMessage(
            chatId,
            "Ошибка получения данных из TS",
            disableWebPagePreview = true
        )


    }

    private suspend fun requestWebOrder(chatId: IdChatIdentifier, webNum: String) {
        Logging.d(tag, "request web order #$webNum")

        val webOrder = botTSOperations.getWebOrder(chatId.chatId, webNum)

        Logging.d(tag, webOrder.toString())

        if (webOrder.result.success) {
            bot.sendMessage(
                chatId,
                botMessage.orderMessage(webOrder.webOrder),
                disableWebPagePreview = true
            )

            bot.sendMessage(
                chatId,
                botMessage.statusCodeResolve(webOrder.webOrder.docStatus),
                disableWebPagePreview = true
            )

        } else bot.sendMessage(
            chatId,
            when (webOrder.result.errorCode) {
                200 -> {
                    "Веб-заявка №$webNum не найдена (возможно это доставка)"
                }

                401, 403 -> {
                    "Отказано в доступе к базе TS. Попробуйте обновить пароль по команде /password"
                }

                else -> {
                    "Ошибка получения информации по веб-заявке №$webNum. Код ошибки: ${webOrder.result.errorCode}"
                }
            },
            disableWebPagePreview = true
        )
    }


    suspend fun fsmOperations() {

    }
}