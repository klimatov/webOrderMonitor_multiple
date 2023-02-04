package data

import data.database.shopWorkers.ShopWorkersDB
import data.database.shopWorkers.mapToShopWorkersDTO
import data.database.shopWorkers.mapToShopWorkersParam
import domain.models.ShopWorkersParam
import domain.repository.WorkersRepositoryDB
import java.util.UUID


class WorkersRepositoryDBImpl() : WorkersRepositoryDB {
    private val tag = this::class.java.simpleName

    override suspend fun getAll(): MutableMap<UUID, ShopWorkersParam> {
//        val serializedData = FileOperations().read("shop_workers.cfg")
//        val type = object : TypeToken<MutableMap<UUID, ShopWorkersParam>>() {}.type
//        return Gson().fromJson<MutableMap<UUID, ShopWorkersParam>>(serializedData, type)?: mutableMapOf()
        return ShopWorkersDB.getAll().associate { it.workerId to it.mapToShopWorkersParam() }.toMutableMap()
    }

    override suspend fun setAll(shopWorkersList: MutableMap<UUID, ShopWorkersParam>) {
//        if (shopWorkersList != null) {
//            val serializedData = Gson().toJson(shopWorkersList)
//            FileOperations().write("shop_workers.cfg", serializedData)
//        }
        //val wList = shopWorkersList.values.toList()

        shopWorkersList.forEach {
            ShopWorkersDB.insert(it.value.mapToShopWorkersDTO())
        }

    }

    override suspend fun getBy(workerId: UUID): ShopWorkersParam {
        TODO("Not yet implemented")
    }

    override suspend fun setBy(shopWorkersParam: ShopWorkersParam) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteBy(workerId: UUID) {
        TODO("Not yet implemented")
    }
}


//        var shopWorkersList: MutableMap<String, ShopWorkersParam> = mutableMapOf()
//        repeat(3) {
//            val workerId = UUID.randomUUID().toString()
//            shopWorkersList[workerId] = ShopWorkersParam(
//                workerId = workerId,
//                login = SecurityData.TS_LOGIN,
//                password = SecurityData.TS_PASSWORD,
//                shop = SecurityData.TS_SHOP,
//                ownerTgId = 0,
//                isActive = true,
//                shopOpen = SecurityData.SHOP_OPENING,
//                shopClose = SecurityData.SHOP_CLOSING,
//                telegramChatId = SecurityData.TELEGRAM_CHAT_ID,
//                workerState = WorkerState.CREATE
//            )
//        }
//        return shopWorkersList