package data

import bot.repository.BotTSRepository
import data.restTS.models.*
import dev.inmo.tgbotapi.types.Identifier

class BotTSRepositoryImpl : BotTSRepository {
    // создаем мапу с экземпляром клиента под каждого юзера
    // FIXME: сделать чистку неиспользуемых
    private val serverTSRepositoryInstances: MutableMap<Identifier, ServerTSRepositoryImpl> = mutableMapOf()

    override suspend fun getRemoteDbVersion(userId: Identifier): Int {
        return serverTSRepositoryInstances[userId]?.netClient?.remoteDbVersion ?: 0
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
        if (!serverTSRepositoryInstances.contains(userId)) serverTSRepositoryInstances[userId] =
            ServerTSRepositoryImpl()
        return serverTSRepositoryInstances[userId] ?: ServerTSRepositoryImpl()
    }

    override suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
//        val response = serverTSRepository.getWebOrderDetail(webNum)
            var webOrder = serverTSRepository.getAllOrderList(webNum)[0]
            val webOrderDetail = serverTSRepository.getWebOrderDetail(webOrder.orderId.toString())
            webOrder.items = webOrderDetail?.items ?: emptyList()
            return if (serverTSRepository.errorCode == null) {
                WebOrderResult(
                    Result(false, null, null),
                    webOrder ?: WebOrder()
                )
            } else {
                WebOrderResult(
                    Result(true, null, serverTSRepository.errorCode),
                    webOrder ?: WebOrder()
                )
            }
        } catch (e: Exception) {
            return WebOrderResult(
                Result(false, e.message, null),
                WebOrder()
            )
        }
    }

    override suspend fun checkUserInstance(userId: Identifier): Boolean {
        return serverTSRepositoryInstances.containsKey(userId)
    }
}