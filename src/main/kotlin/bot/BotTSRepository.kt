package bot

import data.restTS.models.LoginResult
import dev.inmo.tgbotapi.types.Identifier

interface BotTSRepository {
    suspend fun login(login: String, password: String, werk: String, userId: Identifier): LoginResult
}