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
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onNewChatMembers
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

data class PasswordUpdateExpectPassword(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class PasswordUpdateStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) :
    BotState

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
                stateUser.remove(it.context.chatId)

                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${botUser[it.context.chatId]?.tsLogin} " +
                            "\nпароль:${botUser[it.context.chatId]?.tsPassword} " +
                            "\nмагазин:${botUser[it.context.chatId]?.tsShop}"
                )

                // проверка и коннект с апи магазина
                val resultCheckTs = botRepositoryTS.checkUserDataInTS(botUser[it.context.chatId])

                if (it.sourceMessage.content.parseCommandsWithParams().keys.contains("start")) {
                    //внесение в бд, если все ок или запрос по новой если не ок
                    if (resultCheckTs) {
                        Logging.i(tag, "Create new user ${botUser[it.context.chatId]!!.tsLogin}")
                        botRepositoryDB.setUserBy(botUser = botUser[it.context.chatId]!!)
                        allBotUsers[it.context.chatId] = botUser[it.context.chatId]!!

                        sendMessage(
                            it.context,
                            buildEntities { +"Пользователь создан. Доступные команды по команде /start" })

                    } else UserExpectLogin(it.context, it.sourceMessage)
                } else {        // /update
                    if (resultCheckTs) {
                        val oldShop = allBotUsers[it.context.chatId]!!.tsShop
                        Logging.i(tag, "Update user data ${botUser[it.context.chatId]!!.tsLogin}")
                        botRepositoryDB.setUserBy(botUser = botUser[it.context.chatId]!!)
                        allBotUsers[it.context.chatId] = botUser[it.context.chatId]!!

                        sendMessage(
                            it.context,
                            buildEntities { +"Данные пользователя TS обновлены." })

                        // проверка созданного воркера и его удаление если изменился магазин
                        // получаем воркер из БД
                        val requiredShopWorker =
                            botRepositoryDB.getWorkerByShop(oldShop)

                        if (requiredShopWorker != null) {
                            if (allBotUsers[it.context.chatId]!!.tsShop != oldShop) { // если изменился магазин, то удаляем воркер
                                requiredShopWorker.workerState = WorkerState.DELETE // меняем стэйт на удаление
                                botRepositoryDB.deleteWorkerByShop(oldShop) // удаляем из БД
                                BotRepositoryWorkersImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam()) // удалям запущенный воркер
                                sendMessage(it.context, buildEntities {
                                    +"Созданный Вами ранее бот магазина $oldShop удален, т.к. Вы изменили магазин"
                                })
                            } else { // если магазин не менялся, то обновляем воркер
                                Logging.i(tag, "Update worker in DB $oldShop")
                                requiredShopWorker.workerState = WorkerState.UPDATE // меняем стэйт на обновление
                                requiredShopWorker.login = allBotUsers[it.context.chatId]!!.tsLogin
                                requiredShopWorker.password = allBotUsers[it.context.chatId]!!.tsPassword
                                botRepositoryDB.updateWorkerBy(requiredShopWorker) // обновляем воркер в БД
                                BotRepositoryWorkersImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam()) // обновляем запущенный воркер
                                sendMessage(it.context, buildEntities {
                                    +"Данные пользователя TS в чат-боте магазина $oldShop обновлены"
                                })
                            }
                        }
                    } else {
                        sendMessage(
                            it.context,
                            "Проверка на сервере компании НЕ пройдена. Данные пользователя TS НЕ обновлены."
                        )
                    }
                }
                null
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
                send(it.context) { +"Введите ID чата в который добавлен бот и куда он будет скидывать информацию " +
                        "(или введите /stop для отмены создания)" +
                        "\nДанный ID может быть со знаком минус (-). " +
                        "Узнать ID можно введя команду /getchatid В ЧАТЕ куда добавлен бот."
                         }
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
                send(it.context) { +"Введите время закрытия магазина (в часах, например 21)" }
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


            // /password

            strictlyOn<PasswordUpdateExpectPassword> {
                stateUser[it.context.chatId] = it

                botUser[it.context.chatId] = allBotUsers[it.context.chatId]!!

                send(it.context) { +"Введите ваш пароль в TS (не должен быть пустым)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                botUser[it.context.chatId]?.tsPassword = contentMessage.content.text
                PasswordUpdateStopState(it.context, it.sourceMessage)
            }

            strictlyOn<PasswordUpdateStopState> {
                stateUser.remove(it.context.chatId)

                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${botUser[it.context.chatId]?.tsLogin} " +
                            "\nпароль:${botUser[it.context.chatId]?.tsPassword} " +
                            "\nмагазин:${botUser[it.context.chatId]?.tsShop}"
                )
                // проверка и коннект с апи магазина
                val resultCheckTs = botRepositoryTS.checkUserDataInTS(botUser[it.context.chatId])

                //внесение в бд, если все ок
                if (resultCheckTs) {
                    sendMessage(it.context, "Пароль пользователя TS изменен!")
                    Logging.i(tag, "Update password user ${botUser[it.context.chatId]!!.tsLogin}")
                    botRepositoryDB.setUserBy(botUser = botUser[it.context.chatId]!!)
                    allBotUsers[it.context.chatId] = botUser[it.context.chatId]!!

                    // получаем воркер из БД и обновляем его
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(allBotUsers[it.context.chatId]!!.tsShop)
                    if (requiredShopWorker != null) {


                        if (requiredShopWorker.ownerTgId == it.context.chatId) {
                            newWorkers[it.context.chatId] = NewWorker(
                                workerId = requiredShopWorker.workerId,
                                login = botUser[it.context.chatId]!!.tsLogin,
                                password = botUser[it.context.chatId]!!.tsPassword,
                                shop = requiredShopWorker.shop,
                                ownerTgId = requiredShopWorker.ownerTgId,
                                isActive = requiredShopWorker.isActive,
                                shopOpen = requiredShopWorker.shopOpen,
                                shopClose = requiredShopWorker.shopClose,
                                telegramChatId = requiredShopWorker.telegramChatId,
                                workerState = WorkerState.UPDATE
                            )
                            Logging.i(tag, "Update worker in DB ${newWorkers[it.context.chatId]!!.shop}")
                            // обновляем воркер в БД
                            botRepositoryDB.updateWorkerBy(newWorkers[it.context.chatId]!!)
                            // Сохраняем новый/изменившийся воркер для создания/обновления
                            BotRepositoryWorkersImpl.changedWorkers.add(newWorkers[it.context.chatId]!!.mapToShopWorkersParam())
                            sendMessage(
                                it.context,
                                buildEntities { +"Пароль пользователя TS в чат-боте магазина ${allBotUsers[it.context.chatId]!!.tsShop} обновлен" })
                        }
                    }
                    null
                } else sendMessage(it.context, "Проверка в базе TS прошла неудачно, пароль НЕ изменен")
                null
            }


            onCommand(
                "start",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {

                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    sendMessage(
                        it.chat,
                        "Доступные команды:" +
                                "\n/start - список команд" +
                                "\n/getchatid - ваш telegram ID" +
                                "\n/password - обновить пароль TS" +
                                "\n/update - обновить все данные TS" +
                                "\n/create - создать бота магазина" +
                                "\n/delete - удалить бота магазина" +
                                "\n/info - информация о боте вашего магазина"
                    )
                } else {
                    sendTextMessage(it.chat, "Регистрируем вас")
                    startChain(UserExpectLogin(it.chat.id, it))
                }
            }

            onCommand("getchatid",
                initialFilter = {
                    stateUser[it.chat.id.chatId] == null
                }
            ) {
                val userId = it.asFromUser()?.user?.id?.chatId
                val chatId = it.chat.id.chatId
                sendTextMessage(it.chat, "Твой user ID: $userId\nТекущий chat ID: $chatId")
            }

            onCommand("lasterrors",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                sendTextMessage(it.chat, "Последние ошибки:")
                sendTextMessage(chat = it.chat, text = InMemoryCache.getErrors())
            }

            onCommand("create",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    val currentUser = allBotUsers[it.chat.id.chatId]
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(currentUser!!.tsShop)
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
                                    if (requiredShopWorker.ownerTgId == currentUser.tgUserId) "Его создали Вы с логином в TS ${requiredShopWorker.login}."
                                    else "Его создал пользователь TS с логином ${requiredShopWorker.login}."
                        )
                    }
                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onCommand("delete",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
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

            onCommand("info",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    val currentUser = allBotUsers[it.chat.id.chatId]

                    // получаем воркер из БД
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(currentUser!!.tsShop)

                    if (requiredShopWorker != null) {

                        sendMessage(
                            it.chat,
                            "Информация о боте магазина ${requiredShopWorker.shop}:" +
                                    "\nСоздан пользователем TS: ${requiredShopWorker.login}" +
                                    "\nВремя открытия магазина: ${requiredShopWorker.shopOpen}:00" +
                                    "\nВремя закрытия магазина: ${requiredShopWorker.shopClose}:00"
                        )
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

            onCommand(
                "password",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    sendTextMessage(
                        it.chat,
                        "Обновляем пароль пользователя TS ${allBotUsers[it.chat.id.chatId]!!.tsLogin}"
                    )
                    startChain(PasswordUpdateExpectPassword(it.chat.id, it))
                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onCommand(
                "update",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    sendTextMessage(
                        it.chat,
                        "Обновляем данные пользователя TS"
                    )
                    startChain(UserExpectLogin(it.chat.id, it))
                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onNewChatMembers { it ->
                it.chatEvent.members.forEach { println(it.id.chatId) }
                println(getMe().id.chatId.toString())
                if (it.chatEvent.members.any { it.id == getMe().id }) {
                    Logging.d(tag, "Бота добавили в группу ${it.chat}")
                }

                sendTextMessage(
                    it.chat,
                    "ID данного чата: ${it.chat.id.chatId}" +
                            "\nЕго нужно указать при создании чат-бота вашего магазина написав боту ${getMe().username.username}"
                )
            }


            onCommandWithArgs("test",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) { it, myContent ->

            }

            Logging.i(tag, "Telegram Bot started! ${getMe()}")
        }.start()


    }
}