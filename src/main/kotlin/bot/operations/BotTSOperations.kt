package bot.operations

import bot.models.BotUser
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.types.Identifier
import restTS.models.LoginResult
import restTS.models.Result
import restTS.models.WebOrder
import restTS.models.WebOrderResult


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

//    suspend fun getWebOrderDetail(userId: Identifier, orderId: String): WebOrderResult {
//
//    }

    suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult {
        val checkResult = checkOrMakeUserInstance(userId) //если инстанса юзера нет, то берем данные из БД и создаем
        if (!checkResult.success) return WebOrderResult(checkResult, WebOrder())


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
            return WebOrderResult(
                Result(
                    false,
                    webOrder.result.errorMessage,
                    webOrder.result.errorCode
                ), WebOrder()
            )
        }

        return webOrder
    }

    private suspend fun checkOrMakeUserInstance(userId: Identifier): Result {
        if (!botTSRepository.checkUserInstance(userId)) { // если инстанса юзера нет, то создаем
            val botUserData = botRepositoryDB.getUserBy(userId) // берем данные юзера из БД
            if (botUserData != null) {
                val loginResult = checkUserDataInTS(botUserData, userId)
                if (!loginResult.result.success) return Result(
                    false,
                    loginResult.result.errorMessage,
                    loginResult.result.errorCode
                )
            } else return Result(false, null, 403)
        }
        return Result(true, null, null)
    }
}