package bot.repository

import data.restTS.models.LoginResult
import data.restTS.models.WebOrderResult
import dev.inmo.tgbotapi.types.Identifier

interface BotTSRepository {
    suspend fun getRemoteDbVersion(userId: Identifier): Int
    suspend fun login(login: String, password: String, werk: String, userId: Identifier): LoginResult
    suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult
    suspend fun checkUserInstance(userId: Identifier): Boolean
}