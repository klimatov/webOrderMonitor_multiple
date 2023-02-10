package data

import data.restTS.models.Items
import data.restTS.models.RemainsLocal
import data.restTS.models.WebOrder
import data.restTS.net.NetClient
import domain.models.OrderListSimple
import domain.repository.ServerTSRepository
import utils.Logging
import java.time.LocalDateTime

class ServerTSRepositoryImpl: ServerTSRepository {
    private val tag = this::class.java.simpleName
    private var loginTime = LocalDateTime.now()
    val netClient = NetClient()
    override suspend fun login(login: String, password: String, werk: String) {
        if (netClient.login(login, password, werk)) {
            Logging.i(tag, "Connected to base ${netClient.userInfo}")
            loginTime = LocalDateTime.now()
            netClient.getDBVersion()
        } else {
            Logging.e(tag, "Login failed with Error: ${netClient.error}")
            //exitProcess(999)
        }
    }

    override suspend fun getItems(orderId: String?): List<Items> {
        val orderItems = netClient.getWebOrderDetail(orderId, "WRQST")
        if (orderItems == null) return emptyList() else return orderItems.items
    }

    override var errorCode: Int?
        get() = netClient.errorCode
        set(value) {netClient.errorCode = value}

    override suspend fun getRemains(goodCode: String?): List<RemainsLocal> {
        val remains = netClient.localRemains(goodCode)
        if (remains == null) return emptyList() else return remains
    }

    override suspend fun getOrderList(webNum: String?): List<WebOrder> {
        val orderList = netClient.getWebOrderList("new", webNum)
        if (orderList == null) return emptyList() else return orderList.webOrders
    }

    override suspend fun getOrderListSimple(): OrderListSimple? {
        val orderListSimple = netClient.getWebOrderListSimple("new") //all or new  --- получаем список неподтвержденных
        return OrderListSimple(
            errorCode = netClient.errorCode?:0,
            error = netClient.error,
            listWebOrdersSimply = orderListSimple
        )
    }
}