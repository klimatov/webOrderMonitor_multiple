package bot

import cache.InMemoryCache
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithParams
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.chat.*
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.buildEntities
import domain.models.WorkerState
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import orderProcessing.data.SecurityData.TELEGRAM_BOT_TOKEN
import utils.Logging
import java.util.*

sealed interface BotState : State

data class UserExpectLogin(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class UserExpectPassword(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class UserExpectShop(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class UserStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState

data class BotExpectChatId(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class BotExpectOpenTime(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class BotExpectCloseTime(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class BotStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class DeleteExpectConfirmation(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class DeleteStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState

class BotCore(private val job: CompletableJob, private val botRepositoryDB: BotRepositoryDB) {
    private var botUser: MutableMap<Identifier, BotUser> = mutableMapOf()
    private var newWorkers: MutableMap<Identifier, NewWorker> = mutableMapOf()
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

            strictlyOn<UserExpectLogin> {
                botUser[it.context.chatId] = BotUser("", "", "", it.context.chatId, UserRole.USER.toString())

                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш логин в TS (буквами)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                botUser[it.context.chatId]?.tsLogin = contentMessage.content.text
                UserExpectPassword(it.context, it.sourceMessage)
            }

            strictlyOn<UserExpectPassword> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш пароль в TS (не должен быть пустым)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                botUser[it.context.chatId]?.tsPassword = contentMessage.content.text
                UserExpectShop(it.context, it.sourceMessage)
            }

            strictlyOn<UserExpectShop> {
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
                    UserStopState(it.context, it.sourceMessage)
                }
            }

            strictlyOn<UserStopState> {
                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${botUser[it.context.chatId]?.tsLogin} " +
                            "\nпароль:${botUser[it.context.chatId]?.tsPassword} " +
                            "\nмагазин:${botUser[it.context.chatId]?.tsShop}"
                )

                stateUser.remove(it.context.chatId)

                println(botUser[it.context.chatId]) // тут вставить проверку и коннект с апи магазине
                // затем внесение в бд, если все ок или запрос по новой если не ок

                val resultCheckTs = botRepositoryTS.checkUserDataInTS(botUser[it.context.chatId])
                if (resultCheckTs) {
                    Logging.i(tag, "Create new user ${botUser[it.context.chatId]!!.tsLogin}")
                    botRepositoryDB.setUserBy(botUser = botUser[it.context.chatId]!!)
                    allBotUsers[it.context.chatId] = botUser[it.context.chatId]!!
                    null
                } else UserExpectLogin(it.context, it.sourceMessage)

            }

            // /create

            strictlyOn<BotExpectChatId> {
                newWorkers[it.context.chatId] = NewWorker(
                    workerId = UUID.randomUUID(),
                    login = allBotUsers[it.context.chatId]!!.tsLogin,
                    password = allBotUsers[it.context.chatId]!!.tsPassword,
                    shop = allBotUsers[it.context.chatId]!!.tsShop,
                    ownerTgId = it.context.chatId,
                    isActive = true,
                    shopOpen = 10,
                    shopClose = 21,
                    telegramChatId = 0,
                    workerState = WorkerState.CREATE
                )
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ID чата в который добавлен бот и куда он будет скидывать информацию (или /stop для отмены создания)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                val content = contentMessage.content

                when {
                    content is TextContent && content.parseCommandsWithParams().keys.contains("stop") -> BotStopState(
                        it.context,
                        contentMessage
                    )

                    else -> {


                        if (contentMessage.text?.toLongOrNull() != null) {
                            try {
                                val msgChatId = ChatId(contentMessage.text?.toLong()!!)
                                val msgResult = getChat(msgChatId)
                                val chatTitle = when (msgResult) {
                                    is ExtendedSupergroupChatImpl -> msgResult.title
                                    is ExtendedPublicChat -> msgResult.title
                                    is ExtendedChannelChat -> msgResult.title
                                    is ExtendedChannelChatImpl -> msgResult.title
                                    is ExtendedGroupChat -> msgResult.title
                                    is ExtendedGroupChatImpl -> msgResult.title
                                    is ExtendedSupergroupChat -> msgResult.title
                                    else -> msgChatId.chatId.toString()
                                }

                                send(it.context) { +"Чат $chatTitle доступен для бота" }
                                newWorkers[it.context.chatId]?.telegramChatId = contentMessage.text!!.toLong()
                                BotExpectOpenTime(it.context, it.sourceMessage)

                            } catch (e: Exception) {
                                Logging.e(tag, e.message.toString())
                                send(it.context) { +"Некорректный chat ID, возможно Вы не добавили бота в чат как участника." }
                                null
                                BotExpectChatId(it.context, it.sourceMessage)
                            }
                        } else {
                            send(it.context) { +"Некорректный chat ID" }
                            null
                            BotExpectChatId(it.context, it.sourceMessage)
                        }

                    }
                }


            }

            strictlyOn<BotExpectOpenTime> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите время открытия магазина (в часах, например 10)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                if ((contentMessage.content.text.length > 2) || (contentMessage.content.text.toIntOrNull() == null)) {
                    sendMessage(it.context, buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                    it
                } else {
                    newWorkers[it.context.chatId]?.shopOpen = contentMessage.text!!.toInt()
                    BotExpectCloseTime(it.context, it.sourceMessage)
                }
            }

            strictlyOn<BotExpectCloseTime> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите время закрытия магазина (в часах, например 10)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                if ((contentMessage.content.text.length > 2) || (contentMessage.content.text.toIntOrNull() == null)) {
                    sendMessage(it.context, buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                    it
                } else {
                    newWorkers[it.context.chatId]?.shopClose = contentMessage.text!!.toInt()
                    BotStopState(it.context, it.sourceMessage)
                }
            }

            strictlyOn<BotStopState> {
                stateUser.remove(it.context.chatId)


                if (!it.sourceMessage.content.parseCommandsWithParams().keys.contains("stop")) {
                    Logging.i(tag, "Create new worker in DB ${newWorkers[it.context.chatId]!!.shop}")
                    botRepositoryDB.setWorkerBy(newWorkers[it.context.chatId]!!)
                    sendMessage(
                        it.context,
                        buildEntities { +"Чат-бот магазина ${allBotUsers[it.context.chatId]!!.tsShop} создан" })
                    // Сохраняем новый/изменившийся воркер для создания/обновления
                    BotRepositoryWorkersImpl.changedWorkers.add(newWorkers[it.context.chatId]!!.mapToShopWorkersParam())
                } else sendMessage(
                    it.context,
                    buildEntities { +"Создание бота отменено" })

                null
            }


            // /delete


            strictlyOn<DeleteExpectConfirmation> {
                stateUser[it.context.chatId] = it
                send(it.context) {
                    +"Введите 'Да', если вы действительно хотите удалить бота магазина ${allBotUsers[it.context.chatId]!!.tsShop}"
                }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                if (contentMessage.content.text.lowercase() == "да") {
                    // получаем воркер из БД
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(allBotUsers[it.context.chatId]!!.tsShop)

                    if (requiredShopWorker == null) {
                        sendMessage(
                            it.context,
                            buildEntities { +"Бота магазина ${allBotUsers[it.context.chatId]!!.tsShop} нет в базе данных" })
                    } else {
                        // меняем стэйт на удаление
                        requiredShopWorker.workerState = WorkerState.DELETE
                        // удаляем из БД
                        botRepositoryDB.deleteWorkerByShop(allBotUsers[it.context.chatId]!!.tsShop)
                        // удалям запущенный воркер
                        BotRepositoryWorkersImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam())

                        sendMessage(
                            it.context,
                            buildEntities { +"Бот магазина ${allBotUsers[it.context.chatId]!!.tsShop} удален" })
                    }

                    DeleteStopState(it.context, it.sourceMessage)

                } else {
                    sendMessage(it.context, buildEntities { +"Отменяем удаление" })
                    DeleteStopState(it.context, it.sourceMessage)
                }
            }

            strictlyOn<DeleteStopState> {
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
                    startChain(UserExpectLogin(it.chat.id, it))
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

            onCommand("create", initialFilter = { stateUser[it.chat.id.chatId] == null }) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    val currentUser = allBotUsers[it.chat.id.chatId]
                    val requiredShopWorker = botRepositoryDB.checkWorker(currentUser!!.tsShop)
                    if (requiredShopWorker == null) {
                        sendTextMessage(
                            it.chat,
                            "Создаем чат-бота магазина. " +
                                    "Для работы данный бот ${getMe().username.username} " +
                                    "должен быть добавлен в группу в которую он будет скидывать информацию."
                        )
                        startChain(BotExpectChatId(it.chat.id, it))
                    } else {
                        sendTextMessage(
                            it.chat,
                            "Чат-бот магазина ${currentUser.tsShop} уже существует. " +
                                    if (requiredShopWorker.tgUserId == currentUser.tgUserId) "Его создали Вы с логином в TS ${requiredShopWorker.tsLogin}."
                                    else "Его создал пользователь TS с логином ${requiredShopWorker.tsLogin}."
                        )
                    }
                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onCommand("delete", initialFilter = { stateUser[it.chat.id.chatId] == null }) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    val currentUser = allBotUsers[it.chat.id.chatId]

                    // получаем воркер из БД
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(currentUser!!.tsShop)

                    if (requiredShopWorker != null) {

                        if (requiredShopWorker.ownerTgId == currentUser.tgUserId) {
                            sendTextMessage(
                                it.chat,
                                "Удаляем чат-бота магазина ${currentUser.tsShop}. "
                            )
                            startChain(DeleteExpectConfirmation(it.chat.id, it))
                        } else {
                            sendTextMessage(
                                it.chat,
                                "Чат бота магазина ${currentUser.tsShop} создали не Вы. Обратитесь к пользователю TS ${requiredShopWorker.login}"
                            )
                        }

                    } else {
                        sendTextMessage(
                            it.chat,
                            "Чат-бота магазина ${currentUser.tsShop} пока не существует. " +
                                    "Вы можете его создать командой /create"
                        )
                    }
                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onCommandWithArgs("test") { it, myContent ->
//                botRepositoryDB.setBy(botUser = botUser[it.context.chatId]!!)
                println(botRepositoryDB.checkWorker(myContent.first()))
            }

            Logging.i(tag, "Telegram Bot started! ${getMe()}")
        }.start()


    }
}