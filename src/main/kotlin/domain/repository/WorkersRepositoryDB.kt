package domain.repository

import domain.models.ShopWorkersParam
import java.util.UUID

interface WorkersRepositoryDB {

    suspend fun getAll(): MutableMap<UUID, ShopWorkersParam>

    suspend fun getBy(workerId: UUID): ShopWorkersParam

    suspend fun setAll(shopWorkersList: MutableMap<UUID, ShopWorkersParam>)

    suspend fun setBy(shopWorkersParam: ShopWorkersParam)

    suspend fun deleteBy(workerId: UUID)
}