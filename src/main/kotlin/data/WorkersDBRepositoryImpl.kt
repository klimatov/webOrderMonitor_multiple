package data

import data.database.shopParameters.ShopParametersDB
import data.database.shopParameters.mapToShopParameters
import data.database.shopParameters.mapToShopParametersDTO
import data.database.shopWorkers.ShopWorkersDB
import data.database.shopWorkers.mapToShopWorkersDTO
import data.database.shopWorkers.mapToShopWorkersParam
import domain.models.ShopParameters
import domain.models.ShopWorkersParam
import domain.repository.ShopParametersDBRepository
import domain.repository.WorkersDBRepository
import java.util.*


class WorkersDBRepositoryImpl() : WorkersDBRepository, ShopParametersDBRepository {
    private val tag = this::class.java.simpleName

    override suspend fun getAll(): MutableMap<UUID, ShopWorkersParam> {
        return ShopWorkersDB.getAll().associate { it.workerId to it.mapToShopWorkersParam() }.toMutableMap()
    }

    override suspend fun setAll(shopWorkersList: MutableMap<UUID, ShopWorkersParam>) {

        shopWorkersList.forEach {
            ShopWorkersDB.insert(it.value.mapToShopWorkersDTO())
        }

    }

    //ShopParametersDBRepository

    override fun updateShopParameters(shopParameters: ShopParameters) {
        ShopParametersDB.upsert(shopParameters.mapToShopParametersDTO())
    }

    override fun getShopParametersByShop(shop: String): ShopParameters? {
        return ShopParametersDB.getParametersByShop(shop)?.mapToShopParameters()
    }

    override fun updateDayConfirmedCount(shop: String, dayConfirmedCount: Int) {
        ShopParametersDB.updateDayConfirmedCount(shop, dayConfirmedCount)
    }
}

