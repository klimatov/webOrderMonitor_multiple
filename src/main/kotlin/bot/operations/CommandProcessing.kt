package bot.operations

import bot.models.BotInstanceParameters
import bot.models.BotState
import bot.models.BotUser
import bot.models.ConfirmationData
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.utils.row
import domain.orderProcessing.BotMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import utils.Logging
import java.util.*

class CommandProcessing(
    val bot: TelegramBot,
    botRepositoryDB: BotRepositoryDB,
    botTSRepository: BotTSRepository,
    private val stateUser: MutableMap<Identifier, BotState>,
    private val allBotUsers: MutableMap<Identifier, BotUser>,
    private val botInstancesParameters: MutableMap<String, BotInstanceParameters>,
    private val _confirmationDataFlow: MutableSharedFlow<ConfirmationData>
) {

    private val tag = this::class.java.simpleName

    private val botTSOperations = BotTSOperations(botTSRepository, botRepositoryDB)
    private val botMessage = BotMessage()
    private val orderConfirmation = OrderConfirmation(bot, botTSOperations, stateUser, allBotUsers, _confirmationDataFlow, botInstancesParameters)

    suspend fun incomingMessage(
        rawMessage: CommonMessage<MessageContent>,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        val newMessage = rawMessage.text.toString()
        Logging.d(tag, newMessage)
        val chatId: IdChatIdentifier = rawMessage.chat.id

        when {
            (newMessage.toLongOrNull() != null) && (newMessage.length == 9) -> requestWebOrder(chatId, newMessage)
            (newMessage == "send info message") -> sendMessageToAllChats(chatId, defaultBehaviourContextWithFSM)
        }

    }

    private suspend fun sendMessageToAllChats(
        chatIdZ: IdChatIdentifier,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        bot.sendMessage(
            chatIdZ,
            "Введите сообщение для отправки во все чаты:",
            disableWebPagePreview = true
        )

        with(defaultBehaviourContextWithFSM) {
            val sendingMessage = waitTextMessage().filter { message ->
                message.sameChat(chatIdZ)
            }.first()

            val messageButtons = inlineKeyboard {
                row {
                    dataButton("Отмена", "cancel=${chatIdZ}")
                    dataButton("Отправить", "send=${chatIdZ}")
                }
            }
            val messageId = bot.send(
                chatId = chatIdZ,
                text = sendingMessage.content.text + "\n\nОтправляем?",
                replyMarkup = messageButtons,
                disableWebPagePreview = true
            ).messageId

            val userChoice = waitDataCallbackQuery()
                .filter { message ->
                    message.message?.sameChat(chatIdZ) ?: false
                }
                .first()
                .data
                .split("=")

            bot.editMessageReplyMarkup(
                chatIdZ,
                messageId,
                replyMarkup = null
            )

            when (userChoice.first()) {
                "send" -> {
                    bot.edit(
                        chatIdZ,
                        messageId,
                        "Отправляем сообщения"
                    )
                    botInstancesParameters.forEach { botInstance ->
                        bot.send(
                            botInstance.value.targetChatId,
                            sendingMessage.content.text,
                            disableWebPagePreview = true
                        )
                    }
                }

                else -> bot.edit(
                    chatIdZ,
                    messageId,
                    "Отправка отменена"
                )


            }


        }

    }

    suspend fun incomingDeepLink(
        deepLink: String,
        chatId: IdChatIdentifier,
        defaultBehaviourContextWithFSM: DefaultBehaviourContextWithFSM<BotState>
    ) {
        val rawValue = String(Base64.getUrlDecoder().decode(deepLink))
        println(rawValue)
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

            "c" -> {
                Logging.d(tag, "confirm")
                orderConfirmation.confirmWebOrder(
                    chatId,
                    dataMap["o"] ?: "",
                    dataMap["w"] ?: "",
                    dataMap["m"] ?: "",
                    defaultBehaviourContextWithFSM
                )
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
                botMessage.orderMessage(webOrder.webOrder, bot.getMe().username),
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