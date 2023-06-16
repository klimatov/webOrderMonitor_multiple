package restTS.models

import com.google.gson.annotations.SerializedName

data class Auth(
    @SerializedName("result") var result: String? = null,
    @SerializedName("token") var token: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("userInfo") var userInfo: UserInfo? = UserInfo(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class UserInfo(
    @SerializedName("fio") var fio: String? = null,
    @SerializedName("position") var position: String? = null,
    @SerializedName("username") var username: String? = null,
    @SerializedName("hrCode") var hrCode: String? = null
)

data class Error(
    @SerializedName("timestamp") var timestamp: String? = null,
    @SerializedName("error") var error: String? = null
)

data class OrderCount(
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class DbVersion(
    @SerializedName("version") var version: Int? = null
)

data class Remains(
    @SerializedName("remainsLocal") var remainsLocal: List<RemainsLocal> = emptyList(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class RemainsLocal(
    @SerializedName("storageKind") var storageKind: String? = null,
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("goodCode") var goodCode: String? = null,
    @SerializedName("priority") var priority: Int? = null
)

data class Storage(
    @SerializedName("localStorageDtos") var localStorageDtos: List<LocalStorageDtos> = emptyList(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class LocalStorageDtos(
    @SerializedName("storageKind") var storageKind: String? = null,
    @SerializedName("shortName") var shortName: String? = null,
    @SerializedName("priority") var priority: Int? = null
)

data class MainRemains(
    @SerializedName("remains") var remains: List<RemainsLocal> = emptyList(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class ListWebOrderSimply(
    @SerializedName("webOrderSimply") var webOrderSimply: List<WebOrderSimply> = emptyList(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class WebOrderSimply(
    @SerializedName("docDate") var docDate: String? = null,
    @SerializedName("webNum") var webNum: String? = null
)

data class ListWebOrder(
    @SerializedName("webOrders") var webOrders: List<WebOrder> = emptyList(),
    @SerializedName("total") var total: Int? = null,
    @SerializedName("count") var count: Int? = null,
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class Collector(
    @SerializedName("hrCode") var hrCode: String? = null,
    @SerializedName("username") var username: String? = null
)

data class WebOrder(
    @SerializedName("orderId") var orderId: String? = null,
    @SerializedName("ordType") var ordType: String? = null,
    @SerializedName("isLegalEntity") var isLegalEntity: String? = null,
    @SerializedName("fioCustomer") var fioCustomer: String? = null,
    @SerializedName("paid") var paid: String? = null,
    @SerializedName("phone") var phone: String? = null,
    @SerializedName("isRepeated") var isRepeated: String? = null, //new 01.05.2022
    @SerializedName("docVers") var docVers: String? = null, //new 01.05.2022
    @SerializedName("isPassportNeeded") var isPassportNeeded: String? = null, //new 01.05.2022
    @SerializedName("isBonusCardUsed") var isBonusCardUsed: String? = null, //new 01.05.2022
    @SerializedName("orderType") var orderType: String? = null,
    @SerializedName("reasonCode") var reasonCode: String? = null,
    @SerializedName("creationPlace") var creationPlace: String? = null, //new 01.05.2022
    @SerializedName("collector") var collector: Collector? = Collector(),
    @SerializedName("docDate") var docDate: String? = null,
    @SerializedName("docSum") var docSum: Int? = null,
    @SerializedName("docStatus") var docStatus: String? = null,
    @SerializedName("webNum") var webNum: String? = null,
    @SerializedName("messageId") var messageId: Long? = null, // manual
    @SerializedName("activeTime") var activeTime: Long? = null, // manual
    @SerializedName("items") var items: List<Items> = emptyList(), //manual
    @SerializedName("status") var status: Status? = Status(), //new 01.05.2022
    @SerializedName("itemsUpdateStatus") var itemsUpdateStatus: Boolean = false, // manual
    @SerializedName("company") var company: String? = null, //new 20.05.2023
    @SerializedName("sapFioList") var sapFioList: MutableList<String> = mutableListOf() // manual
)

data class WebOrderDetail(
    @SerializedName("webOrder") var webOrder: WebOrder? = WebOrder(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null,
    @SerializedName("items") var items: List<Items> = emptyList(),
    @SerializedName("validate") var validate: Validate? = Validate() //new 01.05.2022
)

data class Validate( //new 01.05.2022
    @SerializedName("validate") var validate: String? = null,
    @SerializedName("message") var message: String? = null
)

data class Items(
    @SerializedName("goodCode") var goodCode: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("amount") var amount: Int? = null,
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("itemNo") var itemNo: Int? = null,
    @SerializedName("barCode") var barCode: String? = null,
    @SerializedName("kit") var kit: String? = null,
    @SerializedName("routeIsNeeded") var routeIsNeeded: String? = null,
    @SerializedName("eshopUrl") var eshopUrl: String? = null,
    @SerializedName("itemId") var itemId: Int? = null,
    @SerializedName("parentiTemNo") var parentiTemNo: String? = null,
    @SerializedName("shelf") var shelf: String? = null,
    @SerializedName("params") var params: List<Params> = emptyList(),
    @SerializedName("incomplet") var incomplet: Incomplet? = Incomplet(), //new 21.05.2022
    @SerializedName("remains") var remains: List<RemainsLocal> = emptyList()//manual
)

data class Incomplet(
    @SerializedName("ogItemId") var ogItemId: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("reasonCode") var reasonCode: String? = null,
    @SerializedName("comments") var comments: String? = null
)

data class Params(
    @SerializedName("ogItemId") var ogItemId: Int? = null,
    @SerializedName("fieldValue") var fieldValue: String? = null,
    @SerializedName("fieldDdescr") var fieldDdescr: String? = null,
    @SerializedName("fieldMask") var fieldMask: String? = null,
    @SerializedName("fieldNeeded") var fieldNeeded: String? = null,
    @SerializedName("fieldName") var fieldName: String? = null
)

data class Status(
    @SerializedName("title") var title: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("rgb") var rgb: String? = null

)

data class WebOrderReasonForIncompletenessItem(
    @SerializedName("shortageReasonDtos") var shortageReasonDtos: List<ShortageReasonDto> = listOf(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class ShortageReasonDto(
    @SerializedName("needComm") var needComm: String? = null,
    @SerializedName("shipped") var shipped: String? = null,
    @SerializedName("priority") var priority: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("reasonCode") var reasonCode: String? = null
)

data class Shelfs(
    @SerializedName("shelfInfoDtoList") var shelfItem: List<ShelfItem> = listOf()
)

data class ShelfItem(
    @SerializedName("shelfId") var shelfId: Int? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("sectionNumber") var sectionNumber: Int? = null,
    @SerializedName("rackNumber") var rackNumber: Int? = null,
    @SerializedName("shelfNumber") var shelfNumber: Int? = null
)

data class PrintersList(
    @SerializedName("pcNameList") var pcNameList: List<PcNameList> = listOf(),
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)

data class PcNameList(
    @SerializedName("pcName") var pcName: String? = null
)

data class SaveWebOrderRes(
    @SerializedName("orderId") var orderId: String? = null,
    @SerializedName("result") var result: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("printOrders") var printOrders: String? = null
)

data class SaveItems(
    @SerializedName("quantity") var quantity: String? = null,
    @SerializedName("itemNo") var itemNo: String? = null,
    @SerializedName("goodCode") var goodCode: String? = null,
    @SerializedName("incomplet") var incomplet: SaveIncomplet? = SaveIncomplet(),
    @SerializedName("shelf") var shelf: String? = null
)

data class SaveIncomplet(
    @SerializedName("comment") var comment: String? = null,
    @SerializedName("reasonCode") var reasonCode: String? = null
)
data class PrintRes(
    @SerializedName("needUpdate") var needUpdate: Boolean? = null
)