package domain.repository

import dev.inmo.tgbotapi.types.Identifier
import domain.models.ShopWorkersParam

interface BotWorkersRepository {
    var changedWorkers: MutableList<ShopWorkersParam>
    val botProcessingRepositoryInstances: MutableMap<String, BotProcessingRepository>
    val botProcessingRepositoryInstance: BotProcessingRepository

}