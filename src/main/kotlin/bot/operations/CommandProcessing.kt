package bot.operations

import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import domain.orderProcessing.BotMessage
import utils.Logging

class CommandProcessing(val bot: TelegramBot, botRepositoryDB: BotRepositoryDB, botTSRepository: BotTSRepository) {
    private val tag = this::class.java.simpleName
    private val botTSOperations = BotTSOperations(botTSRepository, botRepositoryDB)
    private val botMessage = BotMessage()
    suspend fun incomingMessage(rawMessage: CommonMessage<MessageContent>) {
        val newMessage = rawMessage.text.toString()
        Logging.d(tag, newMessage)

        when {
            (newMessage.toLongOrNull() != null) && (newMessage.length == 9) -> {
                Logging.d(tag, "its web #$newMessage")

                val webOrder = botTSOperations.getWebOrder(rawMessage.chat.id.chatId, newMessage)

                Logging.d(tag, webOrder.toString())

                if (webOrder.result.success) {
                    bot.sendMessage(
                        rawMessage.chat.id,
                        botMessage.orderMessage(webOrder.webOrder),
                        disableWebPagePreview = true
                    )

                    bot.sendMessage(
                        rawMessage.chat.id,
                        botMessage.statusCodeResolve(webOrder.webOrder.docStatus),
                        disableWebPagePreview = true
                    )

                } else bot.sendMessage(
                    rawMessage.chat.id,
                    when (webOrder.result.errorCode) {
                        200 -> {
                            "Веб-заявка №$newMessage не найдена (возможно это доставка)"
                        }

                        401, 403 -> {
                            "Отказано в доступе к базе TS. Попробуйте обновить пароль по команде /password"
                        }

                        else -> {
                            "Ошибка получения информации по веб-заявке №$newMessage. Код ошибки: ${webOrder.result.errorCode}"
                        }
                    },
                    disableWebPagePreview = true
                )
            }
        }

    }

    suspend fun fsmOperations() {

    }
}