package domain.repository

import data.restTS.data.Items
import data.restTS.data.RemainsLocal
import data.restTS.data.WebOrder
import data.restTS.data.WebOrderSimply
import domain.models.OrderListSimple

interface ServerTSRepository {
    var errorCode: Int?
    suspend fun login(login: String, password: String, werk: String)
    suspend fun getItems(orderId: String?): List<Items>
    suspend fun getRemains(goodCode: String?): List<RemainsLocal>
    suspend fun getOrderList(webNum: String?): List<WebOrder>
    suspend fun getOrderListSimple(): OrderListSimple?

}