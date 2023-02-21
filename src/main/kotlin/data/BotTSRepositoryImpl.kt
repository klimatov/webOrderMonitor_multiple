package data

import bot.BotTSRepository
import data.restTS.models.LoginResult
import dev.inmo.tgbotapi.types.Identifier

class BotTSRepositoryImpl : BotTSRepository {
    // создаем мапу с экземпляром клиента под каждого юзера
    // FIXME: сделать чистку неиспользуемых
    private val serverTSRepositoryInstances: MutableMap<Identifier, ServerTSRepositoryImpl> = mutableMapOf()

    override suspend fun getRemoteDbVersion(userId: Identifier): Int {
        return serverTSRepositoryInstances[userId]?.netClient?.remoteDbVersion?:0
    }

    override suspend fun login(login: String, password: String, werk: String, userId: Identifier): LoginResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        return serverTSRepository.login(
            login = login,
            password = password,
            werk = werk,
            gmt = "+0300"
        )
    }

    private fun serverTSRepositoryInstance(userId: Identifier): ServerTSRepositoryImpl {
        if (!serverTSRepositoryInstances.contains(userId)) serverTSRepositoryInstances[userId] = ServerTSRepositoryImpl()
        return serverTSRepositoryInstances[userId] ?: ServerTSRepositoryImpl()
    }
}