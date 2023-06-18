package bot

import SecurityData.TELEGRAM_BOT_TOKEN
import bot.models.*
import bot.operations.BotTSOperations
import bot.operations.CommandProcessing
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import cache.InMemoryCache
import dev.inmo.micro_utils.crypto.md5
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithParams
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.migrate_from_chat_id
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.new_chat_title
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.Identifier
import dev.inmo.tgbotapi.types.Username
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.Matrix
import dev.inmo.tgbotapi.types.chat.*
import dev.inmo.tgbotapi.types.message.ChatEvents.MigratedToSupergroup
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import domain.models.WorkerState
import domain.orderProcessing.BotMessage
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import restTS.models.WebOrder
import utils.Logging
import java.util.*


class BotCore(
    private val job: CompletableJob,
    private val botRepositoryDB: BotRepositoryDB,
    private val botTSRepository: BotTSRepository,
) {

    private val tag = this::class.java.simpleName

    private val _confirmationDataFlow = MutableSharedFlow<ConfirmationData>(replay = 0)
    val confirmationDataFlow = _confirmationDataFlow.asSharedFlow()

    val botMessage: BotMessage = BotMessage()

    /** Мапа с параметрами магазина <shop, parameters>*/
    val botInstancesParameters: MutableMap<String, BotInstanceParameters> = mutableMapOf()

    private val botToken = TELEGRAM_BOT_TOKEN
    private val bot = telegramBot(token = botToken)
    private var botName = Username("@")

    private val botTSOperations = BotTSOperations(botTSRepository = botTSRepository, botRepositoryDB = botRepositoryDB)
    private var allBotUsers: MutableMap<Identifier, BotUser> = botRepositoryDB.getAll()
    private var newBotUsers: MutableMap<Identifier, BotUser> = mutableMapOf()
    private var newWorkers: MutableMap<Identifier, NewWorker> = mutableMapOf()
    private var stateUser: MutableMap<Identifier, BotState> = mutableMapOf()

    private val commandProcessing = CommandProcessing(
        bot = bot,
        botRepositoryDB = botRepositoryDB,
        botTSRepository = botTSRepository,
        stateUser = stateUser,
        allBotUsers = allBotUsers,
        botInstancesParameters = botInstancesParameters,
        _confirmationDataFlow
    )

    suspend fun start() {
        botName = bot.getMe().username

        val scope = CoroutineScope(Dispatchers.Default + job)
        bot.buildBehaviourWithFSMAndStartLongPolling<BotState>(scope) {

            strictlyOn<UserExpectLogin> {
                newBotUsers[it.context.chatId] = BotUser(
                    tsLogin = "",
                    tsPassword = "",
                    tsShop = "",
                    tgUserId = it.context.chatId,
                    userRole = UserRole.USER.toString(),
                    sapFio = null,
                    sapPosition = null,
                    sapId = null,
                    deviceType = getDeviceModel(),
                    deviceVersion = "11"
                )

                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш логин в TS (буквами)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                val newLogin = contentMessage.content.text.lowercase()
                when {
                    newLogin.toIntOrNull() != null -> {
                        sendMessage(it.context, buildEntities { +"Логин должен состоять из букв" })
                        it
                    }

                    else -> {
                        newBotUsers[it.context.chatId]?.tsLogin = newLogin
                        UserExpectPassword(it.context, it.sourceMessage)
                    }
                }

            }

            strictlyOn<UserExpectPassword> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите ваш пароль в TS (не должен быть пустым)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                newBotUsers[it.context.chatId]?.tsPassword = contentMessage.content.text.md5()
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
                    newBotUsers[it.context.chatId]?.tsShop = contentMessage.content.text
                    UserStopState(it.context, it.sourceMessage)
                }
            }

            strictlyOn<UserStopState> {
                stateUser.remove(it.context.chatId)

                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${newBotUsers[it.context.chatId]?.tsLogin} " +
                            "\nпароль:* " +
                            "\nмагазин:${newBotUsers[it.context.chatId]?.tsShop}"
                )

                // проверка и коннект с апи магазина
                val resultCheckTs = botTSOperations.checkUserDataInTS(newBotUsers[it.context.chatId], it.context.chatId)

                if (it.sourceMessage.content.parseCommandsWithParams().keys.contains("start")) {
                    //внесение в бд, если все ок или запрос по новой если не ок
                    if (resultCheckTs.result.success) {
                        Logging.i(tag, "Create new user ${newBotUsers[it.context.chatId]!!.tsLogin}")
                        // сохраняем информацию, полученную с сервера
                        newBotUsers[it.context.chatId]?.sapFio = resultCheckTs.userInfo?.fio
                        newBotUsers[it.context.chatId]?.sapPosition = resultCheckTs.userInfo?.position
                        newBotUsers[it.context.chatId]?.sapId = resultCheckTs.userInfo?.hrCode

                        botRepositoryDB.setUserBy(botUser = newBotUsers[it.context.chatId]!!)
                        allBotUsers[it.context.chatId] = newBotUsers[it.context.chatId]!!

                        sendMessage(
                            it.context,
                            buildEntities { +"Пользователь создан. Доступные команды по команде /start" })

                    } else {
                        sendMessage(
                            it.context,
                            "Проверка на сервере компании НЕ пройдена. " +
                                    errorResultMessage(resultCheckTs.result.errorMessage ?: "")
                        )
                    }
                } else {        // /update
                    if (resultCheckTs.result.success) {
                        val oldShop = allBotUsers[it.context.chatId]!!.tsShop
                        Logging.i(tag, "Update user data ${newBotUsers[it.context.chatId]!!.tsLogin}")

                        // сохраняем информацию, полученную с сервера
                        newBotUsers[it.context.chatId]?.sapFio = resultCheckTs.userInfo?.fio
                        newBotUsers[it.context.chatId]?.sapPosition = resultCheckTs.userInfo?.position
                        newBotUsers[it.context.chatId]?.sapId = resultCheckTs.userInfo?.hrCode

                        botRepositoryDB.setUserBy(botUser = newBotUsers[it.context.chatId]!!)
                        allBotUsers[it.context.chatId] = newBotUsers[it.context.chatId]!!

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
                                BotWorkersRepositoryImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam()) // удалям запущенный воркер
                                sendMessage(it.context, buildEntities {
                                    +"Созданный Вами ранее бот магазина $oldShop удален, т.к. Вы изменили магазин"
                                })
                            } else { // если магазин не менялся, то обновляем воркер
                                Logging.i(tag, "Update worker in DB $oldShop")
                                requiredShopWorker.workerState = WorkerState.UPDATE // меняем стэйт на обновление
                                requiredShopWorker.login = allBotUsers[it.context.chatId]!!.tsLogin
                                requiredShopWorker.password = allBotUsers[it.context.chatId]!!.tsPassword
                                botRepositoryDB.updateWorkerBy(requiredShopWorker) // обновляем воркер в БД
                                BotWorkersRepositoryImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam()) // обновляем запущенный воркер
                                sendMessage(it.context, buildEntities {
                                    +"Данные пользователя TS в чат-боте магазина $oldShop обновлены"
                                })
                            }
                        }
                    } else {
                        sendMessage(
                            it.context,
                            "Проверка на сервере компании НЕ пройдена. " +
                                    "Данные пользователя TS НЕ обновлены. " +
                                    errorResultMessage(resultCheckTs.result.errorMessage ?: "")
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
                    workerState = WorkerState.CREATE,
                    gmt = "+0300",
                    deviceType = allBotUsers[it.context.chatId]!!.deviceType,
                    deviceVersion = allBotUsers[it.context.chatId]!!.deviceVersion
                )
                stateUser[it.context.chatId] = it
                send(it.context) {
                    +"Введите ID чата в который добавлен бот и куда он будет скидывать информацию " +
                            "(или введите /stop для отмены создания)" +
                            "\nДанный ID должен быть со знаком минус (-). " +
                            "Узнать ID можно введя команду /id В ЧАТЕ куда добавлен бот."
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

                    contentMessage.text == it.context.chatId.toString() -> {
                        send(it.context) { +"Вы ввели свой личный chat ID, а нужен chat ID группы" }
                        it
                    }

                    else -> {
                        if (contentMessage.text?.toLongOrNull() != null) {
                            var newChatId = contentMessage.text?.toLong() ?: 0
                            if (newChatId > 0) newChatId *= -1 // добавляем минус, если его нет
                            try {
                                val msgChatId = ChatId(newChatId)
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
                                newWorkers[it.context.chatId]?.telegramChatId = newChatId
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


                when (contentMessage.content.text.toIntOrNull()) {
                    in 0..23 -> {
                        newWorkers[it.context.chatId]?.shopOpen = contentMessage.text!!.toInt()
                        BotExpectCloseTime(it.context, it.sourceMessage)
                    }

                    else -> {
                        sendMessage(
                            it.context,
                            buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                        it
                    }
                }
            }

            strictlyOn<BotExpectCloseTime> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите время закрытия магазина (в часах, например 21)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                when (contentMessage.content.text.toIntOrNull()) {
                    in 0..23 -> {
                        newWorkers[it.context.chatId]?.shopClose = contentMessage.text!!.toInt()
                        BotExpectTimezone(it.context, it.sourceMessage)
                    }

                    else -> {
                        sendMessage(
                            it.context,
                            buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                        it
                    }
                }
            }

            strictlyOn<BotExpectTimezone> {
                stateUser[it.context.chatId] = it
                send(it.context) { +"Введите часовой пояс по гринвичу GMT (в часах, например Москва 3, Красноярск 7)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()

                val gmtNew = contentMessage.content.text.toIntOrNull()
                when (gmtNew) {
                    in 0..12 -> {
                        val gmtNewTxt = if (gmtNew!! < 10) "0$gmtNew" else gmtNew.toString()
                        newWorkers[it.context.chatId]?.gmt = "+${gmtNewTxt}00"
                        BotStopState(it.context, it.sourceMessage)
                    }

                    else -> {
                        sendMessage(
                            it.context,
                            buildEntities { +"Некорректное значение '${contentMessage.content.text}'" })
                        it
                    }
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
                    BotWorkersRepositoryImpl.changedWorkers.add(newWorkers[it.context.chatId]!!.mapToShopWorkersParam())
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
                        BotWorkersRepositoryImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam())

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

                newBotUsers[it.context.chatId] = allBotUsers[it.context.chatId]!!

                send(it.context) { +"Введите ваш новый пароль в TS (не должен быть пустым)" }
                val contentMessage = waitTextMessage().filter { message ->
                    message.sameChat(it.sourceMessage)
                }.first()
                newBotUsers[it.context.chatId]?.tsPassword = contentMessage.content.text
                PasswordUpdateStopState(it.context, it.sourceMessage)
            }

            strictlyOn<PasswordUpdateStopState> {
                stateUser.remove(it.context.chatId)

                sendMessage(
                    it.context,
                    "Проверяем: \nлогин в TS:${newBotUsers[it.context.chatId]?.tsLogin} " +
                            "\nпароль:${newBotUsers[it.context.chatId]?.tsPassword} " +
                            "\nмагазин:${newBotUsers[it.context.chatId]?.tsShop}"
                )
                // проверка и коннект с апи магазина
                val resultCheckTs = botTSOperations.checkUserDataInTS(newBotUsers[it.context.chatId], it.context.chatId)

                //внесение в бд, если все ок
                if (resultCheckTs.result.success) {
                    sendMessage(it.context, "Пароль пользователя TS изменен!")
                    Logging.i(tag, "Update password user ${newBotUsers[it.context.chatId]!!.tsLogin}")

                    // сохраняем информацию, полученную с сервера
                    newBotUsers[it.context.chatId]?.sapFio = resultCheckTs.userInfo?.fio
                    newBotUsers[it.context.chatId]?.sapPosition = resultCheckTs.userInfo?.position
                    newBotUsers[it.context.chatId]?.sapId = resultCheckTs.userInfo?.hrCode

                    botRepositoryDB.setUserBy(botUser = newBotUsers[it.context.chatId]!!)
                    allBotUsers[it.context.chatId] = newBotUsers[it.context.chatId]!!

                    // получаем воркер из БД и обновляем его
                    val requiredShopWorker = botRepositoryDB.getWorkerByShop(allBotUsers[it.context.chatId]!!.tsShop)
                    if (requiredShopWorker != null) {


                        if (requiredShopWorker.ownerTgId == it.context.chatId) {
                            newWorkers[it.context.chatId] = NewWorker(
                                workerId = requiredShopWorker.workerId,
                                login = newBotUsers[it.context.chatId]!!.tsLogin,
                                password = newBotUsers[it.context.chatId]!!.tsPassword,
                                shop = requiredShopWorker.shop,
                                ownerTgId = requiredShopWorker.ownerTgId,
                                isActive = requiredShopWorker.isActive,
                                shopOpen = requiredShopWorker.shopOpen,
                                shopClose = requiredShopWorker.shopClose,
                                telegramChatId = requiredShopWorker.telegramChatId,
                                workerState = WorkerState.UPDATE,
                                gmt = requiredShopWorker.gmt,
                                deviceType = requiredShopWorker.deviceType,
                                deviceVersion = requiredShopWorker.deviceVersion
                            )
                            Logging.i(tag, "Update worker in DB ${newWorkers[it.context.chatId]!!.shop}")
                            // обновляем воркер в БД
                            botRepositoryDB.updateWorkerBy(newWorkers[it.context.chatId]!!)
                            // Сохраняем новый/изменившийся воркер для создания/обновления
                            BotWorkersRepositoryImpl.changedWorkers.add(newWorkers[it.context.chatId]!!.mapToShopWorkersParam())
                            sendMessage(
                                it.context,
                                buildEntities { +"Пароль пользователя TS в чат-боте магазина ${allBotUsers[it.context.chatId]!!.tsShop} обновлен" })
                        }
                    }
                    null
                } else {
                    sendMessage(
                        it.context,
                        "Проверка на сервере компании НЕ пройдена. " +
                                "Пароль TS НЕ обновлен. " +
                                errorResultMessage(resultCheckTs.result.errorMessage ?: "")
                    )
                }
                null
            }


            onCommand(
                "start",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                },
                requireOnlyCommandInMessage = true
            ) {

                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    sendMessage(
                        it.chat,
                        "Доступные команды:" +
                                "\n/info - информация о боте вашего магазина" +
                                "\n/id - ID чата (нужно написать в группе)" +
                                "\n/password - обновить пароль TS" +
                                "\n/update - обновить все данные TS" +
                                "\n/create - создать бота магазина" +
                                "\n/delete - удалить бота магазина" +
                                "\n/start - список команд"
                    )
                } else {
                    sendTextMessage(it.chat, botMessage.descriptionMessage())
                    sendTextMessage(it.chat, "Регистрируем вас")
                    startChain(UserExpectLogin(it.chat.id, it))
                }
            }

            onDeepLink(initialFilter = {
                stateUser[it.first.chat.id.chatId] == null
            }) { (it, deepLink) ->
                delete(it.chat, it.messageId)

                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    commandProcessing.incomingDeepLink(
                        deepLink,
                        it.chat.id,
                        this@buildBehaviourWithFSMAndStartLongPolling
                    )
                } else {
                    sendTextMessage(it.chat, botMessage.descriptionMessage())
                    sendTextMessage(it.chat, "Регистрируем вас")
                    startChain(UserExpectLogin(it.chat.id, it))
                }
            }

            onCommand("id",
                initialFilter = {
                    stateUser[it.chat.id.chatId] == null
                }
            ) {
                sendTextMessage(it.chat, "ID данного чата:")
                sendTextMessage(it.chat, it.chat.id.chatId.toString())
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

                        sendTextMessage(it.chat, botMessage.descriptionMessage())

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

            onCommand(
                "test",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null)
                }
            ) {
                Logging.d(tag, "/test")
//                _confirmationDataFlow.emit(
//                    ConfirmationData(
//                        webNum = "12345",
//                        orderId = "54321",
//                        shop = "A001",
//                        collector = Collector(
//                            hrCode = "401999",
//                            username = "ivanovii"
//                        ),
//                        chatId = it.chat.id,
//                        messageId = 0,
//                        sapFio = "Иван Иванов"
//                        )
//                    )

//                val text = buildEntities {
//                    link("[Подробнее]", makeDeepLink(bot.getMe().username, "22"))
//                }
//                sendTextMessage(
//                    it.chat,
//                    text
//                )
            }

            onNewChatMembers { it ->
                if (it.chatEvent.members.any { it.id == getMe().id }) {
                    Logging.d(tag, "Бота добавили в группу ${it.chat}")
                    sendTextMessage(
                        it.chat,
                        "При создании чат-бота вашего магазина нужно указать ID чата, написав боту ${getMe().username.username}\nID данного чата:"
                    )
                    sendTextMessage(it.chat, it.chat.id.chatId.toString())
                }
            }

            onGroupChatCreated {
                Logging.d(tag, "Создана новая группа с ботом ${it.chat.id.chatId} ${it.new_chat_title.toString()}")
                sendTextMessage(
                    it.chat,
                    "При создании чат-бота вашего магазина нужно указать ID чата, написав боту ${getMe().username.username}\nID данного чата:"
                )
                sendTextMessage(it.chat, it.chat.id.chatId.toString())
            }

            onGroupEvent {
                if (it.chatEvent is MigratedToSupergroup) {
                    Logging.d(
                        tag,
                        "Миграция из группы ${it.migrate_from_chat_id?.chatId} в супергруппу ${it.chat.id.chatId}"
                    )

                    val shop =
                        try {
                            botInstancesParameters.filter { current ->
                                current.value.targetChatId == it.migrate_from_chat_id
                            }.keys.first()
                        } catch (e: Exception) {
                            "ZZZZ"
                        }

                    if (botInstancesParameters.containsKey(shop)) {
                        Logging.d(
                            tag,
                            "Меняем ID группы магазина $shop со старого ${it.migrate_from_chat_id?.chatId} на новый ${it.chat.id.chatId}"
                        )
                        botInstancesParameters[shop]?.targetChatId = it.chat.id

                        val requiredShopWorker =
                            botRepositoryDB.getWorkerByShop(shop)

                        if (requiredShopWorker != null) {
                            Logging.i(tag, "Update worker in DB $shop")
                            requiredShopWorker.workerState = WorkerState.UPDATE // меняем стэйт на обновление
                            requiredShopWorker.telegramChatId = it.chat.id.chatId
                            botRepositoryDB.updateWorkerBy(requiredShopWorker) // обновляем воркер в БД
                            BotWorkersRepositoryImpl.changedWorkers.add(requiredShopWorker.mapToShopWorkersParam()) // обновляем запущенный воркер
                            sendMessage(
                                ChatId(requiredShopWorker.ownerTgId),
                                "Изменился ID чата. Данные магазина $shop обновлены"
                            )
                        }

                    } else {
                        sendTextMessage(
                            it.chat,
                            "ID данной группы изменился. При создании чат-бота вашего магазина нужно указать ID чата, написав боту ${getMe().username.username}\nНовый ID данного чата:"
                        )
                        sendTextMessage(it.chat, it.chat.id.chatId.toString())
                    }
                }
            }

            onCommand(
                "status",
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId)
                }
            ) {
                if (allBotUsers.containsKey(it.chat.id.chatId)) {
                    val serverTimeOnline = botMessage.DateDiff(BotWorkersRepositoryImpl.appStartTime)

                    sendTextMessage(
                        it.chat,
                        "Remote DB version: ${BotWorkersRepositoryImpl.remoteDbVersion}\n" +
                                "Server online: ${serverTimeOnline.days}d ${serverTimeOnline.hours}h ${serverTimeOnline.minutes}m\n" +
                                "Last error code: ${BotWorkersRepositoryImpl.lastErrorCode}\n" +
                                "Last error message: ${BotWorkersRepositoryImpl.lastErrorMessage}\n" +

                                BotWorkersRepositoryImpl.shopWorkersList.joinToString(separator = "") { shopWorkersParam ->
                                    println(shopWorkersParam)
                                    if (shopWorkersParam.workerState == WorkerState.WORK) {
                                        val loginT = botMessage.DateDiff(shopWorkersParam.loginTime)
                                        "${shopWorkersParam.shop} login time: ${loginT.days}d ${loginT.hours}h ${loginT.minutes}m\n"
                                    } else ""
                                }

                    )

                } else {
                    sendTextMessage(it.chat, "Зарегистрируйтесь для использования данной команды")
                }
            }

            onDataCallbackQuery {

                val shop = botInstancesParameters.filter { current ->
                    current.value.targetChatId == it.message?.chat?.id
                }.keys.first()

                Logging.d(
                    tag,
                    "Callback: ${it.data} from ${it.user.firstName} ${it.user.lastName} ${it.user.username?.usernameWithoutAt} " +
                            "Chat: ${it.message?.chat?.id?.chatId} " +
                            "Shop: $shop"
                )
                val popupMsg = when (it.data) {
                    "infoRequest" -> botMessage.popupMessage(
                        botInstancesParameters[shop]?.dayConfirmedCount ?: 0
                    )

                    "error403", "error401" -> "Изменился пароль входа в TradeService администратора " +
                            "чат-бота вашего магазина. " +
                            "Сообщите ему о необходимости обновить пароль написав в личку боту."

                    else -> "Ошибка связи с сервером компании"
                }
                answer(
                    it,
                    popupMsg,
                    showAlert = true
                )
            }

            onContentMessage(
                initialFilter = {
                    (stateUser[it.chat.id.chatId] == null) &&
                            (it.asFromUser()?.user?.id?.chatId == it.chat.id.chatId) &&
                            (allBotUsers.containsKey(it.chat.id.chatId))
                }) { commandProcessing.incomingMessage(it, this@buildBehaviourWithFSMAndStartLongPolling) }

            Logging.i(tag, "Telegram Bot started! ${getMe()}")
        }.start()


    }

    private fun errorResultMessage(errorMessage: String): String {
        return with(errorMessage) {
            when {
                contains("User not found in werk") -> "Не верный пользователь или пароль для магазина ${
                    errorMessage.takeLast(4)
                }"

                contains("has been blocked for 5 minutes, with werk") -> "Пользователь ${
                    errorMessage.removePrefix("This login: ").substringBefore(" ")
                } заблокирован на 5 минут для магазина ${errorMessage.takeLast(4)}. Слишком много попыток входа с неверным паролем."

                else -> "Ответ сервера: $errorMessage"
            }
        }
    }

    suspend fun botSendMessage(webOrder: WebOrder?, shop: String): Long {
        try {
            delInfoMsg(shop)

            val messageId = bot.sendMessage(
                botInstancesParameters[shop]!!.targetChatId,
                botMessage.inworkMessage(webOrder, gmt = botInstancesParameters[shop]!!.gmt, botName),
                disableWebPagePreview = true,
                disableNotification = !(botInstancesParameters[shop]?.msgNotification ?: true),
                replyMarkup = makeBottomButtons(webOrder, shop)
            ).messageId
            webOrder?.messageId = messageId
            val bottomButtons = makeBottomButtons(webOrder, shop)
            bot.editMessageReplyMarkup(
                botInstancesParameters[shop]!!.targetChatId,
                messageId,
                bottomButtons
            )

            botInstancesParameters[shop]!!.currentInfoMsg = bottomButtons
            botInstancesParameters[shop]!!.currentInfoMsgId = messageId

            return messageId

        } catch (e: Exception) {
            Logging.e(tag, "$shop Exception: ${e.stackTraceToString()}}")
            return botInstancesParameters[shop]?.currentInfoMsgId ?: 0
        }
    }

    private fun makeBottomButtons(webOrder: WebOrder?, shop: String): InlineKeyboardMarkup {
        val confirmButton = botMessage.confirmButton(webOrder, botName)
        val emptyKeyboard = InlineKeyboardMarkup(listOf())
        val currentInfoMsg = botInstancesParameters[shop]!!.currentInfoMsg
        val infoButton =
            if (currentInfoMsg == null) emptyKeyboard else InlineKeyboardMarkup(listOf(currentInfoMsg.keyboard.last()))
        val resultButtons: InlineKeyboardMarkup = confirmButton + infoButton
        return resultButtons
    }

//    suspend fun botConfirmMessage(webOrder: WebOrder?, shop: String) {
//
//
//        ///!!!!!!!!!!!!!!!!!!!!!!!!!
//        try {
//            val emptyKeyboard = InlineKeyboardMarkup(listOf())
//            val currentInfoMsg = botInstancesParameters[shop]!!.currentInfoMsg
//            val resultButtons =
//                if (webOrder?.messageId == botInstancesParameters[shop]!!.currentInfoMsgId) { // если подтвердили сообщ. с инфокнопкой
//                    if (currentInfoMsg == null) emptyKeyboard else { // если текущее сообщение имело инфокнопку
//                        val infoButton =
//                            InlineKeyboardMarkup(listOf(currentInfoMsg.keyboard.last())) // запоминаем инфокнопку
//                        botInstancesParameters[shop]!!.currentInfoMsg = infoButton // сохраняем текущую инфокнопку
//                        infoButton // возвращаем инфокнопку для вывода в телегу
//                    }
//                } else null
//            bot.editMessageText(
//                chatId = botInstancesParameters[shop]!!.targetChatId,
//                messageId = webOrder?.messageId ?: 0,
//                entities = botMessage.completeMessage(webOrder, gmt = botInstancesParameters[shop]!!.gmt, botName),
//                replyMarkup = resultButtons,
//                disableWebPagePreview = true
//            )
//        } catch (e: Exception) {
//            Logging.e(tag, "$shop Exception: ${e.stackTraceToString()}}")
//        }
//    }

    suspend fun botUpdateMessage(webOrder: WebOrder?, shop: String) {
        try {
            val emptyKeyboard = InlineKeyboardMarkup(listOf())
            val newInfoButton = inlineKeyboard {
                row {
                    dataButton(
                        text = botMessage.infoMessage(
                            botInstancesParameters[shop]!!.notConfirmedOrders,
                            botInstancesParameters[shop]!!.gmt
                        ),
                        data = "infoRequest"
                    )
                }
            }
            val currentInfoMsg = botInstancesParameters[shop]!!.currentInfoMsg

            val processingDocStatus = "WRQST_CRTD"

            val confirmButton =
                if (webOrder?.docStatus == processingDocStatus) {
                    botMessage.confirmButton(webOrder, botName)
                } else emptyKeyboard

            val infoButton =
                if (webOrder?.messageId == botInstancesParameters[shop]!!.currentInfoMsgId) { // если обрабатываем сообщ. с инфокнопкой
                    val tempInfoButton = currentInfoMsg?.takeLast ?: newInfoButton // для первого запуска
                    botInstancesParameters[shop]!!.currentInfoMsg =
                        confirmButton + tempInfoButton // сохраняем клавиатуру текущей инфокнопки
                    tempInfoButton // возвращаем инфокнопку
                } else emptyKeyboard

            val resultButtons = confirmButton + infoButton

            bot.editMessageText(
                chatId = botInstancesParameters[shop]!!.targetChatId,
                messageId = webOrder?.messageId ?: 0,
                entities = if (webOrder?.docStatus == processingDocStatus) {
                    botMessage.inworkMessage(
                        webOrder,
                        gmt = botInstancesParameters[shop]!!.gmt,
                        botName
                    )
                } else {
                    botMessage.completeMessage(webOrder, gmt = botInstancesParameters[shop]!!.gmt, botName)
                },
                disableWebPagePreview = true,
                replyMarkup = resultButtons
            )
        } catch (e: Exception) {
            Logging.e(tag, "$shop Exception: ${e}")
        }
    }

    private operator fun InlineKeyboardMarkup.plus(addedButtons: Any): InlineKeyboardMarkup {
        return when (addedButtons) {
            is InlineKeyboardMarkup -> InlineKeyboardMarkup(this.keyboard + addedButtons.keyboard)
            is List<*> -> InlineKeyboardMarkup((this.keyboard + listOf(addedButtons)) as Matrix<InlineKeyboardButton>)
            else -> InlineKeyboardMarkup(listOf())
        }
    }

    private val InlineKeyboardMarkup?.dropLast: InlineKeyboardMarkup
        get() {
            return if ((this@dropLast == null) || (this@dropLast.keyboard.isEmpty())) {
                InlineKeyboardMarkup(listOf())
            } else {
                InlineKeyboardMarkup(this@dropLast.keyboard.dropLast(1))
            }
        }

    private val InlineKeyboardMarkup?.takeLast: InlineKeyboardMarkup
        get() {
            return if ((this@takeLast == null) || (this@takeLast.keyboard.isEmpty())) {
                InlineKeyboardMarkup(listOf())
            } else {
                InlineKeyboardMarkup(this@takeLast.keyboard.takeLast(1))
            }
        }


    suspend fun botSendInfoMessage(shop: String) {
        try {
            bot.sendMessage(
                botInstancesParameters[shop]!!.targetChatId,
                botMessage.notificationMessage(
                    botInstancesParameters[shop]!!.msgNotification,
                    botInstancesParameters[shop]!!.dayConfirmedCount
                ),
                disableWebPagePreview = true,
                disableNotification = !(botInstancesParameters[shop]!!.msgNotification)
            ).messageId
        } catch (e: Exception) {
            Logging.e(tag, "$shop Exception: ${e.stackTraceToString()}}")
        }
    }

    private suspend fun doUpdateInfoMsg(shop: String, buttonText: String, buttonData: String) {
        if (botInstancesParameters[shop]?.currentInfoMsgId != null) {
            val currentInfoMsg = botInstancesParameters[shop]!!.currentInfoMsg
            val newInfoMsg = inlineKeyboard {
                row {
                    dataButton(buttonText, buttonData)
                }
            }

            val resultButtons = currentInfoMsg.dropLast + newInfoMsg

            if (resultButtons != botInstancesParameters[shop]!!.currentInfoMsg) {
                try {
                    bot.editMessageReplyMarkup(
                        botInstancesParameters[shop]!!.targetChatId,
                        botInstancesParameters[shop]!!.currentInfoMsgId!!,
                        replyMarkup = resultButtons
                    )
                    botInstancesParameters[shop]!!.currentInfoMsg = resultButtons
                } catch (e: Exception) {
                    Logging.e(tag, "$shop Exception: ${e.stackTraceToString()}}")
                }
            }
        }
    }

    suspend fun updateInfoMsg(shop: String) {
        doUpdateInfoMsg(
            shop = shop,
            buttonText = botMessage.infoMessage(
                botInstancesParameters[shop]!!.notConfirmedOrders,
                botInstancesParameters[shop]!!.gmt
            ),
            buttonData = "infoRequest"
        )
    }

    suspend fun updateErrorInfoMsg(shop: String, errorCode: Int) {
        doUpdateInfoMsg(
            shop = shop,
            buttonText = botMessage.infoErrorMessage(errorCode),
            buttonData = "error$errorCode"
        )
    }

    private suspend fun delInfoMsg(shop: String) {
        try {
            val currentInfoMsg: InlineKeyboardMarkup? = botInstancesParameters[shop]!!.currentInfoMsg
            val buttonsWOInfoMessage = currentInfoMsg.dropLast
            bot.editMessageReplyMarkup(
                botInstancesParameters[shop]!!.targetChatId,
                botInstancesParameters[shop]!!.currentInfoMsgId!!,
                replyMarkup = buttonsWOInfoMessage
            )
        } catch (e: Exception) {
            Logging.e(tag, "$shop Exception: ${e.stackTraceToString()}}")
        }
    }

    private fun getDeviceModel(): String {
        val deviceList = listOf<String>(
            "Infinix Infinix X650B",
            "Infinix Infinix X656",
            "LGE 802LG",
            "LGE LM-Q620",
            "Lenovo Lenovo L78121",
            "Oppo CPH2035",
            "realme RMX2180",
            "Samsung SM-A307FN",
            "Samsung SC-54A",
            "Samsung SM-A7050",
            "Samsung SM-M3070",
            "Samsung SM-N9700",
            "Samsung SC-03L",
            "Samsung SM-T865",
            "Tecno TECNO CC7"
        )
        return deviceList.random()
    }

}