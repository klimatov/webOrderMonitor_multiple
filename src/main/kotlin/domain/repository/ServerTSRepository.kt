package domain.repository

import data.restTS.models.Items
import data.restTS.models.RemainsLocal
import data.restTS.models.UserInfo
import data.restTS.models.WebOrder
import domain.models.OrderListSimple

interface ServerTSRepository {
    var errorCode: Int?
    suspend fun login(login: String, password: String, werk: String): UserInfo?
    suspend fun getItems(orderId: String?): List<Items>
    suspend fun getRemains(goodCode: String?): List<RemainsLocal>
    suspend fun getOrderList(webNum: String?): List<WebOrder>
    suspend fun getOrderListSimple(): OrderListSimple?

}