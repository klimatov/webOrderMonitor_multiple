package data.database.shopParameters

import domain.models.ShopParameters
import kotlinx.serialization.Serializable

@Serializable
data class ShopParametersDTO(
    val shop: String,
    val serializedActiveOrders: String,
    val currentInfoMsgId: Long,
    val dayConfirmedCount: Int,
)
fun ShopParameters.mapToShopParametersDTO(): ShopParametersDTO =
    ShopParametersDTO(
        shop = shop,
        serializedActiveOrders = serializedActiveOrders,
        currentInfoMsgId = currentInfoMsgId,
        dayConfirmedCount = dayConfirmedCount,
    )

fun ShopParametersDTO.mapToShopParameters(): ShopParameters =
    ShopParameters(
        shop = shop,
        serializedActiveOrders = serializedActiveOrders,
        currentInfoMsgId = currentInfoMsgId,
        dayConfirmedCount = dayConfirmedCount,
    )