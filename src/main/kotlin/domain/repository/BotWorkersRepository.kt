package domain.repository

import dev.inmo.tgbotapi.types.Identifier
import domain.models.ShopWorkersParam

interface BotWorkersRepository {
    var changedWorkers: MutableList<ShopWorkersParam>
    val botProcessingRepositoryInstance: BotProcessingRepository

}