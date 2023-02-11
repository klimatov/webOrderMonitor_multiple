package domain.repository

import domain.models.ShopWorkersParam
import java.util.UUID

interface WorkersDBRepository {

    suspend fun getAll(): MutableMap<UUID, ShopWorkersParam>

    suspend fun setAll(shopWorkersList: MutableMap<UUID, ShopWorkersParam>)

}