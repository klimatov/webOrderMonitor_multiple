package bot.repository

import dev.inmo.tgbotapi.types.Identifier
import restTS.models.*

interface BotTSRepository {
    suspend fun getRemoteDbVersion(userId: Identifier): Int
    suspend fun login(login: String, password: String, werk: String, userId: Identifier): LoginResult
    suspend fun getWebOrder(userId: Identifier, webNum: String): WebOrderResult
    suspend fun checkUserInstance(userId: Identifier): Boolean
    suspend fun getWebOrderDetail(userId: Identifier, orderId: String): WebOrderResult
    suspend fun getReasonForIncompliteness(userId: Identifier, orderId: String, itemId: String): ReasonsForIncompletnessResult
    suspend fun getShelfs(userId: Identifier): ShelfsResult
    suspend fun getPrintersList(userId: Identifier): PrintersListResult
    suspend fun saveWebOrder(userId: Identifier, orderType: String, orderId: String, company: String, items: List<SaveItems>, collector: Collector, ordType: String): SaveWebOrderResult
    suspend fun printWebOrder(userId: Identifier, pcName: String?, printOrders: String?, printType: String?): PrintResult
}