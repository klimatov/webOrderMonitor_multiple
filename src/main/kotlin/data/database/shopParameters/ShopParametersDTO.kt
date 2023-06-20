package data.database.shopParameters

import domain.models.ShopParameters
import kotlinx.serialization.Serializable

@Serializable
data class ShopParametersDTO(
    val shop: String,
    val serializedActiveOrders: String,
    val currentInfoMsgId: Long,
    val dayRecievedCount: Int,
    val dayConfirmedCount: Int,
    val serializedDayConfirmedByEmployee: String,
)
fun ShopParameters.mapToShopParametersDTO(): ShopParametersDTO =
    ShopParametersDTO(
        shop = shop,
        serializedActiveOrders = serializedActiveOrders,
        currentInfoMsgId = currentInfoMsgId,
        dayRecievedCount = dayRecievedCount,
        dayConfirmedCount = dayConfirmedCount,
        serializedDayConfirmedByEmployee = serializedDayConfirmedByEmployee
    )

fun ShopParametersDTO.mapToShopParameters(): ShopParameters =
    ShopParameters(
        shop = shop,
        serializedActiveOrders = serializedActiveOrders,
        currentInfoMsgId = currentInfoMsgId,
        dayRecievedCount = dayRecievedCount,
        dayConfirmedCount = dayConfirmedCount,
        serializedDayConfirmedByEmployee = serializedDayConfirmedByEmployee
    )