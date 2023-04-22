package bot.operations

import bot.models.BotUser
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import data.restTS.models.LoginResult
import data.restTS.models.WebOrderResult
import dev.inmo.tgbotapi.types.Identifier
import utils.Logging


class BotTSOperations(
    private val botTSRepository: BotTSRepository,
    private val botRepositoryDB: BotRepositoryDB
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
            if (botUserData != null) botTSRepository.login(
                botUserData?.tsLogin ?: "",
                botUserData?.tsPassword ?: "",
                botUserData?.tsShop ?: "",
                userId
            )
        }

        return botTSRepository.getWebOrder(userId, webNum)
    }
}