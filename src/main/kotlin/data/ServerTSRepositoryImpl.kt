package data

import data.restTS.models.*
import data.restTS.net.NetClient
import domain.models.OrderListSimple
import domain.repository.ServerTSRepository
import utils.Logging
import java.time.LocalDateTime
import java.time.ZoneId

class ServerTSRepositoryImpl : ServerTSRepository {
    private val tag = this::class.java.simpleName
    private var loginTime = LocalDateTime.now()
    val netClient = NetClient()
    override suspend fun login(login: String, password: String, werk: String, gmt: String): LoginResult {
        val ver = netClient.getDBVersion(werk) ?: 0
        if (ver > netClient.dbVersion.toInt()) netClient.dbVersion = ver.toString()
        if (netClient.login(login, password, werk, gmt)) {
            Logging.i(tag, "${netClient.shop} Connected to base ${netClient.userInfo}")
            loginTime = LocalDateTime.now(ZoneId.of(gmt))

            Logging.d(tag, "$werk GMT: ${gmt} LocalTime: ${loginTime}")
//            netClient.getDBVersion()
            return LoginResult(
                result = Result(success = true, errorMessage = null, errorCode = netClient.errorCode),
                userInfo = netClient.userInfo
            )
        } else {
            Logging.e(tag, "${netClient.shop} Login failed with Error: ${netClient.error}")
            return LoginResult(
                result = Result(success = false, errorMessage = netClient.error, errorCode = netClient.errorCode),
                userInfo = null
            )
        }
    }

    override suspend fun getItems(orderId: String?): List<Items> {
        val orderItems = netClient.getWebOrderDetail(orderId, "WRQST")
        if (orderItems == null) return emptyList() else return orderItems.items
    }

    override var errorCode: Int?
        get() = netClient.errorCode
        set(value) {
            netClient.errorCode = value
        }

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
            errorCode = netClient.errorCode ?: 0,
            error = netClient.error,
            listWebOrdersSimply = orderListSimple
        )
    }
}