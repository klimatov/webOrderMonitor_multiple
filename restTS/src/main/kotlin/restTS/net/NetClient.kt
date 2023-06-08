package restTS.net

import com.google.gson.Gson
import com.google.gson.JsonArray
import restTS.models.*
import utils.Logging
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class NetClient {
    private val tag = this::class.java.simpleName

    //private var baseURL: String
    private var token = ""
    var shop = ""
    lateinit var userInfo: UserInfo
    var errorMessage = ""
    var errorCode: Int? = null
    var dbVersion = "0"
    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault())
    var gmt = SimpleDateFormat("Z").format(calendar.time) // +0700
    private var _login: String = ""
    private var _password: String = ""
    private var _deviceType = "realme RMX2180"
    private var _deviceVersion = "11"
    var remoteDbVersion: Int? = null
//    var timeZone = TimeZone.getTimeZone("GMT+07:00")
//    val gmt = timeZone.rawOffset.toString()

    //хэшилка пароля для запроса
    private fun md5Hash(str: String): String {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")
        val bytes = str.toByteArray(Charsets.UTF_8)
        val bigInteger = BigInteger(1, instance.digest(bytes))
        val format = String.format("%032x", *Arrays.copyOf<Any>(arrayOf<Any>(bigInteger), 1))
        return format
    }

    fun getDBVersion(werk: String = this.shop): Int? {
        this.shop = werk
        try {
            val response = RetrofitInstance.eldoApi.getDBVersion(shop)?.execute()
            this.errorCode = response?.code()
            //Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")
            if (this.errorCode == 200) {
                val responseJson = Gson().fromJson(response?.body(), DbVersion::class.java)
                this.remoteDbVersion = responseJson.version
                Logging.d(tag, "${this.shop} DB version: ${responseJson.version}")
                return responseJson.version
            } else {
                return 0
            }
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return 0
        }
    }

    fun login(
        login: String = this._login,
        password: String = this._password,
        werk: String = this.shop,
        gmt: String = this.gmt,
        deviceType: String = this._deviceType,
        deviceVersion: String = this._deviceVersion
    ): Boolean {
        this.shop = werk
        this.gmt = gmt
        this._login = login
        this._password = password

        val values =
            hashMapOf(
                "login" to login,
                "password" to password,
                "type" to deviceType,
                "version" to deviceVersion
            )
        try {
            val response =
                RetrofitInstance.eldoApi.login(werk, dbVersion, dbVersion, values)?.execute()
            this.errorCode = response?.code()
            Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            if (response?.isSuccessful!!) {
                val responseJson = Gson().fromJson(response.body(), Auth::class.java)
                val sb = StringBuilder()
                sb.append("Bearer ")
                sb.append(responseJson.token)
                this.token = sb.toString()
                this.userInfo = responseJson.userInfo!!
                return true
            } else {
                this.errorMessage = Gson().fromJson(
                    response.errorBody()?.string(),
                    Error::class.java
                ).error.toString()
                return false
            }
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return false
        }
    }

    fun getWebOrderListSimple(selection: String): List<WebOrderSimply> {
        val str = when (selection) {
            "all" -> "WRQST_CRTD,PWRQT_DLVD,WRQST_ACPT,WRQST_RCVD,DOC_STORN"
            else -> "WRQST_CRTD"
        }
        val hashMap: HashMap<Any?, Any?> = hashMapOf(
            "orderType" to "WRQST",
            "dateFrom" to dateFrom(),
            "dateTo" to "01.01.2030",
            "docStatus" to str,
            "skip" to 0,
            "limit" to 1000
        )
        try {
            val response = RetrofitInstance.eldoApi.getWebOrderListSimple(
                shop,
                dbVersion,
                dbVersion,
                token,
                hashMap
            )?.execute()
            this.errorCode = response?.code()
            // Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")
            if (this.errorCode == 200) {
                val responseJson = Gson().fromJson(response?.body(), ListWebOrderSimply::class.java)
                return responseJson.webOrderSimply
            } else return emptyList()
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun getWebOrderList(selection: String, webNum: String?): ListWebOrder? {
        val str = when (selection) {
//            "all" -> "WRQST_CRTD,PWRQT_DLVD,WRQST_ACPT,WRQST_RCVD,DOC_STORN"
//            "all" -> "*"
            "all" -> "DOC_STORN,PWRQT_SHRT,WRQST_WAIT,PWRQT_DLVR,PWRQT_PRCH,WRQST_RCVD,DWRQT_PRCH,PWRQT_RCVD,WRQST_BNLY,WRQST_BNLN,WRQST_SHPD,PWRQT_PMNT,PWRQT_DLVD,WRQST_ACPT,PWRQT_CRTD,WRQST_CRTD,DWRQT_CRTD,ZEXPIRED"
            else -> "WRQST_CRTD"
        }
        val hashMap: HashMap<Any?, Any?> = hashMapOf(
            "orderType" to "",
//            "orderType" to "WRQST,PWRQT,DWRQT",
            "dateFrom" to dateFrom(),
            "dateTo" to "01.01.2030",
            "docStatus" to str,
            "skip" to 0,
            "phone" to "",
            "webNum" to webNum,
            "sort" to "asc",
            "limit" to 99
        )
        try {
            val response = RetrofitInstance.eldoApi.getWebOrderList(
                shop,
                dbVersion,
                dbVersion,
                gmt,
                token,
                hashMap
            )?.execute()
            this.errorCode = response?.code()
            //Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), ListWebOrder::class.java)
            return responseJson
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return ListWebOrder()
        }
    }

    fun getWebOrderDetail(orderId: String?, type: String?): WebOrderDetail? {
        try {
            Logging.d(tag, token)
            val response = RetrofitInstance.eldoApi.getWebOrderDetail(
                shop,
                dbVersion,
                dbVersion,
                token,
                orderId,
                type // WRQST ?
            )?.execute()
            this.errorCode = response?.code()
            // Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), WebOrderDetail::class.java)
            return responseJson
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return WebOrderDetail()
        }
    }

    fun localRemains(id: String?): List<RemainsLocal> {
        try {
            val response = RetrofitInstance.eldoApi.localRemains(
                shop,
                dbVersion,
                dbVersion,
                token,
                id
            )?.execute()
            this.errorCode = response?.code()
            //Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), Remains::class.java)
            return responseJson.remainsLocal
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun getWebOrderCount(str: String?): Int? {
        try {
            val response = RetrofitInstance.eldoApi.getWebOrderCount(
                shop,
                dbVersion,
                dbVersion,
                token,
                "01.01.2022",
                "01.01.2030",
                str // INWORK ASSEMBLY ISSUED
            )?.execute()
            this.errorCode = response?.code()
            // Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), OrderCount::class.java)
            return responseJson.quantity!!
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return 0
        }
    }

    fun localStorage(): List<LocalStorageDtos> {
        try {
            val response = RetrofitInstance.eldoApi.localStorage(
                shop,
                dbVersion,
                dbVersion,
                token
            )?.execute()
            this.errorCode = response?.code()
            // Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), Storage::class.java)
            return responseJson.localStorageDtos
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun mainRemains(jsonArray: JsonArray?): List<RemainsLocal> {
        val hashMap = hashMapOf("goodCodeList" to jsonArray)
        try {
            val response = RetrofitInstance.eldoApi.mainRemains(
                shop,
                dbVersion,
                dbVersion,
                token,
                hashMap
            )?.execute()
            this.errorCode = response?.code()
            // Logging.d(tag, "${this.shop} Authentication result code: ${response?.code()}")

            val responseJson = Gson().fromJson(response?.body(), MainRemains::class.java)
            return responseJson.remains
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun getReasonForIncompliteness(orderId: String?, itemId: String?): List<ShortageReasonDto> {
        try {
            val response = RetrofitInstance.eldoApi.reasonForIncompliteness(
                shop,
                dbVersion,
                dbVersion,
                token,
                orderId,
                itemId
            )?.execute()
            this.errorCode = response?.code()

            val responseJson = Gson().fromJson(response?.body(), WebOrderReasonForIncompletenessItem::class.java)
            return responseJson.shortageReasonDtos
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun getShelfsAll(): List<ShelfItem> {
        try {
            val response = RetrofitInstance.eldoApi.getShelfsAll(
                shop,
                dbVersion,
                dbVersion,
                token
            )?.execute()
            this.errorCode = response?.code()

            val responseJson = Gson().fromJson(response?.body(), Shelfs::class.java)
            return responseJson.shelfItem
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun getPrintersList(): List<PcNameList> {
        try {
            val response = RetrofitInstance.eldoApi.getPrintersList(
                shop,
                dbVersion,
                dbVersion,
                token
            )?.execute()
            this.errorCode = response?.code()

            val responseJson = Gson().fromJson(response?.body(), PrintersList::class.java)
            return responseJson.pcNameList
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return emptyList()
        }
    }

    fun saveWebOrder(orderType: String?, orderId: String?, company: String?, items: List<SaveItems>, collector: Collector, ordType: String?): SaveWebOrderRes? {
        val hashMap: HashMap<String?, Any?> = hashMapOf(
            "orderType" to orderType,
            "orderId" to orderId,
            "company" to company,
            "type" to "ASSEMBLY",
            "items" to items,
            "isChange" to "N",
            "collector" to collector,
            "ordType" to ordType
        )
        try {
            val response = RetrofitInstance.eldoApi.saveWebOrder(
                shop,
                dbVersion,
                dbVersion,
                gmt,
                token,
                hashMap
            )?.execute()
            this.errorCode = response?.code()

            val responseJson = Gson().fromJson(response?.body(), SaveWebOrderRes::class.java)
            return responseJson
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
            return SaveWebOrderRes()
        }
    }

    fun sendToPrint(pcName: String?, printOrders: String?, printType: String?) {
        val hashMap: HashMap<String?, Any?> = hashMapOf(
            "pcName" to pcName,
            "printOrders" to printOrders,
            "guid" to "",
            "printType" to printType
        )
        try {
            val response = RetrofitInstance.eldoApi.sendToPrint(
                shop,
                dbVersion,
                dbVersion,
                gmt,
                token,
                hashMap
            )?.execute()
            this.errorCode = response?.code()

            val responseJson = Gson().fromJson(response?.body(), PrintRes::class.java)
//            return responseJson
        } catch (e: Exception) {
            Logging.e(tag, "${this.shop} Exception: ${e.message}")
            this.errorMessage = e.message.toString()
            this.errorCode = null
//            return PrintRes()
        }
    }


    private fun dateFrom(): String {
        val result = LocalDate.now().minusDays(35)
        return result.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

}