package data

import domain.models.OrderListSimple
import domain.repository.ServerTSRepository
import restTS.models.*
import restTS.net.NetClient
import utils.Logging
import java.time.LocalDateTime
import java.time.ZoneId

class ServerTSRepositoryImpl : ServerTSRepository {
    private val tag = this::class.java.simpleName
    private var loginTime = LocalDateTime.now()
    val netClient = NetClient()
    override val remoteDbVersion: Int
        get() = netClient.remoteDbVersion ?: 0
    override val lastErrorMessage: String
        get() = netClient.errorMessage
    override val lastErrorCode: Int?
        get() = netClient.errorCode

    override var errorCode: Int?
        get() = netClient.errorCode
        set(value) {
            netClient.errorCode = value
        }
    override var errorMessage: String?
        get() = netClient.errorMessage
        set(value) {
            netClient.errorMessage = value ?: ""
        }

    suspend fun renewLogin(): Boolean {
        val ver = netClient.getDBVersion() ?: 0
        if (ver > netClient.dbVersion.toInt()) netClient.dbVersion = ver.toString()
        return netClient.login()
    }

    override suspend fun login(login: String, password: String, werk: String, gmt: String): LoginResult {
        val ver = netClient.getDBVersion(werk) ?: 0
        if (ver > netClient.dbVersion.toInt()) netClient.dbVersion = ver.toString()
        if (netClient.login(login, password, werk, gmt)) {
            Logging.i(tag, "${netClient.shop} Connected to base ${netClient.userInfo}")
            loginTime = LocalDateTime.now()

            Logging.d(tag, "$werk GMT: $gmt LocalTime: ${LocalDateTime.now(ZoneId.of(gmt))}")
//            netClient.getDBVersion()
            return LoginResult(
                result = Result(success = true, errorMessage = null, errorCode = netClient.errorCode),
                userInfo = netClient.userInfo
            )
        } else {
            Logging.e(tag, "${netClient.shop} Login failed with Error: ${netClient.errorMessage}")
            return LoginResult(
                result = Result(
                    success = false,
                    errorMessage = netClient.errorMessage,
                    errorCode = netClient.errorCode
                ),
                userInfo = null
            )
        }
    }

    override suspend fun getItems(orderId: String?): List<Items> {
        val orderItems = netClient.getWebOrderDetail(orderId, "WRQST")
        if (orderItems == null) return emptyList() else return orderItems.items
    }

    override suspend fun getRemains(goodCode: String?): List<RemainsLocal> {
        val remains = netClient.localRemains(goodCode)
        if (remains == null) return emptyList() else return remains
    }

    override suspend fun getNewOrderList(webNum: String?): List<WebOrder> {
        val orderList = netClient.getWebOrderList("new", webNum)
        if (orderList == null) return emptyList() else return orderList.webOrders
    }

    suspend fun getAllOrderList(webNum: String?): List<WebOrder> { // для запроса инфы по вебке
        val orderList = netClient.getWebOrderList("all", webNum)
        if (orderList == null) return emptyList() else return orderList.webOrders
    }

    override suspend fun getOrderListSimple(): OrderListSimple {
        val orderListSimple = netClient.getWebOrderListSimple("new") //all or new  --- получаем список неподтвержденных
        return OrderListSimple(
            errorCode = netClient.errorCode ?: 0,
            error = netClient.errorMessage,
            listWebOrdersSimply = orderListSimple
        )
    }

    suspend fun getWebOrderDetail(orderId: String): WebOrderDetail? {
        return netClient.getWebOrderDetail(orderId, "WRQST,PWRQT,DWRQT")
    }

    fun getReasonForIncompliteness(orderId: String?, itemId: String?): List<ShortageReasonDto> {
        val reasonsList = netClient.getReasonForIncompliteness(orderId = orderId, itemId = itemId)
        return reasonsList ?: emptyList()
    }

    fun getShelfsAll(): List<ShelfItem> {
        val shelfsList = netClient.getShelfsAll()
        return shelfsList ?: emptyList()
    }

    fun getPrintersList(): List<PcNameList> {
        val printersList = netClient.getPrintersList()
        return printersList ?: emptyList()
    }

    fun saveWebOrder(
        orderType: String?,
        orderId: String?,
        company: String?,
        items: List<SaveItems>,
        collector: Collector,
        ordType: String?
    ): SaveWebOrderRes {
        return netClient.saveWebOrder(orderType, orderId, company, items, collector, ordType) ?: SaveWebOrderRes()
    }

    fun sendToPrint(pcName: String?, printOrders: String?, printType: String?) {
        netClient.sendToPrint(pcName, printOrders, printType)
    }


}