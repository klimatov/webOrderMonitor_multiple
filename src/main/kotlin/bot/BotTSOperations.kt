package bot

import dev.inmo.tgbotapi.types.Identifier


class BotTSOperations(
    private val botTSRepository: BotTSRepository
) {

    suspend fun checkUserDataInTS(botUserData: BotUser?, userId: Identifier): Boolean {
        val userResult = botTSRepository.login(botUserData?.tsLogin?:"", botUserData?.tsPassword?:"", botUserData?.tsShop?:"", userId)
        return userResult.result.success
        // FIXME: добавить проброс ошибки
    }
}