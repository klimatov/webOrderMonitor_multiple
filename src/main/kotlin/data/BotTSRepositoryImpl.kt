package data

import bot.repository.BotTSRepository
import dev.inmo.tgbotapi.types.Identifier
import restTS.models.*

class BotTSRepositoryImpl : BotTSRepository {
    private val tag = this::class.java.simpleName

    // создаем мапу с экземпляром клиента под каждого юзера
    // FIXME: сделать чистку неиспользуемых
    private val serverTSRepositoryInstances: MutableMap<Identifier, ServerTSRepositoryImpl> = mutableMapOf()

    suspend fun renewLogin(userId: Identifier): Boolean {
        return serverTSRepositoryInstance(userId).renewLogin()
    }

    override suspend fun getRemoteDbVersion(userId: Identifier): Int {
        return serverTSRepositoryInstance(userId).netClient?.remoteDbVersion ?: 0
    }

    override suspend fun login(
        login: String,
        password: String,
        werk: String,
        userId: Identifier,
        deviceType: String,
        deviceVersion: String
    ): LoginResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        return serverTSRepository.login(
            login = login,
            password = password,
            werk = werk,
            gmt = "+0300", // TODO: подтягивавть GMT юзера
            deviceType = deviceType,
            deviceVersion = deviceVersion
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
            var response = serverTSRepository.getAllOrderList(webNum)
            if (serverTSRepository.errorCode == 401) {
                renewLogin(userId)
                response = serverTSRepository.getAllOrderList(webNum)
            }

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
            var webOrderDetail = serverTSRepository.getWebOrderDetail(orderId)
            if (serverTSRepository.errorCode == 401) {
                renewLogin(userId)
                webOrderDetail = serverTSRepository.getWebOrderDetail(orderId)
            }
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
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            val reasonsList = serverTSRepository.getReasonForIncompliteness(orderId, itemId)
            return ReasonsForIncompletnessResult(
                result = Result(
                    success = (serverTSRepository.errorCode == 200),
                    errorMessage = serverTSRepository.errorMessage,
                    errorCode = serverTSRepository.errorCode
                ),
                reasonsList = reasonsList
            )
        } catch (e: Exception) {
            return ReasonsForIncompletnessResult(
                result = Result(success = false, errorMessage = e.message, errorCode = null),
                reasonsList = emptyList()
            )
        }
    }

    override suspend fun getShelfs(userId: Identifier): ShelfsResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            val shelfsList = serverTSRepository.getShelfsAll()
            return ShelfsResult(
                result = Result(
                    success = (serverTSRepository.errorCode == 200),
                    errorMessage = serverTSRepository.errorMessage,
                    errorCode = serverTSRepository.errorCode
                ),
                shelfsList = shelfsList
            )
        } catch (e: Exception) {
            return ShelfsResult(
                result = Result(success = false, errorMessage = e.message, errorCode = null),
                shelfsList = emptyList()
            )
        }
    }

    override suspend fun getPrintersList(userId: Identifier): PrintersListResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            val printersList = serverTSRepository.getPrintersList()
            return PrintersListResult(
                result = Result(
                    success = (serverTSRepository.errorCode == 200),
                    errorMessage = serverTSRepository.errorMessage,
                    errorCode = serverTSRepository.errorCode
                ),
                printersList = printersList
            )
        } catch (e: Exception) {
            return PrintersListResult(
                result = Result(success = false, errorMessage = e.message, errorCode = null),
                printersList = emptyList()
            )
        }
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
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            val saveResult = serverTSRepository.saveWebOrder(orderType, orderId, company, items, collector, ordType)
            return SaveWebOrderResult(
                result = Result(
                    success = (serverTSRepository.errorCode == 200),
                    errorMessage = serverTSRepository.errorMessage,
                    errorCode = serverTSRepository.errorCode
                ),
                saveWebOrder = saveResult
            )
        } catch (e: Exception) {
            return SaveWebOrderResult(
                result = Result(success = false, errorMessage = e.message, errorCode = null),
                saveWebOrder = SaveWebOrderRes()
            )
        }
    }

    override suspend fun printWebOrder(
        userId: Identifier,
        pcName: String?,
        printOrders: String?,
        printType: String?
    ): PrintResult {
        val serverTSRepository = serverTSRepositoryInstance(userId)
        try {
            serverTSRepository.sendToPrint(pcName, printOrders, printType)
            return PrintResult(
                result = Result(
                    success = (serverTSRepository.errorCode == 200),
                    errorMessage = serverTSRepository.errorMessage,
                    errorCode = serverTSRepository.errorCode
                )
            )
        } catch (e: Exception) {
            return PrintResult(
                result = Result(success = false, errorMessage = e.message, errorCode = null),
            )
        }
    }

    override suspend fun checkUserInstance(userId: Identifier): Boolean {
        return serverTSRepositoryInstances.containsKey(userId)
    }
}