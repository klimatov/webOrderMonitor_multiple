package domain.repository

import domain.models.ShopParameters

interface ShopParametersDBRepository {
    fun updateShopParameters(shopParameters: ShopParameters)
    fun getShopParametersByShop(shop: String): ShopParameters?
    fun updateDayConfirmedCount(shop: String, dayConfirmedCount: Int)
}