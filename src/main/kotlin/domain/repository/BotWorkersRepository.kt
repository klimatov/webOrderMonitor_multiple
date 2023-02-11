package domain.repository

import domain.models.ShopWorkersParam

interface BotWorkersRepository {
    var changedWorkers: MutableList<ShopWorkersParam>
    val botProcessingRepositoryInstance: BotProcessingRepository
}