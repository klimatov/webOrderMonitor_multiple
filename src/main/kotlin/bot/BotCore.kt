package bot

import cache.InMemoryCache
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import orderProcessing.data.SecurityData.TELEGRAM_BOT_TOKEN
import utils.Logging

sealed interface BotState : State

data class ExpectLogin(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class ExpectPassword(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class ExpectShop(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class StopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState

class BotCore(private val job: CompletableJob, private val botRepositoryDB: BotRepositoryDB) {
    private var botUser: MutableMap<Identifier, BotUser> = mutableMapOf()
    private var stateUser: MutableMap<Identifier, BotState> = mutableMapOf()
    private val tag = this::class.java.simpleName
    private val botToken = TELEGRAM_BOT_TOKEN
    private val bot = telegramBot(token = botToken)
    private val botRepositoryTS = BotRepositoryTS()

    private var allBotUsers: MutableMap<Identifier, BotUser> = botRepositoryDB.getAll()

    init {

    }

    suspend fun start() {

        val scope = CoroutineScope(Dispatchers.Default + job)
        bot.buildBehaviourWithFSMAndStartLongPolling<BotState>(scope) {

            strictlyOn<ExpectLogin> {
                botUser[it.context.chatId] = BotUser("", "", "", it.context.chatId, UserRole.USER.toString())

                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш логин в TS (буквами)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                botUser[it.context.chatId]?.tsLogin = contentMessage.content.text
                ExpectPassword(it.context, it.sourceMessage)
            }

            strictlyOn<ExpectPassword> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш пароль в TS (не должен быть пустым)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                botUser[it.context.chatId]?.tsPassword = contentMessage.content.text
                ExpectShop(it.context, it.sourceMessage)
            }

            strictlyOn<ExpectShop> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш магазин (в формате A000)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                if (contentMessage.content.text.length != 4) {
                    sendMessage(it.context, buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                    it
                } else {
                    botUser[it.context.chatId]?.tsShop = contentMessage.content.text
                    StopState(it.context, it.sourceMessage)
                }
            }

            /*            strictlyOn<ExpectOpenTime> {
                            stateUser[it.context.chatId] = it
                            sendMessage(it.context, buildEntities { +"Введите время открытия магазина (в часах, например 10)" })
                            val contentMessage = waitTextMessage().first()

                            if ((contentMessage.content.text.length > 2)||(contentMessage.content.text.toIntOrNull() == null)) {
                                sendMessage(it.context, buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                                it
                            } else {
                                botUsers[it.context.chatId]?.openTime = contentMessage.content.text.toInt()
                                StopState(it.context)
                            }
                        }*/

            strictlyOn<StopState> {
                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${botUser[it.context.chatId]?.tsLogin} " +
                            "\nпароль:${botUser[it.context.chatId]?.tsPassword} " +
                            "\nмагазин:${botUser[it.context.chatId]?.tsShop}"
                )

                stateUser.remove(it.context.chatId)

                println(botUser[it.context.chatId]) // тут вставить проверку и коннект с апи магазине
                // затем внесение в бд, если все ок или запрос по новой если не ок

                val result = botRepositoryTS.checkUserDataInTS(botUser[it.context.chatId])
                if (result) {
                    println("all ok")
                    botRepositoryDB.setBy(botUser = botUser[it.context.chatId]!!)
                    allBotUsers[it.context.chatId] = botUser[it.context.chatId]!!
                    null
                } else ExpectLogin(it.context, it.sourceMessage)

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

                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    sendMessage(
                        it.chat,
                        "Доступные команды:" +
                                "\n/start - список команд" +
                                "\n/getmyid - ваш telegram ID" +
                                "\n/update - обновить все данные TS" +
                                "\n/password - обновить пароль" +
                                "\n/create - создать бота магазина" +
                                "\n/delete - удалить бота магазина" +
                                "\n/info - информация о боте вашего магазина"
                    )
                } else {
                    sendTextMessage(it.chat, "Регистрируем вас")
                    startChain(ExpectLogin(it.chat.id, it))
                }
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