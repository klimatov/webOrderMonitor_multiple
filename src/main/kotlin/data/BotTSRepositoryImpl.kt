package data

import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.types.Identifier
import restTS.models.*

class BotTSRepositoryImpl : BotTSRepository {
    private val tag = this::class.java.simpleName

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
            gmt = "+0300" // TODO: подтягивавть GMT юзера
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
            val response = serverTSRepository.getAllOrderList(webNum)

            if (response.isNotEmpty()) {
                val webOrder = response[0]
                val webOrderDetail = serverTSRepository.getWebOrderDetail(webOrder.orderId.toString())
                webOrder.items = webOrderDetail?.items ?: emptyList()


                return WebOrderResult(
                    Result(
                        (serverTSRepository.errorCode == 200),
                        serverTSRepository.errorMessage,
                        serverTSRepository.errorCode
                    ),
                    webOrder ?: WebOrder()
                )

            } else return WebOrderResult(
                Result(false, serverTSRepository.errorMessage, serverTSRepository.errorCode),
                WebOrder()
            )
        } catch (e: Exception) {
            return WebOrderResult(
                Result(false, e.message, null),
                WebOrder()
            )
        }
    }

    override suspend fun getWebOrderDetail(userId: Identifier, orderId: String): WebOrderResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            val webOrderDetail = serverTSRepository.getWebOrderDetail(orderId)
            return WebOrderResult(
                Result(
                    (serverTSRepository.errorCode == 200),
                    serverTSRepository.errorMessage,
                    serverTSRepository.errorCode
                ),
                webOrderDetail?.webOrder?.copy(items = webOrderDetail.items) ?: WebOrder()
            )

        } catch (e: Exception) {
            return WebOrderResult(
                Result(false, e.message, null),
                WebOrder()
            )
        }

    }

    override suspend fun getReasonForIncompliteness(
        userId: Identifier,
        orderId: String,
        itemId: String
    ): ReasonsForIncompletnessResult {
        TODO("Not yet implemented")
    }

    override suspend fun getShelfs(userId: Identifier): ShelfsResult {
        TODO("Not yet implemented")
    }

    override suspend fun getPrintersList(userId: Identifier): PrintersListResult {
        TODO("Not yet implemented")
    }

    override suspend fun saveWebOrder(
        userId: Identifier,
        orderType: String,
        orderId: String,
        company: String,
        items: List<SaveItems>,
        collector: Collector,
        ordType: String
    ): SaveWebOrderResult {
        TODO("Not yet implemented")
    }

    override suspend fun printWebOrder(userId: Identifier) {
        TODO("Not yet implemented")
    }

    override suspend fun checkUserInstance(userId: Identifier): Boolean {
        return serverTSRepositoryInstances.containsKey(userId)
    }
}