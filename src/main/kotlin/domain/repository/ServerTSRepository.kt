package domain.repository

import data.restTS.models.Items
import data.restTS.models.LoginResult
import data.restTS.models.RemainsLocal
import data.restTS.models.WebOrder
import domain.models.OrderListSimple

interface ServerTSRepository {
    var errorCode: Int?
    var errorMessage: String?
    val remoteDbVersion: Int
    val lastErrorMessage: String
    val lastErrorCode: Int?
    suspend fun login(login: String, password: String, werk: String, gmt: String): LoginResult
    suspend fun getItems(orderId: String?): List<Items>
    suspend fun getRemains(goodCode: String?): List<RemainsLocal>
    suspend fun getNewOrderList(webNum: String?): List<WebOrder>
    suspend fun getOrderListSimple(): OrderListSimple?

}