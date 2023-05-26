package bot.operations

import bot.models.BotUser
import bot.repository.BotRepositoryDB
import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.types.Identifier
import restTS.models.*


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
    suspend fun saveWebOrder(
        userId: Identifier,
        orderType: String,
        orderId: String,
        company: String,
        items: List<SaveItems>,
        collector: Collector,
        ordType: String
    ): SaveWebOrderResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return SaveWebOrderResult(checkUserInstanceResult, SaveWebOrderRes())
        return botTSRepository.saveWebOrder(userId, orderType, orderId, company, items, collector, ordType)
    }
    suspend fun getPrintersList(userId: Identifier): PrintersListResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return PrintersListResult(checkUserInstanceResult, emptyList())
        return botTSRepository.getPrintersList(userId)
    }
    suspend fun getShelfs(userId: Identifier): ShelfsResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return ShelfsResult(checkUserInstanceResult, emptyList())
        return botTSRepository.getShelfs(userId)
    }

    suspend fun getReasonForIncompliteness(userId: Identifier, orderId: String, itemId: String): ReasonsForIncompletnessResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return ReasonsForIncompletnessResult(checkUserInstanceResult, emptyList())
        return botTSRepository.getReasonForIncompliteness(userId, orderId, itemId)
    }

    suspend fun getWebOrderDetail(userId: Identifier, orderId: String): WebOrderResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return WebOrderResult(checkUserInstanceResult, WebOrder())
        return botTSRepository.getWebOrderDetail(userId, orderId)
    }

    suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult {
        val checkUserInstanceResult = checkOrMakeUserInstance(userId)
        if (!checkUserInstanceResult.success) return WebOrderResult(checkUserInstanceResult, WebOrder())
        return botTSRepository.getWebOrder(userId, webNum)
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