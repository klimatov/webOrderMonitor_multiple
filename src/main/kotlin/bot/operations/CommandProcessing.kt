package bot.operations

import bot.models.BotState
import bot.models.BotUser
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import domain.orderProcessing.BotMessage
import utils.Logging
import java.util.*

class CommandProcessing(
    val bot: TelegramBot,
    botRepositoryDB: BotRepositoryDB,
    botTSRepository: BotTSRepository,
    private val stateUser: MutableMap<Identifier, BotState>,
    private val allBotUsers: MutableMap<Identifier, BotUser>
) {

    private val tag = this::class.java.simpleName

    private val botTSOperations = BotTSOperations(botTSRepository, botRepositoryDB)
    private val botMessage = BotMessage()
    private val orderConfirmation = OrderConfirmation(bot, botTSRepository, stateUser, allBotUsers)

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
                orderConfirmation.confirmWebOrder(chatId, dataMap["order"] ?: "", dataMap["web"] ?: "", defaultBehaviourContextWithFSM)
            }
        }
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

}