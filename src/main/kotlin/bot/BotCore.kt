package bot

import cache.InMemoryCache
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import orderProcessing.data.SecurityData.TELEGRAM_BOT_TOKEN
import utils.Logging

class BotCore(private val job: CompletableJob) {
    private val tag = this::class.java.simpleName
    private val botToken = TELEGRAM_BOT_TOKEN
    private val bot = telegramBot(token = botToken)

    suspend fun start() {
        val scope = CoroutineScope(Dispatchers.Default + job)
        bot.buildBehaviourWithLongPolling(scope){

            onCommand("start") {
                sendTextMessage(it.chat, "Стартуем!")
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

            Logging.i(tag,"Telegram Bot started! ${getMe()}")
        }.start()


    }
}