package bot

import dev.inmo.tgbotapi.types.Identifier
import domain.models.ShopWorkersParam
import domain.repository.BotProcessingRepository
import domain.repository.BotWorkersRepository

object BotWorkersRepositoryImpl : BotWorkersRepository {
    override var changedWorkers: MutableList<ShopWorkersParam> = mutableListOf()
    override val botProcessingRepositoryInstances: MutableMap<String, BotProcessingRepository> = mutableMapOf()
    override val botProcessingRepositoryInstance: BotProcessingRepository
        get() = BotProcessingRepositoryImpl()
}