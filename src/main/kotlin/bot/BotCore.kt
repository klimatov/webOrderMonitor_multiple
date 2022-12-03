package bot

import cache.InMemoryCache
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import orderProcessing.data.SecurityData.TELEGRAM_BOT_TOKEN
import utils.Logging

sealed interface BotState : State

//data class ExpectLogin(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class ExpectLogin(override val context: ChatId) : BotState
data class ExpectPassword(override val context: ChatId) : BotState
data class ExpectShop(override val context: ChatId) : BotState
data class StopState(override val context: ChatId) : BotState

class BotCore(private val job: CompletableJob) {
    private var botUsers: MutableMap<Identifier, BotUserData> = mutableMapOf()
    private var stateUser: MutableMap<Identifier, BotState> = mutableMapOf()
    private val tag = this::class.java.simpleName
    private val botToken = TELEGRAM_BOT_TOKEN
    private val bot = telegramBot(token = botToken)

    init {

    }

    suspend fun start() {
        val scope = CoroutineScope(Dispatchers.Default + job)
        bot.buildBehaviourWithFSMAndStartLongPolling<BotState>(scope) {

            strictlyOn<ExpectLogin> {
                botUsers[it.context.chatId] = BotUserData("", "", "")

                stateUser[it.context.chatId] = it
                sendMessage(it.context, buildEntities { +"Введите ваш логин в TS (буквами)" })
                val contentMessage = waitTextMessage().first()
                botUsers[it.context.chatId]?.login = contentMessage.content.text
                ExpectPassword(it.context)
            }

            strictlyOn<ExpectPassword> {
                stateUser[it.context.chatId] = it
                sendMessage(it.context, buildEntities { +"Введите ваш пароль в TS (не должен быть пустым)" })
                val contentMessage = waitTextMessage().first()
                botUsers[it.context.chatId]?.password = contentMessage.content.text
                ExpectShop(it.context)
            }

            strictlyOn<ExpectShop> {
                stateUser[it.context.chatId] = it
                sendMessage(it.context, buildEntities { +"Введите ваш магазин (в формате A000)" })
                val contentMessage = waitTextMessage().first()

                if (contentMessage.content.text.length != 4) {
                    sendMessage(it.context, buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                    it
                } else {
                    botUsers[it.context.chatId]?.shop = contentMessage.content.text
                    StopState(it.context)
                }
            }

            strictlyOn<StopState> {
                sendMessage(
                    it.context,
                    "Проверяем в TS логин:${botUsers[it.context.chatId]?.login} " +
                            "пароль:${botUsers[it.context.chatId]?.password} " +
                            "магазин:${botUsers[it.context.chatId]?.shop}"
                )

                stateUser.remove(it.context.chatId)

                null
            }

//            strictlyOn<ExpectPasswordOrStopState> {
//                val contentMessage = waitContentMessage().first()
//                val content = contentMessage.content
//
//                when {
//                    content is TextContent && content.parseCommandsWithParams().keys.contains("stop") -> StopState(it.context)
//                    else -> {
//                        StopState(it.context)
////                        execute(content.createResend(it.context))
////                        it
//                    }
//                }
//            }


            onCommand(
                "start",
                initialFilter = { stateUser[it.chat.id.chatId] == null }
            ) {
                sendTextMessage(it.chat, "Стартуем!")
                startChain(ExpectLogin(it.chat.id))
            }

            onCommand("getmyid") {
                val userId = it.asFromUser()?.user?.id?.chatId
                val chatId = it.chat.id.chatId
                sendTextMessage(it.chat, "Твой user ID: $userId\nТекущий chat ID: $chatId")
            }

            onCommand("lasterrors") {
                sendTextMessage(it.chat, "Последние ошибки:")
                sendTextMessage(chat = it.chat, text = InMemoryCache.getErrors())
            }

            Logging.i(tag, "Telegram Bot started! ${getMe()}")
        }.start()


    }
}