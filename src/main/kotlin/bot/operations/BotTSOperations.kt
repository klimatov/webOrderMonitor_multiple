package bot.operations

import bot.models.BotUser
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import data.restTS.models.LoginResult
import data.restTS.models.Result
import data.restTS.models.WebOrder
import data.restTS.models.WebOrderResult
import dev.inmo.tgbotapi.types.Identifier
import utils.Logging


class BotTSOperations(
    private val botTSRepository: BotTSRepository,
    private val botRepositoryDB: BotRepositoryDB,
) {
    private val tag = this::class.java.simpleName
    suspend fun checkUserDataInTS(botUserData: BotUser?, userId: Identifier): LoginResult {
        return botTSRepository.login(
            botUserData?.tsLogin ?: "",
            botUserData?.tsPassword ?: "",
            botUserData?.tsShop ?: "",
            userId
        )
    }

    suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult {
        if (!botTSRepository.checkUserInstance(userId)) { //если инстанса юзера нет, то берем данные из БД и создаем
            Logging.d(tag, "Создаем инстанс юзера")
            val botUserData = botRepositoryDB.getUserBy(userId)
            Logging.d(tag, "Данные юзера из БД: $botUserData")
            if (botUserData != null) {
                val loginResult = checkUserDataInTS(botUserData, userId)
                if (!loginResult.result.success) return WebOrderResult(
                    Result(
                        false,
                        loginResult.result.errorMessage,
                        loginResult.result.errorCode
                    ), WebOrder()
                )
            } else return WebOrderResult(Result(false, null, 403), WebOrder())
        }

        var webOrder = botTSRepository.getWebOrder(userId, webNum)

        if (webOrder.result.errorCode == 401) {
            val botUserData = botRepositoryDB.getUserBy(userId)
            if (botUserData != null) {
                val loginResult = checkUserDataInTS(botUserData, userId)
                if (loginResult.result.success) {
                    webOrder = botTSRepository.getWebOrder(userId, webNum)
                }
            }

        }

        if (webOrder.result.errorCode != 200) {
//            val botUserData = botRepositoryDB.getUserBy(userId)
//            checkUserDataInTS(botUserData, userId)
            return WebOrderResult(Result(
                false,
                webOrder.result.errorMessage,
                webOrder.result.errorCode
            ), WebOrder())
        }

        return webOrder
    }
}