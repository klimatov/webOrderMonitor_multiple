package domain.models

data class ShopParameters(
    val shop: String,
    val serializedActiveOrders: String,
    val currentInfoMsgId: Long,
    val dayRecievedCount: Int
)
