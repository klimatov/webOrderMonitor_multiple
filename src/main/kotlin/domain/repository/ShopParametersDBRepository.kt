package domain.repository

import domain.models.ShopParameters

interface ShopParametersDBRepository {
    fun updateShopParameters(shopParameters: ShopParameters)
    fun getShopParametersByShop(shop: String): ShopParameters?
    fun updateDayRecievedCount(
        shop: String,
        dayRecievedCount: Int,
        dayConfirmedCount: Int,
        serializedDayConfirmedByEmployee: String
    )
}