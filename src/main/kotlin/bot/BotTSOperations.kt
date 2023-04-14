package bot

import data.restTS.models.LoginResult
import dev.inmo.tgbotapi.types.Identifier


class BotTSOperations(
    private val botTSRepository: BotTSRepository
) {

    suspend fun checkUserDataInTS(botUserData: BotUser?, userId: Identifier): LoginResult {
        return botTSRepository.login(
            botUserData?.tsLogin ?: "",
            botUserData?.tsPassword ?: "",
            botUserData?.tsShop ?: "",
            userId
        )
    }
}